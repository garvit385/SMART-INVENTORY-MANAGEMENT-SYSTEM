package com.smartstock.dao;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.model.Category;
import com.smartstock.util.ValidationUtil;
import java.sql.*;
import java.util.*;

public class CategoryDAO implements CrudDAO<Category> {
  private final DatabaseConnection db;

  public CategoryDAO(DatabaseConnection db) {
    this.db = db;
  }

  @Override
  public List<Category> findAll() {
    List<Category> rows = new ArrayList<>();
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement("SELECT * FROM categories ORDER BY name");
        ResultSet r = p.executeQuery()) {
      while (r.next()) rows.add(new Category(r.getInt("id"), r.getString("name")));
      return rows;
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  @Override
  public void save(Category v) {
    String name = ValidationUtil.required(v.getName(), "Category name");
    try (Connection c = db.open();
        PreparedStatement p =
            c.prepareStatement(
                v.getId() == 0
                    ? "INSERT INTO categories(name) VALUES(?)"
                    : "UPDATE categories SET name=? WHERE id=?")) {
      p.setString(1, name);
      if (v.getId() != 0) p.setInt(2, v.getId());
      Sql.requireUpdated(p.executeUpdate());
      if (v.getId() == 0) v.setId(Sql.lastId(c));
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }

  @Override
  public void delete(int id) {
    try (Connection c = db.open();
        PreparedStatement p = c.prepareStatement("DELETE FROM categories WHERE id=?")) {
      p.setInt(1, id);
      Sql.requireUpdated(p.executeUpdate());
    } catch (SQLException e) {
      throw Sql.error(e);
    }
  }
}
