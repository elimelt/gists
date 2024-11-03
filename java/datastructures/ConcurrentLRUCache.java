package datastructures;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentLRUCache<K, V> {
  private final int capacity;
  private final Map<K, V> map;
  private final LinkedList<K> accessOrder;
  private final ReentrantLock lock;

  public ConcurrentLRUCache(int capacity) {
    this.capacity = capacity;
    this.map = new ConcurrentHashMap<>(capacity);
    this.accessOrder = new LinkedList<>();
    this.lock = new ReentrantLock();
  }

  public V get(K key) {
    lock.lock();
    try {
      if (!map.containsKey(key)) {
        return null;
      }
      accessOrder.remove(key);
      accessOrder.addFirst(key);
      return map.get(key);
    } finally {
      lock.unlock();
    }
  }

  public void put(K key, V value) {
    lock.lock();
    try {
      if (map.containsKey(key)) {
        accessOrder.remove(key);
      } else if (map.size() == capacity) {
        K oldest = accessOrder.removeLast();
        map.remove(oldest);
      }
      accessOrder.addFirst(key);
      map.put(key, value);
    } finally {
      lock.unlock();
    }
  }

  public boolean containsKey(K key) {
    lock.lock();
    try {
      return map.containsKey(key);
    } finally {
      lock.unlock();
    }
  }

  public int size() {
    lock.lock();
    try {
      return map.size();
    } finally {
      lock.unlock();
    }
  }

  public static int stressTest(int numThreads, int numOps, int capacity) throws InterruptedException {
    ConcurrentLRUCache<Integer, Integer> cache = new ConcurrentLRUCache<>(capacity);
    List<Thread> threads = new ArrayList<>();
    AtomicInteger hits = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      Thread thread = new Thread(() -> {
        Random random = new Random();
        for (int j = 0; j < numOps; j++) {
          int key = random.nextInt(100);
          if (cache.containsKey(key)) {
            hits.incrementAndGet();
          } else {
            cache.put(key, key);
          }
        }
      });
      threads.add(thread);
      thread.start();
    }

    for (Thread thread : threads)
      thread.join();

    return hits.get();
  }

  public static void main(String[] args) throws InterruptedException {
    for (int nth : new int[] { 1, 2, 4, 8, 16 }) {
      for (int capacity : new int[] { 1, 10, 100, 1000, 10_000, 100_000 }) {
        long totalHits = 0;
        long startTime = System.nanoTime();
        for (int i = 0; i < 5; i++)
          totalHits += stressTest(nth, 1_000_000, capacity);

        long endTime = System.nanoTime();
        System.out.printf("Threads: %d, Capacity: %d, Hits: %d, Time: %.2f ms%n", nth, capacity, totalHits / 5,
            (endTime - startTime) / 1e6);
      }
    }
    System.out.println("Done!");
  }
}
