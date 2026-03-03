// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PINUtil {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int SALT_LENGTH = 16;

  /**
   * Hashes a PIN with a random salt.
   * 
   * @param pin The 6-digit PIN string.
   * @return A formatted string "salt:hash"
   */
  public static String hash(String pin) {
    byte[] salt = new byte[SALT_LENGTH];
    RANDOM.nextBytes(salt);
    String saltStr = Base64.getEncoder().encodeToString(salt);
    String hash = computeHash(pin, salt);
    return saltStr + ":" + hash;
  }

  /**
   * Verifies a PIN against a stored hash.
   * 
   * @param pin    The provided PIN.
   * @param stored The stored "salt:hash" string.
   * @return true if matches.
   */
  public static boolean verify(String pin, String stored) {
    if (stored == null || !stored.contains(":"))
      return false;

    String[] parts = stored.split(":");
    if (parts.length != 2)
      return false;

    byte[] salt = Base64.getDecoder().decode(parts[0]);
    String expectedHash = parts[1];
    String actualHash = computeHash(pin, salt);

    return expectedHash.equals(actualHash);
  }

  private static String computeHash(String pin, byte[] salt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(salt);
      byte[] encodedhash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encodedhash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Hashing algorithm not found", e);
    }
  }
}
