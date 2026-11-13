package com.smartstock.dao;

import com.smartstock.util.AppException;
import java.sql.*;

public final class Sql {
  private Sql() {}

  public static AppException error(SQLException e) {
    String m = e.getMessage();
    if (m != null && m.contains("UNIQUE"))
      return new AppException("A record with that name already exists.", e);
    if (m != null && m.contains("FOREIGN KEY"))
      return new AppException(
          "This record is linked to products. Reassign or delete those products first.", e);
    if (m != null && (m.contains("SQLITE_BUSY") || m.contains("SQLITE_LOCKED")))
      return new AppException("The database is busy. Please retry this operation.", e);
    return new AppException("Database operation failed. Check disk space and file permissions.", e);
  }

  public static int lastId(Connection c) throws SQLException {
    try (PreparedStatement p = c.prepareStatement("SELECT last_insert_rowid()");
        ResultSet r = p.executeQuery()) {
      return r.next() ? r.getInt(1) : 0;
    }
  }

  public static void requireUpdated(int count) {
    if (count != 1) throw new AppException("Record no longer exists. Refresh and try again.");
  }
}
