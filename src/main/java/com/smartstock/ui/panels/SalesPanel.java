package com.smartstock.ui.panels;

import com.smartstock.model.*;
import com.smartstock.ui.*;
import com.smartstock.util.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class SalesPanel extends BasePanel {
  private final JComboBox<Product> product = new JComboBox<>();
  private final JTextField qty = UI.field("Sale Quantity", "1", 6),
      discount = UI.field("Discount Amount", "0.00", 10);
  private final JTable cartTable = UI.table();
  private final List<SaleItem> cart = new ArrayList<>();
  private final JLabel subtotal = UI.label("INR 0.00", 16, Theme.TEXT, true),
      total = UI.label("INR 0.00", 24, Theme.BLUE, true),
      price = UI.label(" ", 12, Theme.MUTED, false);

  public SalesPanel(AppContext ctx, MainDashboard host) {
    super(
        ctx,
        host,
        "Sales & Billing",
        "Build a sale, apply a discount and generate a printable invoice.");
    heading.add(UI.button("Invoice History", this::history), BorderLayout.EAST);
    JPanel card = UI.card();
    JPanel top = UI.stack();
    product.setPreferredSize(new Dimension(330, 38));
    top.add(
        UI.row(
            new JLabel("Product"),
            product,
            new JLabel("Quantity"),
            qty,
            UI.button("Add to Cart", true, this::add)));
    top.add(Box.createVerticalStrut(12));
    top.add(price);
    card.add(top, BorderLayout.NORTH);
    card.add(UI.scroll(cartTable), BorderLayout.CENTER);
    card.add(
        UI.row(
            UI.button("Remove Selected Item", this::remove),
            UI.button(
                "Clear Cart",
                () -> {
                  if (!cart.isEmpty() && UI.confirm(this, "Clear all cart items?")) {
                    cart.clear();
                    updateCart();
                  }
                })),
        BorderLayout.SOUTH);
    body.add(card, BorderLayout.CENTER);
    JPanel summary = UI.card();
    summary.add(
        UI.row(
            new JLabel("Subtotal"),
            subtotal,
            new JLabel("Discount (INR)"),
            discount,
            new JLabel("Grand total"),
            total),
        BorderLayout.CENTER);
    summary.add(UI.button("Complete Sale", true, this::checkout), BorderLayout.EAST);
    body.add(summary, BorderLayout.SOUTH);
    product.addActionListener(
        e -> {
          Product p = (Product) product.getSelectedItem();
          price.setText(
              p == null
                  ? "No products available"
                  : p.getQuantity()
                      + " units available  |  Unit price INR "
                      + ValidationUtil.money(p.getSellingCents()));
        });
    UI.onChange(discount, this::updateTotals);
  }

  private void add() {
    Product p = (Product) product.getSelectedItem();
    if (p == null) throw new AppException("Select a product.");
    int n = ValidationUtil.integer(qty.getText(), "Quantity");
    if (n == 0) throw new AppException("Quantity must be greater than zero.");
    long existing =
        cart.stream()
            .filter(i -> i.getProductId() == p.getId())
            .mapToLong(SaleItem::getQuantity)
            .sum();
    Product live = ctx.products.findById(p.getId());
    if (existing + n > live.getQuantity())
      throw new AppException(
          "Not enough stock. Available: "
              + live.getQuantity()
              + "; already in cart: "
              + existing
              + ".");
    cart.removeIf(i -> i.getProductId() == p.getId());
    cart.add(new SaleItem(p.getId(), live.getName(), (int) (existing + n), live.getSellingCents()));
    qty.setText("1");
    updateCart();
  }

  private void remove() {
    int row = cartTable.getSelectedRow();
    if (row < 0) throw new AppException("Select a cart item first.");
    int id = ((Number) cartTable.getValueAt(row, 0)).intValue();
    cart.removeIf(i -> i.getProductId() == id);
    updateCart();
  }

  private void updateCart() {
    List<Object[]> rows = new ArrayList<>();
    for (SaleItem i : cart)
      rows.add(
          new Object[] {
            i.getProductId(),
            i.getProductName(),
            i.getQuantity(),
            ValidationUtil.money(i.getUnitCents()),
            ValidationUtil.money(i.getTotalCents())
          });
    UI.data(
        cartTable,
        new String[] {"Product ID", "Product", "Quantity", "Unit Price (INR)", "Line Total (INR)"},
        rows);
    updateTotals();
  }

  private void updateTotals() {
    try {
      long sum = 0;
      for (SaleItem i : cart) sum = Math.addExact(sum, i.getTotalCents());
      subtotal.setText("INR " + ValidationUtil.money(sum));
      long d = ValidationUtil.cents(discount.getText());
      if (d > sum) {
        total.setText("Invalid discount");
        total.setForeground(Theme.RED);
      } else {
        total.setText("INR " + ValidationUtil.money(sum - d));
        total.setForeground(Theme.BLUE);
      }
    } catch (RuntimeException e) {
      total.setText("Check amounts");
      total.setForeground(Theme.RED);
    }
  }

  private void checkout() {
    if (cart.isEmpty()) throw new AppException("Add items to the cart first.");
    long d = ValidationUtil.cents(discount.getText());
    if (!UI.confirm(
        this,
        "Complete this sale and reduce inventory? Prices are revalidated from the catalog at"
            + " checkout.")) return;
    Sale sale = ctx.billing.checkout(new ArrayList<>(cart), d);
    cart.clear();
    discount.setText("0.00");
    host.changed();
    InvoiceDialog.show(this, sale);
  }

  private void history() {
    JDialog d = new JDialog(host, "Saved Invoices", Dialog.ModalityType.APPLICATION_MODAL);
    JTable t = UI.table();
    List<Object[]> rows = new ArrayList<>();
    ctx.sales
        .findAll()
        .forEach(
            s ->
                rows.add(
                    new Object[] {
                      s.getId(),
                      s.getInvoiceNumber(),
                      s.getCreatedAt().toLocalDate(),
                      ValidationUtil.money(s.getTotalCents())
                    }));
    UI.data(t, new String[] {"ID", "Invoice", "Date", "Total (INR)"}, rows);
    JPanel p = UI.card();
    p.add(UI.scroll(t), BorderLayout.CENTER);
    p.add(
        UI.row(
            UI.button(
                "Open Invoice",
                true,
                () -> InvoiceDialog.show(d, ctx.sales.findById(UI.selectedId(t)))),
            UI.button("Close", d::dispose)),
        BorderLayout.SOUTH);
    d.setContentPane(p);
    d.setSize(800, 500);
    d.setLocationRelativeTo(this);
    d.setVisible(true);
  }

  @Override
  public void refresh() {
    Product old = (Product) product.getSelectedItem();
    product.removeAllItems();
    for (Product p : ctx.products.findAll()) {
      product.addItem(p);
      if (old != null && old.getId() == p.getId()) product.setSelectedItem(p);
    }
    updateCart();
  }
}
