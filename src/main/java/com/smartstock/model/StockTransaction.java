package com.smartstock.model;

import java.time.LocalDateTime;

public class StockTransaction {
  private final int id, quantity, previousStock, newStock;
  private final String productName, type, remarks;
  private final LocalDateTime createdAt;

  public StockTransaction(
      int id,
      String productName,
      String type,
      int quantity,
      int previousStock,
      int newStock,
      LocalDateTime createdAt,
      String remarks) {
    this.id = id;
    this.productName = productName;
    this.type = type;
    this.quantity = quantity;
    this.previousStock = previousStock;
    this.newStock = newStock;
    this.createdAt = createdAt;
    this.remarks = remarks;
  }

  public int getId() {
    return id;
  }

  public String getProductName() {
    return productName;
  }

  public String getType() {
    return type;
  }

  public int getQuantity() {
    return quantity;
  }

  public int getPreviousStock() {
    return previousStock;
  }

  public int getNewStock() {
    return newStock;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public String getRemarks() {
    return remarks;
  }
}
