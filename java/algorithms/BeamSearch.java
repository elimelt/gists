package algorithms;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Beam search.
 *
 * @param <T> The type of state in the search space
 */
public class BeamSearch<T> {

  @FunctionalInterface
  public interface StateScorer<T> {
    double score(T state);
  }

  @FunctionalInterface
  public interface StateExpander<T> {
    Collection<T> expand(T state);
  }

  private final int beamWidth;
  private final int maxIterations;
  private final StateScorer<T> scorer;
  private final StateExpander<T> expander;
  private final Predicate<T> goalTest;
  private final boolean trackFullPath;
  private final boolean useScoreCache;
  private final double pruningThreshold;

  private BeamSearch(Builder<T> builder) {
    this.beamWidth = builder.beamWidth;
    this.maxIterations = builder.maxIterations;
    this.scorer = builder.scorer;
    this.expander = builder.expander;
    this.goalTest = builder.goalTest;
    this.trackFullPath = builder.trackFullPath;
    this.useScoreCache = builder.useScoreCache;
    this.pruningThreshold = builder.pruningThreshold;
  }

  /**
   * Represents a state and its score in the beam search.
   */
  private static final class ScoredState<T> implements Comparable<ScoredState<T>> {
    final T state;
    final double score;
    final ScoredState<T> parent;

    ScoredState(T state, double score, ScoredState<T> parent) {
      this.state = state;
      this.score = score;
      this.parent = parent;
    }

    @Override
    public int compareTo(ScoredState<T> other) {

      return Double.compare(other.score, this.score);
    }
  }

  /**
   * Single initial state.
   */
  public Optional<T> search(T initialState) {
    T[] initialArray = (T[]) new Object[1];
    initialArray[0] = initialState;
    return searchInternal(initialArray, 1);
  }

  /**
   * Multiple initial states.
   */
  public Optional<T> search(Collection<T> initialStates) {
    if (initialStates.isEmpty()) {
      return Optional.empty();
    }

    int size = initialStates.size();
    T[] initialArray = (T[]) new Object[size];
    int i = 0;
    for (T state : initialStates) {
      initialArray[i++] = state;
    }

    return searchInternal(initialArray, size);
  }

  /**
   * Internal search with arrays.
   */
  private Optional<T> searchInternal(T[] initialStates, int stateCount) {
    HashMap<T, Double> scoreCache = useScoreCache ? new HashMap<>(beamWidth * 4) : null;

    @SuppressWarnings("unchecked")
    ScoredState<T>[] beam = new ScoredState[beamWidth];
    int beamSize = 0;

    for (int i = 0; i < stateCount; i++) {
      T state = initialStates[i];
      double score = scoreState(state, scoreCache);

      if (beamSize < beamWidth || score > beam[beamSize - 1].score) {
        ScoredState<T> scoredState = new ScoredState<>(state, score, null);

        if (beamSize < beamWidth) {
          int insertPos = beamSize;
          while (insertPos > 0 && beam[insertPos - 1].score < score) {
            beam[insertPos] = beam[insertPos - 1];
            insertPos--;
          }
          beam[insertPos] = scoredState;
          beamSize++;
        } else {
          int insertPos = beamSize - 1;
          while (insertPos > 0 && beam[insertPos - 1].score < score) {
            beam[insertPos] = beam[insertPos - 1];
            insertPos--;
          }
          beam[insertPos] = scoredState;
        }
      }
    }

    ScoredState<T> bestGoalState = null;
    ArrayList<ScoredState<T>> candidates = new ArrayList<>(beamWidth * 8);

    for (int iteration = 0; iteration < maxIterations; iteration++) {
      for (int i = 0; i < beamSize; i++) {
        ScoredState<T> current = beam[i];
        if (goalTest.test(current.state)) {
          if (bestGoalState == null || current.score > bestGoalState.score) {
            bestGoalState = current;
          }
        }
      }

      if (bestGoalState != null) {
        if (trackFullPath) {
          return Optional.of(reconstructPath(bestGoalState));
        }
        return Optional.of(bestGoalState.state);
      }

      candidates.clear();
      double bestScoreSoFar = beamSize > 0 ? beam[0].score : Double.NEGATIVE_INFINITY;
      double currentThreshold = bestScoreSoFar * pruningThreshold;

      for (int i = 0; i < beamSize; i++) {
        ScoredState<T> current = beam[i];
        Collection<T> successors = expander.expand(current.state);

        for (T successor : successors) {
          double successorScore = scoreState(successor, scoreCache);

          if (successorScore < currentThreshold) {
            continue;
          }

          candidates.add(new ScoredState<>(
              successor, successorScore, trackFullPath ? current : null));
        }
      }

      beamSize = 0;

      if (candidates.size() <= 64) {

        for (int i = 1; i < candidates.size(); i++) {
          ScoredState<T> key = candidates.get(i);
          int j = i - 1;
          while (j >= 0 && candidates.get(j).score < key.score) {
            candidates.set(j + 1, candidates.get(j));
            j--;
          }
          candidates.set(j + 1, key);
        }
      } else {
        Collections.sort(candidates);
      }

      int candidatesToKeep = Math.min(beamWidth, candidates.size());
      for (int i = 0; i < candidatesToKeep; i++) {
        beam[beamSize++] = candidates.get(i);
      }

      if (beamSize == 0) {
        break;
      }
    }

    if (beamSize > 0) {
      ScoredState<T> bestState = beam[0];
      if (trackFullPath) {
        return Optional.of(reconstructPath(bestState));
      }
      return Optional.of(bestState.state);
    }

    return Optional.empty();
  }

  /**
   * Gets the score for a state, using cache if enabled.
   */
  private double scoreState(T state, HashMap<T, Double> scoreCache) {
    if (scoreCache != null) {
      Double cachedScore = scoreCache.get(state);
      if (cachedScore != null) {
        return cachedScore;
      }
      double score = scorer.score(state);
      scoreCache.put(state, score);
      return score;
    }
    return scorer.score(state);
  }

  /**
   * Reconstructs the path from the initial state to the goal state.
   */
  private T reconstructPath(ScoredState<T> goalState) {
    if (!trackFullPath) {
      return goalState.state;
    }

    ScoredState<T> current = goalState;
    while (current.parent != null) {
      current = current.parent;
    }

    return current.state;
  }

  /**
   * Builder for configuring and creating a BeamSearch instance.
   */
  public static class Builder<T> {
    private final StateScorer<T> scorer;
    private final StateExpander<T> expander;
    private final Predicate<T> goalTest;

    private int beamWidth = 5;
    private int maxIterations = 100;
    private boolean trackFullPath = false;
    private boolean useScoreCache = true;
    private double pruningThreshold = 0.5;

    public Builder(StateScorer<T> scorer, StateExpander<T> expander, Predicate<T> goalTest) {
      this.scorer = Objects.requireNonNull(scorer, "Scorer cannot be null");
      this.expander = Objects.requireNonNull(expander, "Expander cannot be null");
      this.goalTest = Objects.requireNonNull(goalTest, "Goal test cannot be null");
    }

    public Builder<T> withBeamWidth(int beamWidth) {
      if (beamWidth <= 0) {
        throw new IllegalArgumentException("Beam width must be positive");
      }
      this.beamWidth = beamWidth;
      return this;
    }

    public Builder<T> withMaxIterations(int maxIterations) {
      if (maxIterations <= 0) {
        throw new IllegalArgumentException("Max iterations must be positive");
      }
      this.maxIterations = maxIterations;
      return this;
    }

    public Builder<T> withPathTracking(boolean trackFullPath) {
      this.trackFullPath = trackFullPath;
      return this;
    }

    public Builder<T> withScoreCache(boolean useScoreCache) {
      this.useScoreCache = useScoreCache;
      return this;
    }

    public Builder<T> withPruningThreshold(double pruningThreshold) {
      if (pruningThreshold < 0.0 || pruningThreshold > 1.0) {
        throw new IllegalArgumentException("Pruning threshold must be between 0.0 and 1.0");
      }
      this.pruningThreshold = pruningThreshold;
      return this;
    }

    public BeamSearch<T> build() {
      return new BeamSearch<>(this);
    }
  }

  public static void main(String[] args) {

    final int DATASET_SIZE = 10_000_000;
    final int DIMENSION_SIZE = 10_000;
    final int BEAM_WIDTH = 500;
    final int MAX_ITERATIONS = 30;
    final int NUM_BENCHMARK_RUNS = 5;
    final boolean TRACK_PATH = false;
    final boolean WARM_UP_JVM = true;

    class Point {
      final int x, y;

      Point(int x, int y) {
        this.x = x;
        this.y = y;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o)
          return true;
        if (o == null || getClass() != o.getClass())
          return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
      }

      @Override
      public int hashCode() {

        return x * 31 + y;
      }

      @Override
      public String toString() {
        return "(" + x + "," + y + ")";
      }
    }

    System.out.println("Beam Search Performance Benchmark");
    System.out.println("=================================");
    System.out.println("Dataset size: " + DATASET_SIZE);
    System.out.println("Grid dimension: " + DIMENSION_SIZE + "x" + DIMENSION_SIZE);
    System.out.println("Beam width: " + BEAM_WIDTH);
    System.out.println("Max iterations: " + MAX_ITERATIONS);
    System.out.println("Path tracking: " + TRACK_PATH);
    System.out.println();

    System.out.println("Generating random dataset...");
    Random random = new Random(42);

    Point[] initialDataset = new Point[DATASET_SIZE];
    for (int i = 0; i < DATASET_SIZE; i++) {
      initialDataset[i] = new Point(
          random.nextInt(DIMENSION_SIZE),
          random.nextInt(DIMENSION_SIZE));
    }

    final Point goal = new Point(DIMENSION_SIZE - 1, DIMENSION_SIZE - 1);

    StateScorer<Point> scorer = point -> {

      return -1 * (Math.abs(point.x - goal.x) + Math.abs(point.y - goal.y));
    };

    StateExpander<Point> expander = point -> {

      Point[] successors = new Point[8];
      int count = 0;

      int[] dx = { 1, -1, 0, 0, 1, -1, 1, -1 };
      int[] dy = { 0, 0, 1, -1, 1, 1, -1, -1 };

      for (int i = 0; i < 8; i++) {
        int newX = point.x + dx[i];
        int newY = point.y + dy[i];

        if (newX >= 0 && newX < DIMENSION_SIZE && newY >= 0 && newY < DIMENSION_SIZE) {
          successors[count++] = new Point(newX, newY);
        }
      }

      if (count < 8) {
        Point[] trimmed = new Point[count];
        System.arraycopy(successors, 0, trimmed, 0, count);
        return Arrays.asList(trimmed);
      }

      return Arrays.asList(successors);
    };

    Predicate<Point> goalTest = point -> Math.abs(point.x - goal.x) <= 5 && Math.abs(point.y - goal.y) <= 5;

    if (WARM_UP_JVM) {
      System.out.println("Warming up JVM...");

      Point[] warmupDataset = Arrays.copyOf(initialDataset, Math.min(1000, DATASET_SIZE));

      BeamSearch<Point> warmupSearch = new BeamSearch.Builder<>(scorer, expander, goalTest)
          .withBeamWidth(50)
          .withMaxIterations(5)
          .withPathTracking(TRACK_PATH)
          .withScoreCache(true)
          .withPruningThreshold(0.5)
          .build();

      warmupSearch.search(Arrays.asList(warmupDataset));

      for (int i = 0; i < 5; i++) {
        System.gc();
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      System.out.println("Warm-up completed");
      System.out.println();
    }

    System.out.println("Running benchmark...");
    System.out.printf("%-15s %-15s %-15s %-15s %-20s%n",
        "Run", "Time (ms)", "Path Found", "Distance", "Goal State");
    System.out.println("----------------------------------------------------------------");

    Point[] benchmarkDataset = Arrays.copyOf(initialDataset, Math.min(1_000_000, DATASET_SIZE));

    long[] times = new long[NUM_BENCHMARK_RUNS];

    for (int run = 0; run < NUM_BENCHMARK_RUNS; run++) {

      System.gc();

      BeamSearch<Point> beamSearch = new BeamSearch.Builder<>(scorer, expander, goalTest)
          .withBeamWidth(BEAM_WIDTH)
          .withMaxIterations(MAX_ITERATIONS)
          .withPathTracking(TRACK_PATH)
          .withScoreCache(true)
          .withPruningThreshold(0.5)
          .build();

      long startTime = System.nanoTime();
      Optional<Point> result = beamSearch.search(Arrays.asList(benchmarkDataset));
      long endTime = System.nanoTime();

      times[run] = (endTime - startTime) / 1_000_000;

      String pathFound = result.isPresent() ? "Yes" : "No";
      String distance = result.isPresent()
          ? String.valueOf(Math.abs(result.get().x - goal.x) + Math.abs(result.get().y - goal.y))
          : "N/A";
      String goalState = result.isPresent() ? result.get().toString() : "None";

      System.out.printf("%-15d %-15d %-15s %-15s %-20s%n",
          run + 1, times[run], pathFound, distance, goalState);
    }

    Arrays.sort(times);
    double avgTime = 0;
    for (long time : times) {
      avgTime += time;
    }
    avgTime /= times.length;

    System.out.println("----------------------------------------------------------------");
    System.out.printf("%-15s %-15.2f %-15d %-15d%n",
        "Summary", avgTime, times[0], times[times.length - 1]);
    System.out.println("           (Average)    (Min)        (Max)");

    Runtime runtime = Runtime.getRuntime();
    runtime.gc();
    long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    long totalMemory = runtime.totalMemory() / (1024 * 1024);
    long maxMemory = runtime.maxMemory() / (1024 * 1024);

    System.out.println("\nMemory Usage:");
    System.out.println("Used Memory: " + usedMemory + " MB");
    System.out.println("Total Memory: " + totalMemory + " MB");
    System.out.println("Max Memory: " + maxMemory + " MB");
  }
}