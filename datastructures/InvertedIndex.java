package datastructures;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

public class InvertedIndex {
  private final ConcurrentMap<String, Set<String>> index = new ConcurrentHashMap<>();

  public void indexDocument(String documentId, String content) {
    String[] words = content.split("\\W+");
    for (String word : words) {
      index.computeIfAbsent(word.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(documentId);
    }
  }

  public Set<String> search(String term) {
    return index.getOrDefault(term.toLowerCase(), Collections.emptySet());
  }

  public void indexDirectory(Path directory) throws IOException {
    List<Path> files = Files.walk(directory)
        .filter(Files::isRegularFile)
        .collect(Collectors.toList());

    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    for (Path file : files) {
      executor.execute(() -> {
        try {
          String content = new String(Files.readAllBytes(file));
          indexDocument(file.toString(), content);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    }

    executor.shutdown();
    try {
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static void main(String[] args) {
    InvertedIndex index = new InvertedIndex();

    try {
      if (args.length > 0) {
        Path directory = Paths.get(args[0]);
        index.indexDirectory(directory);
      } else {
        // Sample in-memory indexing
        index.indexDocument("doc1", "This is a test document");
        index.indexDocument("doc2", "This document is a test");
        index.indexDocument("doc3", "Test this document");

        // Stress test
        stressTestInMem();
      }

      // Sample search
      Set<String> results = index.search("test");
      System.out.println("Documents containing 'test': " + results);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void stressTestFS(InvertedIndex index) {
    final int NUM_DOCUMENTS = 1000;
    final int NUM_SEARCHES = 1000;

    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // Create and index random documents
    for (int i = 0; i < NUM_DOCUMENTS; i++) {
      final int docId = i;
      executor.execute(() -> {
        String content = generateRandomContent();
        index.indexDocument("doc" + docId, content);
      });
    }

    executor.shutdown();
    try {
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Perform random searches
    long startTime = System.nanoTime();
    for (int i = 0; i < NUM_SEARCHES; i++) {
      String term = generateRandomTerm();
      index.search(term);
    }
    long endTime = System.nanoTime();

    System.out.printf("Time to perform %d searches: %.2f ms%n", NUM_SEARCHES, (endTime - startTime) / 1e6);
  }

  private static String generateRandomContent() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      sb.append(generateRandomTerm()).append(" ");
    }
    return sb.toString();
  }

  private static String generateRandomTerm() {
    int length = ThreadLocalRandom.current().nextInt(3, 10);
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char c = (char) ('a' + ThreadLocalRandom.current().nextInt(26));
      sb.append(c);
    }
    return sb.toString();
  }

  private static String generateRandomDocumentId() {
    return "doc" + ThreadLocalRandom.current().nextInt(1000);
  }

  private static String generateRandomDocument() {
    int numWords = ThreadLocalRandom.current().nextInt(100, 1000);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < numWords; i++) {
      sb.append(generateRandomTerm()).append(" ");
    }
    return sb.toString();
  }

  private static void stressTestInMem() {
    final int NUM_THREADS = 8;
    final int NUM_DOCUMENTS = 2000;
    final int NUM_SEARCHES = 1000;

    InvertedIndex index = new InvertedIndex();
    ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

    long startTime = System.nanoTime();

    for (int i = 0; i < NUM_THREADS; i++) {
      executor.execute(() -> {
        for (int j = 0; j < NUM_DOCUMENTS; j++) {
          String documentId = generateRandomDocumentId();
          String content = generateRandomDocument();
          index.indexDocument(documentId, content);
        }
      });
    }

    executor.shutdown();
    try {
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    long endTime = System.nanoTime();
    System.out.printf("Time to index %d documents: %.2f ms%n", NUM_THREADS * NUM_DOCUMENTS,
        (endTime - startTime) / 1e6);

    startTime = System.nanoTime();
    for (int i = 0; i < NUM_SEARCHES; i++) {
      String term = generateRandomTerm();
      index.search(term);
    }
    endTime = System.nanoTime();
    System.out.printf("Time to perform %d searches: %.2f ms%n", NUM_SEARCHES, (endTime - startTime) / 1e6);
  }
}
