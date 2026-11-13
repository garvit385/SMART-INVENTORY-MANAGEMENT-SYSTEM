package com.smartstock;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.ui.*;
import javax.swing.*;

public final class Main {
  private Main() {}

  public static void main(String[] args) {
    Theme.install();
    Thread.setDefaultUncaughtExceptionHandler(
        (thread, error) -> {
          error.printStackTrace();
          SwingUtilities.invokeLater(
              () -> UI.error(null, "Unexpected error: " + error.getMessage()));
        });
    try {
      DatabaseConnection db = new DatabaseConnection();
      db.initialize();
      AppContext ctx = new AppContext(db);
      SwingUtilities.invokeLater(() -> new LoginFrame(ctx).setVisible(true));
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(
          null, e.getMessage(), "SmartStock startup error", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }
}
