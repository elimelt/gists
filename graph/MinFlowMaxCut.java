package graph;

import java.util.*;

public class MinFlowMaxCut<T> {
  private Map<T, Map<T, Integer>> residualCapacity;

  public int fordFulkerson(WeightedGraph<T> graph, T source, T sink) {
    residualCapacity = new HashMap<>();
    for (T node : graph.getNodes()) {
      residualCapacity.put(node, new HashMap<>());
      for (T neighbor : graph.getNeighbors(node)) {
        residualCapacity.get(node).put(neighbor, graph.getEdgeWeight(node, neighbor));
      }
    }

    int maxFlow = 0;
    List<T> path;

    while ((path = bfs(source, sink)) != null) {
      int pathFlow = Integer.MAX_VALUE;
      for (int i = 0; i < path.size() - 1; i++) {
        T u = path.get(i);
        T v = path.get(i + 1);
        pathFlow = Math.min(pathFlow, residualCapacity.get(u).get(v));
      }

      for (int i = 0; i < path.size() - 1; i++) {
        T u = path.get(i);
        T v = path.get(i + 1);
        residualCapacity.get(u).put(v, residualCapacity.get(u).get(v) - pathFlow);
        residualCapacity.get(v).put(u, residualCapacity.get(v).getOrDefault(u, 0) + pathFlow);
      }

      maxFlow += pathFlow;
    }

    return maxFlow;
  }

  private List<T> bfs(T source, T sink) {
    Queue<T> queue = new LinkedList<>();
    Map<T, T> parentMap = new HashMap<>();
    Set<T> visited = new HashSet<>();
    queue.add(source);
    visited.add(source);

    while (!queue.isEmpty()) {
      T node = queue.poll();
      for (Map.Entry<T, Integer> neighbor : residualCapacity.get(node).entrySet()) {
        if (!visited.contains(neighbor.getKey()) && neighbor.getValue() > 0) {
          parentMap.put(neighbor.getKey(), node);
          visited.add(neighbor.getKey());
          queue.add(neighbor.getKey());

          if (neighbor.getKey().equals(sink)) {
            List<T> path = new LinkedList<>();
            for (T at = sink; at != null; at = parentMap.get(at)) {
              path.add(0, at);
            }
            return path;
          }
        }
      }
    }

    return null;
  }

  // stress test
  public static void main(String[] args) {
    WeightedDirectedGraph<Integer> graph = new WeightedDirectedGraph<>();
    Random random = new Random();
    int numNodes = 100000;
    int numEdges = 500000;

    for (int i = 0; i < numEdges; i++) {
      int source = random.nextInt(numNodes);
      int destination = random.nextInt(numNodes);
      int weight = random.nextInt(100) + 1;
      graph.addEdge(source, destination, weight);
    }

    MinFlowMaxCut<Integer> maxFlowMinCut = new MinFlowMaxCut<>();

    long startTime = System.nanoTime();
    int maxFlow = maxFlowMinCut.fordFulkerson(graph, 0, numNodes - 1);
    long endTime = System.nanoTime();

    System.out.println("Size of graph: " + numNodes);
    System.out.println("Number of edges: " + numEdges);
    System.out.printf("Max Flow: %d%n", maxFlow);
    System.out.printf("Time taken: %.2f ms%n", (endTime - startTime) / 1e6);
  }
}
