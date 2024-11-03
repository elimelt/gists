package functionalutils;

import java.io.IOException;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * A Stream decorator that handles checked exceptions in stream operations.
 * Allows for using methods that throw exceptions directly in stream operations.
 *
 * @param <T> The type of elements in the stream
 */
public class ThrowingStream<T> implements Stream<T> {
    private final Stream<T> delegate;

    private ThrowingStream(Stream<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates a new ThrowingStream from an existing Stream.
     *
     * @param stream The stream to decorate
     * @param <T> The type of elements in the stream
     * @return A new ThrowingStream
     */
    public static <T> ThrowingStream<T> of(Stream<T> stream) {
        return new ThrowingStream<>(stream);
    }

    /**
     * Functional interface for operations that might throw checked exceptions.
     *
     * @param <T> Input type
     * @param <R> Return type
     * @param <E> Exception type
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    /**
     * Functional interface for operations that might throw checked exceptions
     * and don't return a value.
     *
     * @param <T> Input type
     * @param <E> Exception type
     */
    @FunctionalInterface
    public interface ThrowingConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    /**
     * Functional interface for predicates that might throw checked exceptions.
     *
     * @param <T> Input type
     * @param <E> Exception type
     */
    @FunctionalInterface
    public interface ThrowingPredicate<T, E extends Exception> {
        boolean test(T t) throws E;
    }

    /**
     * Maps elements using a function that might throw an exception.
     *
     * @param mapper The mapping function that might throw
     * @param <R> The type of elements in the resulting stream
     * @param <E> The type of exception that might be thrown
     * @return A new stream with mapped elements
     */
    public <R, E extends Exception> ThrowingStream<R> mapThrowing(ThrowingFunction<? super T, ? extends R, E> mapper) {
        return new ThrowingStream<>(delegate.map(t -> {
            try {
                return mapper.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    /**
     * Filters elements using a predicate that might throw an exception.
     *
     * @param predicate The filtering predicate that might throw
     * @param <E> The type of exception that might be thrown
     * @return A new stream with filtered elements
     */
    public <E extends Exception> ThrowingStream<T> filterThrowing(ThrowingPredicate<? super T, E> predicate) {
        return new ThrowingStream<>(delegate.filter(t -> {
            try {
                return predicate.test(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    /**
     * Performs an action that might throw an exception for each element.
     *
     * @param action The action to perform that might throw
     * @param <E> The type of exception that might be thrown
     * @throws RuntimeException wrapping any checked exception thrown
     */
    public <E extends Exception> void forEachThrowing(ThrowingConsumer<? super T, E> action) {
        delegate.forEach(t -> {
            try {
                action.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Handles any exception thrown by the previous operations.
     *
     * @param exceptionHandler The handler for any exceptions
     * @return The same stream for chaining
     */
    public ThrowingStream<T> onException(Consumer<Exception> exceptionHandler) {
        return new ThrowingStream<>(delegate.map(t -> {
            try {
                return t;
            } catch (Exception e) {
                exceptionHandler.accept(e);
                return null;
            }
        }).filter(Objects::nonNull));
    }

    @Override public Stream<T> filter(Predicate<? super T> predicate) {
        return delegate.filter(predicate);
    }

    @Override public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return delegate.map(mapper);
    }

    @Override public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return delegate.mapToInt(mapper);
    }

    @Override public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return delegate.mapToLong(mapper);
    }

    @Override public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return delegate.mapToDouble(mapper);
    }

    @Override public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return delegate.flatMap(mapper);
    }

    @Override public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return delegate.flatMapToInt(mapper);
    }

    @Override public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return delegate.flatMapToLong(mapper);
    }

    @Override public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return delegate.flatMapToDouble(mapper);
    }

    @Override public Stream<T> distinct() {
        return delegate.distinct();
    }

    @Override public Stream<T> sorted() {
        return delegate.sorted();
    }

    @Override public Stream<T> sorted(Comparator<? super T> comparator) {
        return delegate.sorted(comparator);
    }

    @Override public Stream<T> peek(Consumer<? super T> action) {
        return delegate.peek(action);
    }

    @Override public Stream<T> limit(long maxSize) {
        return delegate.limit(maxSize);
    }

    @Override public Stream<T> skip(long n) {
        return delegate.skip(n);
    }

    @Override public void forEach(Consumer<? super T> action) {
        delegate.forEach(action);
    }

    @Override public void forEachOrdered(Consumer<? super T> action) {
        delegate.forEachOrdered(action);
    }

    @Override public Object[] toArray() {
        return delegate.toArray();
    }

    @Override public <A> A[] toArray(IntFunction<A[]> generator) {
        return delegate.toArray(generator);
    }

    @Override public T reduce(T identity, BinaryOperator<T> accumulator) {
        return delegate.reduce(identity, accumulator);
    }

    @Override public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return delegate.reduce(accumulator);
    }

    @Override public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return delegate.reduce(identity, accumulator, combiner);
    }

    @Override public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return delegate.collect(supplier, accumulator, combiner);
    }

    @Override public <R, A> R collect(Collector<? super T, A, R> collector) {
        return delegate.collect(collector);
    }

    @Override public Optional<T> min(Comparator<? super T> comparator) {
        return delegate.min(comparator);
    }

    @Override public Optional<T> max(Comparator<? super T> comparator) {
        return delegate.max(comparator);
    }

    @Override public long count() {
        return delegate.count();
    }

    @Override public boolean anyMatch(Predicate<? super T> predicate) {
        return delegate.anyMatch(predicate);
    }

    @Override public boolean allMatch(Predicate<? super T> predicate) {
        return delegate.allMatch(predicate);
    }

    @Override public boolean noneMatch(Predicate<? super T> predicate) {
        return delegate.noneMatch(predicate);
    }

    @Override public Optional<T> findFirst() {
        return delegate.findFirst();
    }

    @Override public Optional<T> findAny() {
        return delegate.findAny();
    }

    @Override public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override public Spliterator<T> spliterator() {
        return delegate.spliterator();
    }

    @Override public boolean isParallel() {
        return delegate.isParallel();
    }

    @Override public Stream<T> sequential() {
        return delegate.sequential();
    }

    @Override public Stream<T> parallel() {
        return delegate.parallel();
    }

    @Override public Stream<T> unordered() {
        return delegate.unordered();
    }

    @Override public Stream<T> onClose(Runnable closeHandler) {
        return delegate.onClose(closeHandler);
    }

    @Override public void close() {
        delegate.close();
    }

    public static void main(String[] args) {
        List<String> paths = Arrays.asList("file1.txt", "file2.txt", "file3.txt");

        ThrowingStream.of(paths.stream())
            .mapThrowing(path -> {
                if (path.equals("file2.txt")) {
                    throw new IOException("Simulated IO error");
                }
                return path.toUpperCase();
            })
            .onException(e -> System.out.println("Error processing file: " + e.getMessage()))
            .forEach(System.out::println);

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

        ThrowingStream.of(numbers.stream())
            .filterThrowing(n -> {
                if (n == 3) throw new IllegalStateException("Bad number");
                return n % 2 == 0;
            })
            .mapThrowing(n -> {
                if (n == 4) throw new ArithmeticException("Bad calculation");
                return n * 2;
            })
            .onException(e -> System.out.println("Error processing number: " + e.getMessage()))
            .forEach(System.out::println);
    }
}