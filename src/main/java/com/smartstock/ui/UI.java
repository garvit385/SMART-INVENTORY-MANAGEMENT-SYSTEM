package com.smartstock.ui;

import com.smartstock.util.*;
import java.awt.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public final class UI {
  private UI() {}

  public static JLabel label(String text, int size, Color color, boolean bold) {
    JLabel l = new JLabel(text);
    l.setFont(l.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, (float) size));
    l.setForeground(color);
    return l;
  }

  public static JButton button(String text, Runnable action) {
    return button(text, false, action);
  }

  public static JButton button(String text, boolean primary, Runnable action) {
    JButton b = new JButton(text);
    b.setName(text);
    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    b.setFocusPainted(false);
    if (primary) {
      b.setBackground(Theme.BLUE);
      b.setForeground(Color.WHITE);
    }
    b.addActionListener(e -> safe(b, action));
    return b;
  }

  public static void safe(Component owner, Runnable action) {
    try {
      action.run();
    } catch (AppException e) {
      error(owner, e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      error(owner, "Something went wrong. Please try again.\n" + e.getClass().getSimpleName());
    }
  }

  public static void error(Component owner, String text) {
    JOptionPane.showMessageDialog(owner, text, "Please check", JOptionPane.ERROR_MESSAGE);
  }

  public static void success(Component owner, String text) {
    JOptionPane.showMessageDialog(owner, text, "SmartStock", JOptionPane.INFORMATION_MESSAGE);
  }

  public static boolean confirm(Component owner, String text) {
    return JOptionPane.showConfirmDialog(
            owner, text, "Confirm action", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
        == JOptionPane.YES_OPTION;
  }

  public static JPanel row(Component... items) {
    JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    p.setOpaque(false);
    for (Component c : items) p.add(c);
    return p;
  }

  public static JPanel stack() {
    JPanel p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
    p.setOpaque(false);
    return p;
  }

  public static JPanel card() {
    JPanel p =
        new JPanel(new BorderLayout(0, 16)) {
          @Override
          protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(23, 36, 58, 5));
            g.fillRoundRect(0, 2, getWidth() - 1, getHeight() - 2, 18, 18);
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 18, 18);
            g.setColor(Theme.BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 3, 18, 18);
            g.dispose();
          }
        };
    p.setOpaque(false);
    p.setBorder(BorderFactory.createEmptyBorder(23, 23, 23, 23));
    return p;
  }

  public static JTextField field(String name, String value, int columns) {
    JTextField f = new JTextField(value, columns);
    f.setName(name);
    f.putClientProperty("JTextField.placeholderText", name);
    f.setPreferredSize(new Dimension(f.getPreferredSize().width, 38));
    return f;
  }

  public static void onChange(JTextField f, Runnable r) {
    f.getDocument()
        .addDocumentListener(
            new DocumentListener() {
              public void insertUpdate(DocumentEvent e) {
                r.run();
              }

              public void removeUpdate(DocumentEvent e) {
                r.run();
              }

              public void changedUpdate(DocumentEvent e) {
                r.run();
              }
            });
  }

  public static JTable table() {
    JTable t = new JTable();
    t.setName("records-table");
    t.setRowHeight(43);
    t.setAutoCreateRowSorter(true);
    t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    t.setFillsViewportHeight(true);
    t.setBackground(Color.WHITE);
    t.setForeground(Theme.TEXT);
    t.setIntercellSpacing(new Dimension(0, 1));
    t.getTableHeader().setPreferredSize(new Dimension(0, 42));
    t.getTableHeader().setReorderingAllowed(false);
    DefaultTableCellRenderer render =
        new DefaultTableCellRenderer() {
          @Override
          public Component getTableCellRendererComponent(
              JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            JLabel l =
                (JLabel)
                    super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            if (!selected) {
              l.setBackground(row % 2 == 0 ? Color.WHITE : new Color(251, 252, 254));
              l.setForeground(Theme.TEXT);
            }
            String s = String.valueOf(value);
            if (s.equals("Low stock") || s.equals("Out of stock") || s.equals("In stock")) {
              l.setForeground(
                  s.equals("In stock")
                      ? Theme.GREEN
                      : s.equals("Low stock") ? new Color(184, 115, 9) : Theme.RED);
              l.setFont(l.getFont().deriveFont(Font.BOLD));
            } else if (s.equals("Edit  /  Delete")) {
              l.setForeground(Theme.BLUE);
            }
            return l;
          }
        };
    t.setDefaultRenderer(Object.class, render);
    t.setDefaultRenderer(Number.class, render);
    return t;
  }

  public static void data(JTable t, String[] columns, java.util.List<Object[]> rows) {
    Object[][] values = new Object[rows.size()][];
    for (int r = 0; r < rows.size(); r++) {
      values[r] = rows.get(r).clone();
      for (int col = 0; col < columns.length; col++)
        if (columns[col].contains("(INR)") && values[r][col] instanceof String text)
          values[r][col] = new java.math.BigDecimal(text);
    }
    t.setModel(
        new DefaultTableModel(values, columns) {
          @Override
          public boolean isCellEditable(int row, int col) {
            return false;
          }

          @Override
          public Class<?> getColumnClass(int column) {
            for (int i = 0; i < getRowCount(); i++) {
              Object o = getValueAt(i, column);
              if (o != null) return o instanceof Number ? o.getClass() : Object.class;
            }
            return Object.class;
          }
        });
  }

  public static JScrollPane scroll(Component c) {
    JScrollPane s = new JScrollPane(c);
    s.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
    s.getViewport().setBackground(Color.WHITE);
    return s;
  }

  public static JPanel form(String[] labels, JComponent[] fields) {
    JPanel p = new JPanel(new GridBagLayout());
    p.setOpaque(false);
    for (int i = 0; i < labels.length; i++) {
      GridBagConstraints g = new GridBagConstraints();
      g.gridx = 0;
      g.gridy = i;
      g.anchor = GridBagConstraints.WEST;
      g.insets = new Insets(7, 0, 7, 18);
      p.add(new JLabel(labels[i]), g);
      g.gridx = 1;
      g.weightx = 1;
      g.fill = GridBagConstraints.HORIZONTAL;
      g.insets = new Insets(7, 0, 7, 0);
      p.add(fields[i], g);
      fields[i].getAccessibleContext().setAccessibleName(labels[i]);
    }
    return p;
  }

  /** Keep the editor open after validation failures; do not discard the user's work. */
  public static void editor(Component owner, String title, JPanel form, Runnable save) {
    editor(
        owner,
        title,
        form,
        () -> {
          save.run();
          return true;
        });
  }

  public static void editor(Component owner, String title, JPanel form, BooleanSupplier save) {
    Window window = SwingUtilities.getWindowAncestor(owner);
    JDialog d = new JDialog(window, title, Dialog.ModalityType.APPLICATION_MODAL);
    d.setName(title);
    d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    JPanel p = new JPanel(new BorderLayout(0, 18));
    p.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
    p.add(label(title, 23, Theme.TEXT, true), BorderLayout.NORTH);
    p.add(form, BorderLayout.CENTER);
    JPanel actions =
        row(
            button("Cancel", d::dispose),
            button(
                "Save",
                true,
                () -> {
                  if (save.getAsBoolean()) d.dispose();
                }));
    p.add(actions, BorderLayout.SOUTH);
    d.setContentPane(p);
    d.getRootPane().setDefaultButton((JButton) actions.getComponent(1));
    d.pack();
    d.setSize(Math.max(530, d.getWidth()), d.getHeight());
    d.setLocationRelativeTo(owner);
    d.setVisible(true);
  }

  public static int selectedId(JTable table) {
    int row = table.getSelectedRow();
    if (row < 0) throw new AppException("Select a row first.");
    return ((Number) table.getValueAt(row, 0)).intValue();
  }
}
