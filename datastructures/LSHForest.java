package datastructures;
import java.util.*;
import java.util.stream.IntStream;

public class LSHForest {
  private final int numTrees;
  private final int numHashes;
  private final int vectorSize;
  private final List<List<BitSet>> hashTables;

  public LSHForest(int numTrees, int numHashes, int vectorSize) {
    this.numTrees = numTrees;
    this.numHashes = numHashes;
    this.vectorSize = vectorSize;
    this.hashTables = new ArrayList<>(numTrees);
    for (int i = 0; i < numTrees; i++) {
      hashTables.add(new ArrayList<>());
    }
  }

  public void insert(double[] vector) {
    for (int i = 0; i < numTrees; i++) {
      BitSet signature = computeSignature(vector, i);
      hashTables.get(i).add(signature);
    }
  }

  public List<Integer> query(double[] vector, int k) {
    Set<Integer> candidates = new HashSet<>();
    for (int i = 0; i < numTrees; i++) {
      BitSet signature = computeSignature(vector, i);
      List<BitSet> table = hashTables.get(i);
      for (int j = 0; j < table.size(); j++) {
        if (hammingDistance(signature, table.get(j)) <= numHashes / 2) {
          candidates.add(j);
        }
      }
    }

    return candidates.stream()
        .sorted(Comparator.comparingDouble(c -> euclideanDistance(vector, c)))
        .limit(k)
        .toList();
  }

  private BitSet computeSignature(double[] vector, int treeIndex) {
    Random random = new Random(treeIndex);
    BitSet signature = new BitSet(numHashes);
    for (int i = 0; i < numHashes; i++) {
      double[] randomVector = IntStream.range(0, vectorSize)
          .mapToDouble(j -> random.nextGaussian())
          .toArray();
      double dotProduct = IntStream.range(0, vectorSize)
          .mapToDouble(j -> vector[j] * randomVector[j])
          .sum();
      signature.set(i, dotProduct >= 0);
    }
    return signature;
  }

  private int hammingDistance(BitSet bs1, BitSet bs2) {
    BitSet xor = (BitSet) bs1.clone();
    xor.xor(bs2);
    return xor.cardinality();
  }

  private double euclideanDistance(double[] v1, int v2Index) {
    double[] v2 = new double[vectorSize]; // In a real implementation, you'd retrieve the actual vector
    return Math.sqrt(IntStream.range(0, vectorSize)
        .mapToDouble(i -> Math.pow(v1[i] - v2[i], 2))
        .sum());
  }

  // Stress test
  public static void main(String[] args) {
    int numVectors = 10_000;
    int vectorSize = 128;
    int numTrees = 10;
    int numHashes = 8;
    int k = 10;

    LSHForest lshForest = new LSHForest(numTrees, numHashes, vectorSize);
    Random random = new Random();

    // Insert vectors
    long startTime = System.nanoTime();
    for (int i = 0; i < numVectors; i++) {
      double[] vector = IntStream.range(0, vectorSize)
          .mapToDouble(j -> random.nextGaussian())
          .toArray();
      lshForest.insert(vector);
    }
    long endTime = System.nanoTime();
    System.out.printf("Time to insert %d vectors: %.2f ms%n", numVectors, (endTime - startTime) / 1e6);

    // Query vectors
    int numQueries = 1000;
    startTime = System.nanoTime();
    for (int i = 0; i < numQueries; i++) {
      double[] queryVector = IntStream.range(0, vectorSize)
          .mapToDouble(j -> random.nextGaussian())
          .toArray();
      List<Integer> results = lshForest.query(queryVector, k);
    }
    endTime = System.nanoTime();
    System.out.printf("Time for %d queries: %.2f ms%n", numQueries, (endTime - startTime) / 1e6);
    System.out.printf("Average query time: %.2f ms%n", (endTime - startTime) / (numQueries * 1e6));
  }
}