package com.smartstock.ui.panels;

import com.smartstock.model.*;
import com.smartstock.ui.*;
import com.smartstock.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class ProductPanel extends BasePanel {
  private final JTable table = UI.table();
  private final JTextField search = UI.field("Search products…", "", 20);
  private final JComboBox<String> category = new JComboBox<>(),
      status =
          new JComboBox<>(
              new String[] {
                "All stock statuses", "In stock", "Low stock", "Out of stock", "Needs attention"
              });
  private final JLabel count = UI.label(" ", 12, Theme.MUTED, false);
  private boolean loading;

  public ProductPanel(AppContext ctx, MainDashboard host) {
    super(ctx, host, "Products", "Manage your catalog, pricing and inventory in one place.");
    heading.add(UI.button("Add Product", true, () -> edit(null)), BorderLayout.EAST);
    JPanel card = UI.card();
    JPanel toolbar = new JPanel(new BorderLayout());
    toolbar.setOpaque(false);
    toolbar.add(UI.row(search, category, status), BorderLayout.WEST);
    toolbar.add(UI.button("Refresh", this::refresh), BorderLayout.EAST);
    card.add(toolbar, BorderLayout.NORTH);
    card.add(UI.scroll(table), BorderLayout.CENTER);
    JPanel footer = new JPanel(new BorderLayout());
    footer.setOpaque(false);
    footer.add(count, BorderLayout.WEST);
    footer.add(
        UI.row(
            UI.button("Edit Selected", () -> edit(selected())),
            UI.button("Delete Selected", this::delete)),
        BorderLayout.EAST);
    card.add(footer, BorderLayout.SOUTH);
    body.add(card);
    UI.onChange(search, () -> UI.safe(this, this::loadRows));
    category.addActionListener(
        e -> {
          if (!loading) UI.safe(this, this::loadRows);
        });
    status.addActionListener(e -> UI.safe(this, this::loadRows));
    table.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            int row = table.rowAtPoint(e.getPoint()), col = table.columnAtPoint(e.getPoint());
            if (row < 0) return;
            if (col == 9) {
              table.setRowSelectionInterval(row, row);
              Rectangle r = table.getCellRect(row, col, true);
              UI.safe(
                  table,
                  () -> {
                    if (e.getX() - r.x < r.width / 2) edit(selected());
                    else delete();
                  });
            } else if (e.getClickCount() == 2) UI.safe(table, () -> edit(selected()));
          }
        });
  }

  @Override
  public void refresh() {
    loading = true;
    Object prev = category.getSelectedItem();
    category.removeAllItems();
    category.addItem("All categories");
    ctx.categories.findAll().forEach(c -> category.addItem(c.getName()));
    if (prev != null) category.setSelectedItem(prev);
    if (category.getSelectedIndex() < 0) category.setSelectedIndex(0);
    loading = false;
    loadRows();
  }

  private void loadRows() {
    List<Object[]> rows = new ArrayList<>();
    String chosen = String.valueOf(category.getSelectedItem()),
        stock = String.valueOf(status.getSelectedItem());
    for (Product p : ctx.products.findAll(search.getText().trim())) {
      if (category.getSelectedIndex() > 0 && !p.getCategoryName().equals(chosen)) continue;
      if (stock.equals("Needs attention")) {
        if (p.getQuantity() > 0 && p.getQuantity() >= p.getMinStock()) continue;
      } else if (status.getSelectedIndex() > 0 && !p.getStatus().equals(stock)) continue;
      rows.add(
          new Object[] {
            p.getId(),
            p.getName(),
            p.getCategoryName(),
            p.getSupplierName(),
            p.getQuantity(),
            ValidationUtil.money(p.getPurchaseCents()),
            ValidationUtil.money(p.getSellingCents()),
            p.getMinStock(),
            p.getStatus(),
            "Edit  /  Delete"
          });
    }
    UI.data(
        table,
        new String[] {
          "ID",
          "Product Name",
          "Category",
          "Supplier",
          "Qty",
          "Purchase (INR)",
          "Selling (INR)",
          "Min",
          "Status",
          "Actions"
        },
        rows);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    int[] widths = {55, 190, 120, 165, 65, 115, 115, 60, 115, 125};
    for (int i = 0; i < widths.length; i++)
      table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    count.setText(rows.size() + " products  •  Double-click a row to edit");
  }

  public void showLowStock() {
    search.setText("");
    category.setSelectedIndex(0);
    status.setSelectedItem("Needs attention");
  }

  private Product selected() {
    return ctx.products.findById(UI.selectedId(table));
  }

  private void delete() {
    Product p = selected();
    if (UI.confirm(
        this,
        "Delete ‘"
            + p.getName()
            + "’?\nSales and transaction history will be retained. This cannot be undone.")) {
      ctx.inventory.deleteProduct(p.getId());
      host.changed();
    }
  }

  public void edit(Product old) {
    List<Category> cats = ctx.categories.findAll();
    List<Supplier> suppliers = ctx.suppliers.findAll();
    if (cats.isEmpty() || suppliers.isEmpty())
      throw new AppException("Create at least one category and supplier first.");
    JTextField name = UI.field("Product Name", old == null ? "" : old.getName(), 26),
        buy =
            UI.field(
                "Purchase Price",
                old == null ? "0.00" : ValidationUtil.money(old.getPurchaseCents()),
                16),
        sell =
            UI.field(
                "Selling Price",
                old == null ? "0.00" : ValidationUtil.money(old.getSellingCents()),
                16),
        qty = UI.field("Quantity", old == null ? "0" : String.valueOf(old.getQuantity()), 16),
        min = UI.field("Minimum Stock", old == null ? "5" : String.valueOf(old.getMinStock()), 16);
    JComboBox<Category> cat = new JComboBox<>(cats.toArray(Category[]::new));
    JComboBox<Supplier> supp = new JComboBox<>(suppliers.toArray(Supplier[]::new));
    if (old != null) {
      cats.stream()
          .filter(c -> c.getId() == old.getCategoryId())
          .findFirst()
          .ifPresent(cat::setSelectedItem);
      suppliers.stream()
          .filter(s -> s.getId() == old.getSupplierId())
          .findFirst()
          .ifPresent(supp::setSelectedItem);
    }
    JTextArea description = new JTextArea(old == null ? "" : old.getDescription(), 3, 25);
    description.setLineWrap(true);
    description.setWrapStyleWord(true);
    description.setName("Description");
    JPanel form =
        UI.form(
            new String[] {
              "Product name *",
              "Category *",
              "Supplier *",
              "Purchase price (INR) *",
              "Selling price (INR) *",
              "Quantity *",
              "Minimum stock *",
              "Description"
            },
            new JComponent[] {name, cat, supp, buy, sell, qty, min, UI.scroll(description)});
    UI.editor(
        this,
        old == null ? "Add Product" : "Edit Product",
        form,
        () -> {
          long purchase = ValidationUtil.cents(buy.getText()),
              selling = ValidationUtil.cents(sell.getText());
          if (selling < purchase
              && !UI.confirm(
                  this, "Selling price is below purchase price. Save this product anyway?"))
            return false;
          Product p =
              new Product(
                  old == null ? 0 : old.getId(),
                  name.getText(),
                  ((Category) cat.getSelectedItem()).getId(),
                  ((Supplier) supp.getSelectedItem()).getId(),
                  ValidationUtil.integer(qty.getText(), "Quantity"),
                  purchase,
                  selling,
                  ValidationUtil.integer(min.getText(), "Minimum stock"),
                  description.getText());
          ctx.inventory.saveProduct(p);
          host.changed();
          return true;
        });
  }
}
