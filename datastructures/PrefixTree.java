package datastructures;

import java.util.Arrays;
import java.util.Random;

public class PrefixTree {
    private static final int ALPHABET_SIZE = 256; // For unsigned byte

    private static class Node {
        Node[] children = new Node[ALPHABET_SIZE];
        boolean isEndOfWord;
    }

    private final Node root;

    public PrefixTree() { root = new Node(); }

    public void insert(byte[] key) {
        Node curr = root;
        for (byte b : key) {
            int index = Byte.toUnsignedInt(b);
            if (curr.children[index] == null)
                curr.children[index] = new Node();
            curr = curr.children[index];
        }
        curr.isEndOfWord = true;
    }

    public boolean search(byte[] key) {
        Node node = searchNode(key);
        return node != null && node.isEndOfWord;
    }

    public boolean startsWith(byte[] prefix) {
        return searchNode(prefix) != null;
    }

    private Node searchNode(byte[] key) {
        Node curr = root;
        for (byte b : key) {
            int index = Byte.toUnsignedInt(b);
            if (curr.children[index] == null)
                return null;
            curr = curr.children[index];
        }
        return curr;
    }

    public static void main(String[] args) {
        PrefixTree tree = new PrefixTree();
        Random random = new Random();

        int numInsertions = 100_000;
        int keyLength = 10;
        byte[][] keys = new byte[numInsertions][keyLength];

        // Benchmark insertion
        long startTime = System.nanoTime();
        for (int i = 0; i < numInsertions; i++) {
            random.nextBytes(keys[i]);
            tree.insert(keys[i]);
        }
        long endTime = System.nanoTime();
        double insertionTime = (endTime - startTime) / 1e9;
        System.out.printf("Insertion of %d keys: %.3f seconds\n", numInsertions, insertionTime);
        System.out.printf("Average insertion time: %.3f microseconds\n", insertionTime * 1e6 / numInsertions);

        // Benchmark search (existing keys)
        startTime = System.nanoTime();
        for (byte[] key : keys) {
            tree.search(key);
        }
        endTime = System.nanoTime();
        double searchTime = (endTime - startTime) / 1e9;
        System.out.printf("search of %d existing keys: %.3f seconds\n", numInsertions, searchTime);
        System.out.printf("average search time (existing keys): %.3f microseconds\n", searchTime * 1e6 / numInsertions);

        // Benchmark search (non-existing keys)
        startTime = System.nanoTime();
        for (int i = 0; i < numInsertions; i++) {
            byte[] nonExistingKey = new byte[keyLength];
            random.nextBytes(nonExistingKey);
            tree.search(nonExistingKey);
        }
        endTime = System.nanoTime();
        searchTime = (endTime - startTime) / 1e9;
        System.out.printf("search of %d non-existing keys: %.3f seconds\n", numInsertions, searchTime);
        System.out.printf("average search time (non-existing keys): %.3f microseconds\n", searchTime * 1e6 / numInsertions);

        // Benchmark prefix search
        startTime = System.nanoTime();
        for (byte[] key : keys) {
            tree.startsWith(Arrays.copyOf(key, keyLength / 2));
        }
        endTime = System.nanoTime();
        double prefixSearchTime = (endTime - startTime) / 1e9;
        System.out.printf("prefix search of %d keys: %.3f seconds\n", numInsertions, prefixSearchTime);
        System.out.printf("average prefix search time: %.3f microseconds\n", prefixSearchTime * 1e6 / numInsertions);

        // Memory usage estimation
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.printf("estimated memory usage: %.2f MB\n", usedMemory / (1024.0 * 1024.0));
    }
}
