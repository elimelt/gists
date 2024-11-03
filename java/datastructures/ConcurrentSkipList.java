package datastructures;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class ConcurrentSkipList<T extends Comparable<T>> {
  private static final int MAX_LEVEL = 16;

  private final Node<T> head = new Node<>(null, MAX_LEVEL);

  private static class Node<T> {
    final T value;
    final AtomicMarkableReference<Node<T>>[] next;

    @SuppressWarnings("unchecked")
    Node(T value, int level) {
      this.value = value;
      this.next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[level];
      for (int i = 0; i < level; i++) {
        this.next[i] = new AtomicMarkableReference<>(null, false);
      }
    }
  }

  public void add(T value) {
    Node<T>[] update = (Node<T>[]) new Node[MAX_LEVEL];
    Node<T> current = head;

    for (int i = MAX_LEVEL - 1; i >= 0; i--) {
      while (current.next[i].getReference() != null && current.next[i].getReference().value.compareTo(value) < 0) {
        current = current.next[i].getReference();
      }
      update[i] = current;
    }

    int level = randomLevel();
    Node<T> newNode = new Node<>(value, level);

    for (int i = 0; i < level; i++) {
      newNode.next[i].set(update[i].next[i].getReference(), false);
      update[i].next[i].set(newNode, false);
    }
  }

  public boolean contains(T value) {
    Node<T> current = head;

    for (int i = MAX_LEVEL - 1; i >= 0; i--) {
      while (current.next[i].getReference() != null && current.next[i].getReference().value.compareTo(value) < 0) {
        current = current.next[i].getReference();
      }
    }

    current = current.next[0].getReference();
    return current != null && current.value.equals(value);
  }

  private int randomLevel() {
    int level = 1;
    while (level < MAX_LEVEL && ThreadLocalRandom.current().nextDouble() < 0.5) {
      level++;
    }
    return level;
  }

  public static void main(String[] args) {
    ConcurrentSkipList<Integer> skipList = new ConcurrentSkipList<>();
    int numThreads = 4;
    int numOps = 1_000_000;

    Thread[] threads = new Thread[numThreads];

    long startTime = System.nanoTime();

    for (int i = 0; i < numThreads; i++) {
      final int tid = i;
      threads[i] = new Thread(() -> {
        for (int j = 0; j < numOps; j++) {
          skipList.add(tid * numOps + j);
        }
      });
    }

    for (int i = 0; i < numThreads; i++) {
      threads[i].start();
    }

    for (int i = 0; i < numThreads; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    long endTime = System.nanoTime();
    System.out.printf("Time to add %d items: %.2f ms%n", numThreads * numOps, (endTime - startTime) / 1e6);

    startTime = System.nanoTime();
    for (int i = 0; i < numThreads; i++) {
      final int tid = i;
      threads[i] = new Thread(() -> {
        for (int j = 0; j < numOps; j++) {
          skipList.contains(tid * numOps + j);
        }
      });
    }

    for (int i = 0; i < numThreads; i++) {
      threads[i].start();
    }

    for (int i = 0; i < numThreads; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    endTime = System.nanoTime();
    System.out.printf("Time to check %d items: %.2f ms%n", numThreads * numOps, (endTime - startTime) / 1e6);

    System.out.println("Done");
  }
}
