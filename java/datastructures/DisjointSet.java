package datastructures;
import java.util.*;

public class DisjointSet {
  private int[] parent;
  private int[] rank;

  public DisjointSet(int size) {
    parent = new int[size];
    rank = new int[size];
    for (int i = 0; i < size; i++) {
      parent[i] = i;
    }
  }

  public int find(int x) {
    if (parent[x] != x) {
      parent[x] = find(parent[x]); // Path compression
    }
    return parent[x];
  }

  public void union(int x, int y) {
    int rootX = find(x);
    int rootY = find(y);

    if (rootX != rootY) {
      if (rank[rootX] < rank[rootY]) {
        parent[rootX] = rootY;
      } else if (rank[rootX] > rank[rootY]) {
        parent[rootY] = rootX;
      } else {
        parent[rootY] = rootX;
        rank[rootX]++;
      }
    }
  }

  public boolean connected(int x, int y) {
    return find(x) == find(y);
  }

  public static void main(String[] args) {
    // Stress test parameters
    int numOperations = 10_000_000, size = 10_000_000;

    DisjointSet ds = new DisjointSet(size);
    Random random = new Random();

    System.out.println("Performing union operations...");
    long startTime = System.nanoTime();

    for (int i = 0; i < numOperations; i++) {
      int x = random.nextInt(size);
      int y = random.nextInt(size);
      ds.union(x, y);
    }

    long endTime = System.nanoTime();
    System.out.printf("Time taken for %d union operations: %.2f ms%n",
        numOperations, (endTime - startTime) / 1e6);

    System.out.println("Performing connected queries...");
    int connectedCount = 0;
    startTime = System.nanoTime();

    for (int i = 0; i < numOperations; i++) {
      int x = random.nextInt(size);
      int y = random.nextInt(size);
      if (ds.connected(x, y)) {
        connectedCount++;
      }
    }

    endTime = System.nanoTime();
    System.out.printf("Time taken for %d connected queries: %.2f ms%n",
        numOperations, (endTime - startTime) / 1e6);

    System.out.printf("Number of connected pairs: %d (%.2f%%)%n",
        connectedCount, (connectedCount / (double) numOperations) * 100);

    // statistics
    int numSets = 0;
    for (int i = 0; i < size; i++) {
      if (ds.parent[i] == i) {
        numSets++;
      }
    }

    System.out.printf("Number of disjoint sets: %d%n", numSets);

    // memory usage
    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long memory = runtime.totalMemory() - runtime.freeMemory();
    System.out.printf("Approximate memory usage: %.2f MB%n", memory / (1024.0 * 1024.0));
  }
}