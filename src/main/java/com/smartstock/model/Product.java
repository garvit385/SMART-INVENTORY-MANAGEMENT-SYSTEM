package com.smartstock.model;

public class Product {
  private int id, categoryId, supplierId, quantity, minStock;
  private String name, categoryName, supplierName, description;
  private long purchaseCents, sellingCents;

  public Product(
      int id,
      String name,
      int categoryId,
      int supplierId,
      int quantity,
      long purchaseCents,
      long sellingCents,
      int minStock,
      String description) {
    this.id = id;
    this.name = name;
    this.categoryId = categoryId;
    this.supplierId = supplierId;
    this.quantity = quantity;
    this.purchaseCents = purchaseCents;
    this.sellingCents = sellingCents;
    this.minStock = minStock;
    this.description = description;
  }

  public int getId() {
    return id;
  }

  public void setId(int v) {
    id = v;
  }

  public String getName() {
    return name;
  }

  public void setName(String v) {
    name = v;
  }

  public int getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(int v) {
    categoryId = v;
  }

  public int getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(int v) {
    supplierId = v;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int v) {
    quantity = v;
  }

  public int getMinStock() {
    return minStock;
  }

  public void setMinStock(int v) {
    minStock = v;
  }

  public long getPurchaseCents() {
    return purchaseCents;
  }

  public void setPurchaseCents(long v) {
    purchaseCents = v;
  }

  public long getSellingCents() {
    return sellingCents;
  }

  public void setSellingCents(long v) {
    sellingCents = v;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String v) {
    categoryName = v;
  }

  public String getSupplierName() {
    return supplierName;
  }

  public void setSupplierName(String v) {
    supplierName = v;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String v) {
    description = v;
  }

  public String getStatus() {
    return quantity == 0 ? "Out of stock" : quantity < minStock ? "Low stock" : "In stock";
  }

  @Override
  public String toString() {
    return name + "  [" + quantity + " available]";
  }
}
