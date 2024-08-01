package datastructures;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadLocalRandom;

public class LockFreeQueue<T> {
  private static class Node<T> {
    final T item;
    final AtomicReference<Node<T>> next;

    Node(T item) {
      this.item = item;
      this.next = new AtomicReference<>(null);
    }
  }

  private final AtomicReference<Node<T>> head;
  private final AtomicReference<Node<T>> tail;

  public LockFreeQueue() {
    Node<T> dummy = new Node<>(null);
    this.head = new AtomicReference<>(dummy);
    this.tail = new AtomicReference<>(dummy);
  }

  public void enqueue(T item) {
    Node<T> newNode = new Node<>(item);
    while (true) {
      Node<T> curTail = tail.get();
      Node<T> tailNext = curTail.next.get();
      if (curTail == tail.get()) {
        if (tailNext != null) {
          tail.compareAndSet(curTail, tailNext);
        } else {
          if (curTail.next.compareAndSet(null, newNode)) {
            tail.compareAndSet(curTail, newNode);
            return;
          }
        }
      }
    }
  }

  public T dequeue() {
    while (true) {
      Node<T> curHead = head.get();
      Node<T> curTail = tail.get();
      Node<T> headNext = curHead.next.get();
      if (curHead == head.get()) {
        if (curHead == curTail) {
          if (headNext == null) {
            return null;
          }
          tail.compareAndSet(curTail, headNext);
        } else {
          T item = headNext.item;
          if (head.compareAndSet(curHead, headNext)) {
            return item;
          }
        }
      }
    }
  }

  public int size() {
    int count = 0;
    Node<T> cur = head.get();
    while ((cur = cur.next.get()) != null) {
      count++;
    }
    return count;
  }

  // Stress test
  public static void main(String[] args) throws InterruptedException {
    final int NUM_THREADS = 8;
    final int OPERATIONS_PER_THREAD = 10_000_000;
    final LockFreeQueue<Integer> queue = new LockFreeQueue<>();

    Thread[] threads = new Thread[NUM_THREADS];
    long startTime = System.nanoTime();

    for (int i = 0; i < NUM_THREADS; i++) {
      final int threadId = i;
      threads[i] = new Thread(() -> {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
          if (random.nextBoolean()) {
            queue.enqueue(random.nextInt());
          } else {
            queue.dequeue();
          }
        }
      });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    long endTime = System.nanoTime();
    double durationMs = (endTime - startTime) / 1_000_000.0;

    System.out.printf("Time taken for %d operations: %.2f ms%n",
        NUM_THREADS * OPERATIONS_PER_THREAD, durationMs);
    System.out.printf("Operations per second: %.2f%n",
        (NUM_THREADS * OPERATIONS_PER_THREAD) / (durationMs / 1000));
  }
}