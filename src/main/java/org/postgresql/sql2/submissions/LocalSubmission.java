package org.postgresql.sql2.submissions;

import org.postgresql.sql2.PGSubmission;
import org.postgresql.sql2.communication.packets.DataRow;
import org.postgresql.sql2.operations.helpers.ParameterHolder;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class LocalSubmission<T> implements PGSubmission<T> {
  final private Supplier<Boolean> cancel;
  private CompletableFuture<T> publicStage;
  private Consumer<Throwable> errorHandler;
  private Callable<T> localAction;
  private PGSubmission groupSubmission;

  public LocalSubmission(Supplier<Boolean> cancel, Consumer<Throwable> errorHandler, Callable<T> localAction, BaseSubmission groupSubmission) {
    this.cancel = cancel;
    this.errorHandler = errorHandler;
    this.localAction = localAction;
    this.groupSubmission = groupSubmission;
  }

  @Override
  public void setSql(String sql) {

  }

  @Override
  public String getSql() {
    return null;
  }

  @Override
  public AtomicBoolean getSendConsumed() {
    return null;
  }

  @Override
  public ParameterHolder getHolder() {
    return null;
  }

  @Override
  public Types getCompletionType() {
    return Types.LOCAL;
  }

  @Override
  public void setCollector(Collector collector) {

  }

  @Override
  public Object finish() {
    try {
      T localResult = localAction.call();
      if(groupSubmission != null) {
        groupSubmission.addGroupResult(localResult);
      }
      getCompletionStage().toCompletableFuture().complete(localResult);
    } catch (Exception e) {
      getCompletionStage().toCompletableFuture().completeExceptionally(e);
    }
    return null;
  }

  @Override
  public void addRow(DataRow row) {

  }

  @Override
  public void addGroupResult(Object result) {

  }

  @Override
  public List<Integer> getParamTypes() throws ExecutionException, InterruptedException {
    return null;
  }

  @Override
  public int numberOfQueryRepetitions() throws ExecutionException, InterruptedException {
    return 0;
  }

  @Override
  public List<Long> countResult() {
    return null;
  }

  @Override
  public Consumer<Throwable> getErrorHandler() {
    return errorHandler;
  }

  @Override
  public PGSubmission getGroupSubmission() {
    return groupSubmission;
  }

  @Override
  public CompletionStage<Boolean> cancel() {
    return null;
  }

  @Override
  public CompletionStage<T> getCompletionStage() {
    if (publicStage == null)
      publicStage = new CompletableFuture<>();
    return publicStage;
  }
}