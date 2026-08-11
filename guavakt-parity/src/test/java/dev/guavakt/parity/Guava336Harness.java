package dev.guavakt.parity;

import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import java.util.Arrays;
import java.util.List;

/** Normalized probes for public APIs added in Guava 33.5 and 33.6. */
public final class Guava336Harness {
  private Guava336Harness() {}

  public static List<Object> mathTrace() {
    return Arrays.asList(
        IntMath.saturatedAbs(Integer.MIN_VALUE),
        IntMath.saturatedAbs(Integer.MAX_VALUE),
        IntMath.saturatedAbs(-10),
        IntMath.saturatedAbs(0),
        LongMath.saturatedAbs(Long.MIN_VALUE),
        LongMath.saturatedAbs(Long.MAX_VALUE),
        LongMath.saturatedAbs(-10),
        LongMath.saturatedAbs(0));
  }

  public static List<Long> bloomSerializedSizes() {
    return Arrays.asList(
        BloomFilter.create(Funnels.integerFunnel(), 0).serializedSize(),
        BloomFilter.create(Funnels.integerFunnel(), 1).serializedSize(),
        BloomFilter.create(Funnels.integerFunnel(), 100, 0.01).serializedSize(),
        BloomFilter.create(Funnels.integerFunnel(), 1_000, 0.01).serializedSize());
  }

  public static List<Object> endpointPairTrace() {
    EndpointPair<String> ordered = EndpointPair.ordered("a", "b");
    EndpointPair<String> unordered = EndpointPair.unordered("a", "b");
    EndpointPair<String> reversed = EndpointPair.unordered("b", "a");
    return Arrays.asList(
        ordered.toString(),
        unordered.toString(),
        ordered.equals(EndpointPair.ordered("a", "b")),
        ordered.equals(EndpointPair.ordered("b", "a")),
        unordered.equals(reversed),
        unordered.hashCode() == reversed.hashCode(),
        ordered.source(),
        ordered.target(),
        unordered.adjacentNode("a"),
        Lists.newArrayList(unordered),
        failureName(unordered::source),
        failureName(() -> unordered.adjacentNode("missing")));
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
