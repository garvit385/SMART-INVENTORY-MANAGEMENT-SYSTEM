package com.smartstock.ui.panels;

import com.smartstock.model.Admin;
import com.smartstock.ui.*;
import com.smartstock.util.*;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

public class SettingsPanel extends BasePanel {
  public SettingsPanel(AppContext ctx, MainDashboard host, Admin admin) {
    super(ctx, host, "Settings", "Manage your local workspace and keep your account secure.");
    JPanel content = new JPanel(new BorderLayout(0, 22));
    content.setOpaque(false);
    JPanel account = UI.card();
    account.add(UI.label("Account security", 18, Theme.TEXT, true), BorderLayout.NORTH);
    JPasswordField old = new JPasswordField(25),
        next = new JPasswordField(25),
        confirm = new JPasswordField(25);
    old.setName("Current Password");
    next.setName("New Password");
    confirm.setName("Confirm Password");
    account.add(
        UI.form(
            new String[] {
              "Signed in as",
              "Current password",
              "New password (8+ characters)",
              "Confirm new password"
            },
            new JComponent[] {
              new JLabel(admin.getUsername() + " • " + admin.getRole()), old, next, confirm
            }),
        BorderLayout.CENTER);
    account.add(
        UI.row(
            UI.button(
                "Change Password",
                true,
                () -> {
                  char[] a = old.getPassword(), b = next.getPassword(), c = confirm.getPassword();
                  try {
                    if (!Arrays.equals(b, c)) throw new AppException("New passwords do not match.");
                    ctx.auth.changePassword(admin, a, b);
                    old.setText("");
                    next.setText("");
                    confirm.setText("");
                    UI.success(this, "Password changed. Use your new password at next login.");
                  } finally {
                    Arrays.fill(a, '\0');
                    Arrays.fill(b, '\0');
                    Arrays.fill(c, '\0');
                  }
                })),
        BorderLayout.SOUTH);
    content.add(account, BorderLayout.NORTH);
    JPanel info = UI.card();
    info.add(UI.label("Workspace information", 18, Theme.TEXT, true), BorderLayout.NORTH);
    JTextArea text =
        new JTextArea(
            "SmartStock 1.0.0\nSmart Inventory Management System\n\nDatabase: "
                + ctx.db.getPath()
                + "\n"
                + "Storage: Local SQLite, foreign keys enabled\n"
                + "Currency: INR • Amounts stored as exact integer minor units\n"
                + "Date/time: Local system time\n\n"
                + "To back up your data, close SmartStock and copy smart_inventory.db\n"
                + "from the location above to a safe folder. Keep backups out of GitHub.\n\n"
                + "Java "
                + System.getProperty("java.version")
                + " • Swing • FlatLaf • JDBC\n"
                + "Academic desktop edition — single local administrator workspace.");
    text.setEditable(false);
    text.setOpaque(false);
    text.setFont(text.getFont().deriveFont(13f));
    info.add(text, BorderLayout.CENTER);
    content.add(info, BorderLayout.CENTER);
    body.add(content);
  }

  @Override
  public void refresh() {}
}
