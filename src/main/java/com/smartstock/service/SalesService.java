package com.smartstock.service;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.dao.*;
import com.smartstock.model.*;
import com.smartstock.util.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SalesService {
  private final DatabaseConnection db;
  private final ProductDAO products;

  public SalesService(DatabaseConnection db) {
    this.db = db;
    products = new ProductDAO(db);
  }

  public Sale checkout(List<SaleItem> cart) {
    return checkout(cart, 0);
  }

  public Sale checkout(List<SaleItem> cart, long discount) {
    if (cart == null || cart.isEmpty())
      throw new AppException("Add at least one product to the cart.");
    if (discount < 0) throw new AppException("Discount cannot be negative.");
    // Merge duplicate products before validating stock. Never trust a stale UI price.
    Map<Integer, Integer> quantities = new LinkedHashMap<>();
    for (SaleItem item : cart) {
      if (item.getQuantity() <= 0) throw new AppException("Cart quantities must be positive.");
      try {
        quantities.merge(item.getProductId(), item.getQuantity(), Math::addExact);
      } catch (ArithmeticException e) {
        throw new AppException("Cart quantity is too large.");
      }
    }
    try (Connection c = db.open()) {
      c.setAutoCommit(false);
      try {
        List<SaleItem> finalItems = new ArrayList<>();
        long subtotal = 0;
        for (var entry : quantities.entrySet()) {
          Product v = products.findById(c, entry.getKey());
          int qty = entry.getValue();
          if (qty > v.getQuantity())
            throw new AppException(
                "Insufficient stock for " + v.getName() + ". Available: " + v.getQuantity() + ".");
          SaleItem item = new SaleItem(v.getId(), v.getName(), qty, v.getSellingCents());
          try {
            subtotal = Math.addExact(subtotal, item.getTotalCents());
          } catch (ArithmeticException e) {
            throw new AppException("Sale total is too large.");
          }
          finalItems.add(item);
        }
        if (discount > subtotal) throw new AppException("Discount cannot exceed the subtotal.");
        LocalDateTime now = LocalDateTime.now();
        String invoice =
            "SS-"
                + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int saleId;
        try (PreparedStatement p =
            c.prepareStatement(
                "INSERT INTO"
                    + " sales(invoice_number,created_at,subtotal_cents,discount_cents,total_cents)"
                    + " VALUES(?,?,?,?,?)")) {
          p.setString(1, invoice);
          p.setString(2, now.toString());
          p.setLong(3, subtotal);
          p.setLong(4, discount);
          p.setLong(5, subtotal - discount);
          p.executeUpdate();
          saleId = Sql.lastId(c);
        }
        for (SaleItem item : finalItems) {
          Product v = products.findById(c, item.getProductId());
          int after = v.getQuantity() - item.getQuantity();
          try (PreparedStatement p =
              c.prepareStatement("UPDATE products SET quantity=? WHERE id=? AND quantity=?")) {
            p.setInt(1, after);
            p.setInt(2, v.getId());
            p.setInt(3, v.getQuantity());
            Sql.requireUpdated(p.executeUpdate());
          }
          try (PreparedStatement p =
              c.prepareStatement(
                  "INSERT INTO"
                      + " sale_items(sale_id,product_id,product_name,quantity,unit_cents,total_cents)"
                      + " VALUES(?,?,?,?,?,?)")) {
            p.setInt(1, saleId);
            p.setInt(2, v.getId());
            p.setString(3, v.getName());
            p.setInt(4, item.getQuantity());
            p.setLong(5, item.getUnitCents());
            p.setLong(6, item.getTotalCents());
            p.executeUpdate();
          }
          TransactionDAO.record(
              c,
              v.getId(),
              v.getName(),
              "SALE",
              item.getQuantity(),
              v.getQuantity(),
              after,
              invoice);
        }
        c.commit();
        return new Sale(saleId, invoice, now, subtotal, discount, finalItems);
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }
}
