package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.MonthlyRevenue;
import br.com.adaneinstein.wheresmymoney.domain.model.Subcategory;
import br.com.adaneinstein.wheresmymoney.service.CategoryService;
import br.com.adaneinstein.wheresmymoney.service.MonthlyRevenueService;
import br.com.adaneinstein.wheresmymoney.service.MonthlyRevenueService.MonthlyRevenueView;
import br.com.adaneinstein.wheresmymoney.service.MonthlyRevenueService.Totals;
import br.com.adaneinstein.wheresmymoney.tui.component.CalculatorDialog;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.tui.component.Layouts;
import br.com.adaneinstein.wheresmymoney.tui.component.MoneyMask;
import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.AbstractListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Interactable;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.TextGUIGraphics;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.WindowListenerAdapter;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Tela de receitas previstas mensais com checklist do que já foi recebido. */
@Component
@RequiredArgsConstructor
public class MonthlyRevenueScreen {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final MonthlyRevenueService revenueService;
    private final CategoryService categoryService;

    private final List<MonthlyRevenueView> views = new ArrayList<>();
    private YearMonth current = YearMonth.now();

    // ── Public entry point ──────────────────────────────────────────────────────

    public void open(WindowBasedTextGUI gui) {
        current = YearMonth.now();
        BasicWindow window = Layouts.fullScreen("Receitas mensais");

        Label monthLabel = new Label("");
        Label totalsLabel = new Label("");

        RevenueListBox listBox = new RevenueListBox();
        listBox.setOnToggle(() -> toggle(listBox, gui));

        Runnable refresh = () -> reload(listBox, monthLabel, totalsLabel);
        listBox.setRefresh(refresh);

        Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
        header.addComponent(new Button("< Mês", () -> { current = current.minusMonths(1); refresh.run(); }));
        header.addComponent(new EmptySpace());
        header.addComponent(monthLabel);
        header.addComponent(new EmptySpace());
        header.addComponent(new Button("Mês >", () -> { current = current.plusMonths(1); refresh.run(); }));

        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(new Button("Novo (N)", () -> { if (showForm(gui, null)) refresh.run(); }));
        buttons.addComponent(new Button("Editar (E)", () -> { editSelected(gui, listBox); refresh.run(); }));
        buttons.addComponent(new Button("Excluir (D)", () -> { deleteSelected(gui, listBox); refresh.run(); }));
        buttons.addComponent(new Button("Fechar (Esc)", window::close));

        Label hint = new Label(
                "Space/Enter: marcar recebida  PgUp/PgDn: mês  N: novo  E: editar  D: excluir  Esc: sair");

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(hint);
        root.addComponent(new EmptySpace());
        root.addComponent(header);
        root.addComponent(new EmptySpace());
        root.addComponent(listBox, Layouts.GROW);
        root.addComponent(new EmptySpace());
        root.addComponent(totalsLabel);
        root.addComponent(new EmptySpace());
        root.addComponent(buttons);

        refresh.run();

        window.setComponent(root);
        window.addWindowListener(EscClose.of(window));
        window.addWindowListener(new MonthKeys(gui, listBox, refresh));
        gui.addWindowAndWait(window);
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private void reload(RevenueListBox listBox, Label monthLabel, Label totalsLabel) {
        int prevIdx = listBox.getSelectedIndex();

        views.clear();
        views.addAll(revenueService.view(current));

        listBox.clearItems();
        views.forEach(listBox::addItem);
        if (!views.isEmpty()) {
            int next = (prevIdx >= 0 && prevIdx < views.size()) ? prevIdx : 0;
            listBox.setSelectedIndex(next);
        }

        String monthName = current.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        monthLabel.setText(capitalize(monthName) + "/" + current.getYear());

        Totals t = revenueService.totals(current);
        totalsLabel.setText("Previsto: " + CurrencyUtil.format(t.expected())
                + "   Recebido: " + CurrencyUtil.format(t.received())
                + "   A receber: " + CurrencyUtil.format(t.toReceive()));
    }

    private void toggle(RevenueListBox listBox, WindowBasedTextGUI gui) {
        int idx = listBox.getSelectedIndex();
        if (idx < 0 || idx >= views.size()) return;
        MonthlyRevenueView v = views.get(idx);
        try {
            if (v.received()) {
                revenueService.unmarkReceived(v.revenue().getId(), current);
            } else {
                revenueService.markReceived(v.revenue().getId(), current);
            }
        } catch (RuntimeException e) {
            MessageDialog.showMessageDialog(gui, "Erro",
                    "Não foi possível atualizar a receita: " + e.getMessage(), MessageDialogButton.OK);
        }
    }

    // ── CRUD de templates ─────────────────────────────────────────────────────────

    private void editSelected(WindowBasedTextGUI gui, RevenueListBox listBox) {
        int idx = listBox.getSelectedIndex();
        if (idx >= 0 && idx < views.size()) {
            showForm(gui, views.get(idx).revenue());
        }
    }

    private void deleteSelected(WindowBasedTextGUI gui, RevenueListBox listBox) {
        int idx = listBox.getSelectedIndex();
        if (idx < 0 || idx >= views.size()) return;
        MonthlyRevenue r = views.get(idx).revenue();
        MessageDialogButton choice = MessageDialog.showMessageDialog(gui, "Excluir",
                "Excluir a receita prevista \"" + r.getDescription() + "\"?\n"
                        + "As transações já lançadas serão mantidas.",
                MessageDialogButton.Yes, MessageDialogButton.No);
        if (choice == MessageDialogButton.Yes) {
            revenueService.delete(r.getId());
        }
    }

    /** Formulário de criação/edição de template. Retorna true se salvou. */
    private boolean showForm(WindowBasedTextGUI gui, MonthlyRevenue existing) {
        List<Category> categories = categoryService.findAll();
        if (categories.isEmpty()) {
            MessageDialog.showMessageDialog(gui, "Atenção", "Cadastre uma categoria primeiro.", MessageDialogButton.OK);
            return false;
        }

        BasicWindow form = new BasicWindow(existing == null ? "Nova receita prevista" : "Editar receita prevista");
        form.setHints(Set.of(Window.Hint.CENTERED));

        TextBox descBox = new TextBox(new com.googlecode.lanterna.TerminalSize(30, 1));
        TextBox amountBox = new TextBox(new com.googlecode.lanterna.TerminalSize(30, 1));
        MoneyMask.apply(amountBox);
        TextBox dueDayBox = new TextBox(new com.googlecode.lanterna.TerminalSize(30, 1));

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categories.forEach(categoryCombo::addItem);
        ComboBox<SubOption> subCombo = new ComboBox<>();

        Runnable refreshSubs = () -> {
            subCombo.clearItems();
            subCombo.addItem(SubOption.NONE);
            Category selected = categoryCombo.getSelectedItem();
            if (selected != null) {
                List<SubOption> options = new ArrayList<>();
                flattenOptions(categoryService.subcategoryForest(selected.getId()), 0, options);
                options.forEach(subCombo::addItem);
            }
        };
        categoryCombo.addListener((sel, prev, byUser) -> refreshSubs.run());

        if (existing != null) {
            descBox.setText(existing.getDescription());
            amountBox.setText(CurrencyUtil.maskMoney(existing.getAmount()));
            dueDayBox.setText(String.valueOf(existing.getDueDay()));
            if (existing.getCategory() != null) {
                selectCategory(categoryCombo, existing.getCategory().getId());
            }
            refreshSubs.run();
            if (existing.getSubcategory() != null) {
                selectSubcategory(subCombo, existing.getSubcategory().getId());
            }
        } else {
            dueDayBox.setText("1");
            refreshSubs.run();
        }

        Panel grid = new Panel(new GridLayout(2));
        grid.addComponent(new Label("Descrição"));
        grid.addComponent(descBox);
        grid.addComponent(new Label("Valor (R$)"));
        grid.addComponent(amountBox);
        grid.addComponent(new Label("Dia receb. (1-31)"));
        grid.addComponent(dueDayBox);
        grid.addComponent(new Label("Categoria"));
        grid.addComponent(categoryCombo);
        grid.addComponent(new Label("Subcategoria"));
        grid.addComponent(subCombo);

        final boolean[] saved = {false};
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Salvar", () -> {
            if (save(gui, existing, descBox, amountBox, dueDayBox, categoryCombo, subCombo)) {
                saved[0] = true;
                form.close();
            }
        }));
        actions.addComponent(new Button("Cancelar", form::close));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(grid);
        root.addComponent(new EmptySpace());
        root.addComponent(new Label("F4 no campo Valor abre a calculadora"));
        root.addComponent(new EmptySpace());
        root.addComponent(actions);
        form.setComponent(root);
        CalculatorDialog.installShortcut(form, gui, amountBox);
        form.addWindowListener(EscClose.of(form));
        gui.addWindowAndWait(form);
        return saved[0];
    }

    private boolean save(WindowBasedTextGUI gui, MonthlyRevenue existing, TextBox descBox, TextBox amountBox,
                         TextBox dueDayBox, ComboBox<Category> categoryCombo, ComboBox<SubOption> subCombo) {
        try {
            String desc = descBox.getText().trim();
            if (desc.isEmpty()) {
                throw new IllegalArgumentException("Descrição obrigatória");
            }
            var amount = CurrencyUtil.parse(amountBox.getText());
            int dueDay = parseDay(dueDayBox.getText());
            Category category = categoryCombo.getSelectedItem();
            if (category == null) {
                throw new IllegalArgumentException("Selecione uma categoria");
            }
            SubOption opt = subCombo.getSelectedItem();
            Subcategory sub = opt != null ? opt.sub() : null;

            if (existing == null) {
                revenueService.create(desc, amount, dueDay, category, sub);
            } else {
                revenueService.update(existing.getId(), desc, amount, dueDay, category, sub);
            }
            return true;
        } catch (RuntimeException e) {
            MessageDialog.showMessageDialog(gui, "Erro", e.getMessage(), MessageDialogButton.OK);
            return false;
        }
    }

    // ── List box + renderer ─────────────────────────────────────────────────────

    static final class RevenueListBox extends AbstractListBox<MonthlyRevenueView, RevenueListBox> {

        private Runnable onToggle = () -> {};
        private Runnable refresh = () -> {};

        void setOnToggle(Runnable r) { this.onToggle = r; }
        void setRefresh(Runnable r)  { this.refresh = r; }

        @Override
        protected ListItemRenderer<MonthlyRevenueView, RevenueListBox> createDefaultListItemRenderer() {
            return new RevenueRenderer();
        }

        @Override
        public synchronized Interactable.Result handleKeyStroke(KeyStroke keyStroke) {
            boolean isActivate = keyStroke.getKeyType() == KeyType.Enter
                    || (keyStroke.getKeyType() == KeyType.Character
                        && keyStroke.getCharacter() != null
                        && keyStroke.getCharacter() == ' ');
            if (isActivate) {
                int idx = getSelectedIndex();
                if (idx >= 0 && idx < getItemCount()) {
                    onToggle.run();
                    refresh.run();
                    return Interactable.Result.HANDLED;
                }
            }
            return super.handleKeyStroke(keyStroke);
        }
    }

    private static final class RevenueRenderer
            extends AbstractListBox.ListItemRenderer<MonthlyRevenueView, RevenueListBox> {

        @Override
        public String getLabel(RevenueListBox listBox, int index, MonthlyRevenueView item) {
            MonthlyRevenue r = item.revenue();
            String check = item.received() ? "[x] " : "[ ] ";
            String desc = pad(r.getDescription(), 28);
            String amount = pad(CurrencyUtil.format(r.getAmount()), 14);
            return check + desc + amount + "  rec. dia " + r.getDueDay();
        }

        @Override
        public void drawItem(TextGUIGraphics graphics, RevenueListBox listBox,
                             int index, MonthlyRevenueView item, boolean selected, boolean focused) {
            if (selected && focused) {
                graphics.applyThemeStyle(listBox.getThemeDefinition().getSelected());
            } else if (selected) {
                graphics.applyThemeStyle(listBox.getThemeDefinition().getActive());
            } else {
                graphics.applyThemeStyle(listBox.getThemeDefinition().getNormal());
                graphics.setForegroundColor(item.received() ? TextColor.ANSI.GREEN : TextColor.ANSI.YELLOW);
            }
            graphics.fill(' ');
            graphics.putString(0, 0, getLabel(listBox, index, item));
        }

        private static String pad(String s, int width) {
            if (s == null) s = "";
            if (s.length() > width) return s.substring(0, width - 1) + "…";
            return s + " ".repeat(width - s.length());
        }
    }

    // ── Keyboard shortcuts ────────────────────────────────────────────────────────

    private final class MonthKeys extends WindowListenerAdapter {
        private final WindowBasedTextGUI gui;
        private final RevenueListBox listBox;
        private final Runnable refresh;

        MonthKeys(WindowBasedTextGUI gui, RevenueListBox listBox, Runnable refresh) {
            this.gui = gui;
            this.listBox = listBox;
            this.refresh = refresh;
        }

        @Override
        public void onUnhandledInput(Window basePane, KeyStroke key, AtomicBoolean handled) {
            if (key.getKeyType() == KeyType.PageDown) {
                handled.set(true); current = current.plusMonths(1); refresh.run(); return;
            }
            if (key.getKeyType() == KeyType.PageUp) {
                handled.set(true); current = current.minusMonths(1); refresh.run(); return;
            }
            if (key.getKeyType() != KeyType.Character || key.getCharacter() == null) return;
            char c = Character.toLowerCase(key.getCharacter());
            switch (c) {
                case 'n' -> { handled.set(true); if (showForm(gui, null)) refresh.run(); }
                case 'e' -> { handled.set(true); editSelected(gui, listBox); refresh.run(); }
                case 'd' -> { handled.set(true); deleteSelected(gui, listBox); refresh.run(); }
                case '.' -> { handled.set(true); current = current.plusMonths(1); refresh.run(); }
                case ',' -> { handled.set(true); current = current.minusMonths(1); refresh.run(); }
            }
        }
    }

    // ── Subcategory combo helpers ──────────────────────────────────────────────────

    private record SubOption(Subcategory sub, String label) {
        static final SubOption NONE = new SubOption(null, "(nenhuma)");

        @Override
        public String toString() {
            return label;
        }
    }

    private static void flattenOptions(List<CategoryService.SubTree> trees, int depth, List<SubOption> acc) {
        for (CategoryService.SubTree t : trees) {
            String prefix = depth == 0 ? "" : "  ".repeat(depth) + "↳ ";
            acc.add(new SubOption(t.sub(), prefix + t.sub().getName()));
            flattenOptions(t.children(), depth + 1, acc);
        }
    }

    private static void selectCategory(ComboBox<Category> combo, Long id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Category c = combo.getItem(i);
            if (c != null && c.getId().equals(id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static void selectSubcategory(ComboBox<SubOption> combo, Long id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            SubOption opt = combo.getItem(i);
            if (opt != null && opt.sub() != null && opt.sub().getId() != null
                    && opt.sub().getId().equals(id)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static int parseDay(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Dia de recebimento inválido");
        }
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
