package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.Subcategory;
import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import br.com.adaneinstein.wheresmymoney.service.CategoryService;
import br.com.adaneinstein.wheresmymoney.service.TransactionService;
import br.com.adaneinstein.wheresmymoney.tui.component.DatePickerDialog;
import br.com.adaneinstein.wheresmymoney.tui.component.CalculatorDialog;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.tui.component.Layouts;
import br.com.adaneinstein.wheresmymoney.tui.component.MoneyMask;
import br.com.adaneinstein.wheresmymoney.tui.component.PeriodSelector;
import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.ComboBox;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.dialogs.MessageDialog;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;
import com.googlecode.lanterna.gui2.table.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TransactionScreen {

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final CategoryOption ALL_CATEGORIES = new CategoryOption(null, "(todas)");
    private static final SubOption ALL_SUBCATEGORIES = new SubOption(null, "(todas)");
    private static final SubOption NONE = new SubOption(null, "(nenhuma)");

    private static final List<String> SORT_COLUMNS =
            List.of("Data", "Tipo", "Categoria", "Subcategoria", "Descrição", "Valor");

    private final TransactionService transactionService;
    private final CategoryService categoryService;

    private final List<Transaction> allRows = new ArrayList<>();
    private final List<Transaction> rows = new ArrayList<>();

    private Table<String> table;
    private PeriodSelector period;
    private ComboBox<CategoryOption> categoryFilterCombo;
    private ComboBox<SubOption> subcategoryFilterCombo;
    private Label incomeLabel;
    private Label expenseLabel;
    private Label balanceLabel;
    private boolean refreshingSubcategoryFilter = false;
    private String sortColumn = "Data";
    private boolean asc = false; // default: Data desc

    private record CategoryOption(Category category, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** Item do ComboBox de subcategoria: envolve a entidade e exibe o caminho indentado. */
    private record SubOption(Subcategory sub, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    public void open(WindowBasedTextGUI gui) {
        BasicWindow window = Layouts.fullScreen("Transações");

        table = new Table<>("Data", "Tipo", "Categoria", "Subcategoria", "Descrição", "Valor");
        table.setVisibleRows(Layouts.visibleRows(gui, 11));

        period = new PeriodSelector(gui, this::reload);

        categoryFilterCombo = new ComboBox<>();
        subcategoryFilterCombo = new ComboBox<>();
        loadCategoryFilterOptions();
        refreshSubcategoryFilterOptions();

        categoryFilterCombo.addListener((sel, prev, byUser) -> {
            refreshSubcategoryFilterOptions();
            reload();
        });
        subcategoryFilterCombo.addListener((sel, prev, byUser) -> {
            if (!refreshingSubcategoryFilter) {
                reload();
            }
        });

        ComboBox<String> sortCombo = new ComboBox<>();
        SORT_COLUMNS.forEach(sortCombo::addItem);
        sortCombo.addListener((sel, prev, byUser) -> {
            sortColumn = sortCombo.getSelectedItem();
            reload();
        });

        table.setSelectAction(() -> {
            int idx = table.getSelectedRow();
            if (idx >= 0 && idx < rows.size()) {
                if (showForm(gui, rows.get(idx))) {
                    reload();
                }
            }
        });

        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(new Button("Adicionar (N)", () -> {
            if (showForm(gui, null)) {
                reload();
            }
        }));
        buttons.addComponent(new Button("Excluir (D)", () -> {
            deleteSelected(gui, table);
        }));
        buttons.addComponent(new Button("Fechar (Esc)", window::close));

        Panel header = new Panel(new LinearLayout(Direction.HORIZONTAL));
        header.addComponent(period);
        header.addComponent(new Label("  Categoria:"));
        header.addComponent(categoryFilterCombo);
        header.addComponent(new Label("  Subcategoria:"));
        header.addComponent(subcategoryFilterCombo);
        header.addComponent(new Label("  Ordenar:"));
        header.addComponent(sortCombo);

        incomeLabel = colored("", TextColor.ANSI.GREEN_BRIGHT);
        expenseLabel = colored("", TextColor.ANSI.RED_BRIGHT);
        balanceLabel = colored("", TextColor.ANSI.GREEN_BRIGHT);
        Panel totals = new Panel(new LinearLayout(Direction.HORIZONTAL));
        totals.addComponent(incomeLabel);
        totals.addComponent(new Label("   "));
        totals.addComponent(expenseLabel);
        totals.addComponent(new Label("   "));
        totals.addComponent(balanceLabel);

        reload();

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label("Enter edita • N adiciona • D exclui • O ordena (asc/desc) • Esc fecha"));
        root.addComponent(header);
        root.addComponent(new EmptySpace());
        root.addComponent(table, Layouts.GROW);
        root.addComponent(totals);
        root.addComponent(new EmptySpace());
        root.addComponent(buttons);

        window.setComponent(root);
        window.addWindowListener(new TransactionKeys(window, gui, table));
        window.addWindowListener(EscClose.of(window));
        gui.addWindowAndWait(window);
    }

    private void reload() {
        allRows.clear();
        allRows.addAll(transactionService.findBetween(period.start(), period.end()));

        Category selectedCategory = selectedCategoryFilter();
        Subcategory selectedSubcategory = selectedSubcategoryFilter();
        rows.clear();
        for (Transaction t : allRows) {
            if (selectedCategory != null) {
                if (t.getCategory() == null || t.getCategory().getId() == null
                        || !t.getCategory().getId().equals(selectedCategory.getId())) {
                    continue;
                }
            }
            if (selectedSubcategory != null) {
                if (t.getSubcategory() == null || t.getSubcategory().getId() == null
                        || !t.getSubcategory().getId().equals(selectedSubcategory.getId())) {
                    continue;
                }
            }
            rows.add(t);
        }

        sortRows();
        table.getTableModel().clear();
        for (Transaction t : rows) {
            table.getTableModel().addRow(
                    " " + t.getDate().format(BR) + " ",
                    " " + t.getType().getLabel() + " ",
                    " " + (t.getCategory() != null ? t.getCategory().getName() : "-") + " ",
                    " " + (t.getSubcategory() != null ? t.getSubcategory().getName() : "-") + " ",
                    " " + abbreviate(t.getDescription()) + " ",
                    " " + CurrencyUtil.format(t.getAmount()) + " ");
        }
        updateTotals();
    }

    private void loadCategoryFilterOptions() {
        categoryFilterCombo.clearItems();
        categoryFilterCombo.addItem(ALL_CATEGORIES);
        for (Category category : categoryService.findAll()) {
            categoryFilterCombo.addItem(new CategoryOption(category, category.getName()));
        }
        categoryFilterCombo.setSelectedIndex(0);
    }

    private void refreshSubcategoryFilterOptions() {
        refreshingSubcategoryFilter = true;
        try {
            subcategoryFilterCombo.clearItems();
            subcategoryFilterCombo.addItem(ALL_SUBCATEGORIES);

            Category selectedCategory = selectedCategoryFilter();
            if (selectedCategory != null && selectedCategory.getId() != null) {
                List<SubOption> options = new ArrayList<>();
                flattenOptions(categoryService.subcategoryForest(selectedCategory.getId()), 0, options);
                options.forEach(subcategoryFilterCombo::addItem);
            } else {
                for (Category category : categoryService.findAll()) {
                    List<SubOption> options = new ArrayList<>();
                    flattenOptions(categoryService.subcategoryForest(category.getId()), 0, options);
                    for (SubOption option : options) {
                        subcategoryFilterCombo.addItem(new SubOption(
                                option.sub(),
                                category.getName() + " / " + option.label()));
                    }
                }
            }
            subcategoryFilterCombo.setSelectedIndex(0);
        } finally {
            refreshingSubcategoryFilter = false;
        }
    }

    private Category selectedCategoryFilter() {
        if (categoryFilterCombo == null) {
            return null;
        }
        CategoryOption option = categoryFilterCombo.getSelectedItem();
        return option != null ? option.category() : null;
    }

    private Subcategory selectedSubcategoryFilter() {
        if (subcategoryFilterCombo == null) {
            return null;
        }
        SubOption option = subcategoryFilterCombo.getSelectedItem();
        return option != null ? option.sub() : null;
    }

    private void sortRows() {
        Comparator<Transaction> cmp = switch (sortColumn) {
            case "Tipo" -> Comparator.comparing(t -> t.getType().getLabel());
            case "Categoria" -> Comparator.comparing(
                    t -> t.getCategory() != null ? t.getCategory().getName() : "");
            case "Subcategoria" -> Comparator.comparing(
                    t -> t.getSubcategory() != null ? t.getSubcategory().getName() : "");
            case "Descrição" -> Comparator.comparing(
                    Transaction::getDescription, Comparator.nullsFirst(Comparator.naturalOrder()));
            case "Valor" -> Comparator.comparing(Transaction::getAmount);
            default -> Comparator.comparing(Transaction::getDate);
        };
        rows.sort(asc ? cmp : cmp.reversed());
    }

    private void updateTotals() {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (Transaction t : rows) {
            if (t.getType() == TransactionType.INCOME) {
                income = income.add(t.getAmount());
            } else {
                expense = expense.add(t.getAmount());
            }
        }
        BigDecimal balance = income.subtract(expense);
        incomeLabel.setText("Receitas: " + CurrencyUtil.format(income));
        expenseLabel.setText("Despesas: " + CurrencyUtil.format(expense));
        balanceLabel.setText("Saldo: " + CurrencyUtil.format(balance));
        balanceLabel.setForegroundColor(
                balance.signum() >= 0 ? TextColor.ANSI.GREEN_BRIGHT : TextColor.ANSI.RED_BRIGHT);
    }

    private static Label colored(String text, TextColor color) {
        Label label = new Label(text);
        label.setForegroundColor(color);
        return label;
    }

    private void deleteSelected(WindowBasedTextGUI gui, Table<String> table) {
        int idx = table.getSelectedRow();
        if (idx < 0 || idx >= rows.size()) {
            return;
        }
        Transaction t = rows.get(idx);
        MessageDialogButton choice = MessageDialog.showMessageDialog(
                gui, "Excluir", "Excluir \"" + t.getDescription() + "\"?",
                MessageDialogButton.Yes, MessageDialogButton.No);
        if (choice == MessageDialogButton.Yes) {
            transactionService.delete(t.getId());
            reload();
        }
    }

    /** Formulário de criação/edição. Retorna true se salvou. */
    private boolean showForm(WindowBasedTextGUI gui, Transaction existing) {
        List<Category> categories = categoryService.findAll();
        if (categories.isEmpty()) {
            MessageDialog.showMessageDialog(gui, "Atenção", "Cadastre uma categoria primeiro.", MessageDialogButton.OK);
            return false;
        }

        BasicWindow form = new BasicWindow(existing == null ? "Nova transação" : "Editar transação");
        form.setHints(Set.of(Window.Hint.CENTERED));

        Panel grid = new Panel(new GridLayout(2));

        TextBox descBox = new TextBox(new com.googlecode.lanterna.TerminalSize(30, 1));
        TextBox amountBox = new TextBox(new com.googlecode.lanterna.TerminalSize(30, 1));
        MoneyMask.apply(amountBox);
        TextBox dateBox = new TextBox(new com.googlecode.lanterna.TerminalSize(30, 1));
        dateBox.setReadOnly(true);
        TextBox notesBox = new TextBox(new com.googlecode.lanterna.TerminalSize(30, 3));

        final LocalDate[] selectedDate = {existing != null && existing.getDate() != null ? existing.getDate() : LocalDate.now()};
        Runnable refreshDateText = () -> dateBox.setText(selectedDate[0].format(BR));
        refreshDateText.run();

        Button selectDateButton = new Button("Selecionar data", () -> {
            LocalDate picked = DatePickerDialog.show(gui, "Selecionar data", selectedDate[0]);
            if (picked != null) {
                selectedDate[0] = picked;
                refreshDateText.run();
            }
        });

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categories.forEach(categoryCombo::addItem);
        ComboBox<SubOption> subCombo = new ComboBox<>();

        Runnable refreshSubs = () -> {
            subCombo.clearItems();
            subCombo.addItem(NONE); // opção "nenhuma"
            Category selected = categoryCombo.getSelectedItem();
            if (selected != null) {
                List<SubOption> options = new ArrayList<>();
                flattenOptions(categoryService.subcategoryForest(selected.getId()), 0, options);
                options.forEach(subCombo::addItem);
            }
        };
        categoryCombo.addListener((sel, prev, byUser) -> refreshSubs.run());

        // valores iniciais
        if (existing != null) {
            descBox.setText(nz(existing.getDescription()));
            amountBox.setText(CurrencyUtil.maskMoney(existing.getAmount()));
            notesBox.setText(nz(existing.getNotes()));
            if (existing.getCategory() != null) {
                selectCategory(categoryCombo, existing.getCategory().getId());
            }
            refreshSubs.run();
            if (existing.getSubcategory() != null) {
                selectSubcategory(subCombo, existing.getSubcategory().getId());
            }
        } else {
            refreshSubs.run();
        }

        grid.addComponent(new Label("Descrição"));
        grid.addComponent(descBox);
        grid.addComponent(new Label("Valor (R$)"));
        grid.addComponent(amountBox);
        grid.addComponent(new Label("Data"));
        Panel dateField = new Panel(new LinearLayout(Direction.HORIZONTAL));
        dateField.addComponent(dateBox, Layouts.GROW);
        dateField.addComponent(selectDateButton);
        grid.addComponent(dateField);
        grid.addComponent(new Label("Categoria"));
        grid.addComponent(categoryCombo);
        grid.addComponent(new Label("Subcategoria"));
        grid.addComponent(subCombo);
        grid.addComponent(new Label("Notas"));
        grid.addComponent(notesBox);

        final boolean[] saved = {false};
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Salvar", () -> {
            if (save(gui, existing, descBox, amountBox, selectedDate[0], notesBox, categoryCombo, subCombo)) {
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

    private boolean save(WindowBasedTextGUI gui, Transaction existing,
                         TextBox descBox, TextBox amountBox, LocalDate selectedDate, TextBox notesBox,
                         ComboBox<Category> categoryCombo, ComboBox<SubOption> subCombo) {
        try {
            String desc = descBox.getText().trim();
            if (desc.isEmpty()) {
                throw new IllegalArgumentException("Descrição obrigatória");
            }
            var amount = CurrencyUtil.parse(amountBox.getText());
            LocalDate date = selectedDate;
            Category category = categoryCombo.getSelectedItem();
            if (category == null) {
                throw new IllegalArgumentException("Selecione uma categoria");
            }
            SubOption opt = subCombo.getSelectedItem();
            Subcategory sub = opt != null ? opt.sub() : null; // null = "(nenhuma)"
            String notes = notesBox.getText();

            if (existing == null) {
                transactionService.create(desc, amount, date, category, sub, notes);
            } else {
                existing.setDescription(desc);
                existing.setAmount(amount);
                existing.setDate(date);
                existing.setCategory(category);
                existing.setType(category.getType());
                existing.setSubcategory(sub);
                existing.setNotes(notes);
                transactionService.update(existing);
            }
            return true;
        } catch (RuntimeException e) {
            MessageDialog.showMessageDialog(gui, "Erro", e.getMessage(), MessageDialogButton.OK);
            return false;
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

    /** Achata a floresta de subcategorias em opções com caminho indentado para o ComboBox. */
    private static void flattenOptions(List<CategoryService.SubTree> trees, int depth, List<SubOption> acc) {
        for (CategoryService.SubTree t : trees) {
            String prefix = depth == 0 ? "" : "  ".repeat(depth) + "↳ ";
            acc.add(new SubOption(t.sub(), prefix + t.sub().getName()));
            flattenOptions(t.children(), depth + 1, acc);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 28 ? s.substring(0, 27) + "…" : s;
    }

    /** Atalhos N/D na janela da lista. */
    private final class TransactionKeys extends com.googlecode.lanterna.gui2.WindowListenerAdapter {
        private final Window window;
        private final WindowBasedTextGUI gui;
        private final Table<String> table;

        TransactionKeys(Window window, WindowBasedTextGUI gui, Table<String> table) {
            this.window = window;
            this.gui = gui;
            this.table = table;
        }

        @Override
        public void onUnhandledInput(Window basePane, com.googlecode.lanterna.input.KeyStroke key,
                                     java.util.concurrent.atomic.AtomicBoolean handled) {
            if (key.getKeyType() != com.googlecode.lanterna.input.KeyType.Character || key.getCharacter() == null) {
                return;
            }
            char c = Character.toLowerCase(key.getCharacter());
            if (c == 'n') {
                handled.set(true);
                if (showForm(gui, null)) {
                    reload();
                }
            } else if (c == 'd') {
                handled.set(true);
                deleteSelected(gui, table);
            } else if (c == 'o') {
                handled.set(true);
                asc = !asc;
                reload();
            }
        }
    }
}
