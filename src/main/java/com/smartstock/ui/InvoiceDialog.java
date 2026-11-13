package com.smartstock.ui;

import com.smartstock.model.Sale;
import com.smartstock.util.*;
import java.awt.*;
import java.awt.print.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.MessageFormat;
import javax.swing.*;

public final class InvoiceDialog {
  private InvoiceDialog() {}

  public static void show(Component owner, Sale sale) {
    JDialog d =
        new JDialog(
            SwingUtilities.getWindowAncestor(owner),
            "Invoice " + sale.getInvoiceNumber(),
            Dialog.ModalityType.APPLICATION_MODAL);
    d.setName("invoice-dialog");
    JTextArea text = new JTextArea(InvoiceFormatter.text(sale));
    text.setName("invoice-text");
    text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
    text.setEditable(false);
    text.setMargin(new Insets(26, 26, 26, 26));
    text.setCaretPosition(0);
    JPanel p = new JPanel(new BorderLayout(0, 16));
    p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    p.add(UI.scroll(text), BorderLayout.CENTER);
    p.add(
        UI.row(
            UI.button(
                "Print Invoice",
                true,
                () -> {
                  try {
                    boolean printed =
                        text.print(
                            new MessageFormat("SmartStock • Sales Invoice"),
                            new MessageFormat("Page {0}"));
                    if (printed) UI.success(d, "Invoice sent to the printer.");
                  } catch (PrinterException e) {
                    throw new AppException(
                        "Printing is unavailable. Check your printer or save the invoice as text.",
                        e);
                  }
                }),
            UI.button(
                "Save Text",
                () -> {
                  JFileChooser chooser = new JFileChooser();
                  chooser.setSelectedFile(new File(sale.getInvoiceNumber() + ".txt"));
                  if (chooser.showSaveDialog(d) == JFileChooser.APPROVE_OPTION) {
                    Path path = chooser.getSelectedFile().toPath();
                    if (Files.exists(path) && !UI.confirm(d, "Replace existing file?")) return;
                    try {
                      Files.writeString(path, text.getText(), StandardCharsets.UTF_8);
                      UI.success(d, "Invoice saved.");
                    } catch (IOException e) {
                      throw new AppException(
                          "Could not save invoice. Choose a writable location.", e);
                    }
                  }
                }),
            UI.button("Close", d::dispose)),
        BorderLayout.SOUTH);
    d.setContentPane(p);
    d.setSize(750, 650);
    d.setLocationRelativeTo(owner);
    d.setVisible(true);
  }
}
