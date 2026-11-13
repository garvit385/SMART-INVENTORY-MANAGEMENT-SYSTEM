package com.smartstock.dao;

import java.util.List;

public interface CrudDAO<T> {
  List<T> findAll();

  void save(T entity);

  void delete(int id);
}
