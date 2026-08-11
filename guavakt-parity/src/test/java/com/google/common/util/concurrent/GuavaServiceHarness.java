package com.google.common.util.concurrent;

import java.util.ArrayList;
import java.util.List;

/** Deterministic access to Guava AbstractService's protected transition API. */
public final class GuavaServiceHarness {
  private GuavaServiceHarness() {}

  public static List<String> normalLifecycleTrace() {
    ManualService service = new ManualService();
    List<String> trace = attachTrace(service);
    service.startAsync();
    service.started();
    service.stopAsync();
    service.stopped();
    trace.add("state:" + service.state());
    trace.add("starts:" + service.startCalls);
    trace.add("stops:" + service.stopCalls);
    return trace;
  }

  public static List<String> stopDuringStartupTrace() {
    ManualService service = new ManualService();
    List<String> trace = attachTrace(service);
    service.startAsync();
    service.stopAsync();
    trace.add("state-before-started:" + service.state());
    trace.add("cancel-start-before-started:" + service.cancelStartCalls);
    trace.add("stops-before-started:" + service.stopCalls);
    service.started();
    trace.add("state-after-started:" + service.state());
    trace.add("stops-after-started:" + service.stopCalls);
    service.stopped();
    trace.add("state-final:" + service.state());
    return trace;
  }

  public static List<String> stopFromNewTrace() {
    ManualService service = new ManualService();
    List<String> trace = attachTrace(service);
    service.stopAsync();
    trace.add("state:" + service.state());
    trace.add("starts:" + service.startCalls);
    trace.add("stops:" + service.stopCalls);
    return trace;
  }

  public static List<String> failureTrace() {
    ManualService service = new ManualService();
    List<String> trace = attachTrace(service);
    service.startAsync();
    IllegalStateException failure = new IllegalStateException("boom");
    service.failed(failure);
    trace.add("state:" + service.state());
    trace.add("same-cause:" + (service.failureCause() == failure));
    return trace;
  }

  public static String notifyFailedFromNewException() {
    ManualService service = new ManualService();
    try {
      service.failed(new IllegalStateException("boom"));
      return "none";
    } catch (Throwable failure) {
      return failure.getClass().getSimpleName();
    }
  }

  public static List<String> notificationEdgeTrace() {
    List<String> trace = new ArrayList<>();

    ManualService stoppedWhileStarting = new ManualService();
    stoppedWhileStarting.startAsync();
    stoppedWhileStarting.stopped();
    trace.add("stopped-from-starting:" + stoppedWhileStarting.state());

    ManualService repeatedFailure = new ManualService();
    repeatedFailure.startAsync();
    IllegalStateException first = new IllegalStateException("first");
    repeatedFailure.failed(first);
    repeatedFailure.failed(new IllegalStateException("second"));
    trace.add("repeated-failure-keeps-first:" + (repeatedFailure.failureCause() == first));

    ManualService duplicateStarted = new ManualService();
    List<String> duplicateEvents = attachTrace(duplicateStarted);
    duplicateStarted.startAsync();
    duplicateStarted.started();
    try {
      duplicateStarted.started();
      duplicateEvents.add("duplicate-started:none");
    } catch (Throwable failure) {
      duplicateEvents.add("duplicate-started:" + failure.getClass().getSimpleName());
    }
    duplicateEvents.add("duplicate-started-state:" + duplicateStarted.state());
    duplicateEvents.add("duplicate-started-cause:" + duplicateStarted.failureCause().getMessage());
    trace.addAll(duplicateEvents);
    return trace;
  }

  private static List<String> attachTrace(Service service) {
    List<String> trace = new ArrayList<>();
    service.addListener(
        new Service.Listener() {
          @Override public void starting() { trace.add("starting"); }
          @Override public void running() { trace.add("running"); }
          @Override public void stopping(Service.State from) { trace.add("stopping:" + from); }
          @Override public void terminated(Service.State from) { trace.add("terminated:" + from); }
          @Override public void failed(Service.State from, Throwable failure) {
            trace.add("failed:" + from + ":" + failure.getMessage());
          }
        },
        MoreExecutors.directExecutor());
    return trace;
  }

  private static final class ManualService extends AbstractService {
    int startCalls;
    int stopCalls;
    int cancelStartCalls;

    @Override protected void doStart() { startCalls++; }
    @Override protected void doStop() { stopCalls++; }
    @Override protected void doCancelStart() { cancelStartCalls++; }

    void started() { notifyStarted(); }
    void stopped() { notifyStopped(); }
    void failed(Throwable failure) { notifyFailed(failure); }
  }
}
