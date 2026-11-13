package com.smartstock.ui;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.dao.*;
import com.smartstock.service.*;

public final class AppContext {
  public final DatabaseConnection db;
  public final ProductDAO products;
  public final CategoryDAO categories;
  public final SupplierDAO suppliers;
  public final TransactionDAO transactions;
  public final SalesDAO sales;
  public final InventoryService inventory;
  public final SalesService billing;
  public final ReportService reports;
  public final AuthenticationService auth;

  public AppContext(DatabaseConnection db) {
    this.db = db;
    products = new ProductDAO(db);
    categories = new CategoryDAO(db);
    suppliers = new SupplierDAO(db);
    transactions = new TransactionDAO(db);
    sales = new SalesDAO(db);
    inventory = new InventoryService(db);
    billing = new SalesService(db);
    reports = new ReportService(db);
    auth = new AuthenticationService(db);
  }
}
