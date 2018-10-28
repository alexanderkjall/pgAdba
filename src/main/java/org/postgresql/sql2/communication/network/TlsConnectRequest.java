package org.postgresql.sql2.communication.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import jdk.incubator.sql2.SessionProperty;
import org.postgresql.sql2.PgSessionProperty;
import org.postgresql.sql2.communication.BeFrame;
import org.postgresql.sql2.communication.NetworkConnect;
import org.postgresql.sql2.communication.NetworkConnectContext;
import org.postgresql.sql2.communication.NetworkOutputStream;
import org.postgresql.sql2.communication.NetworkReadContext;
import org.postgresql.sql2.communication.NetworkRequest;
import org.postgresql.sql2.communication.NetworkResponse;
import org.postgresql.sql2.communication.NetworkWriteContext;
import org.postgresql.sql2.submissions.ConnectSubmission;
import org.postgresql.sql2.util.BinaryHelper;

public class TlsConnectRequest implements NetworkConnect, NetworkRequest, NetworkResponse {

  /**
   * {@link ConnectSubmission}.
   */
  private final ConnectSubmission connectSubmission;

  /**
   * Instantiate.
   *
   * @param connectSubmission {@link ConnectSubmission}.
   */
  public TlsConnectRequest(ConnectSubmission connectSubmission) {
    this.connectSubmission = connectSubmission;
  }

  /*
   * =================== NetworkRequest ====================
   */

  @Override
  public void connect(NetworkConnectContext context) throws IOException {
    // Undertake connecting
    Map<SessionProperty, Object> properties = context.getProperties();
    context.getSocketChannel().connect(new InetSocketAddress((String) properties.get(PgSessionProperty.HOST),
        (Integer) properties.get(PgSessionProperty.PORT)));
  }

  @Override
  public NetworkRequest finishConnect(NetworkConnectContext context) throws IOException {

    // Handle completion of connect
    if (!context.getSocketChannel().finishConnect()) {
      throw new IOException("Failure to finish connection");
    }

    // Allow undertaking send start up information before any further actions
    return this;
  }

  @Override
  public NetworkRequest write(NetworkWriteContext context) throws IOException {
    NetworkOutputStream wire = context.getOutputStream();
    wire.initPacket();
    wire.write(BinaryHelper.writeInt(80877103)); // fake version string to indicate that we want to start tls
    wire.writeTerminator();
    wire.completePacket();

    // No further immediate requests
    return null;
  }

  @Override
  public boolean isBlocking() {
    return true;
  }

  @Override
  public NetworkResponse getRequiredResponse() {
    return this;
  }

  @Override
  public NetworkResponse read(NetworkReadContext context) throws IOException {
    // Expecting authentication challenge
    BeFrame frame = context.getBeFrame();

    if (frame.getPayload()[0] == 'S') {
      context.startTls();

      NetworkConnectRequest req = new NetworkConnectRequest(connectSubmission);

      context.write(req);

      context.writeRequired();
    } else {
      connectSubmission.getCompletionStage().toCompletableFuture()
          .completeExceptionally(new IllegalStateException("server doesn't support TLS, but TLS was required"));
    }
    return null;
  }

  @Override
  public NetworkResponse handleException(Throwable ex) {
    Portal.doHandleException(connectSubmission, ex);
    return null;
  }
}
