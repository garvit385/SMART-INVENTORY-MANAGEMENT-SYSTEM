package com.smartstock.ui.panels;

import com.smartstock.model.Supplier;
import com.smartstock.ui.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

public class SupplierPanel extends BasePanel {
  private final JTable table = UI.table();
  private final JTextField search = UI.field("Search suppliers…", "", 25);

  public SupplierPanel(AppContext ctx, MainDashboard host) {
    super(ctx, host, "Suppliers", "Keep your supplier network and contact details up to date.");
    heading.add(UI.button("Add Supplier", true, () -> edit(null)), BorderLayout.EAST);
    JPanel card = UI.card();
    card.add(UI.row(search, UI.button("Refresh", this::refresh)), BorderLayout.NORTH);
    card.add(UI.scroll(table), BorderLayout.CENTER);
    card.add(
        UI.row(
            UI.button(
                "View Details",
                () -> {
                  Supplier s = selected();
                  long n =
                      ctx.products.findAll().stream()
                          .filter(p -> p.getSupplierId() == s.getId())
                          .count();
                  JTextArea text =
                      new JTextArea(
                          s.getName()
                              + "\nContact: "
                              + s.getContactPerson()
                              + "\nPhone: "
                              + s.getPhone()
                              + "\nEmail: "
                              + s.getEmail()
                              + "\nAddress: "
                              + s.getAddress()
                              + "\nAssociated products: "
                              + n);
                  text.setEditable(false);
                  JOptionPane.showMessageDialog(
                      this, text, "Supplier Details", JOptionPane.INFORMATION_MESSAGE);
                }),
            UI.button("Edit Supplier", () -> edit(selected())),
            UI.button(
                "Delete Supplier",
                () -> {
                  Supplier s = selected();
                  if (UI.confirm(
                      this,
                      "Delete supplier ‘"
                          + s.getName()
                          + "’? Suppliers linked to products cannot be deleted.")) {
                    ctx.suppliers.delete(s.getId());
                    host.changed();
                  }
                })),
        BorderLayout.SOUTH);
    body.add(card);
    UI.onChange(search, () -> UI.safe(this, this::refresh));
  }

  private Supplier selected() {
    int id = UI.selectedId(table);
    return ctx.suppliers.findAll().stream().filter(s -> s.getId() == id).findFirst().orElseThrow();
  }

  @Override
  public void refresh() {
    java.util.List<Object[]> rows = new ArrayList<>();
    String q = search.getText().toLowerCase(Locale.ROOT);
    for (Supplier s : ctx.suppliers.findAll())
      if ((s.getName() + " " + s.getContactPerson() + " " + s.getEmail() + " " + s.getPhone())
          .toLowerCase(Locale.ROOT)
          .contains(q))
        rows.add(
            new Object[] {
              s.getId(),
              s.getName(),
              s.getContactPerson(),
              s.getPhone(),
              s.getEmail(),
              s.getAddress()
            });
    UI.data(
        table,
        new String[] {"ID", "Supplier Name", "Contact Person", "Phone", "Email", "Address"},
        rows);
  }

  public void edit(Supplier s) {
    JTextField name = UI.field("Supplier Name", s == null ? "" : s.getName(), 26),
        contact = UI.field("Contact Person", s == null ? "" : s.getContactPerson(), 26),
        phone = UI.field("Phone", s == null ? "" : s.getPhone(), 26),
        email = UI.field("Email", s == null ? "" : s.getEmail(), 26);
    JTextArea address = new JTextArea(s == null ? "" : s.getAddress(), 3, 25);
    address.setLineWrap(true);
    address.setWrapStyleWord(true);
    UI.editor(
        this,
        s == null ? "Add Supplier" : "Edit Supplier",
        UI.form(
            new String[] {"Supplier name *", "Contact person", "Phone", "Email", "Address"},
            new JComponent[] {name, contact, phone, email, UI.scroll(address)}),
        () -> {
          ctx.suppliers.save(
              new Supplier(
                  s == null ? 0 : s.getId(),
                  name.getText(),
                  contact.getText(),
                  phone.getText(),
                  email.getText(),
                  address.getText()));
          host.changed();
        });
  }
}
