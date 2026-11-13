package com.smartstock.util;

import com.smartstock.model.*;
import java.time.format.DateTimeFormatter;

public final class InvoiceFormatter {
  private InvoiceFormatter() {}

  public static String text(Sale sale) {
    StringBuilder b =
        new StringBuilder("SMARTSTOCK\nSmart Inventory Management System\nSALES INVOICE\n\n");
    b.append("Invoice: ")
        .append(sale.getInvoiceNumber())
        .append("\nDate: ")
        .append(sale.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")))
        .append("\nCurrency: INR\n")
        .append("-".repeat(68))
        .append('\n');
    for (SaleItem i : sale.getItems())
      b.append(i.getProductName())
          .append('\n')
          .append(
              String.format(
                  "  %d x %s = INR %s%n",
                  i.getQuantity(),
                  ValidationUtil.money(i.getUnitCents()),
                  ValidationUtil.money(i.getTotalCents())));
    return b.append("-".repeat(68))
        .append("\nSubtotal:   INR ")
        .append(ValidationUtil.money(sale.getSubtotalCents()))
        .append("\nDiscount:   INR ")
        .append(ValidationUtil.money(sale.getDiscountCents()))
        .append("\nGRAND TOTAL INR ")
        .append(ValidationUtil.money(sale.getTotalCents()))
        .append("\n\nThank you for your business.\n")
        .toString();
  }
}
