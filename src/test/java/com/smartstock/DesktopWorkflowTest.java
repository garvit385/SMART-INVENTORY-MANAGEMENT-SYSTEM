package com.smartstock;

import static org.junit.jupiter.api.Assertions.*;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.model.*;
import com.smartstock.ui.*;
import com.smartstock.ui.panels.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises actual Swing controls and their listeners on the EDT, using a virtual display in CI.
 */
class DesktopWorkflowTest {
  @TempDir Path temp;
  AppContext ctx;
  LoginFrame login;
  MainDashboard main;

  static <T> T edt(Callable<T> task) throws Exception {
    FutureTask<T> f = new FutureTask<>(task);
    SwingUtilities.invokeAndWait(f);
    return f.get();
  }

  static void run(Runnable task) throws Exception {
    edt(
        () -> {
          task.run();
          return null;
        });
  }

  static void waitFor(BooleanSupplier condition) throws Exception {
    long end = System.currentTimeMillis() + 12000;
    while (!condition.getAsBoolean()) {
      if (System.currentTimeMillis() > end) fail("Timed out waiting for Swing state");
      Thread.sleep(50);
    }
  }

  static <T extends Component> T find(Container root, Class<T> type, Predicate<T> predicate) {
    for (Component c : root.getComponents()) {
      if (type.isInstance(c) && predicate.test(type.cast(c))) return type.cast(c);
      if (c instanceof Container nested) {
        T found = find(nested, type, predicate);
        if (found != null) return found;
      }
    }
    return null;
  }

  static Component named(Container root, String name) {
    return find(root, Component.class, c -> name.equals(c.getName()) && c.isShowing());
  }

  static Window window(String name) {
    return Arrays.stream(Window.getWindows())
        .filter(
            w ->
                w.isShowing()
                    && (name.equals(w.getName())
                        || (w instanceof Dialog d && name.equals(d.getTitle()))))
        .findFirst()
        .orElse(null);
  }

  void click(Container root, String name) throws Exception {
    Component c =
        edt(
            () -> {
              Component found = named(root, name);
              return found != null
                  ? found
                  : find(root, JButton.class, b -> name.equals(b.getText()) && b.isShowing());
            });
    assertNotNull(c, "Missing visible control: " + name);
    assertTrue(c instanceof AbstractButton);
    SwingUtilities.invokeLater(() -> ((AbstractButton) c).doClick());
    Thread.sleep(150);
  }

  void set(Container root, String name, String text) throws Exception {
    run(
        () -> {
          Component c = named(root, name);
          assertNotNull(c, "Missing field " + name);
          ((JTextComponent) c).setText(text);
        });
  }

  void awaitWindow(String name) throws Exception {
    waitFor(() -> window(name) != null);
  }

  JTable table(Container root) throws Exception {
    return edt(() -> find(root, JTable.class, Component::isShowing));
  }

  void selectByName(JTable table, String text) throws Exception {
    run(
        () -> {
          for (int row = 0; row < table.getRowCount(); row++)
            for (int col = 0; col < table.getColumnCount(); col++)
              if (text.equals(String.valueOf(table.getValueAt(row, col)))) {
                table.setRowSelectionInterval(row, row);
                return;
              }
          fail("Row not found: " + text);
        });
  }

  void closeMessage() throws Exception {
    awaitWindow("SmartStock");
    click((Container) window("SmartStock"), "OK");
  }

  void confirmYes() throws Exception {
    awaitWindow("Confirm action");
    Window w = window("Confirm action");
    JButton yes = edt(() -> find(w, JButton.class, b -> b.getText().equals("Yes")));
    assertNotNull(yes);
    SwingUtilities.invokeLater(yes::doClick);
    Thread.sleep(150);
  }

  void screenshot(Window w, String name) throws Exception {
    run(
        () -> {
          try {
            Path folder = Path.of("target/gui-screenshots");
            Files.createDirectories(folder);
            BufferedImage image =
                new BufferedImage(w.getWidth(), w.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            w.paint(g);
            g.dispose();
            ImageIO.write(image, "png", folder.resolve(name + ".png").toFile());
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  @BeforeEach
  void setup() throws Exception {
    Assumptions.assumeFalse(
        GraphicsEnvironment.isHeadless(), "Swing test requires display; use xvfb-run on Linux");
    DatabaseConnection db = new DatabaseConnection(temp.resolve("gui.db"));
    db.initialize();
    ctx = new AppContext(db);
    run(
        () -> {
          Theme.install();
          login = new LoginFrame(ctx);
          login.setVisible(true);
        });
  }

  @AfterEach
  void cleanup() throws Exception {
    run(
        () -> {
          for (Window w : Window.getWindows()) w.dispose();
        });
  }

  @Test
  @Timeout(120)
  void completeDesktopWorkflow() throws Exception {
    screenshot(login, "01-login");
    click(login, "Sign in");
    assertNotNull(edt(() -> find(login, JLabel.class, l -> l.getText().contains("Please enter"))));
    set(login, "Username", "garvit");
    set(login, "Password", "incorrect");
    click(login, "Sign in");
    waitFor(
        () -> {
          try {
            return edt(
                () ->
                    find(login, JLabel.class, l -> l.getText().contains("Invalid username"))
                        != null);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
    set(login, "Password", "garvit@123");
    click(login, "Sign in");
    awaitWindow("main-dashboard");
    main = (MainDashboard) window("main-dashboard");
    run(() -> assertFalse(login.isShowing()));
    screenshot(main, "02-dashboard");
    // Every navigation page is real and refreshable.
    for (String page :
        new String[] {
          "Products",
          "Categories",
          "Suppliers",
          "Stock Management",
          "Sales & Billing",
          "Transactions",
          "Reports",
          "Settings",
          "Dashboard"
        }) {
      click(main, "nav-" + page);
    }
    // Category create/edit/delete through modal forms.
    click(main, "nav-Categories");
    click(main, "Add Category");
    awaitWindow("Add Category");
    set((Container) window("Add Category"), "Category Name", "GUI Category");
    click((Container) window("Add Category"), "Save");
    waitFor(() -> window("Add Category") == null);
    assertTrue(ctx.categories.findAll().stream().anyMatch(c -> c.getName().equals("GUI Category")));
    selectByName(table(main), "GUI Category");
    click(main, "Edit Category");
    awaitWindow("Edit Category");
    set((Container) window("Edit Category"), "Category Name", "GUI Updated");
    click((Container) window("Edit Category"), "Save");
    waitFor(() -> window("Edit Category") == null);
    selectByName(table(main), "GUI Updated");
    click(main, "Delete Category");
    confirmYes();
    assertFalse(ctx.categories.findAll().stream().anyMatch(c -> c.getName().equals("GUI Updated")));
    // Supplier create/edit/delete.
    click(main, "nav-Suppliers");
    click(main, "Add Supplier");
    awaitWindow("Add Supplier");
    set((Container) window("Add Supplier"), "Supplier Name", "GUI Supply");
    set((Container) window("Add Supplier"), "Email", "gui@example.com");
    click((Container) window("Add Supplier"), "Save");
    waitFor(() -> window("Add Supplier") == null);
    selectByName(table(main), "GUI Supply");
    click(main, "Edit Supplier");
    awaitWindow("Edit Supplier");
    set((Container) window("Edit Supplier"), "Contact Person", "GUI Person");
    click((Container) window("Edit Supplier"), "Save");
    waitFor(() -> window("Edit Supplier") == null);
    assertTrue(
        ctx.suppliers.findAll().stream().anyMatch(s -> s.getContactPerson().equals("GUI Person")));
    selectByName(table(main), "GUI Supply");
    click(main, "Delete Supplier");
    confirmYes();
    // Product create and edit, search and status filtering.
    click(main, "nav-Products");
    click(main, "Add Product");
    awaitWindow("Add Product");
    Container editor = (Container) window("Add Product");
    set(editor, "Product Name", "GUI Product");
    set(editor, "Purchase Price", "10.00");
    set(editor, "Selling Price", "15.00");
    set(editor, "Quantity", "10");
    click(editor, "Save");
    waitFor(() -> window("Add Product") == null);
    Product product = ctx.products.findAll("GUI Product").get(0);
    selectByName(table(main), "GUI Product");
    click(main, "Edit Selected");
    awaitWindow("Edit Product");
    set((Container) window("Edit Product"), "Minimum Stock", "3");
    click((Container) window("Edit Product"), "Save");
    waitFor(() -> window("Edit Product") == null);
    assertEquals(3, ctx.products.findById(product.getId()).getMinStock());
    set(main, "Search products…", "GUI Product");
    assertEquals(1, edt(() -> tableUnsafe(main).getRowCount()));
    set(main, "Search products…", "");
    screenshot(main, "03-products");
    // Actual stock form saves and prevents negative balance.
    click(main, "nav-Stock Management");
    run(
        () -> {
          JComboBox<?> box =
              find(
                  main,
                  JComboBox.class,
                  b -> b.isShowing() && b.getItemCount() > 0 && b.getItemAt(0) instanceof Product);
          for (int i = 0; i < box.getItemCount(); i++)
            if (((Product) box.getItemAt(i)).getId() == product.getId()) box.setSelectedIndex(i);
        });
    set(main, "Movement Quantity", "4");
    click(main, "Save Movement");
    closeMessage();
    assertEquals(14, ctx.products.findById(product.getId()).getQuantity());
    run(
        () -> {
          JComboBox<?> op =
              find(main, JComboBox.class, b -> b.isShowing() && b.getItemCount() == 2);
          op.setSelectedIndex(1);
        });
    set(main, "Movement Quantity", "2");
    click(main, "Save Movement");
    closeMessage();
    assertEquals(12, ctx.products.findById(product.getId()).getQuantity());
    set(main, "Movement Quantity", "999");
    click(main, "Save Movement");
    awaitWindow("Please check");
    JButton ok =
        edt(
            () ->
                find(
                    (Container) window("Please check"),
                    JButton.class,
                    b -> b.getText().equals("OK")));
    SwingUtilities.invokeLater(ok::doClick);
    Thread.sleep(150);
    assertEquals(12, ctx.products.findById(product.getId()).getQuantity());
    // Build a cart and complete a real sale, then inspect the generated invoice.
    click(main, "nav-Sales & Billing");
    run(
        () -> {
          JComboBox<?> box = find(main, JComboBox.class, b -> b.isShowing());
          for (int i = 0; i < box.getItemCount(); i++)
            if (((Product) box.getItemAt(i)).getId() == product.getId()) box.setSelectedIndex(i);
        });
    set(main, "Sale Quantity", "2");
    click(main, "Add to Cart");
    set(main, "Discount Amount", "1.00");
    screenshot(main, "04-billing");
    click(main, "Complete Sale");
    confirmYes();
    awaitWindow("invoice-dialog");
    assertEquals(10, ctx.products.findById(product.getId()).getQuantity());
    assertEquals(2900, ctx.sales.findAll().get(0).getTotalCents());
    screenshot(window("invoice-dialog"), "05-invoice");
    click((Container) window("invoice-dialog"), "Close");
    click(main, "nav-Transactions");
    assertTrue(edt(() -> tableUnsafe(main).getRowCount()) > 8);
    screenshot(main, "06-transactions");
    // Export using the actual JFileChooser and button listener.
    click(main, "nav-Reports");
    screenshot(main, "07-reports");
    click(main, "Export CSV");
    waitFor(
        () ->
            Arrays.stream(Window.getWindows())
                .anyMatch(w -> w.isShowing() && find(w, JFileChooser.class, c -> true) != null));
    JFileChooser chooser =
        edt(
            () -> {
              for (Window w : Window.getWindows())
                if (w.isShowing()) {
                  JFileChooser c = find(w, JFileChooser.class, x -> true);
                  if (c != null) return c;
                }
              return null;
            });
    Path csv = temp.resolve("gui-report.csv");
    run(
        () -> {
          chooser.setSelectedFile(csv.toFile());
          chooser.approveSelection();
        });
    closeMessage();
    assertTrue(Files.readString(csv).contains("GUI Product"));
    click(main, "nav-Products");
    selectByName(table(main), "GUI Product");
    click(main, "Delete Selected");
    confirmYes();
    assertTrue(ctx.products.findAll("GUI Product").isEmpty());
    click(main, "Logout");
    confirmYes();
    awaitWindow("login-frame");
    assertFalse(main.isShowing());
    assertTrue(((LoginFrame) window("login-frame")).isShowing());
  }

  @Test
  @Timeout(45)
  void minimumWindowSizeAndNumericSorting() throws Exception {
    Admin admin = ctx.auth.login("garvit", "garvit@123".toCharArray());
    run(
        () -> {
          login.dispose();
          main = new MainDashboard(ctx, admin);
          main.setExtendedState(JFrame.NORMAL);
          main.setSize(1100, 700);
          main.setVisible(true);
        });
    screenshot(main, "08-dashboard-minimum-size");
    click(main, "nav-Products");
    run(
        () -> {
          JTable t = tableUnsafe(main);
          t.getRowSorter().toggleSortOrder(5);
          java.math.BigDecimal previous = java.math.BigDecimal.ZERO;
          for (int r = 0; r < t.getRowCount(); r++) {
            java.math.BigDecimal current = (java.math.BigDecimal) t.getValueAt(r, 5);
            assertTrue(current.compareTo(previous) >= 0, "Prices must sort numerically");
            previous = current;
          }
        });
    click(main, "nav-Sales & Billing");
    screenshot(main, "09-billing-minimum-size");
    for (String page :
        new String[] {
          "Categories", "Suppliers", "Stock Management", "Transactions", "Reports", "Settings"
        }) {
      click(main, "nav-" + page);
      run(() -> assertTrue(main.isShowing()));
    }
  }

  @Test
  @Timeout(30)
  void invoiceCanBeRenderedByJavaPrinting() throws Exception {
    Product p = ctx.products.findById(1);
    Sale sale =
        ctx.billing.checkout(
            java.util.List.of(new SaleItem(p.getId(), p.getName(), 1, p.getSellingCents())));
    JTextArea area = edt(() -> new JTextArea(com.smartstock.util.InvoiceFormatter.text(sale)));
    java.awt.print.Printable printable = edt(() -> area.getPrintable(null, null));
    BufferedImage page = new BufferedImage(850, 1100, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = page.createGraphics();
    try {
      assertEquals(
          java.awt.print.Printable.PAGE_EXISTS,
          printable.print(graphics, new java.awt.print.PageFormat(), 0));
      assertEquals(
          java.awt.print.Printable.NO_SUCH_PAGE,
          printable.print(graphics, new java.awt.print.PageFormat(), 999));
    } finally {
      graphics.dispose();
    }
  }

  private static JTable tableUnsafe(Container root) {
    return find(root, JTable.class, Component::isShowing);
  }
}
