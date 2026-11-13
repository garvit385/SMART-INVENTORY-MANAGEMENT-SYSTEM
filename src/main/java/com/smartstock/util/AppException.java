package com.smartstock.util;

/** An expected validation or persistence failure safe to present to the user. */
public class AppException extends RuntimeException {
  public AppException(String message) {
    super(message);
  }

  public AppException(String message, Throwable cause) {
    super(message, cause);
  }
}
