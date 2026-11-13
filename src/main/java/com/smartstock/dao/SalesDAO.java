package com.smartstock.dao;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.model.*;
import com.smartstock.util.AppException;
import java.sql.*;
import java.time.*;
import java.util.*;

public class SalesDAO {
  private final DatabaseConnection db;

  public SalesDAO(DatabaseConnection db) {
    this.db = db;
  }

  public List<Sale> findAll() {
    List<Sale> rows = new ArrayList<>();
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM sales ORDER BY id DESC");
        ResultSet r = p.executeQuery()) {
      while (r.next()) rows.add(map(c, r));
      return rows;
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  public Sale findById(int id) {
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM sales WHERE id=?")) {
      p.setInt(1, id);
      try (ResultSet r = p.executeQuery()) {
        if (!r.next()) throw new AppException("Invoice not found.");
        return map(c, r);
      }
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  private Sale map(Connection c, ResultSet r) throws SQLException {
    List<SaleItem> items = new ArrayList<>();
    int id = r.getInt("id");
    try (PreparedStatement p =
        c.prepareStatement("SELECT * FROM sale_items WHERE sale_id=? ORDER BY id")) {
      p.setInt(1, id);
      try (ResultSet x = p.executeQuery()) {
        while (x.next())
          items.add(
              new SaleItem(
                  x.getInt("product_id"),
                  x.getString("product_name"),
                  x.getInt("quantity"),
                  x.getLong("unit_cents")));
      }
    }
    return new Sale(
        id,
        r.getString("invoice_number"),
        LocalDateTime.parse(r.getString("created_at")),
        r.getLong("subtotal_cents"),
        r.getLong("discount_cents"),
        items);
  }
}
