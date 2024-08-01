package graph;

public interface DirectedGraph<T> extends Graph<T> {
  void addEdge(T source, T destination);
}
