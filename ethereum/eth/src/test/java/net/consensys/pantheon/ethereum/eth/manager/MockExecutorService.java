package net.consensys.pantheon.ethereum.eth.manager;

import static org.mockito.Mockito.spy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class MockExecutorService implements ExecutorService {

  private final List<Future<?>> scheduledFutures = new ArrayList<>();

  // Test utility for inspecting scheduled futures
  public List<Future<?>> getScheduledFutures() {
    return scheduledFutures;
  }

  @Override
  public void shutdown() {}

  @Override
  public List<Runnable> shutdownNow() {
    return Collections.emptyList();
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return false;
  }

  @Override
  public boolean awaitTermination(final long timeout, final TimeUnit unit)
      throws InterruptedException {
    return false;
  }

  @Override
  public <T> Future<T> submit(final Callable<T> task) {
    CompletableFuture<T> future = new CompletableFuture<>();
    try {
      final T result = task.call();
      future.complete(result);
    } catch (final Exception e) {
      future.completeExceptionally(e);
    }
    future = spy(future);
    scheduledFutures.add(future);
    return future;
  }

  @Override
  public <T> Future<T> submit(final Runnable task, final T result) {
    CompletableFuture<T> future = new CompletableFuture<>();
    try {
      task.run();
      future.complete(result);
    } catch (final Exception e) {
      future.completeExceptionally(e);
    }
    future = spy(future);
    scheduledFutures.add(future);
    return future;
  }

  @Override
  public Future<?> submit(final Runnable task) {
    CompletableFuture<?> future = new CompletableFuture<>();
    try {
      task.run();
      future.complete(null);
    } catch (final Exception e) {
      future.completeExceptionally(e);
    }
    future = spy(future);
    scheduledFutures.add(future);
    return future;
  }

  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
      throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(
      final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void execute(final Runnable command) {}
}
