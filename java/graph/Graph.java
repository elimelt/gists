package graph;

import java.util.*;

public interface Graph<T> {
    void addEdge(T source, T destination);

    List<T> getNeighbors(T node);

    Set<T> getNodes();
}