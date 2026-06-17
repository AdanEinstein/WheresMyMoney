package br.com.adaneinstein.wheresmymoney.service;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verifica a busca semântica de ponta a ponta contra o Ollama real.
 * É PULADO automaticamente se o Ollama não estiver disponível, então não
 * quebra o build em ambientes sem o serviço.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "ollama.enabled=true",
        "ollama.timeout-ms=30000",
        "spring.datasource.url=jdbc:sqlite:./build/itest-wheresmymoney.db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SemanticSearchIntegrationTest {

    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private SemanticSearchService searchService;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private TransactionService transactionService;

    @Test
    void rankingByMeaningPutsFoodAboveTransport() {
        assumeTrue(embeddingService.refreshAvailability(), "Ollama indisponível — teste pulado");

        Category alimentacao = categoryService.create("IT_Alimentação", TransactionType.EXPENSE, "green");
        Category transporte = categoryService.create("IT_Transporte", TransactionType.EXPENSE, "yellow");
        LocalDate today = LocalDate.now();

        transactionService.create("Almoço no restaurante japonês", new BigDecimal("80.00"), today, alimentacao, null, null);
        transactionService.create("Recarga do cartão de transporte", new BigDecimal("50.00"), today, transporte, null, null);

        // "refeição" nem aparece no texto guardado — prova recuperação semântica, não textual.
        List<SearchResult> results = searchService.search("refeição em restaurante", 5);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).origin()).isEqualTo(SearchResult.Origin.SEMANTIC);
        assertThat(results.get(0).transaction().getDescription()).containsIgnoringCase("Almoço");
    }
}
