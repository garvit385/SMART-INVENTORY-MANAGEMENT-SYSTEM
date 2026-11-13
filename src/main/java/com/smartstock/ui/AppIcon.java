package com.smartstock.ui;

import java.awt.*;
import javax.swing.*;

/** Resolution-independent, original line icons; no platform-dependent glyph fonts. */
public class AppIcon implements Icon {
  private final String kind;
  private final Color color;
  private final int size;

  public AppIcon(String kind, Color color) {
    this(kind, color, 20);
  }

  public AppIcon(String kind, Color color, int size) {
    this.kind = kind;
    this.color = color;
    this.size = size;
  }

  @Override
  public int getIconWidth() {
    return size;
  }

  @Override
  public int getIconHeight() {
    return size;
  }

  @Override
  public void paintIcon(Component c, Graphics graphics, int x, int y) {
    Graphics2D g = (Graphics2D) graphics.create();
    g.translate(x, y);
    g.scale(size / 24.0, size / 24.0);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(color);
    g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    switch (kind) {
      case "Dashboard" -> {
        for (int a : new int[] {3, 14})
          for (int b : new int[] {3, 14}) g.drawRoundRect(a, b, 7, 7, 2, 2);
      }
      case "Products", "logo" -> {
        g.drawPolygon(new int[] {3, 12, 21, 21, 12, 3}, new int[] {7, 2, 7, 17, 22, 17}, 6);
        g.drawLine(3, 7, 12, 12);
        g.drawLine(21, 7, 12, 12);
        g.drawLine(12, 12, 12, 22);
        g.drawLine(8, 4, 17, 9);
      }
      case "Categories" -> {
        g.drawRoundRect(3, 5, 18, 15, 2, 2);
        g.drawLine(3, 10, 21, 10);
        g.drawLine(10, 10, 10, 20);
      }
      case "Suppliers" -> {
        g.drawRect(2, 6, 12, 12);
        g.drawRect(14, 10, 7, 8);
        g.drawOval(5, 16, 4, 4);
        g.drawOval(16, 16, 4, 4);
      }
      case "Stock Management" -> {
        g.drawLine(6, 3, 6, 21);
        g.drawLine(3, 7, 6, 3);
        g.drawLine(9, 7, 6, 3);
        g.drawLine(17, 3, 17, 21);
        g.drawLine(14, 17, 17, 21);
        g.drawLine(20, 17, 17, 21);
      }
      case "Sales & Billing" -> {
        g.drawRect(5, 3, 14, 18);
        g.drawLine(8, 8, 16, 8);
        g.drawLine(8, 12, 16, 12);
        g.drawLine(8, 16, 13, 16);
      }
      case "Transactions" -> {
        g.drawArc(3, 3, 18, 18, 30, 300);
        g.drawLine(12, 7, 12, 12);
        g.drawLine(12, 12, 16, 14);
        g.drawLine(20, 3, 21, 8);
        g.drawLine(21, 8, 16, 7);
      }
      case "Reports" -> {
        g.drawLine(3, 21, 21, 21);
        g.drawRect(5, 12, 3, 9);
        g.drawRect(11, 7, 3, 14);
        g.drawRect(17, 3, 3, 18);
      }
      case "Settings" -> {
        g.drawOval(3, 3, 18, 18);
        g.drawOval(9, 9, 6, 6);
        for (int a = 0; a < 8; a++) {
          double angle = a * Math.PI / 4;
          g.drawLine(
              (int) (12 + 8 * Math.cos(angle)),
              (int) (12 + 8 * Math.sin(angle)),
              (int) (12 + 11 * Math.cos(angle)),
              (int) (12 + 11 * Math.sin(angle)));
        }
      }
      case "Logout" -> {
        g.drawLine(10, 3, 3, 3);
        g.drawLine(3, 3, 3, 21);
        g.drawLine(3, 21, 10, 21);
        g.drawLine(9, 12, 22, 12);
        g.drawLine(18, 8, 22, 12);
        g.drawLine(18, 16, 22, 12);
      }
      case "alert" -> {
        g.drawRoundRect(6, 4, 12, 14, 6, 6);
        g.drawLine(4, 18, 20, 18);
        g.drawArc(10, 19, 4, 3, 180, 180);
      }
      default -> {
        g.drawLine(12, 5, 12, 19);
        g.drawLine(5, 12, 19, 12);
      }
    }
    g.dispose();
  }
}
