package com.smartstock.ui;

import com.smartstock.model.*;
import com.smartstock.ui.panels.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;

public class MainDashboard extends JFrame {
  private final AppContext ctx;
  private final Admin admin;
  private final CardLayout layout = new CardLayout();
  private final JPanel pages = new JPanel(layout);
  private final Map<String, BasePanel> panels = new LinkedHashMap<>();
  private final Map<String, JButton> navigation = new LinkedHashMap<>();
  private final JLabel pageTitle = UI.label("Dashboard", 17, Theme.TEXT, true);
  private final JButton notifications;
  private String current = "Dashboard";

  public MainDashboard(AppContext ctx, Admin admin) {
    super("SmartStock | Smart Inventory Management System");
    this.ctx = ctx;
    this.admin = admin;
    setName("main-dashboard");
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(1100, 700));
    setSize(1440, 940);
    setLocationRelativeTo(null);
    setExtendedState(MAXIMIZED_BOTH);
    JPanel root = new JPanel(new BorderLayout());
    JPanel sidebar = new JPanel(new BorderLayout());
    sidebar.setPreferredSize(new Dimension(234, 0));
    sidebar.setBackground(Theme.NAVY);
    sidebar.setBorder(BorderFactory.createEmptyBorder(28, 18, 20, 18));
    JPanel brand = UI.stack();
    JLabel logo = UI.label("  SmartStock", 23, Color.WHITE, true);
    logo.setIcon(new AppIcon("logo", Theme.TEAL, 30));
    brand.add(logo);
    brand.add(Box.createVerticalStrut(9));
    brand.add(UI.label("   INVENTORY WORKSPACE", 9, new Color(147, 169, 201), true));
    brand.add(Box.createVerticalStrut(35));
    sidebar.add(brand, BorderLayout.NORTH);
    JPanel nav = UI.stack();
    nav.add(UI.label("  WORKSPACE", 10, new Color(135, 158, 190), true));
    nav.add(Box.createVerticalStrut(14));
    String[] names = {
      "Dashboard",
      "Products",
      "Categories",
      "Suppliers",
      "Stock Management",
      "Sales & Billing",
      "Transactions",
      "Reports",
      "Settings"
    };
    for (String name : names) {
      JButton b = new JButton(name, new AppIcon(name, new Color(187, 204, 229)));
      b.setName("nav-" + name);
      b.setHorizontalAlignment(SwingConstants.LEFT);
      b.setIconTextGap(13);
      b.setBorder(BorderFactory.createEmptyBorder(13, 12, 13, 8));
      b.setMaximumSize(new Dimension(220, 47));
      b.setBackground(Theme.NAVY);
      b.setForeground(new Color(199, 213, 235));
      b.setFocusPainted(false);
      b.putClientProperty("JButton.buttonType", "borderless");
      b.addActionListener(e -> UI.safe(this, () -> navigate(name)));
      navigation.put(name, b);
      nav.add(b);
      nav.add(Box.createVerticalStrut(5));
    }
    sidebar.add(nav, BorderLayout.CENTER);
    JPanel bottom = UI.stack();
    JButton logout = UI.button("Logout", this::logout);
    logout.setIcon(new AppIcon("Logout", Color.WHITE));
    logout.setBackground(new Color(34, 49, 72));
    logout.setForeground(Color.WHITE);
    logout.setMaximumSize(new Dimension(220, 42));
    bottom.add(logout);
    bottom.add(Box.createVerticalStrut(20));
    bottom.add(UI.label("  LOCAL WORKSPACE  ·  v1.0", 10, new Color(135, 158, 190), false));
    sidebar.add(bottom, BorderLayout.SOUTH);
    root.add(sidebar, BorderLayout.WEST);
    JPanel main = new JPanel(new BorderLayout());
    JPanel top = new JPanel(new BorderLayout());
    top.setBackground(Color.WHITE);
    top.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
            BorderFactory.createEmptyBorder(14, 30, 14, 26)));
    top.add(pageTitle, BorderLayout.WEST);
    notifications =
        UI.button(
            "Alerts",
            () -> {
              navigate("Products");
              ((ProductPanel) panels.get("Products")).showLowStock();
            });
    notifications.setIcon(new AppIcon("alert", Theme.AMBER));
    JPanel tools =
        UI.row(
            UI.label(
                LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")),
                12,
                Theme.MUTED,
                false),
            notifications,
            UI.label(
                "  " + admin.getDisplayName() + "  |  " + admin.getRole(), 12, Theme.TEXT, true));
    top.add(tools, BorderLayout.EAST);
    main.add(top, BorderLayout.NORTH);
    main.add(pages, BorderLayout.CENTER);
    root.add(main, BorderLayout.CENTER);
    setContentPane(root);
    addPage("Dashboard", new DashboardPanel(ctx, this));
    addPage("Products", new ProductPanel(ctx, this));
    addPage("Categories", new CategoryPanel(ctx, this));
    addPage("Suppliers", new SupplierPanel(ctx, this));
    addPage("Stock Management", new StockPanel(ctx, this));
    addPage("Sales & Billing", new SalesPanel(ctx, this));
    addPage("Transactions", new TransactionPanel(ctx, this));
    addPage("Reports", new ReportsPanel(ctx, this));
    addPage("Settings", new SettingsPanel(ctx, this, admin));
    navigate("Dashboard");
  }

  private void addPage(String name, BasePanel panel) {
    panels.put(name, panel);
    pages.add(panel, name);
  }

  public void navigate(String name) {
    current = name;
    pageTitle.setText(name);
    panels.get(name).refresh();
    layout.show(pages, name);
    navigation.forEach(
        (key, b) -> {
          b.setBackground(key.equals(name) ? Theme.BLUE : Theme.NAVY);
          b.setForeground(key.equals(name) ? Color.WHITE : new Color(199, 213, 235));
        });
    updateAlerts();
  }

  public void changed() {
    panels.get(current).refresh();
    updateAlerts();
  }

  private void updateAlerts() {
    long count =
        ctx.products.findAll().stream()
            .filter(p -> p.getQuantity() < p.getMinStock() || p.getQuantity() == 0)
            .count();
    notifications.setText(count + " alerts");
  }

  public void quickAction(String action) {
    switch (action) {
      case "Add Product" -> {
        navigate("Products");
        ((ProductPanel) panels.get("Products")).edit(null);
      }
      case "Add Supplier" -> {
        navigate("Suppliers");
        ((SupplierPanel) panels.get("Suppliers")).edit(null);
      }
      case "Record Stock In" -> navigate("Stock Management");
      case "Record Sale" -> navigate("Sales & Billing");
      default -> navigate("Reports");
    }
  }

  public void logout() {
    if (UI.confirm(this, "Log out of this workspace? Unsaved cart items will be discarded.")) {
      new LoginFrame(ctx).setVisible(true);
      dispose();
    }
  }
}
