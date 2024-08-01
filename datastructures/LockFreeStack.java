package datastructures;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class LockFreeStack<T> {
  private final AtomicReference<Node<T>> top = new AtomicReference<>(null);

  private static class Node<T> {
    final T item;
    Node<T> next;

    Node(T item) {
      this.item = item;
    }
  }

  public void push(T item) {
    Node<T> newNode = new Node<>(item);
    while (true) {
      Node<T> oldTop = top.get();
      newNode.next = oldTop;
      if (top.compareAndSet(oldTop, newNode)) {
        return;
      }
    }
  }

  public T pop() {
    while (true) {
      Node<T> oldTop = top.get();
      if (oldTop == null) {
        return null;
      }
      Node<T> newTop = oldTop.next;
      if (top.compareAndSet(oldTop, newTop)) {
        return oldTop.item;
      }
    }
  }

  // Stress test
  public static void main(String[] args) {
    LockFreeStack<Integer> stack = new LockFreeStack<>();

    // Stress test parameters
    int numThreads = 8;
    int numElements = 10_000_000;

    long startTime = System.currentTimeMillis();
    Thread[] threads = new Thread[numThreads];
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread(() -> {
        for (int j = 0; j < numElements; j++) {
          stack.push(j);
        }
      });
      threads[i].start();
    }

    long endTime = System.currentTimeMillis();
    System.out.printf("Push time: %d ms\n", endTime - startTime);

    startTime = System.currentTimeMillis();
    for (int i = 0; i < numThreads; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    endTime = System.currentTimeMillis();
    System.out.println("Stack size: "
        + IntStream.range(0, numElements).mapToObj(j -> stack.pop()).filter(item -> item != null).count());
    System.out.printf("Time taken to push %d elements: %d ms\n", numThreads * numElements, endTime - startTime);

    startTime = System.currentTimeMillis();
    for (int i = 0; i < numThreads; i++) {
      threads[i] = new Thread(() -> {
        for (int j = 0; j < numElements; j++) {
          stack.pop();
        }
      });
      threads[i].start();
    }

    endTime = System.currentTimeMillis();
    System.out.printf("Pop time: %d ms\n", endTime - startTime);

    startTime = System.currentTimeMillis();
    for (int i = 0; i < numThreads; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    endTime = System.currentTimeMillis();
    System.out.printf("Time taken to pop %d elements: %d ms\n", numThreads * numElements, endTime - startTime);
  }
}