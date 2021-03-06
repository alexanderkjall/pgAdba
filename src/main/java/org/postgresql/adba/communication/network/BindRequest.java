package org.postgresql.adba.communication.network;

import org.postgresql.adba.communication.FrontendTag;
import org.postgresql.adba.communication.NetworkOutputStream;
import org.postgresql.adba.communication.NetworkRequest;
import org.postgresql.adba.communication.NetworkResponse;
import org.postgresql.adba.communication.NetworkWriteContext;
import org.postgresql.adba.operations.helpers.ParameterHolder;
import org.postgresql.adba.operations.helpers.QueryParameter;
import org.postgresql.adba.util.BinaryHelper;

/**
 * Bind {@link NetworkRequest}.
 * 
 * @author Daniel Sagenschneider
 */
public class BindRequest<T> implements NetworkRequest {

  private final Portal portal;

  public BindRequest(Portal portal) {
    this.portal = portal;
  }

  /*
   * ================= NetworkRequest =========================
   */

  @Override
  public NetworkRequest write(NetworkWriteContext context) throws Exception {

    // Obtain the query details
    ParameterHolder holder = portal.getParameterHolder();

    // Write the packet
    NetworkOutputStream wire = context.getOutputStream();
    wire.write(FrontendTag.BIND.getByte());
    wire.initPacket();
    wire.write(portal.getPortalName());
    wire.write(portal.getQuery().getQueryName());
    wire.write(BinaryHelper.writeShort(holder.size()));
    for (QueryParameter qp : holder.parameters()) {
      wire.write(BinaryHelper.writeShort(qp.getParameterFormatCode()));
    }
    wire.write(BinaryHelper.writeShort(holder.size()));
    int paramIndex = 0;
    for (QueryParameter qp : holder.parameters()) {
      byte[] paramData = qp.getParameter(paramIndex++);
      if (paramData.length == 0) { // handling the null special case
        wire.write(BinaryHelper.writeInt(-1));
      } else {
        wire.write(BinaryHelper.writeInt(paramData.length));
        wire.write(paramData);
      }
    }
    wire.writeTerminator();
    wire.writeTerminator();
    wire.completePacket();

    // Next step to execute
    return new ExecuteRequest<>(portal);
  }

  @Override
  public NetworkResponse getRequiredResponse() {
    return new BindResponse(portal);
  }

}