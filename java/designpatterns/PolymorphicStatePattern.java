package designpatterns;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A high-performance implementation of the State pattern using generics.
 * This implementation uses interface-based polymorphism and lazy state transitions
 * to maximize performance.
 */
public class PolymorphicStatePattern {
    /**
     * Generic interface for state behavior
     * @param <T> Type of the context
     * @param <R> Type of the result returned by state operations
     */
    public interface State<T, R> {
        R handle(T context);
        default State<T, R> nextState() {
            return this;
        }
    }

    /**
     * Generic context class that manages states
     * @param <T> Type of the context (self-type)
     * @param <R> Type of the result returned by state operations
     */
    public static class StateContext<T extends StateContext<T, R>, R> {
        private final AtomicReference<State<T, R>> currentState;

        protected StateContext(State<T, R> initialState) {
            this.currentState = new AtomicReference<>(initialState);
        }

        @SuppressWarnings("unchecked")
        public R handleState() {
            R result = currentState.get().handle((T) this);
            State<T, R> nextState = currentState.get().nextState();
            // transition state if needed
            currentState.compareAndSet(currentState.get(), nextState);
            return result;
        }
    }

    // example implementation
    static class Document extends StateContext<Document, String> {
        private final String content;

        public Document(String content) {
            super(DraftState.INSTANCE);
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }

    // concrete state implementations
    private static final class DraftState implements State<Document, String> {
        static final DraftState INSTANCE = new DraftState();

        private DraftState() {}

        @Override
        public String handle(Document context) {
            return "Draft: " + context.getContent();
        }

        @Override
        public State<Document, String> nextState() {
            return ReviewState.INSTANCE;
        }
    }

    private static final class ReviewState implements State<Document, String> {
        static final ReviewState INSTANCE = new ReviewState();

        private ReviewState() {}

        @Override
        public String handle(Document context) {
            return "Under Review: " + context.getContent();
        }

        @Override
        public State<Document, String> nextState() {
            return PublishedState.INSTANCE;
        }
    }

    private static final class PublishedState implements State<Document, String> {
        static final PublishedState INSTANCE = new PublishedState();

        private PublishedState() {}

        @Override
        public String handle(Document context) {
            return "Published: " + context.getContent();
        }
    }

    // benchmark helper
    private static long benchmark(String name, int iterations, Supplier<String> operation) {
        // warmup
        for (int i = 0; i < 1000; i++) {
            operation.get();
        }
        
        long start = System.nanoTime();
        String result = null;
        for (int i = 0; i < iterations; i++) {
            result = operation.get();
        }
        long end = System.nanoTime();
        
        long duration = TimeUnit.NANOSECONDS.toMicros(end - start);
        System.out.printf("%s: %d µs for %d iterations (%.2f µs/op) - Last result: %s%n",
            name, duration, iterations, (double) duration / iterations, result);
        return duration;
    }

    public static void main(String[] args) {
        final int ITERATIONS = 10_000_000;
        
        // single thread benchmark
        Document doc = new Document("Hello World");
        benchmark("Single Thread", ITERATIONS, doc::handleState);

        // multi-thread benchmark
        Document sharedDoc = new Document("Shared Document");
        Thread[] threads = new Thread[4];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                benchmark("Thread-" + Thread.currentThread().getId(), 
                    ITERATIONS / threads.length, 
                    sharedDoc::handleState);
            });
            threads[i].start();
        }

        // wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
