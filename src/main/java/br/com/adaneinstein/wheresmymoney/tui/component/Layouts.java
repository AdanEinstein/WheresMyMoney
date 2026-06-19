package br.com.adaneinstein.wheresmymoney.tui.component;

import br.com.adaneinstein.wheresmymoney.config.AppVersion;
import br.com.adaneinstein.wheresmymoney.tui.AppTheme;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LayoutData;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;

import java.util.Set;

/** Utilitários de layout para as telas aproveitarem o espaço do terminal. */
public final class Layouts {

    /** Faz o componente esticar e preencher o espaço restante no eixo do LinearLayout. */
    public static final LayoutData GROW =
            LinearLayout.createLayoutData(LinearLayout.Alignment.Fill, LinearLayout.GrowPolicy.CanGrow);

    /** Preenche apenas o eixo principal sem forçar crescimento (útil em barras horizontais). */
    public static final LayoutData FILL =
            LinearLayout.createLayoutData(LinearLayout.Alignment.Fill);

    private Layouts() {
    }

    /** Cria uma janela em tela cheia com versão no título. */
    public static BasicWindow fullScreen(String title) {
        BasicWindow window = new BasicWindow(title + "  v" + AppVersion.get());
        window.setHints(Set.of(Window.Hint.FULL_SCREEN));
        return window;
    }

    private static TerminalSize size(WindowBasedTextGUI gui) {
        return gui.getScreen().getTerminalSize();
    }

    public static int rows(WindowBasedTextGUI gui) {
        return size(gui).getRows();
    }

    public static int cols(WindowBasedTextGUI gui) {
        return size(gui).getColumns();
    }

    /** Linhas visíveis para tabelas/painéis, descontando {@code reserved} linhas de cromo. */
    public static int visibleRows(WindowBasedTextGUI gui, int reserved) {
        return Math.max(5, rows(gui) - reserved);
    }

    /** Linha de dicas/atalhos, em cor apagada para não competir com o conteúdo. */
    public static Label hint(String text) {
        Label label = new Label(text);
        label.setForegroundColor(AppTheme.MUTED);
        return label;
    }

    /** Título de seção: cor de destaque e negrito. */
    public static Label title(String text) {
        Label label = new Label(text);
        label.setForegroundColor(AppTheme.ACCENT);
        label.addStyle(SGR.BOLD);
        return label;
    }
}
