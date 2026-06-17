package br.com.adaneinstein.wheresmymoney.tui.screen;

import br.com.adaneinstein.wheresmymoney.domain.model.Category;
import br.com.adaneinstein.wheresmymoney.domain.model.Subcategory;
import br.com.adaneinstein.wheresmymoney.domain.model.TransactionType;
import br.com.adaneinstein.wheresmymoney.service.CategoryService;
import br.com.adaneinstein.wheresmymoney.tui.component.EscClose;
import br.com.adaneinstein.wheresmymoney.tui.component.Layouts;
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
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class CategoryScreen {

    private final CategoryService categoryService;

    // Flat ordered snapshot of what the tree is currently showing.
    private final List<TreeNode> treeNodes = new ArrayList<>();
    // IDs of categories whose children are visible.
    private final Set<Long> expandedIds = new HashSet<>();

    // ── Public entry point ──────────────────────────────────────────────────────

    public void open(WindowBasedTextGUI gui) {
        BasicWindow window = Layouts.fullScreen("Categorias");

        CategoryTreeBox treeBox = new CategoryTreeBox();
        treeBox.setOnToggle(() -> toggleExpand(treeBox));
        treeBox.setOnEnterSub(() -> { editFromTree(gui, treeBox); reload(treeBox); });
        reload(treeBox);

        Panel buttons = new Panel(new LinearLayout(Direction.HORIZONTAL));
        buttons.addComponent(new Button("Nova (N)", () -> {
            if (newCategory(gui)) reload(treeBox);
        }));
        buttons.addComponent(new Button("Add sub (S)", () -> {
            addSubcategoryFromTree(gui, treeBox);
            reload(treeBox);
        }));
        buttons.addComponent(new Button("Editar sub (E)", () -> {
            editFromTree(gui, treeBox);
            reload(treeBox);
        }));
        buttons.addComponent(new Button("Excluir (D)", () -> deleteFromTree(gui, treeBox)));
        buttons.addComponent(new Button("Fechar (Esc)", window::close));

        Label hint = new Label(
                "Space/Enter: expandir  N: nova cat  S: add sub  E: editar sub  D: excluir  Esc: sair");

        Panel root = new Panel(new LinearLayout(Direction.VERTICAL));
        root.addComponent(hint);
        root.addComponent(new EmptySpace());
        root.addComponent(treeBox, Layouts.GROW);
        root.addComponent(new EmptySpace());
        root.addComponent(buttons);

        window.setComponent(root);
        window.addWindowListener(EscClose.of(window));
        window.addWindowListener(new CategoryKeys(window, gui, treeBox));
        gui.addWindowAndWait(window);
    }

    // ── Tree state management ───────────────────────────────────────────────────

    void reload(CategoryTreeBox treeBox) {
        int prevIdx = treeBox.getSelectedIndex();

        treeNodes.clear();
        for (Category c : categoryService.findAll()) {
            boolean expanded = expandedIds.contains(c.getId());
            treeNodes.add(new CategoryNode(c, expanded));
            if (expanded) {
                List<Subcategory> subs = c.getSubcategories();
                for (int i = 0; i < subs.size(); i++) {
                    treeNodes.add(new SubcategoryNode(subs.get(i), i == subs.size() - 1));
                }
            }
        }

        treeBox.clearItems();
        treeNodes.forEach(treeBox::addItem);

        if (!treeNodes.isEmpty()) {
            int next = (prevIdx >= 0 && prevIdx < treeNodes.size()) ? prevIdx : 0;
            treeBox.setSelectedIndex(next);
        }
    }

    void toggleExpand(CategoryTreeBox treeBox) {
        int idx = treeBox.getSelectedIndex();
        if (idx < 0 || idx >= treeNodes.size()) return;
        if (!(treeNodes.get(idx) instanceof CategoryNode cn)) return;

        Long id = cn.category().getId();
        if (expandedIds.contains(id)) expandedIds.remove(id);
        else expandedIds.add(id);

        reload(treeBox);
    }

    // ── CRUD actions ────────────────────────────────────────────────────────────

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

    private void addSubcategoryFromTree(WindowBasedTextGUI gui, CategoryTreeBox treeBox) {
        int idx = treeBox.getSelectedIndex();
        if (idx < 0 || idx >= treeNodes.size()) return;

        TreeNode node = treeNodes.get(idx);
        Category category;
        if (node instanceof CategoryNode cn) {
            category = cn.category();
        } else if (node instanceof SubcategoryNode sn) {
            // If a subcategory is selected, add sibling to the same parent
            category = sn.sub().getCategory();
        } else {
            return;
        }

        String name = TextInputDialog.showDialog(gui, "Nova subcategoria",
                "Nome para subcategoria de \"" + category.getName() + "\":", "");
        if (name != null && !name.isBlank()) {
            categoryService.addSubcategory(category.getId(), name.trim());
            expandedIds.add(category.getId()); // auto-expand to reveal new child
        }
    }

    private void editFromTree(WindowBasedTextGUI gui, CategoryTreeBox treeBox) {
        int idx = treeBox.getSelectedIndex();
        if (idx < 0 || idx >= treeNodes.size()) return;
        if (!(treeNodes.get(idx) instanceof SubcategoryNode sn)) {
            MessageDialog.showMessageDialog(gui, "Editar",
                    "Selecione uma subcategoria para editar.", MessageDialogButton.OK);
            return;
        }

        String newName = TextInputDialog.showDialog(gui, "Editar subcategoria",
                "Novo nome para \"" + sn.sub().getName() + "\":", sn.sub().getName());
        if (newName != null && !newName.isBlank() && !newName.equals(sn.sub().getName())) {
            categoryService.renameSubcategory(sn.sub().getId(), newName.trim());
        }
    }

    private void deleteFromTree(WindowBasedTextGUI gui, CategoryTreeBox treeBox) {
        int idx = treeBox.getSelectedIndex();
        if (idx < 0 || idx >= treeNodes.size()) return;

        TreeNode node = treeNodes.get(idx);

        if (node instanceof CategoryNode cn) {
            MessageDialogButton choice = MessageDialog.showMessageDialog(gui, "Excluir categoria",
                    "Excluir \"" + cn.category().getName() + "\" e todas as suas subcategorias?\n"
                            + "Transações desta categoria impedem a exclusão.",
                    MessageDialogButton.Yes, MessageDialogButton.No);
            if (choice != MessageDialogButton.Yes) return;
            try {
                expandedIds.remove(cn.category().getId());
                categoryService.delete(cn.category().getId());
                reload(treeBox);
            } catch (RuntimeException e) {
                MessageDialog.showMessageDialog(gui, "Erro",
                        "Não foi possível excluir (há transações vinculadas a esta categoria).",
                        MessageDialogButton.OK);
            }

        } else if (node instanceof SubcategoryNode sn) {
            MessageDialogButton choice = MessageDialog.showMessageDialog(gui, "Excluir subcategoria",
                    "Excluir subcategoria \"" + sn.sub().getName() + "\"?\n"
                            + "Transações desta subcategoria impedem a exclusão.",
                    MessageDialogButton.Yes, MessageDialogButton.No);
            if (choice != MessageDialogButton.Yes) return;
            try {
                categoryService.deleteSubcategory(sn.sub().getId());
                reload(treeBox);
            } catch (RuntimeException e) {
                MessageDialog.showMessageDialog(gui, "Erro",
                        "Não foi possível excluir (há transações vinculadas a esta subcategoria).",
                        MessageDialogButton.OK);
            }
        }
    }

    // ── Tree node model ─────────────────────────────────────────────────────────

    sealed interface TreeNode permits CategoryNode, SubcategoryNode {}

    record CategoryNode(Category category, boolean expanded) implements TreeNode {}

    record SubcategoryNode(Subcategory sub, boolean lastChild) implements TreeNode {}

    // ── Custom list box ─────────────────────────────────────────────────────────

    static final class CategoryTreeBox extends AbstractListBox<TreeNode, CategoryTreeBox> {

        private Runnable onToggle      = () -> {};
        private Runnable onEnterSub    = () -> {};

        void setOnToggle(Runnable r)   { this.onToggle   = r; }
        void setOnEnterSub(Runnable r) { this.onEnterSub = r; }

        @Override
        protected AbstractListBox.ListItemRenderer<TreeNode, CategoryTreeBox> createDefaultListItemRenderer() {
            return new TreeNodeRenderer();
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
                    TreeNode node = getItemAt(idx);
                    if (node instanceof CategoryNode) {
                        onToggle.run();
                        return Interactable.Result.HANDLED;
                    } else if (node instanceof SubcategoryNode) {
                        onEnterSub.run();
                        return Interactable.Result.HANDLED;
                    }
                }
            }
            return super.handleKeyStroke(keyStroke);
        }
    }

    // ── Tree node renderer ──────────────────────────────────────────────────────

    private static final class TreeNodeRenderer
            extends AbstractListBox.ListItemRenderer<TreeNode, CategoryTreeBox> {

        // EXPENSE → red, INCOME → green  (bright variants for better visibility)
        private static final TextColor EXPENSE_COLOR = TextColor.ANSI.RED;
        private static final TextColor INCOME_COLOR  = TextColor.ANSI.GREEN;

        @Override
        public String getLabel(CategoryTreeBox listBox, int index, TreeNode item) {
            if (item instanceof CategoryNode cn) {
                String arrow = cn.expanded() ? "▼ " : "▶ ";
                String type  = " [" + cn.category().getType().getLabel() + "]";
                return arrow + cn.category().getName() + type;
            } else if (item instanceof SubcategoryNode sn) {
                String branch = sn.lastChild() ? "  └── " : "  ├── ";
                return branch + sn.sub().getName();
            }
            return item.toString();
        }

        @Override
        public void drawItem(TextGUIGraphics graphics, CategoryTreeBox listBox,
                             int index, TreeNode item, boolean selected, boolean focused) {
            // 1 – Apply theme style (selection highlight, normal background, etc.)
            if (selected && focused) {
                graphics.applyThemeStyle(listBox.getThemeDefinition().getSelected());
            } else if (selected) {
                graphics.applyThemeStyle(listBox.getThemeDefinition().getActive());
            } else {
                graphics.applyThemeStyle(listBox.getThemeDefinition().getNormal());
                // Override foreground for category type colour only when not selected
                // (keeps contrast on the selection background)
                if (item instanceof CategoryNode cn) {
                    graphics.setForegroundColor(
                            cn.category().getType() == TransactionType.EXPENSE
                                    ? EXPENSE_COLOR : INCOME_COLOR);
                }
            }

            // 2 – Clear the row, then draw the label
            graphics.fill(' ');
            graphics.putString(0, 0, getLabel(listBox, index, item));
        }
    }

    // ── Keyboard shortcuts (N / S / E / D) ─────────────────────────────────────

    private final class CategoryKeys extends WindowListenerAdapter {

        private final Window window;
        private final WindowBasedTextGUI gui;
        private final CategoryTreeBox treeBox;

        CategoryKeys(Window window, WindowBasedTextGUI gui, CategoryTreeBox treeBox) {
            this.window  = window;
            this.gui     = gui;
            this.treeBox = treeBox;
        }

        @Override
        public void onUnhandledInput(Window basePane, KeyStroke key, AtomicBoolean handled) {
            if (key.getKeyType() != KeyType.Character || key.getCharacter() == null) return;
            char c = Character.toLowerCase(key.getCharacter());
            switch (c) {
                case 'n' -> { handled.set(true); if (newCategory(gui)) reload(treeBox); }
                case 's' -> { handled.set(true); addSubcategoryFromTree(gui, treeBox); reload(treeBox); }
                case 'e' -> { handled.set(true); editFromTree(gui, treeBox); reload(treeBox); }
                case 'd' -> { handled.set(true); deleteFromTree(gui, treeBox); }
            }
        }
    }
}
