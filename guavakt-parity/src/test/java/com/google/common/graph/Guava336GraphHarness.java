package com.google.common.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Package-local oracle access for BaseGraph.asNetwork(), which is intentionally not public. */
public final class Guava336GraphHarness {
  private Guava336GraphHarness() {}

  public static List<List<String>> closureTraces() {
    MutableGraph<String> dag = GraphBuilder.directed().allowsSelfLoops(true).build();
    dag.putEdge("a", "b");
    dag.putEdge("b", "c");
    dag.addNode("i");

    MutableGraph<String> cycle = GraphBuilder.directed().allowsSelfLoops(true).build();
    cycle.putEdge("a", "b");
    cycle.putEdge("b", "a");
    cycle.putEdge("d", "d");
    cycle.addNode("i");

    MutableGraph<String> undirected = GraphBuilder.undirected().allowsSelfLoops(true).build();
    undirected.putEdge("a", "b");
    undirected.addNode("i");

    return Arrays.asList(
        edges(Graphs.transitiveClosure(dag, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS)),
        edges(Graphs.transitiveClosure(dag, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_FOR_CYCLES)),
        edges(Graphs.transitiveClosure(cycle, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS)),
        edges(Graphs.transitiveClosure(cycle, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_FOR_CYCLES)),
        edges(Graphs.transitiveClosure(undirected, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_ALWAYS)),
        edges(Graphs.transitiveClosure(undirected, Graphs.TransitiveClosureSelfLoopStrategy.ADD_SELF_LOOPS_FOR_CYCLES)));
  }

  public static List<Object> asNetworkTrace() {
    MutableGraph<String> graph = GraphBuilder.directed().allowsSelfLoops(true).build();
    graph.putEdge("a", "b");
    graph.putEdge("a", "a");
    Network<String, EndpointPair<String>> network = graph.asNetwork();

    List<Object> trace = new ArrayList<>();
    trace.add(sorted(network.nodes()));
    trace.add(edges(network.asGraph()));
    trace.add(network.asGraph().equals(graph));
    trace.add(network.allowsParallelEdges());
    trace.add(network.degree("a"));
    trace.add(network.inDegree("a"));
    trace.add(network.outDegree("a"));
    trace.add(edgeStrings(network.inEdges("b")));
    trace.add(edgeStrings(network.outEdges("a")));
    trace.add(edgeStrings(network.edgesConnecting("a", "b")));
    trace.add(network.incidentNodes(EndpointPair.ordered("a", "b")).toString());
    Set<EndpointPair<String>> heldIncoming = network.inEdges("b");

    graph.putEdge("c", "b");
    trace.add(edgeStrings(network.inEdges("b")));
    trace.add(edgeStrings(heldIncoming));
    trace.add(sorted(network.nodes()));
    trace.add(edges(network.asGraph()));
    trace.add(failureName(() -> network.incidentNodes(EndpointPair.ordered("missing", "b"))));
    graph.removeNode("b");
    trace.add(failureName(heldIncoming::size));
    return trace;
  }

  public static List<Object> valueGraphUtilityTrace() {
    List<Object> trace = new ArrayList<>();

    MutableValueGraph<String, Integer> undirected =
        ValueGraphBuilder.<String, Integer>undirected().build();
    trace.add(undirected.putEdgeValue("a", "b", 1));
    trace.add(undirected.putEdgeValue("b", "a", 2));
    trace.add(undirected.edgeValueOrDefault("a", "b", -1));
    trace.add(undirected.edgeValueOrDefault("b", "a", -1));
    trace.add(edgeStrings(undirected.edges()));
    trace.add(undirected.removeEdge("b", "a"));
    trace.add(undirected.hasEdgeConnecting("a", "b"));

    MutableValueGraph<String, Integer> graph =
        ValueGraphBuilder.<String, Integer>directed().allowsSelfLoops(true).build();
    graph.putEdgeValue("a", "b", 10);
    graph.putEdgeValue("b", "c", 20);
    graph.addNode("i");
    ValueGraph<String, Integer> transpose = Graphs.transpose(graph);
    trace.add(transpose.hasEdgeConnecting("b", "a"));
    trace.add(transpose.edgeValueOrDefault("b", "a", -1));
    trace.add(transpose.inDegree("a"));
    trace.add(transpose.outDegree("a"));
    trace.add(Graphs.transpose(transpose) == graph);
    graph.putEdgeValue("c", "a", 30);
    trace.add(transpose.edgeValueOrDefault("a", "c", -1));
    trace.add(edgeStrings(transpose.edges()));

    MutableValueGraph<String, Integer> induced = Graphs.inducedSubgraph(graph, Arrays.asList("a", "b", "i"));
    trace.add(sorted(induced.nodes()));
    trace.add(edgeStrings(induced.edges()));
    trace.add(induced.edgeValueOrDefault("a", "b", -1));
    MutableValueGraph<String, Integer> copy = Graphs.copyOf(graph);
    graph.removeEdge("a", "b");
    trace.add(edgeStrings(copy.edges()));
    trace.add(copy.edgeValueOrDefault("a", "b", -1));
    trace.add(failureName(() -> Graphs.inducedSubgraph(graph, Collections.singletonList("missing"))));
    return trace;
  }

  public static List<Object> networkUtilityTrace() {
    List<Object> trace = new ArrayList<>();
    MutableNetwork<String, String> network =
        NetworkBuilder.<String, String>directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
    network.addEdge("a", "b", "ab1");
    network.addEdge("a", "b", "ab2");
    network.addEdge("b", "c", "bc");
    network.addNode("i");
    trace.add(Graphs.hasCycle(network));
    Network<String, String> transpose = Graphs.transpose(network);
    trace.add(sorted(transpose.successors("b")));
    trace.add(sorted(transpose.predecessors("a")));
    trace.add(sorted(transpose.inEdges("a")));
    trace.add(sorted(transpose.outEdges("b")));
    trace.add(transpose.incidentNodes("ab1").toString());
    trace.add(sorted(transpose.edgesConnecting("b", "a")));
    trace.add(Graphs.transpose(transpose) == network);
    network.addEdge("c", "a", "ca");
    trace.add(transpose.hasEdgeConnecting("a", "c"));
    trace.add(Graphs.hasCycle(network));

    MutableNetwork<String, String> induced = Graphs.inducedSubgraph(network, Arrays.asList("a", "b", "i"));
    trace.add(sorted(induced.nodes()));
    trace.add(sorted(induced.edges()));
    trace.add(induced.allowsParallelEdges());
    MutableNetwork<String, String> copy = Graphs.copyOf(network);
    network.removeEdge("ab1");
    trace.add(sorted(copy.edges()));
    trace.add(copy.allowsSelfLoops());
    trace.add(failureName(() -> Graphs.inducedSubgraph(network, Collections.singletonList("missing"))));

    MutableNetwork<String, String> parallelUndirected =
        NetworkBuilder.<String, String>undirected().allowsParallelEdges(true).build();
    parallelUndirected.addEdge("x", "y", "xy1");
    parallelUndirected.addEdge("x", "y", "xy2");
    trace.add(Graphs.hasCycle(parallelUndirected));
    trace.add(Graphs.hasCycle(parallelUndirected.asGraph()));
    return trace;
  }

  public static List<Object> orderingTrace() {
    List<Object> trace = new ArrayList<>();
    Map<Integer, String> sortedMap = ElementOrder.<Integer>natural().createMap(3);
    sortedMap.put(3, "three");
    sortedMap.put(1, "one");
    sortedMap.put(2, "two");
    trace.add(new ArrayList<>(sortedMap.keySet()));
    trace.add(failureName(() -> ElementOrder.<Integer>insertion().comparator()));

    MutableGraph<Integer> graph =
        GraphBuilder.<Integer>directed().nodeOrder(ElementOrder.<Integer>natural()).build();
    graph.addNode(3);
    graph.addNode(1);
    graph.addNode(2);
    trace.add(new ArrayList<>(graph.nodes()));
    trace.add(graph.nodeOrder().type().name());
    MutableGraph<Integer> graphCopy = Graphs.copyOf(graph);
    trace.add(new ArrayList<>(graphCopy.nodes()));
    trace.add(graphCopy.nodeOrder().type().name());

    MutableValueGraph<Integer, String> valueGraph =
        ValueGraphBuilder.<Integer, String>directed().nodeOrder(ElementOrder.<Integer>natural()).build();
    valueGraph.addNode(3);
    valueGraph.addNode(1);
    valueGraph.addNode(2);
    trace.add(new ArrayList<>(valueGraph.nodes()));
    trace.add(Graphs.copyOf(valueGraph).nodeOrder().type().name());

    MutableNetwork<Integer, String> network =
        NetworkBuilder.<Integer, String>directed()
            .allowsParallelEdges(true)
            .nodeOrder(ElementOrder.<Integer>natural())
            .edgeOrder(ElementOrder.<String>natural())
            .build();
    network.addEdge(3, 1, "z");
    network.addEdge(1, 2, "a");
    network.addEdge(2, 3, "m");
    trace.add(new ArrayList<>(network.nodes()));
    trace.add(new ArrayList<>(network.edges()));
    trace.add(network.nodeOrder().type().name());
    trace.add(network.edgeOrder().type().name());
    MutableNetwork<Integer, String> networkCopy = Graphs.copyOf(network);
    trace.add(new ArrayList<>(networkCopy.nodes()));
    trace.add(new ArrayList<>(networkCopy.edges()));
    trace.add(networkCopy.nodeOrder().type().name());
    trace.add(networkCopy.edgeOrder().type().name());
    return trace;
  }

  public static List<Object> baseAccessorViewTrace() {
    List<Object> trace = new ArrayList<>();

    MutableGraph<String> graph = GraphBuilder.directed().build();
    graph.putEdge("a", "b");
    trace.add(Graphs.transpose(graph).hasEdgeConnecting("missing", "a"));
    Set<String> graphNodes = graph.nodes();
    Set<EndpointPair<String>> graphEdges = graph.edges();
    Set<String> graphSuccessors = graph.successors("a");
    graph.putEdge("b", "c");
    graph.putEdge("a", "c");
    trace.add(sorted(graphNodes));
    trace.add(edgeStrings(graphEdges));
    trace.add(sorted(graphSuccessors));
    graph.removeNode("a");
    trace.add(sorted(graphNodes));
    trace.add(edgeStrings(graphEdges));
    trace.add(failureName(graphSuccessors::size));

    MutableValueGraph<String, Integer> valueGraph = ValueGraphBuilder.<String, Integer>directed().build();
    valueGraph.putEdgeValue("a", "b", 1);
    Set<String> valueNodes = valueGraph.nodes();
    Set<EndpointPair<String>> valueEdges = valueGraph.edges();
    Set<String> valueSuccessors = valueGraph.successors("a");
    valueGraph.putEdgeValue("b", "c", 2);
    valueGraph.putEdgeValue("a", "c", 3);
    trace.add(sorted(valueNodes));
    trace.add(edgeStrings(valueEdges));
    trace.add(sorted(valueSuccessors));
    valueGraph.removeNode("a");
    trace.add(sorted(valueNodes));
    trace.add(edgeStrings(valueEdges));
    trace.add(failureName(valueSuccessors::size));

    MutableNetwork<String, String> network =
        NetworkBuilder.<String, String>directed().allowsParallelEdges(true).build();
    network.addEdge("a", "b", "ab1");
    Set<String> networkNodes = network.nodes();
    Set<String> networkEdges = network.edges();
    Set<String> networkOutEdges = network.outEdges("a");
    Set<String> connecting = network.edgesConnecting("a", "b");
    Set<String> adjacent = network.adjacentEdges("ab1");
    network.addEdge("a", "b", "ab2");
    network.addEdge("b", "c", "bc");
    trace.add(sorted(networkNodes));
    trace.add(sorted(networkEdges));
    trace.add(sorted(networkOutEdges));
    trace.add(sorted(connecting));
    trace.add(sorted(adjacent));
    network.removeNode("a");
    trace.add(sorted(networkNodes));
    trace.add(sorted(networkEdges));
    trace.add(failureName(networkOutEdges::size));
    trace.add(failureName(connecting::size));
    trace.add(failureName(adjacent::size));
    return trace;
  }

  public static List<Object> comparatorEquivalentNodeTrace() {
    Comparator<String> byLength = Comparator.comparingInt(String::length);
    List<Object> trace = new ArrayList<>();

    MutableGraph<String> graph =
        GraphBuilder.<String>directed().nodeOrder(ElementOrder.sorted(byLength)).build();
    trace.add(graph.addNode("a"));
    trace.add(graph.addNode("b"));
    trace.add(graph.putEdge("b", "cc"));
    trace.add(new ArrayList<>(graph.nodes()));
    trace.add(sorted(graph.successors("a")));
    trace.add(edgeStrings(graph.edges()));
    trace.add(graph.hasEdgeConnecting("a", "cc"));
    trace.add(graph.removeNode("b"));
    trace.add(new ArrayList<>(graph.nodes()));

    MutableValueGraph<String, Integer> valueGraph =
        ValueGraphBuilder.<String, Integer>directed().nodeOrder(ElementOrder.sorted(byLength)).build();
    trace.add(valueGraph.addNode("a"));
    trace.add(valueGraph.addNode("b"));
    trace.add(valueGraph.putEdgeValue("b", "cc", 1));
    trace.add(new ArrayList<>(valueGraph.nodes()));
    trace.add(sorted(valueGraph.successors("a")));
    trace.add(edgeStrings(valueGraph.edges()));
    trace.add(valueGraph.hasEdgeConnecting("a", "cc"));
    trace.add(valueGraph.edgeValueOrDefault("a", "cc", -1));
    trace.add(valueGraph.removeNode("b"));
    trace.add(new ArrayList<>(valueGraph.nodes()));

    MutableNetwork<String, String> network =
        NetworkBuilder.<String, String>directed().nodeOrder(ElementOrder.sorted(byLength)).build();
    trace.add(network.addNode("a"));
    trace.add(network.addNode("b"));
    trace.add(network.addEdge("b", "cc", "edge"));
    trace.add(new ArrayList<>(network.nodes()));
    trace.add(sorted(network.successors("a")));
    trace.add(network.incidentNodes("edge").toString());
    trace.add(network.hasEdgeConnecting("a", "cc"));
    trace.add(network.addEdge("b", "cc", "edge"));
    trace.add(failureName(() -> network.addEdge("a", "cc", "edge")));
    trace.add(network.removeNode("b"));
    trace.add(new ArrayList<>(network.nodes()));
    return trace;
  }

  public static List<Object> incidentEdgeOrderTrace() {
    List<Object> trace = new ArrayList<>();
    MutableGraph<String> graph =
        GraphBuilder.<String>directed().incidentEdgeOrder(ElementOrder.<String>stable()).build();
    graph.putEdge("b", "a");
    graph.putEdge("a", "c");
    graph.putEdge("d", "a");
    graph.putEdge("a", "b");
    trace.add(graph.incidentEdgeOrder().type().name());
    trace.add(orderedEdgeStrings(graph.incidentEdges("a")));
    MutableGraph<String> graphCopy = Graphs.copyOf(graph);
    trace.add(graphCopy.incidentEdgeOrder().type().name());
    trace.add(orderedEdgeStrings(graphCopy.incidentEdges("a")));
    trace.add(Graphs.transpose(graph).incidentEdgeOrder().type().name());
    ImmutableGraph<String> immutableGraph = ImmutableGraph.copyOf(graph);
    trace.add(immutableGraph.incidentEdgeOrder().type().name());
    trace.add(orderedEdgeStrings(immutableGraph.incidentEdges("a")));
    trace.add(ImmutableGraph.copyOf(immutableGraph) == immutableGraph);

    MutableValueGraph<String, Integer> valueGraph =
        ValueGraphBuilder.<String, Integer>directed()
            .incidentEdgeOrder(ElementOrder.<String>stable())
            .build();
    valueGraph.putEdgeValue("b", "a", 1);
    valueGraph.putEdgeValue("a", "c", 2);
    valueGraph.putEdgeValue("d", "a", 3);
    valueGraph.putEdgeValue("a", "b", 4);
    trace.add(valueGraph.incidentEdgeOrder().type().name());
    trace.add(orderedEdgeStrings(valueGraph.incidentEdges("a")));
    MutableValueGraph<String, Integer> valueCopy = Graphs.copyOf(valueGraph);
    trace.add(valueCopy.incidentEdgeOrder().type().name());
    trace.add(orderedEdgeStrings(valueCopy.incidentEdges("a")));
    trace.add(Graphs.transpose(valueGraph).incidentEdgeOrder().type().name());
    ImmutableValueGraph<String, Integer> immutableValueGraph = ImmutableValueGraph.copyOf(valueGraph);
    trace.add(immutableValueGraph.incidentEdgeOrder().type().name());
    trace.add(orderedEdgeStrings(immutableValueGraph.incidentEdges("a")));
    trace.add(ImmutableValueGraph.copyOf(immutableValueGraph) == immutableValueGraph);
    trace.add(
        failureName(
            () -> GraphBuilder.<String>directed().incidentEdgeOrder(ElementOrder.<String>insertion())));
    trace.add(
        failureName(
            () -> ValueGraphBuilder.<String, Integer>directed()
                .incidentEdgeOrder(ElementOrder.<String>sorted(String::compareTo))));
    return trace;
  }

  private static List<String> edges(Graph<String> graph) {
    return edgeStrings(graph.edges());
  }

  private static List<String> edgeStrings(Iterable<EndpointPair<String>> edges) {
    List<String> result = new ArrayList<>();
    for (EndpointPair<String> edge : edges) result.add(edge.toString());
    Collections.sort(result);
    return result;
  }

  private static List<String> orderedEdgeStrings(Iterable<EndpointPair<String>> edges) {
    List<String> result = new ArrayList<>();
    for (EndpointPair<String> edge : edges) result.add(edge.toString());
    return result;
  }

  private static List<String> sorted(Set<String> values) {
    List<String> result = new ArrayList<>(values);
    Collections.sort(result);
    return result;
  }

  private static String failureName(Runnable action) {
    try {
      action.run();
      return null;
    } catch (Throwable failure) {
      return failure.getClass().getSimpleName();
    }
  }
}
