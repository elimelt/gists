package graph;

import java.util.*;

class AdjacencyListGraph<T> implements Graph<T> {
    private final Map<T, List<T>> adjacencyList = new HashMap<>();

    @Override
    public void addEdge(T source, T destination) {
        adjacencyList.computeIfAbsent(source, k -> new ArrayList<>()).add(destination);
        adjacencyList.computeIfAbsent(destination, k -> new ArrayList<>()); // Ensure the destination node is in the
                                                                            // graph
    }

    @Override
    public List<T> getNeighbors(T node) {
        return adjacencyList.getOrDefault(node, new ArrayList<>());
    }

    @Override
    public Set<T> getNodes() {
        return adjacencyList.keySet();
    }
}