package com.smartstock.service;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.dao.*;
import com.smartstock.model.Product;
import com.smartstock.util.*;
import java.sql.*;

public class InventoryService {
  private final DatabaseConnection db;
  private final ProductDAO products;

  public InventoryService(DatabaseConnection db) {
    this.db = db;
    products = new ProductDAO(db);
  }

  public void saveProduct(Product v) {
    v.setName(ValidationUtil.required(v.getName(), "Product name"));
    ValidationUtil.nonNegative(v.getQuantity(), "Quantity");
    ValidationUtil.nonNegative(v.getMinStock(), "Minimum stock");
    if (v.getPurchaseCents() < 0
        || v.getSellingCents() < 0
        || v.getPurchaseCents() > 100_000_000_000L
        || v.getSellingCents() > 100_000_000_000L)
      throw new AppException("Prices must be non-negative and within the supported range.");
    if (v.getCategoryId() <= 0 || v.getSupplierId() <= 0)
      throw new AppException("Select a category and supplier.");
    boolean creating = v.getId() == 0;
    try (Connection c = db.open()) {
      c.setAutoCommit(false);
      try {
        int old = creating ? 0 : products.findById(c, v.getId()).getQuantity();
        String sql =
            creating
                ? "INSERT INTO"
                      + " products(name,category_id,supplier_id,quantity,purchase_cents,selling_cents,min_stock,description)"
                      + " VALUES(?,?,?,?,?,?,?,?)"
                : "UPDATE products SET"
                      + " name=?,category_id=?,supplier_id=?,quantity=?,purchase_cents=?,selling_cents=?,min_stock=?,description=?"
                      + " WHERE id=?";
        int id = v.getId();
        try (PreparedStatement p = c.prepareStatement(sql)) {
          p.setString(1, v.getName());
          p.setInt(2, v.getCategoryId());
          p.setInt(3, v.getSupplierId());
          p.setInt(4, v.getQuantity());
          p.setLong(5, v.getPurchaseCents());
          p.setLong(6, v.getSellingCents());
          p.setInt(7, v.getMinStock());
          p.setString(8, v.getDescription());
          if (!creating) p.setInt(9, id);
          Sql.requireUpdated(p.executeUpdate());
          if (creating) id = Sql.lastId(c);
        }
        if (creating || old != v.getQuantity())
          TransactionDAO.record(
              c,
              id,
              v.getName(),
              creating ? "OPENING" : "ADJUSTMENT",
              Math.abs(v.getQuantity() - old),
              old,
              v.getQuantity(),
              creating ? "Opening stock" : "Quantity adjusted in product editor");
        c.commit();
        v.setId(id);
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  public void deleteProduct(int id) {
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement("DELETE FROM products WHERE id=?")) {
      p.setInt(1, id);
      Sql.requireUpdated(p.executeUpdate());
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  public void stockIn(int id, int qty, String remarks) {
    change(id, qty, "IN", remarks);
  }

  public void stockOut(int id, int qty, String remarks) {
    change(id, qty, "OUT", remarks);
  }

  private void change(int id, int qty, String type, String remarks) {
    if (qty <= 0 || qty > 1_000_000_000)
      throw new AppException("Quantity must be a positive whole number up to 1,000,000,000.");
    try (Connection c = db.open()) {
      c.setAutoCommit(false);
      try {
        Product v = products.findById(c, id);
        long next = (long) v.getQuantity() + (type.equals("IN") ? qty : -qty);
        if (next < 0)
          throw new AppException("Insufficient stock. Available: " + v.getQuantity() + ".");
        if (next > 1_000_000_000)
          throw new AppException("Resulting stock exceeds the supported limit.");
        try (PreparedStatement p =
            c.prepareStatement("UPDATE products SET quantity=? WHERE id=? AND quantity=?")) {
          p.setInt(1, (int) next);
          p.setInt(2, id);
          p.setInt(3, v.getQuantity());
          Sql.requireUpdated(p.executeUpdate());
        }
        TransactionDAO.record(
            c,
            id,
            v.getName(),
            type,
            qty,
            v.getQuantity(),
            (int) next,
            remarks == null ? "" : remarks.trim());
        c.commit();
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }
}
