package com.smartstock.service;

import com.smartstock.config.DatabaseConnection;
import com.smartstock.dao.*;
import com.smartstock.model.*;
import com.smartstock.util.ValidationUtil;
import java.math.BigInteger;
import java.util.*;

public class ReportService {
  public static final String[] TYPES = {
    "Complete Inventory", "Low Stock", "Suppliers", "Stock Transactions", "Sales Summary"
  };

  public record Report(String[] columns, List<Object[]> rows, String summary) {}

  private final DatabaseConnection db;

  public ReportService(DatabaseConnection db) {
    this.db = db;
  }

  public Report generate(String type) {
    List<Object[]> rows = new ArrayList<>();
    if (type.equals(TYPES[0]) || type.equals(TYPES[1])) {
      long units = 0;
      BigInteger value = BigInteger.ZERO;
      for (Product p : new ProductDAO(db).findAll()) {
        if (type.equals(TYPES[1]) && !(p.getQuantity() < p.getMinStock() || p.getQuantity() == 0))
          continue;
        rows.add(
            new Object[] {
              p.getId(),
              p.getName(),
              p.getCategoryName(),
              p.getSupplierName(),
              p.getQuantity(),
              ValidationUtil.money(p.getPurchaseCents()),
              ValidationUtil.money(p.getSellingCents()),
              p.getMinStock(),
              p.getStatus()
            });
        units += p.getQuantity();
        value =
            value.add(
                BigInteger.valueOf(p.getQuantity())
                    .multiply(BigInteger.valueOf(p.getPurchaseCents())));
      }
      return new Report(
          new String[] {
            "ID",
            "Product",
            "Category",
            "Supplier",
            "Quantity",
            "Purchase (INR)",
            "Selling (INR)",
            "Minimum",
            "Status"
          },
          rows,
          rows.size()
              + " products  |  "
              + units
              + " units  |  Cost value INR "
              + ValidationUtil.money(value));
    }
    if (type.equals(TYPES[2])) {
      for (Supplier s : new SupplierDAO(db).findAll())
        rows.add(
            new Object[] {
              s.getId(),
              s.getName(),
              s.getContactPerson(),
              s.getPhone(),
              s.getEmail(),
              s.getAddress()
            });
      return new Report(
          new String[] {"ID", "Supplier", "Contact", "Phone", "Email", "Address"},
          rows,
          rows.size() + " suppliers");
    }
    if (type.equals(TYPES[3])) {
      for (StockTransaction t : new TransactionDAO(db).findAll())
        rows.add(
            new Object[] {
              t.getId(),
              t.getProductName(),
              t.getType(),
              t.getQuantity(),
              t.getPreviousStock(),
              t.getNewStock(),
              t.getCreatedAt(),
              t.getRemarks()
            });
      return new Report(
          new String[] {
            "ID", "Product", "Type", "Quantity", "Previous", "Updated", "Date", "Remarks"
          },
          rows,
          rows.size() + " audited movements");
    }
    if (!type.equals(TYPES[4])) throw new com.smartstock.util.AppException("Unknown report type.");
    BigInteger total = BigInteger.ZERO, discount = BigInteger.ZERO;
    for (Sale s : new SalesDAO(db).findAll()) {
      rows.add(
          new Object[] {
            s.getId(),
            s.getInvoiceNumber(),
            s.getCreatedAt(),
            s.getItems().stream().mapToLong(SaleItem::getQuantity).sum(),
            ValidationUtil.money(s.getSubtotalCents()),
            ValidationUtil.money(s.getDiscountCents()),
            ValidationUtil.money(s.getTotalCents())
          });
      total = total.add(BigInteger.valueOf(s.getTotalCents()));
      discount = discount.add(BigInteger.valueOf(s.getDiscountCents()));
    }
    return new Report(
        new String[] {
          "ID", "Invoice", "Date", "Units", "Subtotal (INR)", "Discount (INR)", "Total (INR)"
        },
        rows,
        rows.size()
            + " invoices  |  Revenue INR "
            + ValidationUtil.money(total)
            + "  |  Discounts INR "
            + ValidationUtil.money(discount));
  }
}
