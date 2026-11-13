package com.smartstock.dao;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.model.StockTransaction;
import com.smartstock.util.AppException;
import java.sql.*;
import java.time.*;
import java.util.*;

public class TransactionDAO {
  private final DatabaseConnection db;

  public TransactionDAO(DatabaseConnection db) {
    this.db = db;
  }

  public List<StockTransaction> findAll() {
    return findAll("", "All", null, null);
  }

  public List<StockTransaction> findAll(String search, String type, LocalDate from, LocalDate to) {
    if (from != null && to != null && from.isAfter(to))
      throw new AppException("From date must not be after To date.");
    List<StockTransaction> rows = new ArrayList<>();
    String sql =
        "SELECT * FROM stock_transactions WHERE (instr(lower(product_name),lower(?))>0 OR"
            + " instr(lower(remarks),lower(?))>0) AND (?='All' OR type=?) AND (? IS NULL OR"
            + " created_at>=?) AND (? IS NULL OR created_at<?) ORDER BY id DESC";
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement(sql)) {
      p.setString(1, search);
      p.setString(2, search);
      p.setString(3, type);
      p.setString(4, type);
      String a = from == null ? null : from.atStartOfDay().toString(),
          b = to == null ? null : to.plusDays(1).atStartOfDay().toString();
      p.setString(5, a);
      p.setString(6, a);
      p.setString(7, b);
      p.setString(8, b);
      try (ResultSet r = p.executeQuery()) {
        while (r.next())
          rows.add(
              new StockTransaction(
                  r.getInt("id"),
                  r.getString("product_name"),
                  r.getString("type"),
                  r.getInt("quantity"),
                  r.getInt("previous_stock"),
                  r.getInt("new_stock"),
                  LocalDateTime.parse(r.getString("created_at")),
                  r.getString("remarks")));
      }
      return rows;
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  public static void record(
      Connection c,
      int productId,
      String name,
      String type,
      int quantity,
      int before,
      int after,
      String remarks)
      throws SQLException {
    try (PreparedStatement p =
        c.prepareStatement(
            "INSERT INTO"
                + " stock_transactions(product_id,product_name,type,quantity,previous_stock,new_stock,created_at,remarks)"
                + " VALUES(?,?,?,?,?,?,?,?)")) {
      p.setInt(1, productId);
      p.setString(2, name);
      p.setString(3, type);
      p.setInt(4, quantity);
      p.setInt(5, before);
      p.setInt(6, after);
      p.setString(7, LocalDateTime.now().toString());
      p.setString(8, remarks);
      p.executeUpdate();
    }
  }
}
