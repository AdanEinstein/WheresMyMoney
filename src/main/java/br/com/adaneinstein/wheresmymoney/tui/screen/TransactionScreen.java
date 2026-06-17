package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.Subcategory;
import br.com.adaneinstein.wheresmymoney.domain.model.Transaction;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import br.com.adaneinstein.wheresmymoney.service.CategoryService;
import br.com.adaneinstein.wheresmymoney.service.TransactionService;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.util.CurrencyUtil;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TransactionScreen {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Subcategory NONE = noneSentinel();

    private final TransactionService transactionService;
    private final CategoryService categoryService;

    private final List<Transaction> rows = new ArrayList<>();

    private static Subcategory noneSentinel() {
        Subcategory s = new Subcategory("(nenhuma)");
        s.setId(null);
        return s;
    }

    public void open(WindowBasedTextGUI gui) {
        BasicWindow window = new BasicWindow("Transações");
        window.setHints(Set.of(Window.Hint.CENTERED));

        Table<String> table = new Table<>("Data", "Tipo", "Categoria", "Descrição", "Valor");
        table.setVisibleRows(12);
        reload(table);

        table.setSelectAction(() -> {
            int idx = table.getSelectedRow();
            if (idx >= 0 && idx < rows.size()) {
                if (showForm(gui, rows.get(idx))) {
                    reload(table);
                }
            }
        });

        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(new Button("Adicionar (N)", () -> {
            if (showForm(gui, null)) {
                reload(table);
            }
        }));
        buttons.addComponent(new Button("Excluir (D)", () -> {
            deleteSelected(gui, table);
        }));
        buttons.addComponent(new Button("Fechar (Esc)", window::close));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(new Label("Enter edita • N adiciona • D exclui • Esc fecha"));
        root.addComponent(new EmptySpace());
        root.addComponent(table);
        root.addComponent(new EmptySpace());
        root.addComponent(buttons);

        window.setComponent(root);
        window.addWindowListener(new TransactionKeys(window, gui, table));
        window.addWindowListener(EscClose.of(window));
        gui.addWindowAndWait(window);
    }

    private void reload(Table<String> table) {
        rows.clear();
        rows.addAll(transactionService.findAll());
        table.getTableModel().clear();
        for (Transaction t : rows) {
            table.getTableModel().addRow(
                    t.getDate().format(BR),
                    t.getType().getLabel(),
                    t.getCategory() != null ? t.getCategory().getName() : "-",
                    abbreviate(t.getDescription()),
                    CurrencyUtil.format(t.getAmount()));
        }
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
            reload(table);
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
        TextBox dateBox = new TextBox(new com.googlecode.lanterna.TerminalSize(30, 1));
        TextBox notesBox = new TextBox(new com.googlecode.lanterna.TerminalSize(30, 3));

        ComboBox<Category> categoryCombo = new ComboBox<>();
        categories.forEach(categoryCombo::addItem);
        ComboBox<Subcategory> subCombo = new ComboBox<>();

        Runnable refreshSubs = () -> {
            subCombo.clearItems();
            subCombo.addItem(NONE); // opção "nenhuma"
            Category selected = categoryCombo.getSelectedItem();
            if (selected != null) {
                categoryService.subcategoriesOf(selected.getId()).forEach(subCombo::addItem);
            }
        };
        categoryCombo.addListener((sel, prev, byUser) -> refreshSubs.run());

        // valores iniciais
        if (existing != null) {
            descBox.setText(nz(existing.getDescription()));
            amountBox.setText(existing.getAmount() != null ? existing.getAmount().toPlainString() : "");
            dateBox.setText(existing.getDate() != null ? existing.getDate().format(ISO) : LocalDate.now().format(ISO));
            notesBox.setText(nz(existing.getNotes()));
            if (existing.getCategory() != null) {
                selectCategory(categoryCombo, existing.getCategory().getId());
            }
            refreshSubs.run();
            if (existing.getSubcategory() != null) {
                selectSubcategory(subCombo, existing.getSubcategory().getId());
            }
        } else {
            dateBox.setText(LocalDate.now().format(ISO));
            refreshSubs.run();
        }

        grid.addComponent(new Label("Descrição"));
        grid.addComponent(descBox);
        grid.addComponent(new Label("Valor (R$)"));
        grid.addComponent(amountBox);
        grid.addComponent(new Label("Data (aaaa-mm-dd)"));
        grid.addComponent(dateBox);
        grid.addComponent(new Label("Categoria"));
        grid.addComponent(categoryCombo);
        grid.addComponent(new Label("Subcategoria"));
        grid.addComponent(subCombo);
        grid.addComponent(new Label("Notas"));
        grid.addComponent(notesBox);

        final boolean[] saved = {false};
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Salvar", () -> {
            if (save(gui, existing, descBox, amountBox, dateBox, notesBox, categoryCombo, subCombo)) {
                saved[0] = true;
                form.close();
            }
        }));
        actions.addComponent(new Button("Cancelar", form::close));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(grid);
        root.addComponent(new EmptySpace());
        root.addComponent(actions);
        form.setComponent(root);
        form.addWindowListener(EscClose.of(form));
        gui.addWindowAndWait(form);
        return saved[0];
    }

    private boolean save(WindowBasedTextGUI gui, Transaction existing,
                         TextBox descBox, TextBox amountBox, TextBox dateBox, TextBox notesBox,
                         ComboBox<Category> categoryCombo, ComboBox<Subcategory> subCombo) {
        try {
            String desc = descBox.getText().trim();
            if (desc.isEmpty()) {
                throw new IllegalArgumentException("Descrição obrigatória");
            }
            var amount = CurrencyUtil.parse(amountBox.getText());
            LocalDate date = parseDate(dateBox.getText());
            Category category = categoryCombo.getSelectedItem();
            if (category == null) {
                throw new IllegalArgumentException("Selecione uma categoria");
            }
            Subcategory sub = subCombo.getSelectedItem();
            if (sub != null && sub.getId() == null) {
                sub = null; // sentinela "(nenhuma)"
            }
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

    private static LocalDate parseDate(String raw) {
        String s = raw == null ? "" : raw.trim();
        try {
            return LocalDate.parse(s, ISO);
        } catch (RuntimeException ignored) {
            try {
                return LocalDate.parse(s, BR);
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Data inválida: " + raw);
            }
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

    private static void selectSubcategory(ComboBox<Subcategory> combo, Long id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Subcategory s = combo.getItem(i);
            if (s != null && s.getId() != null && s.getId().equals(id)) {
                combo.setSelectedIndex(i);
                return;
            }
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
                    reload(table);
                }
            } else if (c == 'd') {
                handled.set(true);
                deleteSelected(gui, table);
            }
        }
    }
}
