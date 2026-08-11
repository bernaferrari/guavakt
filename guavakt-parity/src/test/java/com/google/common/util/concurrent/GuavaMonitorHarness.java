package com.google.common.util.concurrent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Deterministic, normalized Monitor traces against Guava itself. */
public final class GuavaMonitorHarness {
  private GuavaMonitorHarness() {}

  public static List<String> immediateTrace() {
    Monitor monitor = new Monitor(true);
    AtomicBoolean ready = new AtomicBoolean();
    Monitor.Guard guard = monitor.newGuard(ready::get);
    List<String> trace = new ArrayList<>();
    trace.add("fair:" + monitor.isFair());
    trace.add("occupied-before:" + monitor.isOccupied());
    trace.add("try-false:" + monitor.tryEnterIf(guard));
    trace.add("occupied-after-false:" + monitor.isOccupied());
    trace.add("zero-enter:" + monitor.enter(Duration.ZERO));
    trace.add("depth-one:" + monitor.getOccupiedDepth());
    monitor.enter();
    trace.add("depth-two:" + monitor.getOccupiedDepth());
    monitor.leave();
    monitor.leave();
    trace.add("zero-guard-false:" + callEnterWhen(monitor, guard));
    ready.set(true);
    trace.add("zero-guard-true:" + callEnterWhen(monitor, guard));
    if (monitor.isOccupiedByCurrentThread()) monitor.leave();
    trace.add("wrong-monitor:" + exceptionName(() -> new Monitor().tryEnterIf(guard)));
    trace.add("unoccupied-wait:" + exceptionName(() -> monitor.waitFor(guard)));
    return trace;
  }

  public static List<String> predicateFailureTrace() {
    Monitor monitor = new Monitor();
    Monitor.Guard broken = monitor.newGuard(() -> { throw new IllegalStateException("boom"); });
    List<String> trace = new ArrayList<>();
    trace.add("enter-if:" + exceptionName(() -> monitor.enterIf(broken)));
    trace.add("occupied-after-enter-if:" + monitor.isOccupiedByCurrentThread());
    trace.add("try-enter-if:" + exceptionName(() -> monitor.tryEnterIf(broken)));
    trace.add("occupied-after-try:" + monitor.isOccupiedByCurrentThread());
    return trace;
  }

  public static List<String> waiterTrace() throws Exception {
    Monitor monitor = new Monitor();
    AtomicBoolean ready = new AtomicBoolean();
    Monitor.Guard guard = monitor.newGuard(ready::get);
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(1);
    AtomicBoolean observedSatisfied = new AtomicBoolean();
    Thread waiter = new Thread(() -> {
      started.countDown();
      try {
        monitor.enterWhen(guard);
        try {
          observedSatisfied.set(ready.get());
        } finally {
          monitor.leave();
        }
      } catch (InterruptedException failure) {
        throw new AssertionError(failure);
      } finally {
        done.countDown();
      }
    });
    waiter.start();
    if (!started.await(2, TimeUnit.SECONDS)) throw new AssertionError("waiter did not start");
    awaitWaiter(monitor, guard);

    List<String> trace = new ArrayList<>();
    trace.add("has-waiters:" + monitor.hasWaiters(guard));
    trace.add("wait-count:" + monitor.getWaitQueueLength(guard));
    monitor.enter();
    try {
      ready.set(true);
    } finally {
      monitor.leave();
    }
    if (!done.await(2, TimeUnit.SECONDS)) throw new AssertionError("waiter did not finish");
    waiter.join(2_000);
    trace.add("saw-satisfied:" + observedSatisfied.get());
    trace.add("wait-count-after:" + monitor.getWaitQueueLength(guard));
    return trace;
  }

  public static List<String> interruptTrace() {
    Monitor monitor = new Monitor();
    Monitor.Guard never = monitor.newGuard(() -> false);
    List<String> trace = new ArrayList<>();
    Thread.interrupted();
    try {
      Thread.currentThread().interrupt();
      trace.add("uninterruptible-enter:" + monitor.enter(Duration.ZERO));
      trace.add("status-restored:" + Thread.currentThread().isInterrupted());
      Thread.interrupted();
      monitor.leave();

      Thread.currentThread().interrupt();
      trace.add("interruptible-enter:" + exceptionName(monitor::enterInterruptibly));
      trace.add("status-cleared:" + !Thread.currentThread().isInterrupted());

      Thread.currentThread().interrupt();
      trace.add("interruptible-guard:" + exceptionName(() -> monitor.enterWhen(never, Duration.ZERO)));
      trace.add("guard-status-cleared:" + !Thread.currentThread().isInterrupted());
      return trace;
    } finally {
      Thread.interrupted();
      while (monitor.isOccupiedByCurrentThread()) monitor.leave();
    }
  }

  private static boolean callEnterWhen(Monitor monitor, Monitor.Guard guard) {
    try {
      return monitor.enterWhen(guard, Duration.ZERO);
    } catch (InterruptedException failure) {
      throw new AssertionError(failure);
    }
  }

  private static void awaitWaiter(Monitor monitor, Monitor.Guard guard) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
    while (!monitor.hasWaiters(guard)) {
      if (System.nanoTime() >= deadline) throw new AssertionError("guard waiter was not registered");
      Thread.sleep(1);
    }
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
}
