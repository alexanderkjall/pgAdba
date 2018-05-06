package org.postgresql.sql2.operations;

import jdk.incubator.sql2.Operation;
import jdk.incubator.sql2.Submission;
import org.postgresql.sql2.PGConnection;
import org.postgresql.sql2.PGSubmission;

import java.time.Duration;
import java.util.function.Consumer;

public class PGCloseOperation implements Operation<Void> {
  private PGConnection connection;

  public PGCloseOperation(PGConnection connection) {
    this.connection = connection;
  }

  @Override
  public Operation<Void> onError(Consumer<Throwable> handler) {
    return null;
  }

  @Override
  public Operation<Void> timeout(Duration minTime) {
    return null;
  }

  @Override
  public Submission<Void> submit() {
    PGSubmission<Void> submission = new PGSubmission<>(this::cancel);
    submission.setConnectionSubmission(false);
    submission.setSql(null);
    submission.setHolder(null);
    submission.setCompletionType(PGSubmission.Types.CLOSE);
    connection.addSubmissionOnQue(submission);
    return submission;
  }

  private boolean cancel() {
    // todo set life cycle to canceled
    return true;
  }
}
