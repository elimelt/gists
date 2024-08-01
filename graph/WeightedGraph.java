package graph;

import java.util.*;

public interface WeightedGraph<T> extends Graph<T> {
    void addEdge(T source, T destination, int weight);

    int getEdgeWeight(T source, T destination);
}
