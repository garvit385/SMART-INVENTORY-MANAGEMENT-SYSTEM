package com.smartstock.model;

public class Admin extends User {
  public Admin(int id, String username, String displayName) {
    super(id, username, displayName);
  }

  @Override
  public String getRole() {
    return "Administrator";
  }
}
