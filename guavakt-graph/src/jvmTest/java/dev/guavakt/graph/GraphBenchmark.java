package dev.guavakt.graph;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/** JVM-only traces for construction, traversal, and mutation of the KMP graph implementation. */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class GraphBenchmark {
  @Benchmark
  public int buildSparseGraphBatch(BaseState state) {
    return state.workload.buildSparseGraphBatch();
  }

  @Benchmark
  public int reachableDenseDagBatch(BaseState state) {
    return state.workload.reachableDenseDagBatch();
  }

  @Benchmark
  public int mutateEdgeBatch(MutationState state) {
    return state.workload.mutateEdgeBatch();
  }

  @State(Scope.Thread)
  public static class BaseState {
    GraphBenchmarkWorkload workload;

    @Setup(Level.Trial)
    public void createWorkload() {
      workload = new GraphBenchmarkWorkload();
    }
  }

  @State(Scope.Thread)
  public static class MutationState extends BaseState {
    @Setup(Level.Invocation)
    public void reset() {
      workload.prepareMutation();
    }
  }
}
