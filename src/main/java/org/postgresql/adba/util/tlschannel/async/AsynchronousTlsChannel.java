/*
MIT License

Copyright (c) [2015-2018] all contributors of https://github.com/marianobarrios/tls-channel, Alexander Kjäll

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package org.postgresql.adba.util.tlschannel.async;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.postgresql.adba.util.tlschannel.TlsChannel;
import org.postgresql.adba.util.tlschannel.async.AsynchronousTlsChannelGroup.ReadOperation;
import org.postgresql.adba.util.tlschannel.async.AsynchronousTlsChannelGroup.RegisteredSocket;
import org.postgresql.adba.util.tlschannel.async.AsynchronousTlsChannelGroup.WriteOperation;
import org.postgresql.adba.util.tlschannel.impl.ByteBufferSet;

/**
 * An {@link AsynchronousByteChannel} that works using {@link TlsChannel}s.
 */
public class AsynchronousTlsChannel implements ExtendedAsynchronousByteChannel {

  private class FutureReadResult extends CompletableFuture<Integer> {

    ReadOperation op;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      super.cancel(mayInterruptIfRunning);
      return group.doCancelRead(registeredSocket, op);
    }
  }

  private class FutureWriteResult extends CompletableFuture<Integer> {

    WriteOperation op;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      super.cancel(mayInterruptIfRunning);
      return group.doCancelWrite(registeredSocket, op);
    }
  }

  private final AsynchronousTlsChannelGroup group;
  private final TlsChannel tlsChannel;
  private final RegisteredSocket registeredSocket;

  /**
   * Initializes a new instance of this class.
   *
   * @param channelGroup group to associate new new channel to
   * @param tlsChannel existing TLS channel to be used asynchronously
   * @param socketChannel underlying socket
   * @throws ClosedChannelException if any of the underlying channels are closed.
   * @throws IllegalArgumentException is the socket is in blocking mode
   */
  public AsynchronousTlsChannel(
      AsynchronousTlsChannelGroup channelGroup,
      TlsChannel tlsChannel,
      SocketChannel socketChannel) throws ClosedChannelException, IllegalArgumentException {
    if (!tlsChannel.isOpen() || !socketChannel.isOpen()) {
      throw new ClosedChannelException();
    }
    if (socketChannel.isBlocking()) {
      throw new IllegalArgumentException("socket channel must be in non-blocking mode");
    }
    this.group = channelGroup;
    this.tlsChannel = tlsChannel;
    this.registeredSocket = channelGroup.registerSocket(tlsChannel, socketChannel);
  }

  @Override
  public <A> void read(
      ByteBuffer dst,
      A attach, CompletionHandler<Integer, ? super A> handler) {
    checkReadOnly(dst);
    if (!dst.hasRemaining()) {
      completeWithZeroInt(attach, handler);
      return;
    }
    group.startRead(
        registeredSocket,
        new ByteBufferSet(dst),
        0, TimeUnit.MILLISECONDS,
        c -> group.executor.submit(() -> handler.completed((int) c, attach)),
        e -> group.executor.submit(() -> handler.failed(e, attach)));
  }

  @Override
  public <A> void read(
      ByteBuffer dst,
      long timeout, TimeUnit unit,
      A attach, CompletionHandler<Integer, ? super A> handler) {
    checkReadOnly(dst);
    if (!dst.hasRemaining()) {
      completeWithZeroInt(attach, handler);
      return;
    }
    group.startRead(
        registeredSocket,
        new ByteBufferSet(dst),
        timeout, unit,
        c -> group.executor.submit(() -> handler.completed((int) c, attach)),
        e -> group.executor.submit(() -> handler.failed(e, attach)));
  }

  @Override
  public <A> void read(
      ByteBuffer[] dsts, int offset, int length,
      long timeout, TimeUnit unit,
      A attach, CompletionHandler<Long, ? super A> handler) {
    ByteBufferSet bufferSet = new ByteBufferSet(dsts, offset, length);
    if (bufferSet.isReadOnly()) {
      throw new IllegalArgumentException("buffer is read-only");
    }
    if (!bufferSet.hasRemaining()) {
      completeWithZeroLong(attach, handler);
      return;
    }
    group.startRead(
        registeredSocket,
        bufferSet,
        timeout, unit,
        c -> group.executor.submit(() -> handler.completed(c, attach)),
        e -> group.executor.submit(() -> handler.failed(e, attach)));
  }

  @Override
  public Future<Integer> read(ByteBuffer dst) {
    checkReadOnly(dst);
    if (!dst.hasRemaining()) {
      return CompletableFuture.completedFuture(0);
    }
    FutureReadResult future = new FutureReadResult();
    ReadOperation op = group.startRead(
        registeredSocket,
        new ByteBufferSet(dst),
        0, TimeUnit.MILLISECONDS,
        c -> future.complete((int) c),
        future::completeExceptionally);
    future.op = op;
    return future;
  }

  private void checkReadOnly(ByteBuffer dst) {
    if (dst.isReadOnly()) {
      throw new IllegalArgumentException("buffer is read-only");
    }
  }

  @Override
  public <A> void write(ByteBuffer src, A attach, CompletionHandler<Integer, ? super A> handler) {
    if (!src.hasRemaining()) {
      completeWithZeroInt(attach, handler);
      return;
    }
    group.startWrite(
        registeredSocket,
        new ByteBufferSet(src),
        0, TimeUnit.MILLISECONDS,
        c -> group.executor.submit(() -> handler.completed((int) c, attach)),
        e -> group.executor.submit(() -> handler.failed(e, attach)));
  }

  @Override
  public <A> void write(
      ByteBuffer src,
      long timeout, TimeUnit unit,
      A attach, CompletionHandler<Integer, ? super A> handler) {
    if (!src.hasRemaining()) {
      completeWithZeroInt(attach, handler);
      return;
    }
    group.startWrite(
        registeredSocket,
        new ByteBufferSet(src),
        timeout, unit,
        c -> group.executor.submit(() -> handler.completed((int) c, attach)),
        e -> group.executor.submit(() -> handler.failed(e, attach)));
  }

  @Override
  public <A> void write(
      ByteBuffer[] srcs, int offset, int length,
      long timeout, TimeUnit unit,
      A attach, CompletionHandler<Long, ? super A> handler) {
    ByteBufferSet bufferSet = new ByteBufferSet(srcs, offset, length);
    if (!bufferSet.hasRemaining()) {
      completeWithZeroLong(attach, handler);
      return;
    }
    group.startWrite(
        registeredSocket,
        bufferSet,
        timeout, unit,
        c -> group.executor.submit(() -> handler.completed(c, attach)),
        e -> group.executor.submit(() -> handler.failed(e, attach)));
  }

  @Override
  public Future<Integer> write(ByteBuffer src) {
    if (!src.hasRemaining()) {
      return CompletableFuture.completedFuture(0);
    }
    FutureWriteResult future = new FutureWriteResult();
    WriteOperation op = group.startWrite(
        registeredSocket,
        new ByteBufferSet(src),
        0, TimeUnit.MILLISECONDS,
        c -> future.complete((int) c),
        future::completeExceptionally);
    future.op = op;
    return future;
  }

  private <A> void completeWithZeroInt(A attach, CompletionHandler<Integer, ? super A> handler) {
    group.executor.submit(() -> handler.completed(0, attach));
  }

  private <A> void completeWithZeroLong(A attach, CompletionHandler<Long, ? super A> handler) {
    group.executor.submit(() -> handler.completed(0L, attach));
  }

  /**
   * Tells whether or not this channel is open.
   *
   * @return {@code true} if, and only if, this channel is open
   */
  @Override
  public boolean isOpen() {
    return tlsChannel.isOpen();
  }

  /**
   * Closes this channel.
   *
   * <p>This method will close the underlying {@link TlsChannel} and also deregister it from its group.</p>
   *
   * @throws IOException If an I/O error occurs
   */
  @Override
  public void close() throws IOException {
    tlsChannel.close();
    registeredSocket.close();
  }
}
