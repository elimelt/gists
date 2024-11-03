package datastructures;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * A Red-Black tree implementation that supports a few of the
 * operations of a Map.
 *
 * @param <K> The key type, must be Comparable
 * @param <V> The value type
 */
public class RedBlackTree<K extends Comparable<K>, V> {
    private static final boolean RED = true;
    private static final boolean BLACK = false;

    private Node root;
    private int size;
    private final Object writeLock = new Object();

    private class Node {
        K key;
        V value;
        Node left, right;
        boolean color;
        // Maintain size of subtree for efficient rank operations
        int size = 1;

        Node(K key, V value, boolean color) {
            this.key = key;
            this.value = value;
            this.color = color;
        }
    }

    /**
     * Inserts or updates a key-value pair in the Red-Black tree.
     * This operation maintains the Red-Black tree properties through rotations and
     * color flips.
     *
     * @param key   The key to insert/update (must not be null)
     * @param value The value to associate with the key
     * @throws IllegalArgumentException if key is null
     *
     *                                  Runtime: O(log n) average and worst case
     *                                  Space: O(1) additional space
     *                                  Thread Safety: Synchronized on writeLock for
     *                                  concurrent modifications
     */
    public void put(K key, V value) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null");
        synchronized (writeLock) {
            root = put(root, key, value);
            root.color = BLACK;
        }
    }

    private Node put(Node h, K key, V value) {
        if (h == null) {
            size++;
            return new Node(key, value, RED);
        }

        int cmp = key.compareTo(h.key);
        if (cmp < 0)
            h.left = put(h.left, key, value);
        else if (cmp > 0)
            h.right = put(h.right, key, value);
        else
            h.value = value;

        // Fix any right-leaning reds
        if (isRed(h.right) && !isRed(h.left))
            h = rotateLeft(h);
        if (isRed(h.left) && isRed(h.left.left))
            h = rotateRight(h);
        if (isRed(h.left) && isRed(h.right))
            flipColors(h);

        h.size = 1 + size(h.left) + size(h.right);
        return h;
    }

    /**
     * Retrieves the value associated with a given key.
     * Uses iterative traversal for better performance than recursion.
     *
     * @param key The key to search for (must not be null)
     * @return The value associated with the key, or null if key not found
     * @throws IllegalArgumentException if key is null
     *
     *                                  Runtime: O(log n) average and worst case
     *                                  Space: O(1) as it uses iteration instead of
     *                                  recursion
     *                                  Thread Safety: Naturally thread-safe for
     *                                  reads
     */
    public V get(K key) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null");
        Node x = root;
        while (x != null) {
            int cmp = key.compareTo(x.key);
            if (cmp < 0)
                x = x.left;
            else if (cmp > 0)
                x = x.right;
            else
                return x.value;
        }
        return null;
    }

    /**
     * Deletes a key and its associated value from the tree.
     * Maintains Red-Black tree properties through complex color manipulations.
     *
     * @param key The key to delete (must not be null)
     * @throws IllegalArgumentException if key is null
     *
     *                                  Runtime: O(log n) average and worst case
     *                                  Space: O(log n) due to recursion
     *                                  Thread Safety: Synchronized on writeLock for
     *                                  concurrent modifications
     */
    public void delete(K key) {
        if (key == null)
            throw new IllegalArgumentException("Key cannot be null");
        if (!contains(key))
            return;

        synchronized (writeLock) {
            if (!isRed(root.left) && !isRed(root.right))
                root.color = RED;

            root = delete(root, key);
            size--;
            if (!isEmpty())
                root.color = BLACK;
        }
    }

    private Node delete(Node h, K key) {
        if (key.compareTo(h.key) < 0) {
            if (!isRed(h.left) && !isRed(h.left.left))
                h = moveRedLeft(h);
            h.left = delete(h.left, key);
        } else {
            if (isRed(h.left))
                h = rotateRight(h);
            if (key.compareTo(h.key) == 0 && (h.right == null))
                return null;
            if (!isRed(h.right) && !isRed(h.right.left))
                h = moveRedRight(h);
            if (key.compareTo(h.key) == 0) {
                Node x = min(h.right);
                h.key = x.key;
                h.value = x.value;
                h.right = deleteMin(h.right);
            } else
                h.right = delete(h.right, key);
        }
        return balance(h);
    }

    // Helper methods
    private boolean isRed(Node x) {
        return x != null && x.color == RED;
    }

    private Node rotateLeft(Node h) {
        Node x = h.right;
        h.right = x.left;
        x.left = h;
        x.color = h.color;
        h.color = RED;
        x.size = h.size;
        h.size = 1 + size(h.left) + size(h.right);
        return x;
    }

    private Node rotateRight(Node h) {
        Node x = h.left;
        h.left = x.right;
        x.right = h;
        x.color = h.color;
        h.color = RED;
        x.size = h.size;
        h.size = 1 + size(h.left) + size(h.right);
        return x;
    }

    private void flipColors(Node h) {
        h.color = !h.color;
        h.left.color = !h.left.color;
        h.right.color = !h.right.color;
    }

    private Node moveRedLeft(Node h) {
        flipColors(h);
        if (isRed(h.right.left)) {
            h.right = rotateRight(h.right);
            h = rotateLeft(h);
            flipColors(h);
        }
        return h;
    }

    private Node moveRedRight(Node h) {
        flipColors(h);
        if (isRed(h.left.left)) {
            h = rotateRight(h);
            flipColors(h);
        }
        return h;
    }

    private Node balance(Node h) {
        if (isRed(h.right))
            h = rotateLeft(h);
        if (isRed(h.left) && isRed(h.left.left))
            h = rotateRight(h);
        if (isRed(h.left) && isRed(h.right))
            flipColors(h);

        h.size = size(h.left) + size(h.right) + 1;
        return h;
    }

    // Utility methods
    public boolean isEmpty() {
        return root == null;
    }

    public int size() {
        return size;
    }

    private int size(Node x) {
        return x == null ? 0 : x.size;
    }

    public boolean contains(K key) {
        return get(key) != null;
    }

    public K min() {
        if (isEmpty())
            throw new NoSuchElementException("Empty tree");
        return min(root).key;
    }

    private Node min(Node x) {
        if (x.left == null)
            return x;
        return min(x.left);
    }

    private Node deleteMin(Node h) {
        if (h.left == null)
            return null;
        if (!isRed(h.left) && !isRed(h.left.left))
            h = moveRedLeft(h);
        h.left = deleteMin(h.left);
        return balance(h);
    }

    // in-order traversal
    public void inorder(Consumer<K> consumer) {
        inorder(root, consumer);
    }

    private void inorder(Node x, Consumer<K> consumer) {
        if (x == null)
            return;
        inorder(x.left, consumer);
        consumer.accept(x.key);
        inorder(x.right, consumer);
    }

    public static void main(String[] args) {
        TreeBenchmark.benchmark();
    }

    private static class TreeBenchmark {
        static class BenchmarkResult {
            final int size;
            final double rbInsertions;
            final double rbLookups;
            final double rbDeletions;
            final double tmInsertions;
            final double tmLookups;
            final double tmDeletions;

            BenchmarkResult(int size, double rbInsertions, double rbLookups, double rbDeletions,
                    double tmInsertions, double tmLookups, double tmDeletions) {
                this.size = size;
                this.rbInsertions = rbInsertions;
                this.rbLookups = rbLookups;
                this.rbDeletions = rbDeletions;
                this.tmInsertions = tmInsertions;
                this.tmLookups = tmLookups;
                this.tmDeletions = tmDeletions;
            }
        }

        private static double benchmarkOperation(Runnable operation, int iterations) {
            System.gc(); // request garbage collection before timing
            long startTime = System.nanoTime();
            operation.run();
            long endTime = System.nanoTime();
            return iterations / ((endTime - startTime) / 1_000_000_000.0);
        }

        private static BenchmarkResult runBenchmark(int size, int iterations) {
            RedBlackTree<Integer, String> rbTree = new RedBlackTree<>();
            TreeMap<Integer, String> treeMap = new TreeMap<>();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            // Generate test data
            Integer[] keys = new Integer[size];
            for (int i = 0; i < size; i++) {
                keys[i] = random.nextInt(size * 10);
            }

            // Warmup
            for (int i = 0; i < Math.min(1000, size); i++) {
                int key = keys[i % size];
                rbTree.put(key, "value-" + key);
                treeMap.put(key, "value-" + key);
                rbTree.get(key);
                treeMap.get(key);
                rbTree.delete(key);
                treeMap.remove(key);
            }

            // Benchmark insertions
            double rbInsertions = benchmarkOperation(() -> {
                for (int i = 0; i < iterations; i++) {
                    int key = random.nextInt(size * 10);
                    rbTree.put(key, "value-" + key);
                }
            }, iterations);

            double tmInsertions = benchmarkOperation(() -> {
                for (int i = 0; i < iterations; i++) {
                    int key = random.nextInt(size * 10);
                    treeMap.put(key, "value-" + key);
                }
            }, iterations);

            // Benchmark lookups
            double rbLookups = benchmarkOperation(() -> {
                for (int i = 0; i < iterations; i++) {
                    rbTree.get(keys[i % size]);
                }
            }, iterations);

            double tmLookups = benchmarkOperation(() -> {
                for (int i = 0; i < iterations; i++) {
                    treeMap.get(keys[i % size]);
                }
            }, iterations);

            // Benchmark deletions
            double rbDeletions = benchmarkOperation(() -> {
                for (int i = 0; i < iterations; i++) {
                    int key = keys[i % size];
                    rbTree.delete(key);
                }
            }, iterations);

            double tmDeletions = benchmarkOperation(() -> {
                for (int i = 0; i < iterations; i++) {
                    int key = keys[i % size];
                    treeMap.remove(key);
                }
            }, iterations);

            return new BenchmarkResult(size, rbInsertions, rbLookups, rbDeletions,
                    tmInsertions, tmLookups, tmDeletions);
        }

        public static void benchmark() {
            List<BenchmarkResult> results = new ArrayList<>();

            // Test different sizes exponentially
            int[] sizes = { 100, 1000, 10_000, 100_000, 1_000_000 };

            System.out.println("Starting benchmarks across different tree sizes...");
            System.out.println("Size\tOperation\tRedBlackTree\tTreeMap\tRB/TM Ratio");
            System.out.println("------------------------------------------------------------");

            for (int size : sizes) {
                int iterations = Math.min(1_000_000, size * 10);
                BenchmarkResult result = runBenchmark(size, iterations);
                results.add(result);

                // Print results in a formatted table
                System.out.printf("%d\tInsertions\t%.2f\t%.2f\t%.2f%%%n",
                        size, result.rbInsertions, result.tmInsertions,
                        (result.rbInsertions / result.tmInsertions) * 100);
                System.out.printf("%d\tLookups\t\t%.2f\t%.2f\t%.2f%%%n",
                        size, result.rbLookups, result.tmLookups,
                        (result.rbLookups / result.tmLookups) * 100);
                System.out.printf("%d\tDeletions\t%.2f\t%.2f\t%.2f%%%n",
                        size, result.rbDeletions, result.tmDeletions,
                        (result.rbDeletions / result.tmDeletions) * 100);
                System.out.println("------------------------------------------------------------");
            }

            // Print summary statistics
            System.out.println("\nPerformance Summary:");
            double avgInsertRatio = results.stream()
                    .mapToDouble(r -> r.rbInsertions / r.tmInsertions).average().orElse(0);
            double avgLookupRatio = results.stream()
                    .mapToDouble(r -> r.rbLookups / r.tmLookups).average().orElse(0);
            double avgDeleteRatio = results.stream()
                    .mapToDouble(r -> r.rbDeletions / r.tmDeletions).average().orElse(0);

            System.out.printf("Average RedBlackTree/TreeMap performance ratios:%n");
            System.out.printf("Insertions: %.2f%%%n", avgInsertRatio * 100);
            System.out.printf("Lookups: %.2f%%%n", avgLookupRatio * 100);
            System.out.printf("Deletions: %.2f%%%n", avgDeleteRatio * 100);

            // Log scaling behavior
            System.out.println("\nScaling Analysis (relative to previous size):");
            for (int i = 1; i < results.size(); i++) {
                BenchmarkResult curr = results.get(i);
                BenchmarkResult prev = results.get(i - 1);
                double sizeFactor = (double) curr.size / prev.size;
                double expectedSlowdown = Math.log(sizeFactor) / Math.log(2); // log2 of size increase

                System.out.printf("Size increase %d -> %d (factor: %.1fx)%n",
                        prev.size, curr.size, sizeFactor);
                System.out.printf("Expected slowdown factor (log2): %.2fx%n", expectedSlowdown);
                System.out.printf("Actual slowdown factors:%n");
                System.out.printf("- RB Insert: %.2fx, TM Insert: %.2fx%n",
                        prev.rbInsertions / curr.rbInsertions,
                        prev.tmInsertions / curr.tmInsertions);
                System.out.printf("- RB Lookup: %.2fx, TM Lookup: %.2fx%n",
                        prev.rbLookups / curr.rbLookups,
                        prev.tmLookups / curr.tmLookups);
                System.out.printf("- RB Delete: %.2fx, TM Delete: %.2fx%n",
                        prev.rbDeletions / curr.rbDeletions,
                        prev.tmDeletions / curr.tmDeletions);
                System.out.println();
            }
        }
    }
}