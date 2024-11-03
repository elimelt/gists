package graph;

import java.util.*;

public class WeightedDirectedGraph<T> implements WeightedGraph<T>, DirectedGraph<T> {
  private final Map<T, List<Edge<T>>> adjacencyList = new HashMap<>();

  @Override
  public void addEdge(T source, T destination) {
    addEdge(source, destination, 1);
  }

  @Override
  public void addEdge(T source, T destination, int weight) {
    adjacencyList.computeIfAbsent(source, k -> new ArrayList<>()).add(new Edge<>(destination, weight));
    adjacencyList.computeIfAbsent(destination, k -> new ArrayList<>()); // Ensure the destination node is in the graph
  }

  @Override
  public int getEdgeWeight(T source, T destination) {
    return adjacencyList.getOrDefault(source, new ArrayList<>()).stream()
        .filter(edge -> edge.destination.equals(destination))
        .map(edge -> edge.weight)
        .findFirst()
        .orElse(Integer.MAX_VALUE);
  }

  @Override
  public List<T> getNeighbors(T node) {
    List<T> neighbors = new ArrayList<>();
    for (Edge<T> edge : adjacencyList.getOrDefault(node, new ArrayList<>())) {
      neighbors.add(edge.destination);
    }
    return neighbors;
  }

  @Override
  public Set<T> getNodes() {
    return adjacencyList.keySet();
  }

  private static class Edge<T> {
    final T destination;
    final int weight;

    Edge(T destination, int weight) {
      this.destination = destination;
      this.weight = weight;
    }
  }
}
