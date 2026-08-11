package com.google.common.util.concurrent;

import java.util.concurrent.TimeUnit;

/** Test-only access to Guava's package-private deterministic RateLimiter clock factory. */
public final class GuavaRateLimiterHarness {
  private final FakeStopwatch stopwatch;
  private final RateLimiter limiter;

  private GuavaRateLimiterHarness(FakeStopwatch stopwatch, RateLimiter limiter) {
    this.stopwatch = stopwatch;
    this.limiter = limiter;
  }

  public static GuavaRateLimiterHarness bursty(double permitsPerSecond) {
    FakeStopwatch stopwatch = new FakeStopwatch();
    return new GuavaRateLimiterHarness(
        stopwatch,
        RateLimiter.create(permitsPerSecond, stopwatch));
  }

  public static GuavaRateLimiterHarness warmingUp(
      double permitsPerSecond, long warmupMicros) {
    FakeStopwatch stopwatch = new FakeStopwatch();
    return new GuavaRateLimiterHarness(
        stopwatch,
        RateLimiter.create(
            permitsPerSecond,
            warmupMicros,
            TimeUnit.MICROSECONDS,
            3.0,
            stopwatch));
  }

  public boolean tryAcquire(int permits) {
    return limiter.tryAcquire(permits);
  }

  public boolean tryAcquire(int permits, long timeoutMicros) {
    return limiter.tryAcquire(permits, timeoutMicros, TimeUnit.MICROSECONDS);
  }

  public void advanceMicros(long micros) {
    stopwatch.micros += micros;
  }

  public void setRate(double permitsPerSecond) {
    limiter.setRate(permitsPerSecond);
  }

  public double getRate() {
    return limiter.getRate();
  }

  @Override
  public String toString() {
    return limiter.toString();
  }

  private static final class FakeStopwatch extends RateLimiter.SleepingStopwatch {
    long micros;

    @Override
    protected long readMicros() {
      return micros;
    }

    @Override
    protected void sleepMicrosUninterruptibly(long microsToSleep) {
      micros += microsToSleep;
    }
  }
}
