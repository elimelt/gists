package designpatterns;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A low-level performance-focused implementation of the State pattern.
 * This implementation uses enums and inlined state transitions to maximize
 * performance.
 */
public class InlineStatePattern {
    /**
     * Enum representing various states in the State pattern.
     */
    public enum DocumentState {
        DRAFT {
            @Override
            public String handle(Document context) {
                return "Draft: " + context.getContent();
            }

            @Override
            public DocumentState nextState() {
                return REVIEW;
            }
        },
        REVIEW {
            @Override
            public String handle(Document context) {
                return "Under Review: " + context.getContent();
            }

            @Override
            public DocumentState nextState() {
                return PUBLISHED;
            }
        },
        PUBLISHED {
            @Override
            public String handle(Document context) {
                return "Published: " + context.getContent();
            }

            @Override
            public DocumentState nextState() {
                return this; // No transition out of published
            }
        };

        public abstract String handle(Document context);

        public abstract DocumentState nextState();
    }

    /**
     * Context that manages the state of the document.
     */
    public static class Document {
        private volatile DocumentState currentState;
        private final String content;

        public Document(String content) {
            this.content = content;
            this.currentState = DocumentState.DRAFT;
        }

        public String getContent() {
            return content;
        }

        public String handleState() {
            DocumentState state = currentState;
            String result = state.handle(this);

            DocumentState nextState = state.nextState();
            // Atomic state transition if state has changed
            if (nextState != state) {
                currentState = nextState;
            }
            return result;
        }
    }

    // Optimized benchmark with loop unrolling for even tighter loop performance
    private static long benchmark(String name, int iterations, Supplier<String> operation) {
        // Warmup
        for (int i = 0; i < 1000; i++) {
            operation.get();
        }

        long start = System.nanoTime();
        String result = null;

        int i = 0;
        // Loop unrolling in blocks of 4
        for (; i + 4 <= iterations; i += 4) {
            result = operation.get();
            result = operation.get();
            result = operation.get();
            result = operation.get();
        }
        // Remaining iterations
        for (; i < iterations; i++) {
            result = operation.get();
        }

        long end = System.nanoTime();

        long duration = TimeUnit.NANOSECONDS.toMicros(end - start);
        System.out.printf("%s: %d µs for %d iterations (%.2f µs/op) - Last result: %s%n",
                name, duration, iterations, (double) duration / iterations, result);
        return duration;
    }

    public static void main(String[] args) {
        // Document sharedDoc2 = new Document("Multi-thread Test");
        // Thread[] threads2 = new Thread[4];
        // for (int i = 0; i < threads2.length; i++) {
        //     threads2[i] = new Thread(() -> {
        //         benchmark("Polymorphic Pattern ignore - Multi-thread Low Contention", 2_500_000, sharedDoc2::handleState);
        //     });
        //     threads2[i].start();
        // }

        Document sharedDoc1 = new InlineStatePattern.Document("Multi-thread Test");
        Thread[] threads1 = new Thread[4];
        for (int i = 0; i < threads1.length; i++) {
            threads1[i] = new Thread(() -> {
                benchmark("Inline Pattern ignore - Multi-thread Low Contention", 2_500_000, sharedDoc1::handleState);
            });
            threads1[i].start();
        }

        Document sharedDoc2 = new InlineStatePattern.Document("Multi-thread Test");
        Thread[] threads2 = new Thread[4];
        for (int i = 0; i < threads2.length; i++) {
            threads2[i] = new Thread(() -> {
                benchmark("Inline Pattern - Multi-thread Low Contention", 2_500_000, sharedDoc2::handleState);
            });
            threads2[i].start();
        }
    }
}
