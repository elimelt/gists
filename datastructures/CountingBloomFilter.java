package datastructures;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;

public class CountingBloomFilter<T> {
  private final int[] counters;
  private final int size;
  private final int numHashFunctions;
  private final MessageDigest md;

  public CountingBloomFilter(int size, int numHashFunctions) {
    this.size = size;
    this.numHashFunctions = numHashFunctions;
    this.counters = new int[size];
    try {
      this.md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 not supported", e);
    }
  }

  public void add(T item) {
    for (int i = 0; i < numHashFunctions; i++) {
      int index = getHash(item, i);
      if (counters[index] < Integer.MAX_VALUE) {
        counters[index]++;
      }
    }
  }

  public void remove(T item) {
    for (int i = 0; i < numHashFunctions; i++) {
      int index = getHash(item, i);
      if (counters[index] > 0) {
        counters[index]--;
      }
    }
  }

  public boolean mightContain(T item) {
    for (int i = 0; i < numHashFunctions; i++) {
      if (counters[getHash(item, i)] == 0) {
        return false;
      }
    }
    return true;
  }

  private int getHash(T item, int i) {
    md.reset();
    md.update(item.toString().getBytes());
    md.update(ByteBuffer.allocate(4).putInt(i).array());
    byte[] digest = md.digest();
    return Math.abs(ByteBuffer.wrap(digest).getInt()) % size;
  }

  public double getEstimatedFalsePositiveRate(int numItems) {
    return Math.pow(1 - Math.exp(-numHashFunctions * (double) numItems / size), numHashFunctions);
  }

  // Stress test
  public static void main(String[] args) {
    int size = 1_000_000;
    int numHashFunctions = 5;
    CountingBloomFilter<String> filter = new CountingBloomFilter<>(size, numHashFunctions);

    int numItems = 100_000;
    Set<String> addedItems = new HashSet<>();

    long startTime = System.nanoTime();
    for (int i = 0; i < numItems; i++) {
      String item = UUID.randomUUID().toString();
      filter.add(item);
      addedItems.add(item);
    }
    long endTime = System.nanoTime();
    System.out.printf("Time to add %d items: %.2f ms%n", numItems, (endTime - startTime) / 1e6);

    int numTests = 1_000_000;
    int falsePositives = 0;
    startTime = System.nanoTime();
    for (int i = 0; i < numTests; i++) {
      String testItem = UUID.randomUUID().toString();
      if (filter.mightContain(testItem) && !addedItems.contains(testItem)) {
        falsePositives++;
      }
    }
    endTime = System.nanoTime();
    System.out.printf("Time to perform %d lookups: %.2f ms%n", numTests, (endTime - startTime) / 1e6);

    double actualFPRate = (double) falsePositives / numTests;
    double estimatedFPRate = filter.getEstimatedFalsePositiveRate(numItems);
    System.out.printf("False positive rate - Actual: %.6f, Estimated: %.6f%n", actualFPRate, estimatedFPRate);

    int numRemovals = numItems / 2;
    startTime = System.nanoTime();
    Iterator<String> iterator = addedItems.iterator();
    for (int i = 0; i < numRemovals; i++) {
      String item = iterator.next();
      filter.remove(item);
      iterator.remove();
    }
    endTime = System.nanoTime();
    System.out.printf("Time to remove %d items: %.2f ms%n", numRemovals, (endTime - startTime) / 1e6);

    int falseNegatives = 0;
    for (String item : addedItems) {
      if (!filter.mightContain(item)) {
        falseNegatives++;
      }
    }
    System.out.printf("False negatives after removal: %d (%.6f%%)%n",
        falseNegatives, (double) falseNegatives / addedItems.size() * 100);
  }
}