package com.smartstock.model;

public class SaleItem {
  private final int productId;
  private final String productName;
  private final int quantity;
  private final long unitCents;

  public SaleItem(int productId, String productName, int quantity, long unitCents) {
    this.productId = productId;
    this.productName = productName;
    this.quantity = quantity;
    this.unitCents = unitCents;
  }

  public int getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public int getQuantity() {
    return quantity;
  }

  public long getUnitCents() {
    return unitCents;
  }

  public long getTotalCents() {
    return Math.multiplyExact(quantity, unitCents);
  }
}
