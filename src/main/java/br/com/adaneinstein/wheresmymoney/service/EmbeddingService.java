package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.config.OllamaProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Gera embeddings via Ollama local. Tolerante a falha: se o Ollama não
 * responder, marca-se indisponível e devolve {@code null} (a busca cai para
 * o modo textual). Veja {@link SemanticSearchService}.
 */
@Service
@Slf4j
public class EmbeddingService {

    private final OllamaProperties props;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean available = new AtomicBoolean(false);

    public EmbeddingService(OllamaProperties props, HttpClient httpClient, ObjectMapper objectMapper) {
        this.props = props;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        refreshAvailability();
    }

    public boolean isAvailable() {
        return available.get();
    }

    public String getModel() {
        return props.getModel();
    }

    /** Reconsulta o Ollama (GET /api/tags) e atualiza o estado de disponibilidade. */
    public boolean refreshAvailability() {
        if (!props.isEnabled()) {
            available.set(false);
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofMillis(props.getTimeoutMs()))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            boolean ok = response.statusCode() == 200;
            available.set(ok);
            if (ok) {
                log.info("Ollama disponível em {} (modelo {})", props.getBaseUrl(), props.getModel());
            } else {
                log.warn("Ollama respondeu status {} — busca usará fallback textual", response.statusCode());
            }
            return ok;
        } catch (Exception e) {
            available.set(false);
            log.warn("Ollama indisponível ({}) — busca usará fallback textual", e.getMessage());
            return false;
        }
    }

    /**
     * Gera o embedding do texto. Retorna {@code null} (sem lançar) se o Ollama
     * estiver desligado ou falhar; o chamador decide o fallback.
     */
    public float[] embed(String text) {
        if (!props.isEnabled() || text == null || text.isBlank()) {
            return null;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", props.getModel());
            body.put("prompt", text);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/api/embeddings"))
                    .timeout(Duration.ofMillis(props.getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Ollama /api/embeddings status {} — fallback", response.statusCode());
                available.set(false);
                return null;
            }
            JsonNode node = objectMapper.readTree(response.body()).get("embedding");
            if (node == null || !node.isArray() || node.isEmpty()) {
                log.warn("Resposta do Ollama sem campo 'embedding' — fallback");
                return null;
            }
            float[] vector = new float[node.size()];
            for (int i = 0; i < node.size(); i++) {
                vector[i] = (float) node.get(i).asDouble();
            }
            available.set(true);
            return vector;
        } catch (Exception e) {
            available.set(false);
            log.warn("Falha ao gerar embedding ({}) — fallback textual", e.getMessage());
            return null;
        }
    }
}
