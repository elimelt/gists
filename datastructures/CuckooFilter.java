package datastructures;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;

public class CuckooFilter<T> {
  private static final int MAX_KICKS = 500;
  private final int numBuckets;
  private final int bucketSize;
  private final long[][] buckets;
  private final Random random;

  public CuckooFilter(int capacity, int bucketSize) {
    this.numBuckets = nextPowerOf2(capacity / bucketSize);
    this.bucketSize = bucketSize;
    this.buckets = new long[numBuckets][bucketSize];
    this.random = new Random();
  }

  public boolean add(T item) {
    long hash = hash(item);
    int fingerprint = fingerprint(hash);
    int i1 = index(hash);
    int i2 = index(hash ^ fingerprint);

    if (insert(i1, fingerprint) || insert(i2, fingerprint)) {
      return true;
    }

    int i = random.nextBoolean() ? i1 : i2;
    for (int n = 0; n < MAX_KICKS; n++) {
      int j = random.nextInt(bucketSize);
      long temp = buckets[i][j];
      buckets[i][j] = fingerprint;
      fingerprint = (int) temp;
      i = index(i ^ fingerprint);
      if (insert(i, fingerprint)) {
        return true;
      }
    }
    return false;
  }

  public boolean contains(T item) {
    long hash = hash(item);
    int fingerprint = fingerprint(hash);
    int i1 = index(hash);
    int i2 = index(hash ^ fingerprint);
    return contains(i1, fingerprint) || contains(i2, fingerprint);
  }

  public boolean remove(T item) {
    long hash = hash(item);
    int fingerprint = fingerprint(hash);
    int i1 = index(hash);
    int i2 = index(hash ^ fingerprint);
    return remove(i1, fingerprint) || remove(i2, fingerprint);
  }

  private boolean insert(int i, int fingerprint) {
    for (int j = 0; j < bucketSize; j++) {
      if (buckets[i][j] == 0) {
        buckets[i][j] = fingerprint;
        return true;
      }
    }
    return false;
  }

  private boolean contains(int i, int fingerprint) {
    for (int j = 0; j < bucketSize; j++) {
      if (buckets[i][j] == fingerprint) {
        return true;
      }
    }
    return false;
  }

  private boolean remove(int i, int fingerprint) {
    for (int j = 0; j < bucketSize; j++) {
      if (buckets[i][j] == fingerprint) {
        buckets[i][j] = 0;
        return true;
      }
    }
    return false;
  }

  private long hash(T item) {
    long h = item.hashCode();
    h ^= h >>> 33;
    h *= 0xff51afd7ed558ccdL;
    h ^= h >>> 33;
    h *= 0xc4ceb9fe1a85ec53L;
    h ^= h >>> 33;
    return h;
  }

  private int fingerprint(long hash) {
    return (int) (hash & ((1 << 16) - 1)) | 1;
  }

  private int index(long hash) {
    return (int) (hash & (numBuckets - 1));
  }

  private static int nextPowerOf2(int n) {
    return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
  }

  // Stress test
  public static void main(String[] args) {
    int capacity = 1_000_000;
    int bucketSize = 4;
    CuckooFilter<String> filter = new CuckooFilter<>(capacity, bucketSize);

    int numItems = 900_000;
    Set<String> items = new HashSet<>();
    Random random = new Random();

    // Add items
    long startTime = System.nanoTime();
    for (int i = 0; i < numItems; i++) {
      String item = "item" + random.nextInt(1_000_000);
      if (filter.add(item)) {
        items.add(item);
      }
    }
    long endTime = System.nanoTime();
    System.out.printf("Time to add %d items: %.2f ms%n", items.size(), (endTime - startTime) / 1e6);

    // Check for false positives
    int numTests = 1_000_000;
    int falsePositives = 0;
    startTime = System.nanoTime();
    for (int i = 0; i < numTests; i++) {
      String testItem = "item" + random.nextInt(10_000_000);
      if (filter.contains(testItem) && !items.contains(testItem)) {
        falsePositives++;
      }
    }
    endTime = System.nanoTime();
    System.out.printf("Time for %d queries: %.2f ms%n", numTests, (endTime - startTime) / 1e6);
    System.out.printf("False positive rate: %.6f%n", (double) falsePositives / numTests);

    // Remove items
    int numRemovals = items.size() / 2;
    Set<String> removedItems = new HashSet<>();
    startTime = System.nanoTime();
    for (String item : items) {
      if (removedItems.size() >= numRemovals)
        break;
      if (filter.remove(item)) {
        removedItems.add(item);
      }
    }
    endTime = System.nanoTime();
    System.out.printf("Time to remove %d items: %.2f ms%n", removedItems.size(), (endTime - startTime) / 1e6);

    // Check for false negatives
    int falseNegatives = 0;
    for (String item : items) {
      if (!removedItems.contains(item) && !filter.contains(item)) {
        falseNegatives++;
      }
    }
    System.out.printf("False negatives: %d (%.6f%%)%n", falseNegatives,
        (double) falseNegatives / (items.size() - removedItems.size()) * 100);
  }
}