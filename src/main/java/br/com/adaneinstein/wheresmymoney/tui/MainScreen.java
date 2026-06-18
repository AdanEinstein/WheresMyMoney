package br.com.adaneinstein.wheresmymoney.tui;

import br.com.adaneinstein.wheresmymoney.tui.screen.CategoryScreen;
import br.com.adaneinstein.wheresmymoney.tui.screen.DashboardScreen;
import br.com.adaneinstein.wheresmymoney.tui.screen.MonthlyPaymentScreen;
import br.com.adaneinstein.wheresmymoney.tui.screen.MonthlyRevenueScreen;
import br.com.adaneinstein.wheresmymoney.tui.screen.ReportScreen;
import br.com.adaneinstein.wheresmymoney.tui.screen.SearchScreen;
import br.com.adaneinstein.wheresmymoney.tui.screen.TransactionScreen;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class MainScreen {

    private final DashboardScreen dashboardScreen;
    private final TransactionScreen transactionScreen;
    private final CategoryScreen categoryScreen;
    private final MonthlyPaymentScreen monthlyPaymentScreen;
    private final MonthlyRevenueScreen monthlyRevenueScreen;
    private final ReportScreen reportScreen;
    private final SearchScreen searchScreen;

    public void start() throws Exception {
        Screen screen = new TerminalScreen(new DefaultTerminalFactory().createTerminal());
        screen.startScreen();
        try {
            WindowBasedTextGUI gui = new MultiWindowTextGUI(screen);
            gui.setTheme(AppTheme.build());
            showMenu(gui);
        } finally {
            screen.stopScreen();
        }
    }

    private void showMenu(WindowBasedTextGUI gui) {
        BasicWindow window = new BasicWindow();
        window.setHints(Set.of(Window.Hint.CENTERED));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        Label title = new Label("  💰  WheresMyMoney  ");
        title.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        root.addComponent(title);
        Label subtitle = new Label("  Controle de gastos e receitas  ");
        subtitle.setForegroundColor(TextColor.ANSI.CYAN);
        root.addComponent(subtitle);
        root.addComponent(new EmptySpace());

        Panel menu = new Panel(new GridLayout(1));
        menu.addComponent(new Button("[D] Dashboard", () -> dashboardScreen.open(gui)));
        menu.addComponent(new Button("[T] Transações", () -> transactionScreen.open(gui)));
        menu.addComponent(new Button("[C] Categorias", () -> categoryScreen.open(gui)));
        menu.addComponent(new Button("[P] Pagamentos mensais", () -> monthlyPaymentScreen.open(gui)));
        menu.addComponent(new Button("[I] Receitas mensais", () -> monthlyRevenueScreen.open(gui)));
        menu.addComponent(new Button("[R] Relatórios", () -> reportScreen.open(gui)));
        menu.addComponent(new Button("[/] Busca inteligente", () -> searchScreen.open(gui)));
        menu.addComponent(new EmptySpace());
        menu.addComponent(new Button("[Q] Sair", window::close));
        root.addComponent(menu);
        root.addComponent(new EmptySpace());
        root.addComponent(new Label("Atalhos: D T C P I R /  •  Q sai"));

        window.setComponent(root);
        window.addWindowListener(new MenuKeys(gui, window));
        gui.addWindowAndWait(window);
    }

    /** Atalhos globais do menu principal. */
    private final class MenuKeys extends WindowListenerAdapter {
        private final WindowBasedTextGUI gui;
        private final Window window;

        MenuKeys(WindowBasedTextGUI gui, Window window) {
            this.gui = gui;
            this.window = window;
        }

        @Override
        public void onUnhandledInput(Window basePane, KeyStroke key, AtomicBoolean handled) {
            if (key.getKeyType() == KeyType.EOF) {
                handled.set(true);
                window.close();
                return;
            }
            if (key.getKeyType() != KeyType.Character || key.getCharacter() == null) {
                return;
            }
            char c = Character.toLowerCase(key.getCharacter());
            handled.set(true);
            switch (c) {
                case 'd' -> dashboardScreen.open(gui);
                case 't' -> transactionScreen.open(gui);
                case 'c' -> categoryScreen.open(gui);
                case 'p' -> monthlyPaymentScreen.open(gui);
                case 'i' -> monthlyRevenueScreen.open(gui);
                case 'r' -> reportScreen.open(gui);
                case '/' -> searchScreen.open(gui);
                case 'q' -> window.close();
                default -> handled.set(false);
            }
        }
    }
}
