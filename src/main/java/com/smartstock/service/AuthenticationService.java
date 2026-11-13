package com.smartstock.service;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.dao.Sql;
import com.smartstock.model.Admin;
import com.smartstock.util.*;
import java.sql.*;
import java.util.Arrays;

public class AuthenticationService {
  private final DatabaseConnection db;

  public AuthenticationService(DatabaseConnection db) {
    this.db = db;
  }

  public Admin login(String username, char[] password) {
    try {
      String user = ValidationUtil.required(username, "Username");
      if (password.length == 0) throw new AppException("Password is required.");
      try (Connection c = db.open();
          PreparedStatement p = c.prepareStatement("SELECT * FROM admins WHERE username=?")) {
        p.setString(1, user);
        try (ResultSet r = p.executeQuery()) {
          if (r.next() && PasswordUtil.verify(password, r.getString("password_hash")))
            return new Admin(r.getInt("id"), r.getString("username"), r.getString("display_name"));
        }
        throw new AppException("Invalid username or password.");
      } catch (SQLException e) {
        throw Sql.error(e);
      }
    } finally {
      Arrays.fill(password, '\0');
    }
  }

  public void changePassword(Admin admin, char[] current, char[] replacement) {
    try {
      if (replacement.length < 8)
        throw new AppException("New password must contain at least 8 characters.");
      login(admin.getUsername(), current);
      try (Connection c = db.open();
          PreparedStatement p =
              c.prepareStatement("UPDATE admins SET password_hash=? WHERE id=?")) {
        p.setString(1, PasswordUtil.hash(replacement));
        p.setInt(2, admin.getId());
        Sql.requireUpdated(p.executeUpdate());
      } catch (SQLException e) {
        throw Sql.error(e);
      }
    } finally {
      Arrays.fill(current, '\0');
      Arrays.fill(replacement, '\0');
    }
  }
}
