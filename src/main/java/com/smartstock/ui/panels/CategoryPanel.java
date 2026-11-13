package com.smartstock.ui.panels;

import com.smartstock.model.Category;
import com.smartstock.ui.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

public class CategoryPanel extends BasePanel {
  private final JTable table = UI.table();
  private final JTextField search = UI.field("Search categories…", "", 25);

  public CategoryPanel(AppContext ctx, MainDashboard host) {
    super(ctx, host, "Categories", "Organize your catalog with clear, reusable categories.");
    heading.add(UI.button("Add Category", true, () -> edit(null)), BorderLayout.EAST);
    JPanel card = UI.card();
    card.add(UI.row(search, UI.button("Refresh", this::refresh)), BorderLayout.NORTH);
    card.add(UI.scroll(table), BorderLayout.CENTER);
    card.add(
        UI.row(
            UI.button("Edit Category", () -> edit(selected())),
            UI.button(
                "Delete Category",
                () -> {
                  Category c = selected();
                  if (UI.confirm(
                      this,
                      "Delete category ‘"
                          + c.getName()
                          + "’? Categories assigned to products cannot be deleted.")) {
                    ctx.categories.delete(c.getId());
                    host.changed();
                  }
                })),
        BorderLayout.SOUTH);
    body.add(card);
    UI.onChange(search, () -> UI.safe(this, this::refresh));
  }

  private Category selected() {
    int id = UI.selectedId(table);
    return ctx.categories.findAll().stream().filter(c -> c.getId() == id).findFirst().orElseThrow();
  }

  private void edit(Category c) {
    JTextField name = UI.field("Category Name", c == null ? "" : c.getName(), 25);
    UI.editor(
        this,
        c == null ? "Add Category" : "Edit Category",
        UI.form(new String[] {"Category name *"}, new JComponent[] {name}),
        () -> {
          ctx.categories.save(new Category(c == null ? 0 : c.getId(), name.getText()));
          host.changed();
        });
  }

  @Override
  public void refresh() {
    java.util.List<Object[]> rows = new ArrayList<>();
    var products = ctx.products.findAll();
    ctx.categories.findAll().stream()
        .filter(
            c ->
                c.getName()
                    .toLowerCase(Locale.ROOT)
                    .contains(search.getText().toLowerCase(Locale.ROOT)))
        .forEach(
            c ->
                rows.add(
                    new Object[] {
                      c.getId(),
                      c.getName(),
                      products.stream().filter(p -> p.getCategoryId() == c.getId()).count()
                    }));
    UI.data(table, new String[] {"Category ID", "Category Name", "Assigned Products"}, rows);
  }
}
