package com.bernaferrari.guavakt.parity;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.ImmutableTable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Runtime null/duplicate probes that Kotlin cannot express after Guava adopted strict JSpecify bounds. */
public final class GuavaTableNullHarness {
  private GuavaTableNullHarness() {}

  public static List<String> arrayTableConstructionFailures() {
    return Arrays.asList(
        failureName(() -> ArrayTable.create(Collections.<String>emptyList(), List.of("c"))),
        failureName(() -> ArrayTable.create(List.of("r", "r"), List.of("c"))),
        failureName(() -> ArrayTable.create(Arrays.asList((String) null), List.of("c"))));
  }

  public static List<String> immutableTableFailures() {
    return Arrays.asList(
        failureName(
            () ->
                ImmutableTable.<String, String, Integer>builder()
                    .put("r", "c", 1)
                    .put("r", "c", 2)
                    .buildOrThrow()),
        failureName(() -> ImmutableTable.builder().put(null, "c", 1)),
        failureName(() -> ImmutableTable.builder().put("r", null, 1)),
        failureName(() -> ImmutableTable.builder().put("r", "c", null)));
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
