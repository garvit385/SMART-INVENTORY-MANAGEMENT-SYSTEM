package com.smartstock.ui.panels;

import com.smartstock.model.StockTransaction;
import com.smartstock.ui.*;
import com.smartstock.util.AppException;
import java.awt.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import javax.swing.*;

public class TransactionPanel extends BasePanel {
  private final JTable table = UI.table();
  private final JTextField search = UI.field("Search product or remarks…", "", 20),
      from = UI.field("YYYY-MM-DD", "", 10),
      to = UI.field("YYYY-MM-DD", "", 10);
  private final JComboBox<String> type =
      new JComboBox<>(new String[] {"All", "OPENING", "IN", "OUT", "SALE", "ADJUSTMENT"});
  private final JLabel count = UI.label(" ", 12, Theme.MUTED, false);

  public TransactionPanel(AppContext ctx, MainDashboard host) {
    super(
        ctx,
        host,
        "Transactions",
        "A read-only history of every stock movement. Dates use your local time.");
    JPanel card = UI.card();
    JPanel filters = UI.stack();
    filters.add(UI.row(search, new JLabel("Type"), type));
    filters.add(Box.createVerticalStrut(12));
    filters.add(
        UI.row(
            new JLabel("From"),
            from,
            new JLabel("To"),
            to,
            UI.button("Apply Filters", this::refresh),
            UI.button(
                "Clear",
                () -> {
                  search.setText("");
                  from.setText("");
                  to.setText("");
                  type.setSelectedIndex(0);
                  refresh();
                }),
            UI.button("Refresh", this::refresh)));
    card.add(filters, BorderLayout.NORTH);
    card.add(UI.scroll(table), BorderLayout.CENTER);
    card.add(count, BorderLayout.SOUTH);
    body.add(card);
    search.addActionListener(e -> UI.safe(this, this::refresh));
    type.addActionListener(e -> UI.safe(this, this::refresh));
  }

  private LocalDate date(String s) {
    try {
      return s.isBlank() ? null : LocalDate.parse(s.trim());
    } catch (DateTimeParseException e) {
      throw new AppException("Dates must use YYYY-MM-DD, for example 2026-11-13.");
    }
  }

  @Override
  public void refresh() {
    java.util.List<Object[]> rows = new ArrayList<>();
    for (StockTransaction t :
        ctx.transactions.findAll(
            search.getText().trim(),
            String.valueOf(type.getSelectedItem()),
            date(from.getText()),
            date(to.getText())))
      rows.add(
          new Object[] {
            t.getId(),
            t.getProductName(),
            t.getType(),
            t.getQuantity(),
            t.getPreviousStock(),
            t.getNewStock(),
            t.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")),
            t.getRemarks()
          });
    UI.data(
        table,
        new String[] {
          "ID", "Product", "Type", "Quantity", "Previous Stock", "Updated Stock", "Date", "Remarks"
        },
        rows);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    int[] w = {55, 200, 110, 85, 110, 110, 190, 280};
    for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
    count.setText(rows.size() + " movements  •  Audit records cannot be edited manually");
  }
}
