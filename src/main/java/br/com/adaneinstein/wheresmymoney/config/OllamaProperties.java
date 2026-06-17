package br.com.adaneinstein.wheresmymoney.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama")
public class OllamaProperties {

    /** URL base do Ollama local. */
    private String baseUrl = "http://localhost:11434";

    /** Modelo de embedding (ex.: nomic-embed-text). */
    private String model = "nomic-embed-text";

    /** Liga/desliga a integração; se false, a busca usa só fallback textual. */
    private boolean enabled = true;

    /** Timeout das chamadas HTTP em milissegundos. */
    private int timeoutMs = 5000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
