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

package org.postgresql.adba.util.tlschannel.impl;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import org.postgresql.adba.util.tlschannel.NeedsReadException;
import org.postgresql.adba.util.tlschannel.NeedsTaskException;
import org.postgresql.adba.util.tlschannel.NeedsWriteException;
import org.postgresql.adba.util.tlschannel.TrackingAllocator;
import org.postgresql.adba.util.tlschannel.WouldBlockException;
import org.postgresql.adba.util.tlschannel.util.TlsChannelCallbackException;
import org.postgresql.adba.util.tlschannel.util.Util;

public class TlsChannelImpl implements ByteChannel {

  private static final Logger logger = Logger.getLogger(TlsChannelImpl.class.getName());

  public static final int buffersInitialSize = 4096;

  /**
   * Official TLS max data size is 2^14 = 16k. Use 1024 more to account for the overhead
   */
  public static final int maxTlsPacketSize = 17 * 1024;

  private static class UnwrapResult {

    public final int bytesProduced;
    public final HandshakeStatus lastHandshakeStatus;
    public final boolean wasClosed;

    public UnwrapResult(int bytesProduced, HandshakeStatus lastHandshakeStatus, boolean wasClosed) {
      this.bytesProduced = bytesProduced;
      this.lastHandshakeStatus = lastHandshakeStatus;
      this.wasClosed = wasClosed;
    }
  }

  private static class WrapResult {

    public final int bytesConsumed;
    public final HandshakeStatus lastHandshakeStatus;

    public WrapResult(int bytesConsumed, HandshakeStatus lastHandshakeStatus) {
      this.bytesConsumed = bytesConsumed;
      this.lastHandshakeStatus = lastHandshakeStatus;
    }
  }

  /**
   * Used to signal EOF conditions from the underlying channel.
   */
  public static class EofException extends Exception {

    /**
     * For efficiency, override this method to do nothing.
     */
    @Override
    public Throwable fillInStackTrace() {
      return this;
    }

  }

  private final ReadableByteChannel readChannel;
  private final WritableByteChannel writeChannel;
  private final SSLEngine engine;
  private BufferHolder inEncrypted;
  private final Consumer<SSLSession> initSessionCallback;

  private final boolean runTasks;
  private final TrackingAllocator encryptedBufAllocator;
  private final TrackingAllocator plainBufAllocator;
  private final boolean waitForCloseConfirmation;

  /**
   * constructs the tlschannel.
   * @param readChannel readChannel
   * @param writeChannel writeChannel
   * @param engine engine
   * @param inEncrypted inEncrypted
   * @param initSessionCallback initSessionCallback
   * @param runTasks runTasks
   * @param plainBufAllocator plainBufAllocator
   * @param encryptedBufAllocator encryptedBufAllocator
   * @param releaseBuffers releaseBuffers
   * @param waitForCloseConfirmation waitForCloseConfirmation
   */
  public TlsChannelImpl(
      ReadableByteChannel readChannel,
      WritableByteChannel writeChannel,
      SSLEngine engine,
      Optional<BufferHolder> inEncrypted,
      Consumer<SSLSession> initSessionCallback,
      boolean runTasks,
      TrackingAllocator plainBufAllocator,
      TrackingAllocator encryptedBufAllocator,
      boolean releaseBuffers,
      boolean waitForCloseConfirmation) {
    // @formatter:on
    this.readChannel = readChannel;
    this.writeChannel = writeChannel;
    this.engine = engine;
    this.inEncrypted = inEncrypted.orElseGet(() ->
        new BufferHolder(
            "inEncrypted",
            Optional.empty(),
            encryptedBufAllocator,
            buffersInitialSize,
            maxTlsPacketSize,
            false /* plainData */,
            releaseBuffers));
    this.initSessionCallback = initSessionCallback;
    this.runTasks = runTasks;
    this.plainBufAllocator = plainBufAllocator;
    this.encryptedBufAllocator = encryptedBufAllocator;
    this.waitForCloseConfirmation = waitForCloseConfirmation;
    inPlain = new BufferHolder(
        "inPlain",
        Optional.empty(),
        plainBufAllocator,
        buffersInitialSize,
        maxTlsPacketSize,
        true /* plainData */,
        releaseBuffers);
    outEncrypted = new BufferHolder(
        "outEncrypted",
        Optional.empty(),
        encryptedBufAllocator,
        buffersInitialSize,
        maxTlsPacketSize,
        false /* plainData */,
        releaseBuffers);
  }

  private final Lock initLock = new ReentrantLock();
  private final Lock readLock = new ReentrantLock();
  private final Lock writeLock = new ReentrantLock();

  private volatile boolean negotiated = false;

  /**
   * Whether a IOException was received from the underlying channel or from the {@link SSLEngine}.
   */
  private volatile boolean invalid = false;

  /**
   * Whether a close_notify was already sent.
   */
  private volatile boolean shutdownSent = false;

  /**
   * Whether a close_notify was already received.
   */
  private volatile boolean shutdownReceived = false;

  // decrypted data from inEncrypted
  private BufferHolder inPlain;

  // contains data encrypted to send to the underlying channel
  private BufferHolder outEncrypted;

  // handshake wrap() method calls need a buffer to read from, even when they
  // actually do not read anything
  private final ByteBufferSet dummyOut = new ByteBufferSet(new ByteBuffer[]{});

  public Consumer<SSLSession> getSessionInitCallback() {
    return initSessionCallback;
  }

  public TrackingAllocator getPlainBufferAllocator() {
    return plainBufAllocator;
  }

  public TrackingAllocator getEncryptedBufferAllocator() {
    return encryptedBufAllocator;
  }

  /**
   * Reads from the channel.
   * @param dest to read to
   * @return bytes read
   * @throws IOException if something goes wrong in underlying channels
   * @throws NeedsTaskException if the tls handshake needs to run a task
   */
  public long read(ByteBufferSet dest) throws IOException, NeedsTaskException {
    checkReadBuffer(dest);
    if (!dest.hasRemaining()) {
      return 0;
    }
    handshake();
    readLock.lock();
    try {
      if (invalid || shutdownSent) {
        throw new ClosedChannelException();
      }
      HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
      int bytesToReturn = inPlain.nullOrEmpty() ? 0 : inPlain.buffer.position();
      while (true) {
        if (bytesToReturn > 0) {
          if (inPlain.nullOrEmpty()) {
            return bytesToReturn;
          } else {
            return transferPendingPlain(dest);
          }
        }
        if (shutdownReceived) {
          return -1;
        }
        Util.assertTrue(inPlain.nullOrEmpty());
        switch (handshakeStatus) {
          case NEED_UNWRAP:
          case NEED_WRAP:
            bytesToReturn = handshake(Optional.of(dest), Optional.of(handshakeStatus));
            handshakeStatus = NOT_HANDSHAKING;
            break;
          case NOT_HANDSHAKING:
          case FINISHED:
            UnwrapResult res = readAndUnwrap(Optional.of(dest), NOT_HANDSHAKING /* statusCondition */,
                false /* closing */);
            if (res.wasClosed) {
              return -1;
            }
            bytesToReturn = res.bytesProduced;
            handshakeStatus = res.lastHandshakeStatus;
            break;
          case NEED_TASK:
            handleTask();
            handshakeStatus = engine.getHandshakeStatus();
            break;
          default:
            throw new RuntimeException("reached default clause in switch");
        }
      }
    } catch (EofException e) {
      return -1;
    } finally {
      readLock.unlock();
    }
  }

  private void handleTask() throws NeedsTaskException {
    if (runTasks) {
      engine.getDelegatedTask().run();
    } else {
      throw new NeedsTaskException(engine.getDelegatedTask());
    }
  }

  private int transferPendingPlain(ByteBufferSet dstBuffers) {
    inPlain.buffer.flip(); // will read
    int bytes = dstBuffers.putRemaining(inPlain.buffer);
    inPlain.buffer.compact(); // will write
    boolean disposed = inPlain.release();
    if (!disposed) {
      inPlain.zeroRemaining();
    }
    return bytes;
  }

  private UnwrapResult unwrapLoop(Optional<ByteBufferSet> dest, HandshakeStatus statusCondition, boolean closing)
      throws SSLException {
    ByteBufferSet effDest = dest.orElseGet(() -> {
      inPlain.prepare();
      return new ByteBufferSet(inPlain.buffer);
    });
    while (true) {
      Util.assertTrue(inPlain.nullOrEmpty());
      SSLEngineResult result = callEngineUnwrap(effDest);
      /*
       * Note that data can be returned even in case of overflow, in that
       * case, just return the data.
       */
      if (result.bytesProduced() > 0 || result.getStatus() == Status.BUFFER_UNDERFLOW
          || !closing && result.getStatus() == Status.CLOSED
          || result.getHandshakeStatus() != statusCondition) {
        boolean wasClosed = result.getStatus() == Status.CLOSED;
        return new UnwrapResult(result.bytesProduced(), result.getHandshakeStatus(), wasClosed);
      }
      if (result.getStatus() == Status.BUFFER_OVERFLOW) {
        if (dest.isPresent() && effDest == dest.get()) {
          /*
           * The client-supplier buffer is not big enough. Use the
           * internal inPlain buffer, also ensure that it is bigger
           * than the too-small supplied one.
           */
          inPlain.prepare();
          ensureInPlainCapacity(Math.min(((int) dest.get().remaining()) * 2, maxTlsPacketSize));
        } else {
          inPlain.enlarge();
        }
        // inPlain changed, re-create the wrapper
        effDest = new ByteBufferSet(inPlain.buffer);
      }
    }
  }

  private SSLEngineResult callEngineUnwrap(ByteBufferSet dest) throws SSLException {
    inEncrypted.buffer.flip();
    try {
      SSLEngineResult result = engine.unwrap(inEncrypted.buffer, dest.array, dest.offset, dest.length);
      if (logger.isLoggable(Level.INFO)) {
        logger.log(Level.INFO, "engine.unwrap() result [" + Util.resultToString(result) + "]. Engine status: "
            + result.getHandshakeStatus() + "; inEncrypted " + inEncrypted + "; inPlain: " + dest);
      }
      return result;
    } catch (SSLException e) {
      // something bad was received from the underlying channel, we cannot
      // continue
      invalid = true;
      throw e;
    } finally {
      inEncrypted.buffer.compact();
    }
  }

  private int readFromChannel() throws IOException, EofException {
    try {
      return readFromChannel(readChannel, inEncrypted.buffer);
    } catch (WouldBlockException e) {
      throw e;
    } catch (IOException e) {
      invalid = true;
      throw e;
    }
  }

  /**
   * reads from channel supplied.
   * @param readChannel to read from
   * @param buffer to write to
   * @return number of bytes written
   * @throws IOException if something goes wrong
   * @throws EofException if terminated
   */
  public static int readFromChannel(ReadableByteChannel readChannel, ByteBuffer buffer)
      throws IOException, EofException {
    Util.assertTrue(buffer.hasRemaining());
    logger.log(Level.INFO, "Reading from channel");
    int c = readChannel.read(buffer); // IO block
    logger.log(Level.INFO, "Read from channel; response: " + c + ", buffer: " + buffer);
    if (c == -1) {
      throw new EofException();
    }
    if (c == 0) {
      throw new NeedsReadException();
    }
    return c;
  }

  // write

  /**
   * write data to the channel to be encrypted.
   * @param source source to read data from
   * @return number of bytes
   * @throws IOException if something goes wrong in underlying channel
   */
  public long write(ByteBufferSet source) throws IOException {
    /*
     * Note that we should enter the write loop even in the case that the source buffer has no remaining bytes,
     * as it could be the case, in non-blocking usage, that the user is forced to call write again after the
     * underlying channel is available for writing, just to write pending encrypted bytes.
     */
    handshake();
    writeLock.lock();
    try {
      if (invalid || shutdownSent) {
        throw new ClosedChannelException();
      }
      return wrapAndWrite(source);
    } finally {
      writeLock.unlock();
    }
  }

  private long wrapAndWrite(ByteBufferSet source) throws IOException {
    long bytesToConsume = source.remaining();
    long bytesConsumed = 0;
    outEncrypted.prepare();
    try {
      while (true) {
        writeToChannel();
        if (bytesConsumed == bytesToConsume) {
          return bytesToConsume;
        }
        WrapResult res = wrapLoop(source);
        bytesConsumed += res.bytesConsumed;
      }
    } finally {
      outEncrypted.release();
    }
  }

  private WrapResult wrapLoop(ByteBufferSet source) throws SSLException {
    while (true) {
      SSLEngineResult result = callEngineWrap(source);
      switch (result.getStatus()) {
        case OK:
        case CLOSED:
          return new WrapResult(result.bytesConsumed(), result.getHandshakeStatus());
        case BUFFER_OVERFLOW:
          Util.assertTrue(result.bytesConsumed() == 0);
          outEncrypted.enlarge();
          break;
        case BUFFER_UNDERFLOW:
          throw new IllegalStateException();
        default:
          throw new RuntimeException("reached default clause in switch");
      }
    }
  }

  private SSLEngineResult callEngineWrap(ByteBufferSet source) throws SSLException {
    try {
      SSLEngineResult result = engine.wrap(source.array, source.offset, source.length, outEncrypted.buffer);
      if (logger.isLoggable(Level.INFO)) {
        logger.log(Level.INFO, "engine.wrap() result: [" + Util.resultToString(result) + "]; engine status: "
            + result.getHandshakeStatus() + "; srcBuffer: " + source + ", outEncrypted: " + outEncrypted);
      }
      return result;
    } catch (SSLException e) {
      invalid = true;
      throw e;
    }
  }

  private void ensureInPlainCapacity(int newCapacity) {
    if (inPlain.buffer.capacity() < newCapacity) {
      logger.log(Level.INFO, "inPlain buffer too small, increasing from " + inPlain.buffer.capacity() + " to " + newCapacity);
      inPlain.resize(newCapacity);
    }
  }

  private void writeToChannel() throws IOException {
    if (outEncrypted.buffer.position() == 0) {
      return;
    }
    outEncrypted.buffer.flip();
    try {
      try {
        writeToChannel(writeChannel, outEncrypted.buffer);
      } catch (WouldBlockException e) {
        throw e;
      } catch (IOException e) {
        invalid = true;
        throw e;
      }
    } finally {
      outEncrypted.buffer.compact();
    }
  }

  private static void writeToChannel(WritableByteChannel channel, ByteBuffer src) throws IOException {
    while (src.hasRemaining()) {
      logger.log(Level.INFO, "Writing to channel: " + src);
      int c = channel.write(src);
      if (c == 0) {
        /*
         * If no bytesProduced were written, it means that the socket is
         * non-blocking and needs more buffer space, so stop the loop
         */
        throw new NeedsWriteException();
      }
      // blocking SocketChannels can write less than all the bytesProduced
      // just before an error the loop forces the exception
    }
  }

  // handshake and close

  /**
   * Force new negotiation.
   * @throws IOException on end of stream
   */
  public void renegotiate() throws IOException {
    try {
      doHandshake(true /* force */);
    } catch (EofException e) {
      throw new ClosedChannelException();
    }
  }

  /**
   * Do a negotiation if this connection is new and it hasn't been done already.
   * @throws IOException on end of stream
   */
  public void handshake() throws IOException {
    try {
      doHandshake(false /* force */);
    } catch (EofException e) {
      throw new ClosedChannelException();
    }
  }

  private void doHandshake(boolean force) throws IOException, EofException {
    if (!force && negotiated) {
      return;
    }
    initLock.lock();
    if (invalid || shutdownSent) {
      throw new ClosedChannelException();
    }
    try {
      if (force || !negotiated) {
        engine.beginHandshake();
        logger.log(Level.INFO, "Called engine.beginHandshake()");
        handshake(Optional.empty(), Optional.empty());
        // call client code
        try {
          initSessionCallback.accept(engine.getSession());
        } catch (Exception e) {
          logger.log(Level.INFO, "client code threw exception in session initialization callback", e);
          throw new TlsChannelCallbackException("session initialization callback failed", e);
        }
        negotiated = true;
      }
    } finally {
      initLock.unlock();
    }
  }

  private int handshake(Optional<ByteBufferSet> dest, Optional<HandshakeStatus> handshakeStatus)
      throws IOException, EofException {
    readLock.lock();
    try {
      writeLock.lock();
      try {
        Util.assertTrue(inPlain.nullOrEmpty());
        outEncrypted.prepare();
        try {
          writeToChannel(); // IO block
          return handshakeLoop(dest, handshakeStatus);
        } finally {
          outEncrypted.release();
        }
      } finally {
        writeLock.unlock();
      }
    } finally {
      readLock.unlock();
    }
  }

  private int handshakeLoop(Optional<ByteBufferSet> dest, Optional<HandshakeStatus> handshakeStatus)
      throws IOException, EofException {
    Util.assertTrue(inPlain.nullOrEmpty());
    HandshakeStatus status = handshakeStatus.orElseGet(() -> engine.getHandshakeStatus());
    while (true) {
      switch (status) {
        case NEED_WRAP:
          Util.assertTrue(outEncrypted.nullOrEmpty());
          WrapResult wrapResult = wrapLoop(dummyOut);
          status = wrapResult.lastHandshakeStatus;
          writeToChannel(); // IO block
          break;
        case NEED_UNWRAP:
          UnwrapResult res = readAndUnwrap(dest, NEED_UNWRAP /* statusCondition */, false /* closing */);
          status = res.lastHandshakeStatus;
          if (res.bytesProduced > 0) {
            return res.bytesProduced;
          }
          break;
        case NOT_HANDSHAKING:
          /*
           * This should not really happen using SSLEngine, because
           * handshaking ends with a FINISHED status. However, we accept
           * this value to permit the use of a pass-through stub engine
           * with no encryption.
           */
          return 0;
        case NEED_TASK:
          handleTask();
          status = engine.getHandshakeStatus();
          break;
        case FINISHED:
          return 0;
        default:
          throw new RuntimeException("reached default clause in switch");
      }
    }
  }

  private UnwrapResult readAndUnwrap(Optional<ByteBufferSet> dest, HandshakeStatus statusCondition, boolean closing)
      throws IOException, EofException {
    inEncrypted.prepare();
    try {
      while (true) {
        Util.assertTrue(inPlain.nullOrEmpty());
        UnwrapResult res = unwrapLoop(dest, statusCondition, closing);
        if (res.bytesProduced > 0 || res.lastHandshakeStatus != statusCondition || !closing && res.wasClosed) {
          if (res.wasClosed) {
            shutdownReceived = true;
          }
          return res;
        }
        if (!inEncrypted.buffer.hasRemaining()) {
          inEncrypted.enlarge();
        }
        readFromChannel(); // IO block
      }
    } finally {
      inEncrypted.release();
    }
  }

  /**
   * close the tls encryption and the underlying channels.
   * @throws IOException if something goes wrong
   */
  public void close() throws IOException {
    tryShutdown();
    writeChannel.close();
    readChannel.close();
    /*
     * After closing the underlying channels, locks should be taken fast.
     */
    readLock.lock();
    try {
      writeLock.lock();
      try {
        freeBuffers();
      } finally {
        writeLock.unlock();
      }
    } finally {
      readLock.unlock();
    }
  }

  private void tryShutdown() {
    if (!readLock.tryLock()) {
      return;
    }
    try {
      if (!writeLock.tryLock()) {
        return;
      }
      try {
        if (!shutdownSent) {
          try {
            boolean closed = shutdown();
            if (!closed && waitForCloseConfirmation) {
              shutdown();
            }
          } catch (Throwable e) {
            logger.log(Level.INFO, "error doing TLS shutdown on close(), continuing: " + e.getMessage());
          }
        }
      } finally {
        writeLock.unlock();
      }
    } finally {
      readLock.unlock();
    }
  }

  /**
   * terminate.
   * @return true on success
   * @throws IOException if anything goes wrong
   */
  public boolean shutdown() throws IOException {
    readLock.lock();
    try {
      writeLock.lock();
      try {
        if (invalid) {
          throw new ClosedChannelException();
        }
        if (!shutdownSent) {
          shutdownSent = true;
          outEncrypted.prepare();
          try {
            writeToChannel(); // IO block
            engine.closeOutbound();
            wrapLoop(dummyOut);
            writeToChannel(); // IO block
          } finally {
            outEncrypted.release();
          }
          /*
           * If this side is the first to send close_notify, then,
           * inbound is not done and false should be returned (so the
           * client waits for the response. If this side is the
           * second, then inbound was already done, and we can return
           * true.
           */
          if (shutdownReceived) {
            freeBuffers();
          }
          return shutdownReceived;
        }
        /*
         * If we reach this point, then we just have to read the close
         * notification from the client. Only try to do it if necessary,
         * to make this method idempotent.
         */
        if (!shutdownReceived) {
          try {
            // IO block
            readAndUnwrap(Optional.empty(), NEED_UNWRAP /* statusCondition */, true /* closing */);
            Util.assertTrue(shutdownReceived);
          } catch (EofException e) {
            throw new ClosedChannelException();
          }
        }
        freeBuffers();
        return true;
      } finally {
        writeLock.unlock();
      }
    } finally {
      readLock.unlock();
    }
  }

  private void freeBuffers() {
    if (inEncrypted != null) {
      inEncrypted.dispose();
      inEncrypted = null;
    }
    if (inPlain != null) {
      inPlain.dispose();
      inPlain = null;
    }
    if (outEncrypted != null) {
      outEncrypted.dispose();
      outEncrypted = null;
    }
  }

  public boolean isOpen() {
    return !invalid && writeChannel.isOpen() && readChannel.isOpen();
  }

  /**
   * throws if it's readOnly.
   * @param dest to check
   */
  public static void checkReadBuffer(ByteBufferSet dest) {
    if (dest.isReadOnly()) {
      throw new IllegalArgumentException();
    }
  }

  public SSLEngine engine() {
    return engine;
  }

  public boolean getRunTasks() {
    return runTasks;
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    return (int) read(new ByteBufferSet(dst));
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    return (int) write(new ByteBufferSet(src));
  }

  public boolean shutdownReceived() {
    return shutdownReceived;
  }

  public boolean shutdownSent() {
    return shutdownSent;
  }

  public ReadableByteChannel plainReadableChannel() {
    return readChannel;
  }

  public WritableByteChannel plainWritableChannel() {
    return writeChannel;
  }
}