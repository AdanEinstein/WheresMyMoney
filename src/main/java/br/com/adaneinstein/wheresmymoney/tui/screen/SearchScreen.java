package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;
import br.com.adaneinstein.wheresmymoney.service.EmbeddingService;
import br.com.adaneinstein.wheresmymoney.service.SearchResult;
import br.com.adaneinstein.wheresmymoney.service.SemanticSearchService;
import br.com.adaneinstein.wheresmymoney.service.TransactionService;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Borders;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SearchScreen {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SemanticSearchService searchService;
    private final EmbeddingService embeddingService;
    private final TransactionService transactionService;

    public void open(WindowBasedTextGUI gui) {
        BasicWindow window = new BasicWindow("Busca inteligente");
        window.setHints(Set.of(Window.Hint.CENTERED));

        Label status = new Label("");
        updateStatus(status);

        TextBox queryBox = new TextBox(new com.googlecode.lanterna.TerminalSize(36, 1));
        Panel results = new Panel(new LinearLayout(Direction.VERTICAL));
        results.addComponent(new Label("Digite uma consulta e pressione Buscar."));

        Runnable doSearch = () -> {
            List<SearchResult> found = searchService.search(queryBox.getText(), 15);
            renderResults(results, found);
        };

        Panel bar = new Panel(new LinearLayout(Direction.HORIZONTAL));
        bar.addComponent(new Label("Buscar:"));
        bar.addComponent(queryBox);
        bar.addComponent(new Button("Buscar", doSearch));
        bar.addComponent(new Button("Reindexar", () -> {
            int n = transactionService.reindexAll(true);
            updateStatus(status);
            MessageDialog.showMessageDialog(gui, "Reindexar",
                    embeddingService.isAvailable()
                            ? n + " transações reindexadas."
                            : "Ollama indisponível — nada reindexado.",
                    MessageDialogButton.OK);
        }));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(status);
        root.addComponent(new EmptySpace());
        root.addComponent(bar);
        root.addComponent(new EmptySpace());
        root.addComponent(results.withBorder(Borders.singleLine("Resultados")));
        root.addComponent(new EmptySpace());
        root.addComponent(new Button("Fechar (Esc)", window::close));

        window.setComponent(root);
        window.addWindowListener(EscClose.of(window));
        gui.addWindowAndWait(window);
    }

    private void updateStatus(Label status) {
        boolean ok = embeddingService.refreshAvailability();
        if (ok) {
            status.setText("● Ollama online (" + embeddingService.getModel() + ") — busca semântica ativa");
            status.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        } else {
            status.setText("○ Ollama offline — usando busca textual (fallback)");
            status.setForegroundColor(TextColor.ANSI.YELLOW_BRIGHT);
        }
    }

    private void renderResults(Panel results, List<SearchResult> found) {
        results.removeAllComponents();
        if (found.isEmpty()) {
            results.addComponent(new Label("Nenhum resultado."));
            return;
        }
        for (SearchResult r : found) {
            Transaction t = r.transaction();
            String badge = r.origin() == SearchResult.Origin.SEMANTIC
                    ? String.format("[SEM %.2f]", r.score())
                    : "[TXT]";
            Label line = new Label(String.format("%-9s %s  %-28s %14s  %s",
                    badge, t.getDate().format(BR), abbreviate(t.getDescription()),
                    CurrencyUtil.format(t.getAmount()),
                    t.getCategory() != null ? t.getCategory().getName() : ""));
            line.setForegroundColor(r.origin() == SearchResult.Origin.SEMANTIC
                    ? TextColor.ANSI.CYAN_BRIGHT : TextColor.ANSI.WHITE);
            results.addComponent(line);
        }
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 28 ? s.substring(0, 27) + "…" : s;
    }
}
