package datastructures;

import java.util.*;

public class SuffixTree {
    private static final int ALPHABET_SIZE = 256;

    private static class Node {
        Node[] children;
        int start;
        int end;
        Node suffixLink;

        Node(int start, int end) {
            this.children = new Node[ALPHABET_SIZE];
            this.start = start;
            this.end = end;
            this.suffixLink = null;
        }

        int edgeLength(int position) {
            return Math.min(end, position) - start + 1;
        }
    }

    private byte[] text;
    private Node root;
    private Node activeNode;
    private int activeEdge;
    private int activeLength;
    private int remainingSuffixCount;
    private int leafEnd;
    private int size;
    private Node lastNewNode;

    public SuffixTree() {
        root = new Node(-1, -1);
        activeNode = root;
        activeEdge = -1;
        activeLength = 0;
        remainingSuffixCount = 0;
        leafEnd = -1;
        size = 0;
    }

    private void extendSuffixTree(int position) {
        leafEnd = position;
        remainingSuffixCount++;
        lastNewNode = null;

        while (remainingSuffixCount > 0) {
            if (activeLength == 0) {
                activeEdge = position;
            }

            if (activeNode.children[text[activeEdge] & 0xFF] == null) {
                activeNode.children[text[activeEdge] & 0xFF] = new Node(position, leafEnd);
                if (lastNewNode != null) {
                    lastNewNode.suffixLink = activeNode;
                    lastNewNode = null;
                }
            } else {
                Node next = activeNode.children[text[activeEdge] & 0xFF];
                if (walkDown(next, position)) {
                    continue;
                }

                if (text[next.start + activeLength] == text[position]) {
                    if (lastNewNode != null && activeNode != root) {
                        lastNewNode.suffixLink = activeNode;
                        lastNewNode = null;
                    }
                    activeLength++;
                    break;
                }

                Node split = new Node(next.start, next.start + activeLength - 1);
                activeNode.children[text[activeEdge] & 0xFF] = split;
                split.children[text[position] & 0xFF] = new Node(position, leafEnd);
                next.start += activeLength;
                split.children[text[next.start] & 0xFF] = next;

                if (lastNewNode != null) {
                    lastNewNode.suffixLink = split;
                }
                lastNewNode = split;
            }

            remainingSuffixCount--;
            if (activeNode == root && activeLength > 0) {
                activeLength--;
                activeEdge = position - remainingSuffixCount + 1;
            } else if (activeNode != root) {
                activeNode = activeNode.suffixLink != null ? activeNode.suffixLink : root;
            }
        }
    }

    private boolean walkDown(Node node, int position) {
        int edgeLength = node.edgeLength(position);
        if (activeLength >= edgeLength) {
            activeEdge += edgeLength;
            activeLength -= edgeLength;
            activeNode = node;
            return true;
        }
        return false;
    }

    public void build(byte[] text) {
        this.text = text;
        int n = text.length;
        for (int i = 0; i < n; i++) {
            extendSuffixTree(i);
        }
        size = n;
    }

    public boolean search(byte[] pattern) {
        if (root == null || pattern.length == 0) {
            return false;
        }
        Node curr = root;
        int i = 0;
        while (i < pattern.length) {
            int ch = pattern[i] & 0xFF;
            if (curr.children[ch] == null) {
                return false;
            }
            Node child = curr.children[ch];
            int j = 0;
            while (j < child.edgeLength(size - 1) && i < pattern.length) {
                if ((text[child.start + j] & 0xFF) != (pattern[i] & 0xFF)) {
                    return false;
                }
                i++;
                j++;
            }
            if (j == child.edgeLength(size - 1)) {
                curr = child;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        SuffixTree tree = new SuffixTree();
        Random random = new Random();

        int n = 5_000_000;
        byte[] data = new byte[n];
        random.nextBytes(data);

        // benchmark tree construction
        long startTime = System.nanoTime();
        tree.build(data);
        long endTime = System.nanoTime();
        double constructionTime = (endTime - startTime) / 1e9;
        System.out.printf("construction of Suffix Tree for %d bytes: %.3f seconds\n", n, constructionTime);

        // benchmark search (existing patterns)
        int numSearches = 10_000;
        int patternLength = 10;
        startTime = System.nanoTime();
        for (int i = 0; i < numSearches; i++) {
            int start = random.nextInt(n - patternLength);
            byte[] pattern = Arrays.copyOfRange(data, start, start + patternLength);
            tree.search(pattern);
        }
        endTime = System.nanoTime();
        double searchTime = (endTime - startTime) / 1e9;
        System.out.printf("search of %d existing patterns: %.3f seconds\n", numSearches, searchTime);
        System.out.printf("average search time (existing patterns): %.3f microseconds\n", searchTime * 1e6 / numSearches);

        // benchmark search (non-existing patterns)
        startTime = System.nanoTime();
        for (int i = 0; i < numSearches; i++) {
            byte[] pattern = new byte[patternLength];
            random.nextBytes(pattern);
            tree.search(pattern);
        }
        endTime = System.nanoTime();
        searchTime = (endTime - startTime) / 1e9;
        System.out.printf("search of %d non-existing patterns: %.3f seconds\n", numSearches, searchTime);
        System.out.printf("average search time (non-existing patterns): %.3f microseconds\n", searchTime * 1e6 / numSearches);

        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.printf("estimated memory usage: %.2f MB\n", usedMemory / (1024.0 * 1024.0));
    }
}