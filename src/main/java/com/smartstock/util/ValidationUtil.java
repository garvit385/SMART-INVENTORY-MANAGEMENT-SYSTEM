package com.smartstock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ValidationUtil {
  private ValidationUtil() {}

  public static String required(String text, String label) {
    if (text == null || text.isBlank()) throw new AppException(label + " is required.");
    if (text.trim().length() > 500)
      throw new AppException(label + " is too long (maximum 500 characters).");
    return text.trim();
  }

  public static int nonNegative(int n, String label) {
    if (n < 0 || n > 1_000_000_000)
      throw new AppException(label + " must be between 0 and 1,000,000,000.");
    return n;
  }

  public static int integer(String text, String label) {
    try {
      return nonNegative(Integer.parseInt(text.trim()), label);
    } catch (NumberFormatException e) {
      throw new AppException(label + " must be a whole number.");
    }
  }

  public static long cents(String text) {
    try {
      long n =
          new BigDecimal(text.trim())
              .setScale(2, RoundingMode.UNNECESSARY)
              .movePointRight(2)
              .longValueExact();
      if (n < 0 || n > 100_000_000_000L)
        throw new AppException("Amount must be between 0 and 1,000,000,000.00.");
      return n;
    } catch (NumberFormatException | ArithmeticException e) {
      throw new AppException("Enter a valid amount with at most two decimal places.");
    }
  }

  public static String money(long cents) {
    return BigDecimal.valueOf(cents, 2).toPlainString();
  }

  public static String money(java.math.BigInteger cents) {
    return new BigDecimal(cents, 2).toPlainString();
  }

  public static void contact(String phone, String email) {
    if (!email.isBlank() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new AppException("Enter a valid email address.");
    if (!phone.isBlank()
        && (!phone.matches("[+0-9() .-]{7,25}") || phone.replaceAll("\\D", "").length() < 7))
      throw new AppException("Enter a valid phone number (at least 7 digits).");
  }
}
