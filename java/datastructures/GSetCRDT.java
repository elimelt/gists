package datastructures;
import java.util.*;
import java.util.concurrent.*;

public class GSetCRDT<E> {
  private final Set<E> elements;

  public GSetCRDT() {
    this.elements = Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  public void add(E element) {
    elements.add(element);
  }

  public boolean contains(E element) {
    return elements.contains(element);
  }

  public Set<E> getElements() {
    return new HashSet<>(elements);
  }

  public void merge(GSetCRDT<E> other) {
    elements.addAll(other.getElements());
  }

  // Stress test
  public static void main(String[] args) throws InterruptedException {
    final int NUM_THREADS = 8;
    final int OPERATIONS_PER_THREAD = 100_000;
    final int MERGE_INTERVAL = 10_000;

    List<GSetCRDT<Integer>> replicas = new ArrayList<>();
    for (int i = 0; i < NUM_THREADS; i++) {
      replicas.add(new GSetCRDT<>());
    }

    ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS);
    CountDownLatch latch = new CountDownLatch(NUM_THREADS);

    long startTime = System.nanoTime();

    for (int i = 0; i < NUM_THREADS; i++) {
      final int replicaId = i;
      executorService.submit(() -> {
        Random random = new Random();
        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
          replicas.get(replicaId).add(random.nextInt(OPERATIONS_PER_THREAD * NUM_THREADS));
          if (j % MERGE_INTERVAL == 0) {
            int otherReplicaId = random.nextInt(NUM_THREADS);
            replicas.get(replicaId).merge(replicas.get(otherReplicaId));
          }
        }
        latch.countDown();
      });
    }

    latch.await();
    executorService.shutdown();

    long endTime = System.nanoTime();
    double durationMs = (endTime - startTime) / 1e6;

    System.out.printf("Time taken for %d operations: %.2f ms%n",
        NUM_THREADS * OPERATIONS_PER_THREAD, durationMs);

    // Merge all replicas
    GSetCRDT<Integer> finalSet = new GSetCRDT<>();
    for (GSetCRDT<Integer> replica : replicas) {
      finalSet.merge(replica);
    }

    System.out.printf("Final set size: %d%n", finalSet.getElements().size());
  }
}