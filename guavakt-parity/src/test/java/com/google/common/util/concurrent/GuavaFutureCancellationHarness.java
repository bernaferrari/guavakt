package com.google.common.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Exposes cancellation/setFuture edges protected by Guava AbstractFuture. */
public final class GuavaFutureCancellationHarness {
  private GuavaFutureCancellationHarness() {}

  public static List<String> cancelThenSetFutureTrace() {
    ProbeFuture outer = new ProbeFuture();
    RecordingFuture delegate = new RecordingFuture();
    outer.cancel(true);
    boolean accepted = outer.link(delegate);
    return List.of(
        "accepted:" + accepted,
        "delegate-cancelled:" + delegate.isCancelled(),
        "delegate-interrupt:" + delegate.interruptRequested,
        "outer-interrupt:" + outer.interruptedCancellation(),
        "outer-interrupt-calls:" + outer.interruptCalls);
  }

  public static List<String> setFutureThenCancelTrace() {
    ProbeFuture outer = new ProbeFuture();
    RecordingFuture delegate = new RecordingFuture();
    boolean accepted = outer.link(delegate);
    outer.cancel(true);
    return List.of(
        "accepted:" + accepted,
        "delegate-cancelled:" + delegate.isCancelled(),
        "delegate-interrupt:" + delegate.interruptRequested,
        "outer-interrupt:" + outer.interruptedCancellation(),
        "outer-interrupt-calls:" + outer.interruptCalls);
  }

  public static List<String> cancelledDelegateTrace() {
    SettableFuture<Integer> delegate = SettableFuture.create();
    delegate.cancel(true);
    ProbeFuture outer = new ProbeFuture();
    boolean accepted = outer.link(delegate);
    return List.of(
        "accepted:" + accepted,
        "outer-cancelled:" + outer.isCancelled(),
        "outer-interrupt:" + outer.interruptedCancellation(),
        "outer-interrupt-calls:" + outer.interruptCalls);
  }

  private static final class ProbeFuture extends AbstractFuture<Integer> {
    int interruptCalls;

    boolean link(ListenableFuture<? extends Integer> delegate) {
      return setFuture(delegate);
    }

    boolean interruptedCancellation() {
      return wasInterrupted();
    }

    @Override protected void interruptTask() {
      interruptCalls++;
    }
  }

  private static final class RecordingFuture implements ListenableFuture<Integer> {
    private final List<RunnableExecutor> listeners = new ArrayList<>();
    boolean cancelled;
    boolean interruptRequested;

    @Override public synchronized boolean cancel(boolean mayInterruptIfRunning) {
      if (cancelled) return false;
      cancelled = true;
      interruptRequested = mayInterruptIfRunning;
      for (RunnableExecutor listener : new ArrayList<>(listeners)) listener.execute();
      listeners.clear();
      return true;
    }

    @Override public synchronized boolean isCancelled() { return cancelled; }
    @Override public synchronized boolean isDone() { return cancelled; }

    @Override public Integer get() throws InterruptedException, ExecutionException {
      throw new CancellationException("cancelled");
    }

    @Override public Integer get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
      return get();
    }

    @Override public synchronized void addListener(Runnable listener, Executor executor) {
      if (cancelled) executor.execute(listener);
      else listeners.add(new RunnableExecutor(listener, executor));
    }
  }

  private static final class RunnableExecutor {
    final Runnable runnable;
    final Executor executor;

    RunnableExecutor(Runnable runnable, Executor executor) {
      this.runnable = runnable;
      this.executor = executor;
    }

    void execute() { executor.execute(runnable); }
  }
}
