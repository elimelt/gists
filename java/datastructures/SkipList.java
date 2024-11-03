package datastructures;
import java.util.*;

public class SkipList<T extends Comparable<T>> {
  private static final double PROBABILITY = 0.5;
  private final Node<T> head;
  private int maxLevel;
  private final Random random;

  private static class Node<T> {
    T value;
    Node<T>[] next;

    @SuppressWarnings("unchecked")
    Node(T value, int level) {
      this.value = value;
      this.next = new Node[level + 1];
    }
  }

  public SkipList() {
    this.head = new Node<>(null, 0);
    this.maxLevel = 0;
    this.random = new Random();
  }

  private int randomLevel() {
    int level = 0;
    while (random.nextDouble() < PROBABILITY && level < 32) {
      level++;
    }
    return level;
  }

  public void insert(T value) {
    int level = randomLevel();
    Node<T> newNode = new Node<>(value, level);
    if (level > maxLevel) {
      head.next = Arrays.copyOf(head.next, level + 1);
      maxLevel = level;
    }

    Node<T> current = head;
    for (int i = maxLevel; i >= 0; i--) {
      while (current.next[i] != null && current.next[i].value.compareTo(value) < 0) {
        current = current.next[i];
      }
      if (i <= level) {
        newNode.next[i] = current.next[i];
        current.next[i] = newNode;
      }
    }
  }

  public boolean search(T value) {
    Node<T> current = head;
    for (int i = maxLevel; i >= 0; i--) {
      while (current.next[i] != null && current.next[i].value.compareTo(value) < 0) {
        current = current.next[i];
      }
    }
    return current.next[0] != null && current.next[0].value.compareTo(value) == 0;
  }

  public static void main(String[] args) {
    SkipList<Integer> skipList = new SkipList<>();
    int size = 1_000_000;
    int numOperations = 1_000_000;

    // Insertion
    System.out.println("Inserting elements...");
    long startTime = System.nanoTime();
    for (int i = 0; i < size; i++) {
      skipList.insert(i);
    }
    long endTime = System.nanoTime();
    System.out.printf("Time taken for %d insertions: %.2f ms%n", size, (endTime - startTime) / 1e6);

    // Search
    System.out.println("Performing searches...");
    Random random = new Random();
    int found = 0;
    startTime = System.nanoTime();
    for (int i = 0; i < numOperations; i++) {
      int value = random.nextInt(size * 2); // Search for some values not in the list
      if (skipList.search(value)) {
        found++;
      }
    }
    endTime = System.nanoTime();
    System.out.printf("Time taken for %d searches: %.2f ms%n", numOperations, (endTime - startTime) / 1e6);
    System.out.printf("Search hit rate: %.2f%%%n", (found / (double) numOperations) * 100);

    // Memory usage
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long memory = runtime.totalMemory() - runtime.freeMemory();
    System.out.printf("Approximate memory usage: %.2f MB%n", memory / (1024.0 * 1024.0));
  }
}