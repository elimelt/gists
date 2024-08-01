package graph;

import java.util.*;

public class DijkstrasAlgorithm<T> {
    public Map<T, Integer> findShortestPaths(Graph<T> graph, T startNode, Map<T, Map<T, Integer>> edgeWeights) {
        Map<T, Integer> distances = new HashMap<>();
        Set<T> visited = new HashSet<>();
        PriorityQueue<NodeDistance<T>> priorityQueue = new PriorityQueue<>(Comparator.comparingInt(nd -> nd.distance));
        priorityQueue.add(new NodeDistance<>(startNode, 0));
        distances.put(startNode, 0);

        while (!priorityQueue.isEmpty()) {
            NodeDistance<T> current = priorityQueue.poll();
            if (!visited.contains(current.node)) {
                visited.add(current.node);
                for (T neighbor : graph.getNeighbors(current.node)) {
                    int newDist = distances.get(current.node) + edgeWeights.get(current.node).get(neighbor);
                    if (newDist < distances.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                        distances.put(neighbor, newDist);
                        priorityQueue.add(new NodeDistance<>(neighbor, newDist));
                    }
                }
            }
        }

        return distances;
    }

    private static class NodeDistance<T> {
        T node;
        int distance;

        NodeDistance(T node, int distance) {
            this.node = node;
            this.distance = distance;
        }
    }
}
