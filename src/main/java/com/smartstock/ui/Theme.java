package com.smartstock.ui;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.*;
import javax.swing.*;

public final class Theme {
  public static final Color BG = Color.decode("#F5F7FB"),
      NAVY = Color.decode("#17243A"),
      BLUE = Color.decode("#2563EB"),
      TEAL = Color.decode("#14B8A6"),
      TEXT = Color.decode("#1E293B"),
      MUTED = Color.decode("#64748B"),
      BORDER = Color.decode("#E5EAF2"),
      GREEN = Color.decode("#16A34A"),
      AMBER = Color.decode("#F59E0B"),
      RED = Color.decode("#DC2626");

  private Theme() {}

  public static void install() {
    FlatLightLaf.setup();
    UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
    UIManager.put("Component.arc", 12);
    UIManager.put("Button.arc", 12);
    UIManager.put("TextComponent.arc", 10);
    UIManager.put("Component.focusWidth", 1);
    UIManager.put("Component.focusColor", BLUE);
    UIManager.put("Button.margin", new Insets(10, 16, 10, 16));
    UIManager.put("Table.rowHeight", 43);
    UIManager.put("Table.showHorizontalLines", true);
    UIManager.put("Table.showVerticalLines", false);
    UIManager.put("Table.gridColor", BORDER);
    UIManager.put("Table.selectionBackground", new Color(232, 240, 255));
    UIManager.put("Table.selectionForeground", TEXT);
    UIManager.put("TableHeader.background", new Color(248, 250, 253));
    UIManager.put("TableHeader.foreground", MUTED);
    UIManager.put("ScrollBar.width", 10);
    UIManager.put("Panel.background", BG);
    UIManager.put("Label.foreground", TEXT);
    UIManager.put("OptionPane.background", BG);
  }
}
