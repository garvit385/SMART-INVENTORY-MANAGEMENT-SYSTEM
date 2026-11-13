package com.smartstock.dao;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.model.Supplier;
import com.smartstock.util.ValidationUtil;
import java.sql.*;
import java.util.*;

public class SupplierDAO implements CrudDAO<Supplier> {
  private final DatabaseConnection db;

  public SupplierDAO(DatabaseConnection db) {
    this.db = db;
  }

  @Override
  public List<Supplier> findAll() {
    List<Supplier> rows = new ArrayList<>();
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM suppliers ORDER BY name");
        ResultSet r = p.executeQuery()) {
      while (r.next())
        rows.add(
            new Supplier(
                r.getInt("id"),
                r.getString("name"),
                r.getString("contact_person"),
                r.getString("phone"),
                r.getString("email"),
                r.getString("address")));
      return rows;
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  @Override
  public void save(Supplier v) {
    String name = ValidationUtil.required(v.getName(), "Supplier name");
    ValidationUtil.contact(v.getPhone(), v.getEmail());
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement(
                v.getId() == 0
                    ? "INSERT INTO suppliers(name,contact_person,phone,email,address)"
                        + " VALUES(?,?,?,?,?)"
                    : "UPDATE suppliers SET name=?,contact_person=?,phone=?,email=?,address=? WHERE"
                        + " id=?")) {
      p.setString(1, name);
      p.setString(2, v.getContactPerson().trim());
      p.setString(3, v.getPhone().trim());
      p.setString(4, v.getEmail().trim());
      p.setString(5, v.getAddress().trim());
      if (v.getId() != 0) p.setInt(6, v.getId());
      Sql.requireUpdated(p.executeUpdate());
      if (v.getId() == 0) v.setId(Sql.lastId(c));
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  @Override
  public void delete(int id) {
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement("DELETE FROM suppliers WHERE id=?")) {
      p.setInt(1, id);
      Sql.requireUpdated(p.executeUpdate());
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }
}
