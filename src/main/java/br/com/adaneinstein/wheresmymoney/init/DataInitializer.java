package br.com.adaneinstein.wheresmymoney.init;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.Subcategory;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import br.com.adaneinstein.wheresmymoney.domain.repository.CategoryRepository;
import br.com.adaneinstein.wheresmymoney.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Semeia categorias padrão (pt-BR) na primeira execução e faz backfill de
 * embeddings das transações pendentes quando o Ollama estiver disponível.
 * Roda antes do TUI ({@code @Order(1)}).
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            seedCategories();
            log.info("Categorias padrão criadas.");
        }
        int reindexed = transactionService.reindexAll(false);
        if (reindexed > 0) {
            log.info("Backfill de embeddings: {} transações.", reindexed);
        }
    }

    private void seedCategories() {
        // Despesas
        expense("Alimentação", "green", "Mercado", "Restaurante", "Delivery");
        expense("Transporte", "yellow", "Combustível", "App/Táxi", "Transporte público");
        expense("Moradia", "blue", "Aluguel", "Condomínio", "Energia", "Água", "Internet");
        expense("Saúde", "magenta", "Farmácia", "Consultas", "Plano de saúde");
        expense("Lazer", "cyan", "Streaming", "Viagens", "Restaurantes");
        expense("Educação", "white", "Cursos", "Livros");
        expense("Assinaturas", "red", "Software", "Música", "Notícias");
        // Receitas
        income("Salário", "green", "Salário mensal", "Bônus");
        income("Freelance", "cyan", "Projetos", "Consultoria");
        income("Investimentos", "yellow", "Dividendos", "Juros");
    }

    private void expense(String name, String color, String... subs) {
        save(name, TransactionType.EXPENSE, color, subs);
    }

    private void income(String name, String color, String... subs) {
        save(name, TransactionType.INCOME, color, subs);
    }

    private void save(String name, TransactionType type, String color, String... subs) {
        Category category = new Category(name, type, color);
        for (String sub : subs) {
            category.addSubcategory(new Subcategory(sub));
        }
        categoryRepository.save(category);
    }

    /** Exposto para testes/seed manual. */
    public List<String> defaultExpenseNames() {
        return List.of("Alimentação", "Transporte", "Moradia", "Saúde", "Lazer", "Educação", "Assinaturas");
    }
}
