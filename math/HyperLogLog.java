package math;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;

public class HyperLogLog<T> {
  private final int[] registers;
  private final int numRegisters;
  private final int registerSize;
  private final MessageDigest md;

  public HyperLogLog(int precision) {
    this.numRegisters = 1 << precision;
    this.registerSize = 5; // 5 bits per register
    this.registers = new int[numRegisters];
    try {
      this.md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 not supported", e);
    }
  }

  public void add(T item) {
    long hash = hash(item);
    int index = (int) (hash >>> (Long.SIZE - Integer.numberOfTrailingZeros(numRegisters)));
    int rank = Long.numberOfLeadingZeros((hash << Integer.numberOfTrailingZeros(numRegisters))
        | (1 << (Integer.numberOfTrailingZeros(numRegisters) - 1)) + 1) + 1;
    registers[index] = Math.max(registers[index], rank);
  }

  public long cardinality() {
    double sum = 0;
    int zeroes = 0;
    for (int value : registers) {
      sum += Math.pow(2, -value);
      if (value == 0) {
        zeroes++;
      }
    }
    double estimate = (0.7213 / (1 + 1.079 / numRegisters)) * numRegisters * numRegisters / sum;
    if (estimate <= 2.5 * numRegisters) {
      if (zeroes > 0) {
        estimate = numRegisters * Math.log((double) numRegisters / zeroes);
      }
    } else if (estimate > (1 / 30.0) * (1L << 32)) {
      estimate = -1L << 32 * (long) Math.log(1 - (estimate / (1L << 32)));
    }
    return Math.round(estimate);
  }

  private long hash(T item) {
    md.reset();
    md.update(item.toString().getBytes());
    byte[] digest = md.digest();
    return ByteBuffer.wrap(digest).getLong();
  }

  // Stress test
  public static void main(String[] args) {
    int precision = 14; // 2^14 registers
    HyperLogLog<String> hll = new HyperLogLog<>(precision);

    int numUniqueItems = 1_000_000;
    Set<String> uniqueItems = new HashSet<>();
    Random random = new Random();

    // Add items
    long startTime = System.nanoTime();
    for (int i = 0; i < numUniqueItems; i++) {
      String item = UUID.randomUUID().toString();
      hll.add(item);
      uniqueItems.add(item);
    }
    long endTime = System.nanoTime();
    System.out.printf("Time to add %d unique items: %.2f ms%n", numUniqueItems, (endTime - startTime) / 1e6);

    // Estimate cardinality
    startTime = System.nanoTime();
    long estimatedCardinality = hll.cardinality();
    endTime = System.nanoTime();
    System.out.printf("Time to estimate cardinality: %.2f ms%n", (endTime - startTime) / 1e6);

    double error = Math.abs(estimatedCardinality - numUniqueItems) / (double) numUniqueItems * 100;
    System.out.printf("Actual cardinality: %d%n", numUniqueItems);
    System.out.printf("Estimated cardinality: %d%n", estimatedCardinality);
    System.out.printf("Error: %.2f%%%n", error);

    // Test with duplicates
    int numDuplicates = 1_000_000;
    startTime = System.nanoTime();
    for (int i = 0; i < numDuplicates; i++) {
      String item = uniqueItems.toArray(new String[0])[random.nextInt(uniqueItems.size())];
      hll.add(item);
    }
    endTime = System.nanoTime();
    System.out.printf("Time to add %d duplicates: %.2f ms%n", numDuplicates, (endTime - startTime) / 1e6);

    estimatedCardinality = hll.cardinality();
    error = Math.abs(estimatedCardinality - numUniqueItems) / (double) numUniqueItems * 100;
    System.out.printf("Estimated cardinality after adding duplicates: %d%n", estimatedCardinality);
    System.out.printf("Error after adding duplicates: %.2f%%%n", error);
  }
}