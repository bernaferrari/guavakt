package com.google.common.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Exposes package-private Guava reader behavior to the differential Kotlin suite. */
public final class GuavaCharSequenceReaderHarness {
  private GuavaCharSequenceReaderHarness() {}

  public static List<String> trace(String input) throws IOException {
    CharSequenceReader reader = new CharSequenceReader(input);
    List<String> result = new ArrayList<>();
    char[] buffer = new char[] {'_', '_', '_', '_'};
    result.add("zero=" + reader.read(buffer, 1, 0));
    result.add("ready=" + reader.ready());
    result.add("marks=" + reader.markSupported());
    reader.mark(0);
    result.add("first=" + reader.read());
    result.add("chunk=" + reader.read(buffer, 1, 2) + ":" + new String(buffer));
    reader.reset();
    result.add("skip=" + reader.skip(3));
    result.add("last=" + reader.read());
    result.add("eof=" + reader.read());
    result.add("skip-eof=" + reader.skip(1));
    reader.close();
    try {
      reader.read();
      result.add("closed=none");
    } catch (IOException failure) {
      result.add("closed=" + failure.getClass().getSimpleName());
    }
    return result;
  }
}
