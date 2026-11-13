package com.smartstock.ui.panels;

import com.smartstock.service.ReportService;
import com.smartstock.ui.*;
import com.smartstock.util.CSVExporter;
import java.awt.*;
import java.io.File;
import java.nio.file.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ReportsPanel extends BasePanel {
  private final JComboBox<String> type = new JComboBox<>(ReportService.TYPES);
  private final JTable table = UI.table();
  private final JLabel summary = UI.label(" ", 13, Theme.BLUE, true);
  private ReportService.Report current;

  public ReportsPanel(AppContext ctx, MainDashboard host) {
    super(
        ctx, host, "Reports", "Turn your inventory data into clear, exportable business insights.");
    heading.add(UI.button("Export CSV", true, this::export), BorderLayout.EAST);
    JPanel card = UI.card();
    card.add(
        UI.row(new JLabel("Report"), type, UI.button("Generate Report", this::refresh)),
        BorderLayout.NORTH);
    card.add(UI.scroll(table), BorderLayout.CENTER);
    card.add(summary, BorderLayout.SOUTH);
    body.add(card);
    type.addActionListener(e -> UI.safe(this, this::refresh));
  }

  @Override
  public void refresh() {
    current = ctx.reports.generate(String.valueOf(type.getSelectedItem()));
    UI.data(table, current.columns(), current.rows());
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    for (int i = 0; i < table.getColumnCount(); i++)
      table.getColumnModel().getColumn(i).setPreferredWidth(i == 1 ? 240 : 145);
    summary.setText(current.summary());
  }

  private void export() {
    refresh();
    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
    chooser.setSelectedFile(
        new File(
            "SmartStock_" + String.valueOf(type.getSelectedItem()).replace(' ', '_') + ".csv"));
    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      Path path = chooser.getSelectedFile().toPath();
      if (!path.toString().toLowerCase().endsWith(".csv")) path = Path.of(path + ".csv");
      if (Files.exists(path) && !UI.confirm(this, "Replace existing CSV file?")) return;
      CSVExporter.export(path, current);
      UI.success(this, "Report exported to:\n" + path);
    }
  }
}
