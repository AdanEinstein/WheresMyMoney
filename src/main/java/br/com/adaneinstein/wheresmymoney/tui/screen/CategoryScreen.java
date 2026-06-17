package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import br.com.adaneinstein.wheresmymoney.service.CategoryService;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.tui.component.Layouts;
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
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.gui2.table.Table;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CategoryScreen {

    private final CategoryService categoryService;
    private final List<Category> rows = new ArrayList<>();

    public void open(WindowBasedTextGUI gui) {
        BasicWindow window = Layouts.fullScreen("Categorias");

        Table<String> table = new Table<>("Categoria", "Tipo", "Subcategorias");
        table.setVisibleRows(Layouts.visibleRows(gui, 5));
        reload(table);

        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(new Button("Nova categoria", () -> {
            if (newCategory(gui)) {
                reload(table);
            }
        }));
        buttons.addComponent(new Button("Add subcategoria", () -> {
            addSubcategory(gui, table);
            reload(table);
        }));
        buttons.addComponent(new Button("Excluir", () -> {
            deleteSelected(gui, table);
        }));
        buttons.addComponent(new Button("Fechar (Esc)", window::close));

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(table, Layouts.GROW);
        root.addComponent(new EmptySpace());
        root.addComponent(buttons);
        window.setComponent(root);
        window.addWindowListener(EscClose.of(window));
        gui.addWindowAndWait(window);
    }

    private void reload(Table<String> table) {
        rows.clear();
        rows.addAll(categoryService.findAll());
        table.getTableModel().clear();
        for (Category c : rows) {
            table.getTableModel().addRow(
                    c.getName(),
                    c.getType().getLabel(),
                    String.join(", ", c.getSubcategories().stream().map(Object::toString).toList()));
        }
    }

    private boolean newCategory(WindowBasedTextGUI gui) {
        BasicWindow form = new BasicWindow("Nova categoria");
        form.setHints(Set.of(Window.Hint.CENTERED));

        TextBox nameBox = new TextBox(new com.googlecode.lanterna.TerminalSize(24, 1));
        ComboBox<TransactionType> typeCombo = new ComboBox<>();
        typeCombo.addItem(TransactionType.EXPENSE);
        typeCombo.addItem(TransactionType.INCOME);

        Panel grid = new Panel(new GridLayout(2));
        grid.addComponent(new Label("Nome"));
        grid.addComponent(nameBox);
        grid.addComponent(new Label("Tipo"));
        grid.addComponent(typeCombo);

        final boolean[] saved = {false};
        Panel actions = new Panel(new LinearLayout(Direction.HORIZONTAL));
        actions.addComponent(new Button("Salvar", () -> {
            String name = nameBox.getText().trim();
            if (name.isEmpty()) {
                MessageDialog.showMessageDialog(gui, "Erro", "Nome obrigatório", MessageDialogButton.OK);
                return;
            }
            categoryService.create(name, typeCombo.getSelectedItem(), "white");
            saved[0] = true;
            form.close();
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

    private void addSubcategory(WindowBasedTextGUI gui, Table<String> table) {
        int idx = table.getSelectedRow();
        if (idx < 0 || idx >= rows.size()) {
            return;
        }
        Category category = rows.get(idx);
        String name = TextInputDialog.showDialog(gui, "Subcategoria",
                "Nome da subcategoria para \"" + category.getName() + "\":", "");
        if (name != null && !name.isBlank()) {
            categoryService.addSubcategory(category.getId(), name.trim());
        }
    }

    private void deleteSelected(WindowBasedTextGUI gui, Table<String> table) {
        int idx = table.getSelectedRow();
        if (idx < 0 || idx >= rows.size()) {
            return;
        }
        Category category = rows.get(idx);
        MessageDialogButton choice = MessageDialog.showMessageDialog(gui, "Excluir",
                "Excluir \"" + category.getName() + "\" e suas subcategorias?\n"
                        + "Transações dessa categoria impedem a exclusão.",
                MessageDialogButton.Yes, MessageDialogButton.No);
        if (choice != MessageDialogButton.Yes) {
            return;
        }
        try {
            categoryService.delete(category.getId());
            reload(table);
        } catch (RuntimeException e) {
            MessageDialog.showMessageDialog(gui, "Erro",
                    "Não foi possível excluir (há transações usando esta categoria).", MessageDialogButton.OK);
        }
    }
}
