package dev.guavakt.parity;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.List;

public final class GuavaMultisetNullHarness {
  private GuavaMultisetNullHarness() {}

  public static List<Object> trace() {
    Multiset<String> multiset = LinkedHashMultiset.create();
    List<Object> trace = new ArrayList<>();
    trace.add(multiset.add(null, 3));
    trace.add(multiset.count(null));
    trace.add(multiset.remove(null, 1));
    trace.add(multiset.count(null));
    trace.add(multiset.elementSet().remove(null));
    trace.add(multiset.isEmpty());
    return trace;
  }
}
