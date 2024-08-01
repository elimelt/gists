package datastructures;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import networking.GenericDataStructureHttpServer;

public class ConcurrentRingBuffer<T> {
  private final T[] buffer;
  private final AtomicInteger head;
  private final AtomicInteger tail;
  private final int capacity;

  @SuppressWarnings("unchecked")
  public ConcurrentRingBuffer(int capacity) {
    this.capacity = capacity;
    this.buffer = (T[]) new Object[capacity];
    this.head = new AtomicInteger(0);
    this.tail = new AtomicInteger(0);
  }

  public boolean offer(T item) {
    int currentTail = tail.get();
    int newTail = (currentTail + 1) % capacity;
    if (newTail == head.get()) {
      return false; // Buffer is full
    }
    buffer[currentTail] = item;
    tail.set(newTail);
    return true;
  }

  public T poll() {
    int currentHead = head.get();
    if (currentHead == tail.get()) {
      return null; // Buffer is empty
    }
    T item = buffer[currentHead];
    head.set((currentHead + 1) % capacity);
    return item;
  }

  public boolean isEmpty() {
    return head.get() == tail.get();
  }

  public boolean isFull() {
    return (tail.get() + 1) % capacity == head.get();
  }

  public int size() {
    return (tail.get() - head.get() + capacity) % capacity;
  }

  public long stressTest(int numThreads, int numOps) throws InterruptedException {
    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread(() -> {
        for (int j = 0; j < numOps; j++) {
          offer(null);
          poll();
        }
      });
    }

    long start = System.nanoTime();
    for (Thread thread : threads) {
      thread.start();
    }
    for (Thread thread : threads) {
      thread.join();
    }
    return System.nanoTime() - start;
  }

  public static void main(String[] args) throws InterruptedException {
    ConcurrentRingBuffer<Integer> buffer = new ConcurrentRingBuffer<>(1000);

    for (int nThreads = 1; nThreads <= 8; nThreads *= 2) {
      for (int nOps = 100; nOps <= 1_000_000; nOps *= 2) {
        long time = buffer.stressTest(nThreads, nOps);
        System.out.printf("Threads: %d, Ops: %d, Time: %d ms\n", nThreads, nOps, time / 1_000_000);
      }
    }
  }
}
