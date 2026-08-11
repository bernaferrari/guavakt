package dev.guavakt.cache;

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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * JVM-only throughput measurements for the public coroutine cache contract.
 *
 * Each invocation starts from a controlled cache state. The miss workloads hold loaders at a
 * barrier until every request has joined, so they measure same-key coalescing and distinct-key
 * parallel coordination rather than a scheduler race. Results are emitted by jmhCoroutineCache.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class CoroutineLoadingCacheBenchmark {
  @Benchmark
  public int hotHitBatch(HotState state) {
    return state.workload.hotHitBatch();
  }

  @Benchmark
  public int sameKeyMissBatch(SameKeyState state) {
    return state.workload.sameKeyMissBatch();
  }

  @Benchmark
  public int distinctKeyMissBatch(DistinctKeyState state) {
    return state.workload.distinctKeyMissBatch();
  }

  @Benchmark
  public int evictingWriteBatch(EvictingWriteState state) {
    return state.workload.evictingWriteBatch();
  }

  public abstract static class WorkloadState {
    CoroutineCacheBenchmarkWorkload workload;

    @Setup(Level.Trial)
    public void createWorkload() {
      workload = new CoroutineCacheBenchmarkWorkload();
    }

    @TearDown(Level.Trial)
    public void closeWorkload() {
      workload.close();
    }
  }

  @State(Scope.Thread)
  public static class HotState extends WorkloadState {
    @Setup(Level.Invocation)
    public void reset() {
      workload.prepareHotHits();
    }
  }

  @State(Scope.Thread)
  public static class SameKeyState extends WorkloadState {
    @Setup(Level.Invocation)
    public void reset() {
      workload.prepareSameKeyMisses();
    }
  }

  @State(Scope.Thread)
  public static class DistinctKeyState extends WorkloadState {
    @Setup(Level.Invocation)
    public void reset() {
      workload.prepareDistinctKeyMisses();
    }
  }

  @State(Scope.Thread)
  public static class EvictingWriteState extends WorkloadState {
    @Setup(Level.Invocation)
    public void reset() {
      workload.prepareEvictingWrites();
    }
  }
}
