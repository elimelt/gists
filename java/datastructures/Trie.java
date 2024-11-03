package datastructures;
import java.util.*;
import java.io.*;

public class Trie {
  private TrieNode root;

  public Trie() {
    root = new TrieNode();
  }

  private class TrieNode {
    Map<Character, TrieNode> children;
    boolean isEndOfWord;

    TrieNode() {
      children = new HashMap<>();
      isEndOfWord = false;
    }
  }

  public void insert(String word) {
    TrieNode current = root;
    for (char ch : word.toCharArray()) {
      current = current.children.computeIfAbsent(ch, c -> new TrieNode());
    }
    current.isEndOfWord = true;
  }

  public boolean search(String word) {
    TrieNode node = searchPrefix(word);
    return node != null && node.isEndOfWord;
  }

  public boolean startsWith(String prefix) {
    return searchPrefix(prefix) != null;
  }

  private TrieNode searchPrefix(String prefix) {
    TrieNode current = root;
    for (char ch : prefix.toCharArray()) {
      TrieNode node = current.children.get(ch);
      if (node == null) {
        return null;
      }
      current = node;
    }
    return current;
  }

  public static void main(String[] args) {
    int n = 2;
    Trie trie = new Trie();
    List<String> dictionary;

    long startTime = System.nanoTime();

    // Load a dictionary if provided arg
    if (args.length > 0) {
      dictionary = loadDictionary(args[0]);
      System.out.println("Dictionary size: " + dictionary.size());
    } else {
      List<String> baseWords = List.of("apple", "banana", "cherry", "date", "fig", "grape", "kiwi", "lemon", "mango",
          "orange",
          "peach", "pear", "plum", "raspberry", "strawberry", "watermelon");
      dictionary = new ArrayList<>();
      for (int i = 0; i < 1000 * n; i++) {
        for (int j = 0; j < 1000 * n; j++) {
          String w1 = baseWords.get(i % baseWords.size());
          String w2 = baseWords.get(j % baseWords.size());
          String rs = String.valueOf((long) (Math.random() * 1_000_000));
          dictionary.add(w1 + rs + w2);
        }
      }
    }

    for (String word : dictionary) {
      trie.insert(word);
    }
    long endTime = System.nanoTime();
    System.out.printf("Time to build trie: %.2f ms%n", (endTime - startTime) / 1e6);

    // Test search performance
    int numSearchTests = 1_000_000;
    Random random = new Random();
    startTime = System.nanoTime();
    int found = 0;
    for (int i = 0; i < numSearchTests; i++) {
      String word = dictionary.get(random.nextInt(dictionary.size()));
      if (trie.search(word)) {
        found++;
      }
    }
    endTime = System.nanoTime();
    System.out.printf("Search performance: %.2f ns per search%n", (endTime - startTime) / (double) numSearchTests);
    System.out.printf("Search accuracy: %.2f%%%n", (found / (double) numSearchTests) * 100);

    // Test prefix search performance
    startTime = System.nanoTime();
    int prefixFound = 0;
    for (int i = 0; i < numSearchTests; i++) {
      String word = dictionary.get(random.nextInt(dictionary.size()));
      String prefix = word.substring(0, Math.min(3, word.length()));
      if (trie.startsWith(prefix)) {
        prefixFound++;
      }
    }
    endTime = System.nanoTime();
    System.out.printf("Prefix search performance: %.2f ns per search%n",
        (endTime - startTime) / (double) numSearchTests);
    System.out.printf("Prefix search accuracy: %.2f%%%n", (prefixFound / (double) numSearchTests) * 100);
  }

  private static List<String> loadDictionary(String filename) {
    List<String> dictionary = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
      String line;
      while ((line = reader.readLine()) != null) {
        dictionary.add(line.trim().toLowerCase());
      }
    } catch (IOException e) {
      System.err.println("Error loading dictionary: " + e.getMessage());
      System.exit(1);
    }
    return dictionary;
  }
}