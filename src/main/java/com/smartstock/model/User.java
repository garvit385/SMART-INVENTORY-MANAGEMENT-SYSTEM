package com.smartstock.model;

public abstract class User {
  private final int id;
  private final String username;
  private String displayName;

  protected User(int id, String username, String displayName) {
    this.id = id;
    this.username = username;
    this.displayName = displayName;
  }

  public int getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String name) {
    displayName = name;
  }

  public abstract String getRole();
}
