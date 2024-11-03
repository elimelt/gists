package datastructures;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;

public class ConsistentHashRing<T> {
  private final SortedMap<Long, T> ring = new TreeMap<>();
  private final int virtualNodes;
  private final MessageDigest md;

  public ConsistentHashRing(int virtualNodes) {
    this.virtualNodes = virtualNodes;
    try {
      this.md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD5 not supported", e);
    }
  }

  public void addNode(T node) {
    for (int i = 0; i < virtualNodes; i++) {
      ring.put(hash(node.toString() + i), node);
    }
  }

  public void removeNode(T node) {
    for (int i = 0; i < virtualNodes; i++) {
      ring.remove(hash(node.toString() + i));
    }
  }

  public T getNode(String key) {
    if (ring.isEmpty()) {
      return null;
    }
    long hash = hash(key);
    if (!ring.containsKey(hash)) {
      SortedMap<Long, T> tailMap = ring.tailMap(hash);
      hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
    }
    return ring.get(hash);
  }

  private long hash(String key) {
    md.reset();
    md.update(key.getBytes());
    byte[] digest = md.digest();
    return ByteBuffer.wrap(digest).getLong();
  }

  public int size() {
    return ring.size() / virtualNodes;
  }

  // Stress test
  public static void main(String[] args) {
    ConsistentHashRing<String> ring = new ConsistentHashRing<>(100);
    int numNodes = 10;
    int numKeys = 1_000_000;

    for (int i = 0; i < numNodes; i++)
      ring.addNode("Node" + i);

    Map<String, Integer> distribution = new HashMap<>();
    long startTime = System.nanoTime();
    for (int i = 0; i < numKeys; i++) {
      String key = "Key" + i;
      String node = ring.getNode(key);
      distribution.merge(node, 1, Integer::sum);
    }
    long endTime = System.nanoTime();

    System.out.printf("Time to distribute %d keys: %.2f ms%n", numKeys, (endTime - startTime) / 1e6);

    int min = Integer.MAX_VALUE, max = 0;
    double sum = 0;
    for (int count : distribution.values()) {
      min = Math.min(min, count);
      max = Math.max(max, count);
      sum += count;
    }
    double avg = sum / numNodes;
    double stdDev = Math.sqrt(distribution.values().stream()
        .mapToDouble(count -> Math.pow(count - avg, 2))
        .sum() / numNodes);

    System.out.printf("Distribution - Min: %d, Max: %d, Avg: %.2f, StdDev: %.2f%n", min, max, avg, stdDev);

    // Test node removal and rebalancing
    String nodeToRemove = "Node" + (numNodes / 2);
    ring.removeNode(nodeToRemove);

    Map<String, Integer> newDistribution = new HashMap<>();
    startTime = System.nanoTime();
    for (int i = 0; i < numKeys; i++) {
      String key = "Key" + i;
      String node = ring.getNode(key);
      newDistribution.merge(node, 1, Integer::sum);
    }
    endTime = System.nanoTime();

    System.out.printf("Time to redistribute after node removal: %.2f ms%n", (endTime - startTime) / 1e6);

    long keysRebalanced = distribution.entrySet().stream()
        .filter(e -> !e.getKey().equals(nodeToRemove))
        .mapToLong(e -> Math.abs(e.getValue() - newDistribution.getOrDefault(e.getKey(), 0)))
        .sum();

    System.out.printf("Keys rebalanced after node removal: %d (%.2f%%)%n",
        keysRebalanced, (keysRebalanced * 100.0) / numKeys);
  }
}