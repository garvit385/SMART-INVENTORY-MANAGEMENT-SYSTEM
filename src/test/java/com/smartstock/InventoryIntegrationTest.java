package com.smartstock;

import static org.junit.jupiter.api.Assertions.*;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.model.*;
import com.smartstock.service.ReportService;
import com.smartstock.ui.AppContext;
import com.smartstock.util.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class InventoryIntegrationTest {
  @TempDir Path temp;
  AppContext ctx;

  @BeforeEach
  void init() {
    DatabaseConnection db = new DatabaseConnection(temp.resolve("smart_inventory.db"));
    db.initialize();
    ctx = new AppContext(db);
  }

  Product create(String name, int qty) {
    Product p = new Product(0, name, 1, 1, qty, 10000, 15000, 5, "Integration test item");
    ctx.inventory.saveProduct(p);
    return p;
  }

  long count(String table) throws SQLException {
    try (Connection c = ctx.db.open();
        PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM " + table);
        ResultSet r = p.executeQuery()) {
      return r.next() ? r.getLong(1) : 0;
    }
  }

  @Test
  void schemaAndDemoSeedAreIdempotent() throws Exception {
    assertEquals(8, ctx.products.findAll().size());
    assertEquals(5, ctx.categories.findAll().size());
    assertEquals(3, ctx.suppliers.findAll().size());
    assertEquals(8, ctx.transactions.findAll().size());
    ctx.db.initialize();
    assertEquals(8, ctx.products.findAll().size());
    try (Connection c = ctx.db.open();
        PreparedStatement p = c.prepareStatement("PRAGMA foreign_key_check");
        ResultSet r = p.executeQuery()) {
      assertFalse(r.next());
    }
  }

  @Test
  void loginSuccessAndPasswordHashing() throws Exception {
    Admin a = ctx.auth.login("garvit", "garvit@123".toCharArray());
    assertEquals("Administrator", a.getRole());
    try (Connection c = ctx.db.open();
        PreparedStatement p = c.prepareStatement("SELECT password_hash FROM admins");
        ResultSet r = p.executeQuery()) {
      assertTrue(r.next());
      String h = r.getString(1);
      assertFalse(h.contains("garvit@123"));
      assertTrue(PasswordUtil.verify("garvit@123".toCharArray(), h));
    }
  }

  @Test
  void invalidAndEmptyLoginAreRejected() {
    assertThrows(AppException.class, () -> ctx.auth.login("garvit", "wrong".toCharArray()));
    assertThrows(AppException.class, () -> ctx.auth.login("", "".toCharArray()));
    assertThrows(AppException.class, () -> ctx.auth.login("' OR 1=1 --", "anything".toCharArray()));
  }

  @Test
  void passwordsUseUniqueSalts() {
    assertNotEquals(
        PasswordUtil.hash("same".toCharArray()), PasswordUtil.hash("same".toCharArray()));
    assertFalse(PasswordUtil.verify("x".toCharArray(), "malformed"));
  }

  @Test
  void passwordCanBeChanged() {
    Admin a = ctx.auth.login("garvit", "garvit@123".toCharArray());
    ctx.auth.changePassword(a, "garvit@123".toCharArray(), "NewPass@456".toCharArray());
    assertThrows(AppException.class, () -> ctx.auth.login("garvit", "garvit@123".toCharArray()));
    assertNotNull(ctx.auth.login("garvit", "NewPass@456".toCharArray()));
    assertThrows(
        AppException.class,
        () -> ctx.auth.changePassword(a, "NewPass@456".toCharArray(), "short".toCharArray()));
  }

  @Test
  void categoryCrudAndDuplicates() {
    Category c = new Category(0, "Test Category");
    ctx.categories.save(c);
    assertTrue(c.getId() > 0);
    c.setName("Updated category");
    ctx.categories.save(c);
    assertTrue(
        ctx.categories.findAll().stream().anyMatch(x -> x.getName().equals("Updated category")));
    assertThrows(
        AppException.class, () -> ctx.categories.save(new Category(0, "updated CATEGORY")));
    ctx.categories.delete(c.getId());
    assertFalse(ctx.categories.findAll().stream().anyMatch(x -> x.getId() == c.getId()));
  }

  @Test
  void assignedCategoryCannotBeDeleted() {
    assertThrows(AppException.class, () -> ctx.categories.delete(1));
  }

  @Test
  void supplierCrudAndContactValidation() {
    Supplier s =
        new Supplier(0, "Test Supply", "Jane", "+91 99999 11111", "jane@example.com", "Street 1");
    ctx.suppliers.save(s);
    s.setContactPerson("John");
    ctx.suppliers.save(s);
    assertTrue(ctx.suppliers.findAll().stream().anyMatch(x -> x.getContactPerson().equals("John")));
    s.setEmail("not-email");
    assertThrows(AppException.class, () -> ctx.suppliers.save(s));
    s.setEmail("");
    s.setPhone("abc");
    assertThrows(AppException.class, () -> ctx.suppliers.save(s));
    ctx.suppliers.delete(s.getId());
  }

  @Test
  void assignedSupplierCannotBeDeleted() {
    assertThrows(AppException.class, () -> ctx.suppliers.delete(1));
  }

  @Test
  void productCreateEditDeleteAndAudit() {
    Product p = create("Test Product", 10);
    assertTrue(p.getId() > 0);
    assertEquals(10, ctx.products.findById(p.getId()).getQuantity());
    p.setName("Renamed Product");
    p.setQuantity(15);
    ctx.inventory.saveProduct(p);
    assertEquals("Renamed Product", ctx.products.findById(p.getId()).getName());
    assertTrue(
        ctx.transactions.findAll().stream()
            .anyMatch(
                t ->
                    t.getType().equals("ADJUSTMENT")
                        && t.getPreviousStock() == 10
                        && t.getNewStock() == 15));
    ctx.inventory.deleteProduct(p.getId());
    assertThrows(AppException.class, () -> ctx.products.findById(p.getId()));
    assertTrue(
        ctx.transactions.findAll().stream()
            .anyMatch(t -> t.getProductName().equals("Test Product")));
  }

  @Test
  void productInputAndDuplicateValidation() {
    Product p = create("Unique Product", 1);
    assertThrows(AppException.class, () -> create("unique PRODUCT", 2));
    p.setQuantity(-1);
    assertThrows(AppException.class, () -> ctx.inventory.saveProduct(p));
    p.setQuantity(1);
    p.setPurchaseCents(-1);
    assertThrows(AppException.class, () -> ctx.inventory.saveProduct(p));
    p.setPurchaseCents(100);
    p.setName(" ");
    assertThrows(AppException.class, () -> ctx.inventory.saveProduct(p));
  }

  @Test
  void searchTreatsSqlMetacharactersAsText() {
    create("Special 100% Item", 2);
    assertEquals(1, ctx.products.findAll("100%").size());
    assertEquals(0, ctx.products.findAll("' OR 1=1 --").size());
  }

  @Test
  void stockInOutAndAudit() {
    Product p = create("Stock Test", 10);
    ctx.inventory.stockIn(p.getId(), 7, "PO-123");
    ctx.inventory.stockOut(p.getId(), 4, "Issued");
    assertEquals(13, ctx.products.findById(p.getId()).getQuantity());
    var tx = ctx.transactions.findAll("Stock Test", "OUT", LocalDate.now(), LocalDate.now());
    assertEquals(1, tx.size());
    assertEquals(17, tx.get(0).getPreviousStock());
    assertEquals(13, tx.get(0).getNewStock());
  }

  @Test
  void invalidStockMovementLeavesNoTrace() {
    Product p = create("No negatives", 2);
    int n = ctx.transactions.findAll().size();
    assertThrows(AppException.class, () -> ctx.inventory.stockOut(p.getId(), 3, "fail"));
    assertThrows(AppException.class, () -> ctx.inventory.stockIn(p.getId(), 0, "fail"));
    assertEquals(2, ctx.products.findById(p.getId()).getQuantity());
    assertEquals(n, ctx.transactions.findAll().size());
  }

  @Test
  void transactionInsertFailureRollsBackStock() throws Exception {
    Product p = create("Rollback Stock", 10);
    try (Connection c = ctx.db.open();
        Statement s = c.createStatement()) {
      s.execute(
          "CREATE TRIGGER fail_audit BEFORE INSERT ON stock_transactions BEGIN SELECT RAISE(ABORT,"
              + " 'test audit failure'); END");
    }
    assertThrows(AppException.class, () -> ctx.inventory.stockIn(p.getId(), 5, "must rollback"));
    assertEquals(10, ctx.products.findById(p.getId()).getQuantity());
  }

  @Test
  void saleUsesDatabasePricesDiscountAndStock() throws Exception {
    Product p = create("Sale Item", 10);
    Sale s = ctx.billing.checkout(List.of(new SaleItem(p.getId(), "untrusted", 3, 1)), 500);
    assertEquals(45000, s.getSubtotalCents());
    assertEquals(44500, s.getTotalCents());
    assertEquals(7, ctx.products.findById(p.getId()).getQuantity());
    assertEquals(1, count("sales"));
    assertEquals(1, count("sale_items"));
    assertEquals(s.getInvoiceNumber(), ctx.sales.findById(s.getId()).getInvoiceNumber());
    assertTrue(InvoiceFormatter.text(s).contains("GRAND TOTAL INR 445.00"));
    assertEquals(1, ctx.transactions.findAll("Sale Item", "SALE", null, null).size());
  }

  @Test
  void duplicateCartProductsAreMergedBeforeStockValidation() {
    Product p = create("Duplicate Cart", 5);
    assertThrows(
        AppException.class,
        () ->
            ctx.billing.checkout(
                List.of(
                    new SaleItem(p.getId(), p.getName(), 3, 15000),
                    new SaleItem(p.getId(), p.getName(), 3, 15000))));
    assertEquals(5, ctx.products.findById(p.getId()).getQuantity());
  }

  @Test
  void validDuplicateCartProductsProduceSingleLine() {
    Product p = create("Merged", 5);
    Sale s =
        ctx.billing.checkout(
            List.of(new SaleItem(p.getId(), "", 1, 0), new SaleItem(p.getId(), "", 2, 0)));
    assertEquals(1, s.getItems().size());
    assertEquals(3, s.getItems().get(0).getQuantity());
    assertEquals(2, ctx.products.findById(p.getId()).getQuantity());
  }

  @Test
  void invalidSalesAreRejected() throws Exception {
    Product p = create("Invalid Sale", 2);
    assertThrows(AppException.class, () -> ctx.billing.checkout(List.of()));
    assertThrows(
        AppException.class, () -> ctx.billing.checkout(List.of(new SaleItem(p.getId(), "", 0, 0))));
    assertThrows(
        AppException.class,
        () -> ctx.billing.checkout(List.of(new SaleItem(p.getId(), "", 1, 0)), 999999));
    assertThrows(
        AppException.class,
        () -> ctx.billing.checkout(List.of(new SaleItem(p.getId(), "", 1, 0)), -1));
    assertEquals(0, count("sales"));
    assertEquals(2, ctx.products.findById(p.getId()).getQuantity());
  }

  @Test
  void saleFailureRollsBackEveryTable() throws Exception {
    Product a = create("Rollback A", 5), b = create("Rollback B", 5);
    long tx = count("stock_transactions");
    try (Connection c = ctx.db.open();
        Statement s = c.createStatement()) {
      s.execute(
          "CREATE TRIGGER fail_second BEFORE INSERT ON sale_items WHEN NEW.product_name='Rollback"
              + " B' BEGIN SELECT RAISE(ABORT,'simulated failure'); END");
    }
    assertThrows(
        AppException.class,
        () ->
            ctx.billing.checkout(
                List.of(new SaleItem(a.getId(), "", 2, 0), new SaleItem(b.getId(), "", 2, 0))));
    assertEquals(5, ctx.products.findById(a.getId()).getQuantity());
    assertEquals(5, ctx.products.findById(b.getId()).getQuantity());
    assertEquals(0, count("sales"));
    assertEquals(0, count("sale_items"));
    assertEquals(tx, count("stock_transactions"));
  }

  @Test
  void deleteSoldProductPreservesInvoice() {
    Product p = create("Historical item", 5);
    Sale sale = ctx.billing.checkout(List.of(new SaleItem(p.getId(), "", 1, 0)));
    ctx.inventory.deleteProduct(p.getId());
    assertEquals(
        "Historical item", ctx.sales.findById(sale.getId()).getItems().get(0).getProductName());
  }

  @Test
  void invoiceNumbersAreUnique() {
    Product p = create("Invoices", 10);
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < 4; i++)
      ids.add(ctx.billing.checkout(List.of(new SaleItem(p.getId(), "", 1, 0))).getInvoiceNumber());
    assertEquals(4, ids.size());
  }

  @Test
  void allReportsContainRealData() {
    for (String type : ReportService.TYPES) {
      var r = ctx.reports.generate(type);
      assertNotNull(r.summary());
      assertTrue(r.columns().length > 0);
    }
    assertEquals(8, ctx.reports.generate("Complete Inventory").rows().size());
    assertEquals(3, ctx.reports.generate("Low Stock").rows().size());
  }

  @Test
  void csvExportsRealRowsAndEscapesContent() throws Exception {
    create("Quoted, \"product\"", 3);
    Path out = temp.resolve("inventory.csv");
    CSVExporter.export(out, ctx.reports.generate("Complete Inventory"));
    String text = Files.readString(out);
    assertTrue(text.contains("\"Quoted, \"\"product\"\"\""));
    assertEquals(10, Files.readAllLines(out).size());
    var report =
        new ReportService.Report(
            new String[] {"Name"}, Collections.singletonList(new Object[] {"=SUM(A1)"}), "");
    CSVExporter.export(out, report);
    assertTrue(Files.readString(out).contains("'=SUM(A1)"));
    assertThrows(
        AppException.class, () -> CSVExporter.export(temp.resolve("missing/path.csv"), report));
  }

  @Test
  void restartingKeepsDataAndDoesNotReseed() {
    Product p = create("Persistent", 9);
    ctx.inventory.stockOut(p.getId(), 2, "persist");
    DatabaseConnection restarted = new DatabaseConnection(ctx.db.getPath());
    restarted.initialize();
    AppContext fresh = new AppContext(restarted);
    assertEquals(7, fresh.products.findById(p.getId()).getQuantity());
    assertEquals(9, fresh.products.findAll().size());
  }

  @Test
  void datesAndTypesFilterCorrectly() {
    Product p = create("Dates", 4);
    ctx.inventory.stockIn(p.getId(), 1, "today");
    assertEquals(
        1, ctx.transactions.findAll("Dates", "IN", LocalDate.now(), LocalDate.now()).size());
    assertTrue(
        ctx.transactions.findAll("Dates", "IN", LocalDate.now().plusDays(1), null).isEmpty());
    assertThrows(
        AppException.class,
        () -> ctx.transactions.findAll("", "All", LocalDate.now(), LocalDate.now().minusDays(1)));
  }

  @Test
  void monetaryParsingIsExact() {
    assertEquals(12345, ValidationUtil.cents("123.45"));
    assertEquals("0.01", ValidationUtil.money(1));
    assertThrows(AppException.class, () -> ValidationUtil.cents("1.234"));
    assertThrows(AppException.class, () -> ValidationUtil.cents("-1"));
    assertThrows(AppException.class, () -> ValidationUtil.integer("1.5", "Quantity"));
  }
}
