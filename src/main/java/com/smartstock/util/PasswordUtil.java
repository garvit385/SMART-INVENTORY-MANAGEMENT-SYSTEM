package com.smartstock.util;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
  private static final int ITERATIONS = 210_000;

  private PasswordUtil() {}

  public static String hash(char[] password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return ITERATIONS
        + ":"
        + Base64.getEncoder().encodeToString(salt)
        + ":"
        + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
  }

  public static boolean verify(char[] password, String stored) {
    try {
      String[] parts = stored.split(":");
      return MessageDigest.isEqual(
          Base64.getDecoder().decode(parts[2]),
          derive(password, Base64.getDecoder().decode(parts[1]), Integer.parseInt(parts[0])));
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static byte[] derive(char[] password, byte[] salt, int iterations) {
    PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (GeneralSecurityException e) {
      throw new AppException("Password security is unavailable.", e);
    } finally {
      spec.clearPassword();
    }
  }
}
