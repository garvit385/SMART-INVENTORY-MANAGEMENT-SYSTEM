package com.smartstock.ui.panels;

import com.smartstock.model.Product;
import com.smartstock.ui.*;
import com.smartstock.util.*;
import java.awt.*;
import javax.swing.*;

public class StockPanel extends BasePanel {
  private final JComboBox<Product> product = new JComboBox<>();
  private final JComboBox<String> type = new JComboBox<>(new String[] {"Stock In", "Stock Out"});
  private final JTextField qty = UI.field("Movement Quantity", "1", 22),
      reference = UI.field("Reference / Remarks", "", 22);
  private final JLabel available = UI.label(" ", 14, Theme.BLUE, true);
  private final JTable history = UI.table();

  public StockPanel(AppContext ctx, MainDashboard host) {
    super(
        ctx,
        host,
        "Stock Management",
        "Keep track of stock coming in and going out with a simple movement history.");
    JPanel card = UI.card();
    card.add(UI.label("Record a stock movement", 18, Theme.TEXT, true), BorderLayout.NORTH);
    JPanel form =
        UI.form(
            new String[] {
              "Operation", "Product *", "Current balance", "Quantity *", "Reference / remarks"
            },
            new JComponent[] {type, product, available, qty, reference});
    card.add(form, BorderLayout.CENTER);
    card.add(
        UI.row(
            UI.button("Save Movement", true, this::save),
            UI.button("Refresh Products", this::refresh)),
        BorderLayout.SOUTH);
    card.setPreferredSize(new Dimension(0, 330));
    body.add(card, BorderLayout.NORTH);
    JPanel recent = UI.card();
    recent.add(UI.label("Recent stock movements", 17, Theme.TEXT, true), BorderLayout.NORTH);
    recent.add(UI.scroll(history), BorderLayout.CENTER);
    body.add(recent, BorderLayout.CENTER);
    product.addActionListener(e -> updateBalance());
  }

  private void updateBalance() {
    Product p = (Product) product.getSelectedItem();
    available.setText(
        p == null
            ? "No products available"
            : p.getQuantity() + " units available  •  " + p.getStatus());
  }

  private void save() {
    Product p = (Product) product.getSelectedItem();
    if (p == null) throw new AppException("Add a product before recording stock.");
    int n = ValidationUtil.integer(qty.getText(), "Quantity");
    if (type.getSelectedIndex() == 0) ctx.inventory.stockIn(p.getId(), n, reference.getText());
    else ctx.inventory.stockOut(p.getId(), n, reference.getText());
    reference.setText("");
    qty.setText("1");
    host.changed();
    UI.success(this, "Stock movement saved. Inventory and transaction history are updated.");
  }

  @Override
  public void refresh() {
    Product previous = (Product) product.getSelectedItem();
    product.removeAllItems();
    for (Product p : ctx.products.findAll()) {
      product.addItem(p);
      if (previous != null && previous.getId() == p.getId()) product.setSelectedItem(p);
    }
    updateBalance();
    java.util.List<Object[]> rows = new java.util.ArrayList<>();
    ctx.transactions.findAll().stream()
        .limit(8)
        .forEach(
            t ->
                rows.add(
                    new Object[] {
                      t.getId(),
                      t.getProductName(),
                      t.getType(),
                      t.getQuantity(),
                      t.getPreviousStock(),
                      t.getNewStock(),
                      t.getCreatedAt().toLocalDate()
                    }));
    UI.data(
        history,
        new String[] {"ID", "Product", "Type", "Qty", "Previous", "Updated", "Date"},
        rows);
  }
}
