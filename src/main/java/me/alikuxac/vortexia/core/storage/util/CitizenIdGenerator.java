// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class CitizenIdGenerator {

  private static final String PREFIX = "VX";
  private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final int RANDOM_LENGTH = 5;
  private static final Random RANDOM = new Random();

  public static String generate() {
    String timestamp = YearMonth.now().format(DateTimeFormatter.ofPattern("yyMM"));
    StringBuilder randomPart = new StringBuilder(RANDOM_LENGTH);
    for (int i = 0; i < RANDOM_LENGTH; i++) {
      randomPart.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
    }
    return PREFIX + timestamp + randomPart.toString();
  }
}
