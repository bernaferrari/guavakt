package dev.guavakt.parity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Range;

public final class GuavaImmutableNullHarness {
  private GuavaImmutableNullHarness() {}

  public static String listNullFailure() {
    return failure(() -> ImmutableList.of((Object) null));
  }

  public static String setNullFailure() {
    return failure(() -> ImmutableSet.of((Object) null));
  }

  public static String mapNullKeyFailure() {
    return failure(() -> ImmutableMap.of(null, 1));
  }

  public static String mapNullValueFailure() {
    return failure(() -> ImmutableMap.of("key", null));
  }

  public static String sortedSetNullFailure() {
    return failure(() -> ImmutableSortedSet.of((String) null));
  }

  public static String sortedMapNullKeyFailure() {
    return failure(() -> ImmutableSortedMap.of((String) null, 1));
  }

  public static String sortedMapNullValueFailure() {
    return failure(() -> ImmutableSortedMap.of("key", null));
  }

  public static String listMultimapNullKeyFailure() {
    return failure(() -> ImmutableListMultimap.builder().put(null, 1));
  }

  public static String listMultimapNullValueFailure() {
    return failure(() -> ImmutableListMultimap.builder().put("key", null));
  }

  public static String setMultimapNullKeyFailure() {
    return failure(() -> ImmutableSetMultimap.builder().put(null, 1));
  }

  public static String setMultimapNullValueFailure() {
    return failure(() -> ImmutableSetMultimap.builder().put("key", null));
  }

  public static String rangeSetNullRangeFailure() {
    return failure(() -> ImmutableRangeSet.builder().add(null));
  }

  public static String rangeMapNullValueFailure() {
    return failure(() -> ImmutableRangeMap.builder().put(Range.closed(1, 2), null));
  }

  public static String multisetNullFactoryFailure() {
    return failure(() -> ImmutableMultiset.of((Object) null));
  }

  public static String multisetNullZeroCopiesFailure() {
    return failure(() -> ImmutableMultiset.builder().addCopies(null, 0));
  }

  private static String failure(Runnable operation) {
    try {
      operation.run();
      return null;
    } catch (Throwable failure) {
      return failure.getClass().getSimpleName();
    }
  }
}
