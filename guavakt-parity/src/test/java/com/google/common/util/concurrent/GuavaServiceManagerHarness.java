package com.google.common.util.concurrent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Deterministic, normalized ServiceManager traces against Guava itself. */
public final class GuavaServiceManagerHarness {
  private GuavaServiceManagerHarness() {}

  public static List<String> normalLifecycleTrace() {
    List<String> stopOrder = new ArrayList<>();
    ImmediateService first = new ImmediateService("first", stopOrder);
    ImmediateService second = new ImmediateService("second", stopOrder);
    ServiceManager manager = new ServiceManager(List.of(first, second));
    List<String> events = attachEvents(manager);
    List<String> trace = new ArrayList<>();
    trace.add("healthy-before:" + manager.isHealthy());
    manager.startAsync();
    manager.awaitHealthy();
    trace.add("healthy-after:" + manager.isHealthy());
    trace.add("running-count:" + manager.servicesByState().get(Service.State.RUNNING).size());
    trace.add("events-after-start:" + events);
    manager.stopAsync();
    manager.awaitStopped();
    trace.add("stop-order:" + stopOrder);
    trace.add("terminal-count:" + manager.servicesByState().get(Service.State.TERMINATED).size());
    trace.add("events-final:" + events);
    return trace;
  }

  public static List<String> emptyLifecycleTrace() {
    ServiceManager manager = new ServiceManager(List.of());
    List<String> events = attachEvents(manager);
    List<String> trace = new ArrayList<>();
    trace.add("healthy-before:" + manager.isHealthy());
    trace.add("states-before-empty:" + manager.servicesByState().isEmpty());
    manager.startAsync();
    manager.awaitHealthy();
    trace.add("healthy-after:" + manager.isHealthy());
    trace.add("events-after-start:" + events);
    manager.stopAsync();
    manager.awaitStopped();
    trace.add("states-final-empty:" + manager.servicesByState().isEmpty());
    trace.add("events-final:" + events);
    return trace;
  }

  public static List<String> failureTrace() {
    FailingService service = new FailingService();
    ServiceManager manager = new ServiceManager(List.of(service));
    List<String> events = attachEvents(manager);
    manager.startAsync();
    List<String> trace = new ArrayList<>();
    trace.add("state:" + service.state());
    trace.add("events:" + events);
    try {
      manager.awaitHealthy();
      trace.add("await-healthy:none");
    } catch (Throwable failure) {
      trace.add("await-healthy:" + failure.getClass().getSimpleName());
    }
    manager.awaitStopped();
    trace.add("stopped:true");
    return trace;
  }

  public static List<String> validationAndTimeoutTrace() {
    List<String> trace = new ArrayList<>();
    ManualService duplicate = new ManualService();
    trace.add("duplicate:" + exceptionName(() -> new ServiceManager(List.of(duplicate, duplicate))));
    ManualService nonNew = new ManualService();
    nonNew.stopAsync();
    trace.add("non-new:" + exceptionName(() -> new ServiceManager(List.of(nonNew))));
    ManualService pending = new ManualService();
    ServiceManager manager = new ServiceManager(List.of(pending));
    manager.startAsync();
    trace.add("healthy-timeout:" + exceptionName(() -> manager.awaitHealthy(Duration.ZERO)));
    trace.add("stopped-timeout:" + exceptionName(() -> manager.awaitStopped(Duration.ZERO)));
    return trace;
  }

  private static List<String> attachEvents(ServiceManager manager) {
    List<String> events = new ArrayList<>();
    manager.addListener(
        new ServiceManager.Listener() {
          @Override public void healthy() { events.add("healthy"); }
          @Override public void stopped() { events.add("stopped"); }
          @Override public void failure(Service service) { events.add("failure"); }
        },
        MoreExecutors.directExecutor());
    return events;
  }

  private static String exceptionName(ThrowingRunnable action) {
    try {
      action.run();
      return "none";
    } catch (Throwable failure) {
      return failure.getClass().getSimpleName();
    }
  }

  private interface ThrowingRunnable { void run() throws Exception; }

  private static class ManualService extends AbstractService {
    @Override protected void doStart() {}
    @Override protected void doStop() {}
  }

  private static final class ImmediateService extends AbstractService {
    final String name;
    final List<String> stopOrder;

    ImmediateService(String name, List<String> stopOrder) {
      this.name = name;
      this.stopOrder = stopOrder;
    }

    @Override protected void doStart() { notifyStarted(); }
    @Override protected void doStop() {
      stopOrder.add(name);
      notifyStopped();
    }
  }

  private static final class FailingService extends AbstractService {
    @Override protected void doStart() { notifyFailed(new IllegalStateException("boom")); }
    @Override protected void doStop() {}
  }
}
