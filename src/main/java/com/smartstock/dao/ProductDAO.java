package com.smartstock.dao;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.model.Product;
import com.smartstock.util.*;
import java.sql.*;
import java.util.*;

/** Product reads. Writes are orchestrated by InventoryService to preserve the audit trail. */
public class ProductDAO {
  private final DatabaseConnection db;
  private static final String SELECT =
      "SELECT p.*,c.name category_name,s.name supplier_name FROM products p JOIN categories c ON"
          + " p.category_id=c.id JOIN suppliers s ON p.supplier_id=s.id";

  public ProductDAO(DatabaseConnection db) {
    this.db = db;
  }

  public List<Product> findAll() {
    return findAll("");
  }

  public List<Product> findAll(String search) {
    List<Product> rows = new ArrayList<>();
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement(SELECT + " WHERE instr(lower(p.name),lower(?))>0 ORDER BY p.name")) {
      p.setString(1, search);
      try (ResultSet r = p.executeQuery()) {
        while (r.next()) rows.add(map(r));
      }
      return rows;
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  public Product findById(int id) {
    try (Connection c = db.open()) {
      return findById(c, id);
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  public Product findById(Connection c, int id) throws SQLException {
    try (PreparedStatement p = c.prepareStatement(SELECT + " WHERE p.id=?")) {
      p.setInt(1, id);
      try (ResultSet r = p.executeQuery()) {
        if (!r.next()) throw new AppException("Product not found. Refresh and try again.");
        return map(r);
      }
    }
  }

  private Product map(ResultSet r) throws SQLException {
    Product v =
        new Product(
            r.getInt("id"),
            r.getString("name"),
            r.getInt("category_id"),
            r.getInt("supplier_id"),
            r.getInt("quantity"),
            r.getLong("purchase_cents"),
            r.getLong("selling_cents"),
            r.getInt("min_stock"),
            r.getString("description"));
    v.setCategoryName(r.getString("category_name"));
    v.setSupplierName(r.getString("supplier_name"));
    return v;
  }
}
