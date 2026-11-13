package com.smartstock.config;

import com.smartstock.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;

public class DatabaseConnection {
  private final Path path;

  public DatabaseConnection() {
    this(defaultPath());
  }

  public DatabaseConnection(Path path) {
    this.path = path.toAbsolutePath();
  }

  private static Path defaultPath() {
    String override = System.getProperty("smartstock.db");
    if (override != null) return Path.of(override);
    String os = System.getProperty("os.name").toLowerCase();
    String base =
        os.contains("win") ? System.getenv("LOCALAPPDATA") : System.getenv("XDG_DATA_HOME");
    if (base == null || base.isBlank())
      base =
          Path.of(
                  System.getProperty("user.home"),
                  os.contains("mac") ? "Library/Application Support" : ".local/share")
              .toString();
    return Path.of(base, "SmartStock", "smart_inventory.db");
  }

  public Path getPath() {
    return path;
  }

  public Connection open() throws SQLException {
    Connection c = DriverManager.getConnection("jdbc:sqlite:" + path);
    try (Statement s = c.createStatement()) {
      s.execute("PRAGMA foreign_keys=ON");
      s.execute("PRAGMA busy_timeout=5000");
    }
    return c;
  }

  public void initialize() {
    try {
      Files.createDirectories(path.getParent());
      try (Connection c = open();
          InputStream in = getClass().getResourceAsStream("/schema.sql")) {
        if (in == null) throw new IOException("Missing schema.sql resource");
        String sql =
            new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)--.*$", "");
        try (Statement s = c.createStatement()) {
          for (String part : sql.split(";")) if (!part.isBlank()) s.execute(part);
        }
        c.setAutoCommit(false);
        try {
          try (PreparedStatement q = c.prepareStatement("SELECT COUNT(*) FROM admins");
              ResultSet r = q.executeQuery()) {
            if (r.next() && r.getInt(1) == 0)
              try (PreparedStatement p =
                  c.prepareStatement(
                      "INSERT INTO admins(username,display_name,password_hash) VALUES(?,?,?)")) {
                p.setString(1, "garvit");
                p.setString(2, "Demo Administrator");
                p.setString(3, PasswordUtil.hash("garvit@123".toCharArray()));
                p.executeUpdate();
              }
          }
          try (PreparedStatement q =
                  c.prepareStatement("SELECT id, username, password_hash FROM admins ORDER BY id")) {
            try (ResultSet r = q.executeQuery()) {
              while (r.next()) {
                String username = r.getString("username");
                String hash = r.getString("password_hash");
                if ("admin".equalsIgnoreCase(username)) {
                  try (PreparedStatement p =
                      c.prepareStatement(
                          "UPDATE admins SET username=?,password_hash=? WHERE id=?")) {
                    p.setString(1, "garvit");
                    p.setString(2, PasswordUtil.hash("garvit@123".toCharArray()));
                    p.setInt(3, r.getInt("id"));
                    p.executeUpdate();
                  }
                } else if ("garvit".equalsIgnoreCase(username)
                    && !PasswordUtil.verify("garvit@123".toCharArray(), hash)) {
                  try (PreparedStatement p =
                      c.prepareStatement("UPDATE admins SET password_hash=? WHERE id=?")) {
                    p.setString(1, PasswordUtil.hash("garvit@123".toCharArray()));
                    p.setInt(2, r.getInt("id"));
                    p.executeUpdate();
                  }
                }
              }
            }
          }
          // Seed only an entirely empty inventory; never replenish user-deleted records.
          try (PreparedStatement q =
                  c.prepareStatement(
                      "SELECT (SELECT COUNT(*) FROM categories)+(SELECT COUNT(*) FROM"
                          + " suppliers)+(SELECT COUNT(*) FROM products)+(SELECT COUNT(*) FROM"
                          + " stock_transactions)");
              ResultSet r = q.executeQuery()) {
            if (r.next() && r.getInt(1) == 0) seed(c);
          }
          c.commit();
        } catch (Exception e) {
          c.rollback();
          throw e;
        }
      }
    } catch (Exception e) {
      throw new AppException(
          "Cannot initialize database at " + path + ". Check folder permissions.", e);
    }
  }

  private void seed(Connection c) throws SQLException {
    try (PreparedStatement p = c.prepareStatement("INSERT INTO categories(name) VALUES(?)")) {
      for (String name :
          new String[] {"Electronics", "Stationery", "Grocery", "Clothing", "Accessories"}) {
        p.setString(1, name);
        p.executeUpdate();
      }
    }
    try (PreparedStatement p =
        c.prepareStatement(
            "INSERT INTO suppliers(name,contact_person,phone,email,address) VALUES(?,?,?,?,?)")) {
      String[][] rows = {
        {"Nova Electronics", "Arjun Mehta", "+91 98765 43210", "orders@example.com", "Bengaluru"},
        {"Paper & Co.", "Priya Shah", "+91 98765 43211", "paper@example.com", "Mumbai"},
        {"Everyday Wholesale", "Ravi Kumar", "+91 98765 43212", "wholesale@example.com", "Delhi"}
      };
      for (String[] row : rows) {
        for (int i = 0; i < row.length; i++) p.setString(i + 1, row[i]);
        p.executeUpdate();
      }
    }
    String[] names = {
      "Wireless Mouse",
      "USB-C Hub",
      "A5 Notebook",
      "Ballpoint Pen Pack",
      "Arabica Coffee 250g",
      "Cotton T-Shirt",
      "Laptop Sleeve",
      "HDMI Cable"
    };
    int[] cats = {1, 1, 2, 2, 3, 4, 5, 1},
        supp = {1, 1, 2, 2, 3, 3, 1, 1},
        qty = {48, 6, 120, 8, 32, 24, 0, 65},
        min = {10, 10, 20, 15, 10, 8, 5, 12};
    long[] buy = {65000, 120000, 9000, 6000, 28000, 30000, 45000, 18000},
        sell = {99900, 189900, 14900, 9900, 39900, 59900, 79900, 29900};
    for (int i = 0; i < names.length; i++) {
      try (PreparedStatement p =
          c.prepareStatement(
              "INSERT INTO"
                  + " products(id,name,category_id,supplier_id,quantity,purchase_cents,selling_cents,min_stock,description)"
                  + " VALUES(?,?,?,?,?,?,?,?,?)")) {
        p.setInt(1, i + 1);
        p.setString(2, names[i]);
        p.setInt(3, cats[i]);
        p.setInt(4, supp[i]);
        p.setInt(5, qty[i]);
        p.setLong(6, buy[i]);
        p.setLong(7, sell[i]);
        p.setInt(8, min[i]);
        p.setString(9, "Demonstration inventory item");
        p.executeUpdate();
      }
      try (PreparedStatement p =
          c.prepareStatement(
              "INSERT INTO"
                  + " stock_transactions(product_id,product_name,type,quantity,previous_stock,new_stock,created_at,remarks)"
                  + " VALUES(?,?,'OPENING',?,0,?,?,?)")) {
        p.setInt(1, i + 1);
        p.setString(2, names[i]);
        p.setInt(3, qty[i]);
        p.setInt(4, qty[i]);
        p.setString(5, LocalDateTime.now().toString());
        p.setString(6, "Demo opening balance");
        p.executeUpdate();
      }
    }
  }
}
