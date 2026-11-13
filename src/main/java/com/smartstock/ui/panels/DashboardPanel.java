package com.smartstock.ui.panels;

import com.smartstock.model.*;
import com.smartstock.ui.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class DashboardPanel extends BasePanel {
  public DashboardPanel(AppContext ctx, MainDashboard host) {
    super(
        ctx,
        host,
        "Inventory at a glance",
        "Welcome back. Here is what is happening across your inventory.");
    heading.add(UI.button("Refresh Dashboard", this::refresh), BorderLayout.EAST);
    // Keep cards and quick actions usable at 1100 x 700 instead of compressing them.
    remove(body);
    DashboardCanvas canvas = new DashboardCanvas();
    canvas.add(body, BorderLayout.CENTER);
    JScrollPane scroll = new JScrollPane(canvas);
    scroll.setBorder(null);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(24);
    scroll.getViewport().setBackground(Theme.BG);
    add(scroll, BorderLayout.CENTER);
  }

  private static class DashboardCanvas extends JPanel implements Scrollable {
    DashboardCanvas() {
      super(new BorderLayout());
      setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(900, 720);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
      return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
      return 200;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return getParent() != null && getParent().getHeight() > 720;
    }
  }

  @Override
  public void refresh() {
    body.removeAll();
    List<Product> products = ctx.products.findAll();
    List<Product> low =
        products.stream()
            .filter(p -> p.getQuantity() < p.getMinStock() || p.getQuantity() == 0)
            .toList();
    long units = products.stream().mapToLong(Product::getQuantity).sum();
    JPanel stats = new JPanel(new GridLayout(1, 4, 18, 0));
    stats.setOpaque(false);
    stats.add(
        stat(
            "Total Products",
            String.valueOf(products.size()),
            "Products",
            "Active catalog items",
            Theme.BLUE));
    stats.add(
        stat(
            "Total Stock Units",
            String.valueOf(units),
            "Stock Management",
            "Units currently on hand",
            Theme.TEAL));
    stats.add(
        stat(
            "Low Stock Items",
            String.valueOf(low.size()),
            "alert",
            "Including out-of-stock items",
            Theme.AMBER));
    stats.add(
        stat(
            "Total Suppliers",
            String.valueOf(ctx.suppliers.findAll().size()),
            "Suppliers",
            "Your supply network",
            Theme.BLUE));
    body.add(stats, BorderLayout.NORTH);
    JPanel content = new JPanel(new GridLayout(2, 1, 0, 20));
    content.setOpaque(false);
    JPanel upper = new JPanel(new GridLayout(1, 2, 20, 0));
    upper.setOpaque(false);
    JPanel overview = UI.card();
    overview.add(UI.label("Inventory overview", 17, Theme.TEXT, true), BorderLayout.NORTH);
    int out = (int) products.stream().filter(p -> p.getQuantity() == 0).count(),
        lowCount = low.size() - out,
        healthy = products.size() - low.size();
    overview.add(new StockChart(healthy, lowCount, out), BorderLayout.CENTER);
    upper.add(overview);
    JPanel alerts = UI.card();
    JPanel ah = new JPanel(new BorderLayout());
    ah.setOpaque(false);
    ah.add(UI.label("Low-stock alerts", 17, Theme.TEXT, true), BorderLayout.WEST);
    ah.add(
        UI.button(
            "Review Stock",
            () -> {
              host.navigate("Products");
            }),
        BorderLayout.EAST);
    alerts.add(ah, BorderLayout.NORTH);
    JTable at = UI.table();
    List<Object[]> ar = new ArrayList<>();
    low.forEach(
        p -> ar.add(new Object[] {p.getName(), p.getQuantity(), p.getMinStock(), p.getStatus()}));
    UI.data(at, new String[] {"Product", "On Hand", "Minimum", "Status"}, ar);
    alerts.add(UI.scroll(at), BorderLayout.CENTER);
    upper.add(alerts);
    content.add(upper);
    JPanel lower = new JPanel(new BorderLayout(20, 0));
    lower.setOpaque(false);
    JPanel recent = UI.card();
    recent.add(UI.label("Recent transactions", 17, Theme.TEXT, true), BorderLayout.NORTH);
    JTable rt = UI.table();
    List<Object[]> rr = new ArrayList<>();
    ctx.transactions.findAll().stream()
        .limit(5)
        .forEach(
            t ->
                rr.add(
                    new Object[] {
                      t.getProductName(),
                      t.getType(),
                      t.getQuantity(),
                      t.getNewStock(),
                      t.getCreatedAt().toLocalDate()
                    }));
    UI.data(rt, new String[] {"Product", "Type", "Quantity", "Balance", "Date"}, rr);
    recent.add(UI.scroll(rt), BorderLayout.CENTER);
    lower.add(recent, BorderLayout.CENTER);
    JPanel quick = UI.card();
    quick.setPreferredSize(new Dimension(240, 0));
    quick.add(UI.label("Quick actions", 17, Theme.TEXT, true), BorderLayout.NORTH);
    JPanel actions = new JPanel(new GridLayout(5, 1, 0, 9));
    actions.setOpaque(false);
    for (String s :
        new String[] {
          "Add Product", "Add Supplier", "Record Stock In", "Record Sale", "View Reports"
        }) actions.add(UI.button(s, s.equals("Add Product"), () -> host.quickAction(s)));
    quick.add(actions, BorderLayout.CENTER);
    lower.add(quick, BorderLayout.EAST);
    content.add(lower);
    body.add(content, BorderLayout.CENTER);
    body.revalidate();
    body.repaint();
  }

  private JPanel stat(String label, String value, String icon, String hint, Color accent) {
    JPanel p = UI.card();
    JLabel top = UI.label(label, 13, Theme.MUTED, false);
    top.setIcon(new AppIcon(icon, accent, 22));
    top.setIconTextGap(10);
    p.add(top, BorderLayout.NORTH);
    p.add(UI.label(value, 33, Theme.TEXT, true), BorderLayout.CENTER);
    p.add(UI.label(hint, 11, Theme.MUTED, false), BorderLayout.SOUTH);
    return p;
  }

  private static class StockChart extends JPanel {
    private final int[] values;

    StockChart(int healthy, int low, int out) {
      values = new int[] {healthy, low, out};
      setBackground(Color.WHITE);
      setPreferredSize(new Dimension(380, 180));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      super.paintComponent(graphics);
      Graphics2D g = (Graphics2D) graphics.create();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int total = Arrays.stream(values).sum(),
          size = Math.min(145, Math.min(getHeight() - 20, getWidth() / 2 - 15)),
          x = 8,
          y = (getHeight() - size) / 2;
      Color[] colors = {Theme.TEAL, Theme.AMBER, Theme.RED};
      int angle = 90;
      if (total == 0) {
        g.setColor(Theme.BORDER);
        g.fillOval(x, y, size, size);
      } else
        for (int i = 0; i < 3; i++) {
          int arc = i == 2 ? 450 - angle : (int) Math.round(360.0 * values[i] / total);
          g.setColor(colors[i]);
          g.fillArc(x, y, size, size, angle, arc);
          angle += arc;
        }
      int inner = (int) (size * .72), offset = (size - inner) / 2;
      g.setColor(Color.WHITE);
      g.fillOval(x + offset, y + offset, inner, inner);
      g.setColor(Theme.TEXT);
      g.setFont(getFont().deriveFont(Font.BOLD, 26f));
      String n = String.valueOf(total);
      g.drawString(n, x + size / 2 - g.getFontMetrics().stringWidth(n) / 2, y + size / 2 + 6);
      g.setFont(getFont().deriveFont(10f));
      g.setColor(Theme.MUTED);
      g.drawString("PRODUCTS", x + size / 2 - 26, y + size / 2 + 24);
      String[] names = {"In stock", "Low stock", "Out of stock"};
      int lx = x + size + 25, ly = Math.max(25, getHeight() / 2 - 40);
      for (int i = 0; i < 3; i++) {
        g.setColor(colors[i]);
        g.fillRoundRect(lx, ly + i * 34, 9, 9, 3, 3);
        g.setColor(Theme.MUTED);
        g.setFont(getFont().deriveFont(12f));
        g.drawString(names[i] + "   " + values[i], lx + 19, ly + i * 34 + 9);
      }
      g.dispose();
    }
  }
}
