package datastructures;

import java.util.*;

public class FenwickTree {
    private int[] tree;
    private int n;

    public FenwickTree(int n) {
        this.n = n;
        tree = new int[n + 1];
    }

    public void update(int i, int delta) {
        for (; i <= n; i += i & -i)
            tree[i] += delta;
    }

    public int query(int i) {
        int sum = 0;
        for (; i > 0; i -= i & -i) sum += tree[i];
        return sum;
    }

    public int rangeSum(int i, int j) { return query(j) - query(i - 1); }

    public static void main(String[] args) {
        int size = 1_000_000;
        FenwickTree fenwickTree = new FenwickTree(size);
        Random random = new Random();

        // Insertion
        System.out.println("Inserting elements...");
        long startTime = System.nanoTime();
        for (int i = 1; i <= size; i++)
            fenwickTree.update(i, random.nextInt(100));

        long endTime = System.nanoTime();
        System.out.printf("Time taken for %d insertions: %.2f ms%n", size, (endTime - startTime) / 1e6);

        // Range sum queries
        int numQueries = 1_000_000;
        System.out.println("Performing range sum queries...");
        startTime = System.nanoTime();
        long totalSum = 0;
        for (int i = 0; i < numQueries; i++) {
            int left = random.nextInt(size) + 1;
            int right = random.nextInt(size - left + 1) + left;
            totalSum += fenwickTree.rangeSum(left, right);
        }
        endTime = System.nanoTime();
        System.out.printf("Time taken for %d range sum queries: %.2f ms%n", numQueries, (endTime - startTime) / 1e6);
        System.out.printf("Average range sum: %.2f%n", totalSum / (double) numQueries);

        // Updates
        System.out.println("Performing updates...");
        startTime = System.nanoTime();
        for (int i = 0; i < numQueries; i++) {
            int index = random.nextInt(size) + 1;
            int delta = random.nextInt(100) - 50;  // Random value between -50 and 49
            fenwickTree.update(index, delta);
        }
        endTime = System.nanoTime();
        System.out.printf("Time taken for %d updates: %.2f ms%n", numQueries, (endTime - startTime) / 1e6);

        // Memory usage
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memory = runtime.totalMemory() - runtime.freeMemory();
        System.out.printf("Approximate memory usage: %.2f MB%n", memory / (1024.0 * 1024.0));
    }
}
