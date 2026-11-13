package com.smartstock.model;

import java.time.LocalDateTime;
import java.util.*;

public class Sale {
  private final int id;
  private final String invoiceNumber;
  private final LocalDateTime createdAt;
  private final long subtotalCents, discountCents;
  private final List<SaleItem> items;

  public Sale(
      int id,
      String number,
      LocalDateTime date,
      long subtotal,
      long discount,
      List<SaleItem> items) {
    this.id = id;
    invoiceNumber = number;
    createdAt = date;
    subtotalCents = subtotal;
    discountCents = discount;
    this.items = List.copyOf(items);
  }

  public int getId() {
    return id;
  }

  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public long getSubtotalCents() {
    return subtotalCents;
  }

  public long getDiscountCents() {
    return discountCents;
  }

  public long getTotalCents() {
    return subtotalCents - discountCents;
  }

  public List<SaleItem> getItems() {
    return items;
  }
}
