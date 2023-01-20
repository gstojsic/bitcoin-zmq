package io.github.gstojsic.bitcoin.zmq;

import io.github.gstojsic.bitcoin.zmq.topic.HashBlockListener;
import io.github.gstojsic.bitcoin.zmq.topic.HashBlockMessage;
import io.github.gstojsic.bitcoin.zmq.topic.HashTxListener;
import io.github.gstojsic.bitcoin.zmq.topic.HashTxMessage;
import io.github.gstojsic.bitcoin.zmq.topic.RawBlockListener;
import io.github.gstojsic.bitcoin.zmq.topic.RawBlockMessage;
import io.github.gstojsic.bitcoin.zmq.topic.RawTxListener;
import io.github.gstojsic.bitcoin.zmq.topic.RawTxMessage;
import io.github.gstojsic.bitcoin.zmq.topic.SequenceListener;
import io.github.gstojsic.bitcoin.zmq.topic.SequenceMessage;
import io.github.gstojsic.bitcoin.zmq.topic.SequenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotificationSubscriber implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(NotificationSubscriber.class);
    private static final int MEMPOOL_SEQ_OFFSET = 32;
    private static final int ULONG_LENGTH = 8;
    private final ZContext context = new ZContext();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final String address;
    private final List<HashTxListener> hashTxListeners = new ArrayList<>();
    private final List<RawTxListener> rawTxListeners = new ArrayList<>();
    private final List<HashBlockListener> hashBlockListeners = new ArrayList<>();
    private final List<RawBlockListener> rawBlockListeners = new ArrayList<>();
    private final List<SequenceListener> sequenceListeners = new ArrayList<>();

    public NotificationSubscriber(String host, int port) {
        address = "tcp://%s:%d".formatted(host, port);
    }

    public void addHashTxListener(HashTxListener listener) {
        hashTxListeners.add(listener);
    }

    public void addRawTxListener(RawTxListener listener) {
        rawTxListeners.add(listener);
    }

    public void addHashBlockListener(HashBlockListener listener) {
        hashBlockListeners.add(listener);
    }

    public void addRawBlockListener(RawBlockListener listener) {
        rawBlockListeners.add(listener);
    }

    public void addSequenceListener(SequenceListener listener) {
        sequenceListeners.add(listener);
    }

    public void run() {
        // create subscriber zeromq socket
        ZMQ.Socket socket = context.createSocket(SocketType.SUB);
        boolean connected = socket.connect(address);
        if (!connected)
            throw new RuntimeException("could not connect to zeromq socket %s".formatted(address));

        boolean subscribed = socket.subscribe(""); // subscribe to all topics
        if (!subscribed)
            throw new RuntimeException("could not subscribe to zeromq topics");

        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                String topic = socket.recvStr(0);
                switch (topic) {
                    case "hashtx" -> {
                        byte[] txHash = socket.recv(0);
                        byte[] seq = socket.recv(0);
                        if (hashTxListeners.isEmpty())
                            continue;
                        HashTxMessage message = new HashTxMessage(toUnsignedInt(seq), txHash);
                        hashTxListeners.forEach((listener) -> {
                            try {
                                listener.onMessage(message);
                            } catch (Throwable e) {
                                logger.error("failed to process hashTx message for listener:{}", listener);
                            }
                        });
                    }
                    case "rawtx" -> {
                        byte[] rawTx = socket.recv(0);
                        byte[] seq = socket.recv(0);
                        if (rawTxListeners.isEmpty())
                            continue;
                        RawTxMessage message = new RawTxMessage(toUnsignedInt(seq), rawTx);
                        rawTxListeners.forEach((listener) -> {
                            try {
                                listener.onMessage(message);
                            } catch (Throwable e) {
                                logger.error("failed to process rawTx message for listener:{}", listener);
                            }
                        });
                    }
                    case "hashblock" -> {
                        byte[] hashBlock = socket.recv(0);
                        byte[] seq = socket.recv(0);
                        if (hashBlockListeners.isEmpty())
                            continue;
                        HashBlockMessage message = new HashBlockMessage(toUnsignedInt(seq), hashBlock);
                        hashBlockListeners.forEach((listener) -> {
                            try {
                                listener.onMessage(message);
                            } catch (Throwable e) {
                                logger.error("failed to process hashBlock message for listener:{}", listener);
                            }
                        });
                    }
                    case "rawblock" -> {
                        byte[] rawBlock = socket.recv(0);
                        byte[] seq = socket.recv(0);
                        if (rawBlockListeners.isEmpty())
                            continue;
                        RawBlockMessage message = new RawBlockMessage(toUnsignedInt(seq), rawBlock);
                        rawBlockListeners.forEach((listener) -> {
                            try {
                                listener.onMessage(message);
                            } catch (Throwable e) {
                                logger.error("failed to process rawblock message for listener:{}", listener);
                            }
                        });
                    }
                    case "sequence" -> {
                        byte[] sequenceMsg = socket.recv(0);
                        byte[] seq = socket.recv(0);
                        if (sequenceListeners.isEmpty())
                            continue;

                        byte[] hash = new byte[32];
                        System.arraycopy(sequenceMsg, 0, hash, 0, hash.length);

                        SequenceType type = resolveSequenceType(sequenceMsg[32]);

                        Long mempoolSeq = (type == SequenceType.TRANSACTIONHASH_ADDED || type == SequenceType.TRANSACTIONHASH_REMOVED)
                                ? resolveMempoolSeq(sequenceMsg)
                                : null;

                        SequenceMessage message = new SequenceMessage(toUnsignedInt(seq), hash, type, mempoolSeq);
                        sequenceListeners.forEach((listener) -> {
                            try {
                                listener.onMessage(message);
                            } catch (Throwable e) {
                                logger.error("failed to process sequence message for listener:{}", listener);
                            }
                        });
                    }
                    default -> logger.error("unknown topic or message part:{}", topic);
                }
            }
        });
    }

    @Override
    public void close() {
        try {
            context.close();
            executor.close();
            boolean done = executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!done)
                logger.error("timeout while waiting for zmq executor to terminate");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static long toUnsignedInt(byte[] bytes) {
        return ((long) (bytes[3] & 0xff) << 24) + ((bytes[2] & 0xff) << 16) + ((bytes[1] & 0xff) << 8) + (bytes[0] & 0xff);
    }

    private static long resolveMempoolSeq(byte[] bytes) {
        return ByteBuffer.wrap(bytes, MEMPOOL_SEQ_OFFSET, ULONG_LENGTH).getLong();
    }

    private static SequenceType resolveSequenceType(byte b) {
        switch (b) {
            case 67 -> {
                return SequenceType.BLOCKHASH_CONNECTED;
            }
            case 68 -> {
                return SequenceType.BLOCKHASH_DISCONNECTED;
            }
            case 82 -> {
                return SequenceType.TRANSACTIONHASH_REMOVED;
            }
            case 65 -> {
                return SequenceType.TRANSACTIONHASH_ADDED;
            }
            default -> throw new IllegalArgumentException("invalid SequenceType:%d".formatted(b));
        }
    }
}