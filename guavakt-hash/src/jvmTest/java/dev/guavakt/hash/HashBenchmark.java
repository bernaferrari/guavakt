package dev.guavakt.hash;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/** JVM-only traces for one-shot and streaming KMP hash implementations. */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class HashBenchmark {
  private final HashBenchmarkWorkload workload = new HashBenchmarkWorkload();

  @Benchmark
  public int murmur3OneShotBatch() {
    return workload.murmur3OneShotBatch();
  }

  @Benchmark
  public int murmur3StreamingBatch() {
    return workload.murmur3StreamingBatch();
  }

  @Benchmark
  public int sha256StreamingBatch() {
    return workload.sha256StreamingBatch();
  }
}
