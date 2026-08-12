package com.bernaferrari.guavakt.parity;

import com.google.common.collect.ImmutableBiMap;

public final class GuavaBiMapNullHarness {
  private GuavaBiMapNullHarness() {}

  public static String nullKeyFailure() {
    try {
      ImmutableBiMap.builder().put(null, 1).build();
      return null;
    } catch (Throwable failure) {
      return failure.getClass().getSimpleName();
    }
  }

  public static String nullValueFailure() {
    try {
      ImmutableBiMap.builder().put("key", null).build();
      return null;
    } catch (Throwable failure) {
      return failure.getClass().getSimpleName();
    }
  }
}
