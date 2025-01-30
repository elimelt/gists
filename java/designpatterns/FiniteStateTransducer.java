package designpatterns;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A genericimplementation of a Finite State Transducer.
 *
 * @param <I> The input symbol type
 * @param <O> The output symbol type
 * @param <S> The state type
 */
public class FiniteStateTransducer<I, O, S> {
  // kept for debugging and in case of extension, but can optionally be removed
  @SuppressWarnings("unused")
  private final Set<S> states;

  private final Map<S, Map<I, Transition<I, O, S>>> transitions;
  private final Set<S> acceptingStates;
  private final S initialState;

  private static final class Transition<I, O, S> {
    final S nextState;
    final O output;

    Transition(S nextState, O output) {
      this.nextState = nextState;
      this.output = output;
    }
  }

  private FiniteStateTransducer(Builder<I, O, S> builder) {
    this.transitions = Collections.unmodifiableMap(builder.transitions);
    this.states = Collections.unmodifiableSet(builder.states);
    this.acceptingStates = Collections.unmodifiableSet(builder.acceptingStates);
    this.initialState = builder.initialState;
  }

  /**
   * Process an input sequence and produce the corresponding output sequence.
   * Returns null if the input sequence is not accepted by the FST.
   *
   * @param input The input sequence to process
   * @return The output sequence if accepted, null otherwise
   */
  public List<O> process(List<I> input) {
    if (input == null || input.isEmpty()) {
      return Collections.emptyList();
    }

    List<O> output = new ArrayList<>(input.size());
    S currentState = initialState;

    for (I symbol : input) {
      Map<I, Transition<I, O, S>> stateTransitions = transitions.get(currentState);
      if (stateTransitions == null) {
        return null;
      }

      Transition<I, O, S> transition = stateTransitions.get(symbol);
      if (transition == null) {
        return null;
      }

      output.add(transition.output);
      currentState = transition.nextState;
    }

    return acceptingStates.contains(currentState) ? output : null;
  }

  /**
   * Builder for FST
   */
  public static class Builder<I, O, S> {
    private final Map<S, Map<I, Transition<I, O, S>>> transitions;
    private final Set<S> states;
    private final Set<S> acceptingStates;
    private S initialState;

    public Builder() {
      this.transitions = new HashMap<>();
      this.states = new HashSet<>();
      this.acceptingStates = new HashSet<>();
    }

    public Builder<I, O, S> addState(S state, boolean accepting) {
      states.add(state);
      if (accepting) {
        acceptingStates.add(state);
      }
      return this;
    }

    public Builder<I, O, S> setInitialState(S state) {
      if (!states.contains(state)) {
        throw new IllegalArgumentException("Initial state must be added first");
      }
      this.initialState = state;
      return this;
    }

    public Builder<I, O, S> addTransition(S fromState, I input, S toState, O output) {
      if (!states.contains(fromState) || !states.contains(toState)) {
        throw new IllegalArgumentException("States must be added before transitions");
      }

      transitions.computeIfAbsent(fromState, k -> new HashMap<>())
          .put(input, new Transition<>(toState, output));
      return this;
    }

    public FiniteStateTransducer<I, O, S> build() {
      if (initialState == null) {
        throw new IllegalStateException("Initial state must be set");
      }
      return new FiniteStateTransducer<>(this);
    }
  }

  /**
   * Class purely for benchmarking, not needed for the FST itself. However, it is useful
   * to have on hand in case you want to tune your JVM for better performance.
   */
  private static class Benchmark<I, O, S> {
    private static final int WARMUP_ITERATIONS = 5;
    private static final int WARMUP_SECONDS = 10;
    private static final int MEASUREMENT_SECONDS = 30;
    private static final int MIN_SAMPLE_SIZE = 1000;

    private final FiniteStateTransducer<I, O, S> fst;
    private final List<I> possibleInputs;
    private final int inputLength;
    private final List<List<I>> testInputs;
    private final ThreadLocalRandom random;

    public Benchmark(FiniteStateTransducer<I, O, S> fst, List<I> possibleInputs, int inputLength) {
      this.fst = fst;
      this.possibleInputs = possibleInputs;
      this.inputLength = inputLength;
      this.random = ThreadLocalRandom.current();
      this.testInputs = generateTestInputs(MIN_SAMPLE_SIZE);
    }

    private List<List<I>> generateTestInputs(int count) {
      List<List<I>> inputs = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        List<I> input = new ArrayList<>(inputLength);
        for (int j = 0; j < inputLength; j++) {
          input.add(possibleInputs.get(random.nextInt(possibleInputs.size())));
        }
        inputs.add(input);
      }
      return inputs;
    }

    private void runForDuration(int seconds) {
      long endTime = System.nanoTime() + seconds * 1_000_000_000L;
      int inputIdx = 0;

      while (System.nanoTime() < endTime) {
        fst.process(testInputs.get(inputIdx));
        inputIdx = (inputIdx + 1) % testInputs.size();

        // avoid caching to make more realistic
        if (inputIdx == 0) {
          testInputs.set(random.nextInt(testInputs.size()),
              generateTestInputs(1).get(0));
        }
      }
    }

    public BenchmarkResult run() {
      System.gc();

      System.out.println("Starting warmup phase...");
      for (int i = 0; i < WARMUP_ITERATIONS; i++) {
        runForDuration(WARMUP_SECONDS);
      }

      System.out.println("Starting measurement phase...");
      List<Double> throughputs = new ArrayList<>();
      List<Double> latencies = new ArrayList<>();

      long startTime = System.nanoTime();
      long endTime = startTime + MEASUREMENT_SECONDS * 1_000_000_000L;
      int operationCount = 0;

      while (System.nanoTime() < endTime) {
        long opStart = System.nanoTime();
        fst.process(testInputs.get(operationCount % testInputs.size()));
        long opEnd = System.nanoTime();

        operationCount++;
        latencies.add((double) (opEnd - opStart));

        // Calculate throughput every second
        if (operationCount % 1000 == 0) {
          long elapsed = System.nanoTime() - startTime;
          double throughput = (double) operationCount / (elapsed / 1_000_000_000.0);
          throughputs.add(throughput);
        }
      }

      return new BenchmarkResult(throughputs, latencies, operationCount);
    }
  }

  private static class BenchmarkResult {
    private final List<Double> throughputs;
    private final List<Double> latencies;
    private final int totalOperations;

    public BenchmarkResult(List<Double> throughputs, List<Double> latencies, int totalOperations) {
      this.throughputs = throughputs;
      this.latencies = latencies;
      this.totalOperations = totalOperations;
    }

    public double getAverageThroughput() {
      return throughputs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double getP99Latency() {
      Collections.sort(latencies);
      int index = (int) (latencies.size() * 0.99);
      return latencies.get(index);
    }

    public double getMedianLatency() {
      Collections.sort(latencies);
      return latencies.get(latencies.size() / 2);
    }

    @Override
    public String toString() {
      return String.format(
          "Benchmark Results:%n" +
              "  Total Operations: %d%n" +
              "  Avg Throughput: %.2f ops/sec%n" +
              "  Median Latency: %.2f ns%n" +
              "  P99 Latency: %.2f ns",
          totalOperations,
          getAverageThroughput(),
          getMedianLatency(),
          getP99Latency());
    }
  }

  /**
   * Main method to run benchmarks
   *
   * @param args Command-line arguments (unused)
   */
  public static void main(String[] args) {
    FiniteStateTransducer<String, String, String> nlpFst = new Builder<String, String, String>()
        .addState("START", false)
        .addState("SUBJECT", false)
        .addState("VERB", false)
        .addState("OBJECT", true)
        .addState("ADJECTIVE", false)
        .setInitialState("START")
        .addTransition("START", "NOUN", "SUBJECT", "SUBJECT")
        .addTransition("START", "PRONOUN", "SUBJECT", "SUBJECT")
        .addTransition("START", "ADJECTIVE", "ADJECTIVE", "MODIFIER")
        .addTransition("ADJECTIVE", "NOUN", "SUBJECT", "SUBJECT")
        .addTransition("SUBJECT", "VERB", "VERB", "PREDICATE")
        .addTransition("VERB", "NOUN", "OBJECT", "OBJECT")
        .addTransition("VERB", "PRONOUN", "OBJECT", "OBJECT")
        .addTransition("VERB", "ADJECTIVE", "ADJECTIVE", "MODIFIER")
        .addTransition("ADJECTIVE", "NOUN", "OBJECT", "OBJECT")
        .build();

    List<String> possibleInputs = Arrays.asList(
        "NOUN", "PRONOUN", "VERB", "ADJECTIVE");

    System.out.println("Running performance benchmarks... (this should take a while)");
    System.out.println("============================================");

    int[] inputLengths = { 10, 50, 100 };
    for (int length : inputLengths) {
      System.out.printf("%nBenchmarking with input length: %d%n", length);
      Benchmark<String, String, String> benchmark = new Benchmark<>(nlpFst, possibleInputs, length);
      BenchmarkResult result = benchmark.run();
      System.out.println(result);
    }

    List<String> input = Arrays.asList(
        "PRONOUN", "VERB", "ADJECTIVE", "NOUN");
    List<String> output = nlpFst.process(input);
    System.out.println("\nValidation Example:");
    System.out.println("Input:  " + input);
    System.out.println("Output: " + output);
  }
}