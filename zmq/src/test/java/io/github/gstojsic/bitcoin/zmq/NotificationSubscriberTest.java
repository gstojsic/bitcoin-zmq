package io.github.gstojsic.bitcoin.zmq;

import io.github.gstojsic.bitcoin.proxy.BitcoinProxy;
import io.github.gstojsic.bitcoin.zmq.topic.SequenceType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NotificationSubscriberTest {

    private static final DockerImageName DOCKER_IMAGE = DockerImageName.parse("stole/bitcoin-docker:24.0");
    private static final int RPC_PORT = 18443;
    private static final int P2P_PORT = 18444;
    private static final String RPC_USER = "btcUser";
    private static final String RPC_PWD = "wUrKTR9O5zr4WJBwJjWpUXfSppDnOzhZpwIFs1Soxyc=";
    private static final String RPC_AUTH = "btcUser:d013855531d4ffbd5fdece0c2a2080cf$01db22c9da8f30e17d471daa2f02e4e49605930750f8a6f73141320ea6a6a282";
    private static final int ZMQ_PORT = 28332;
    private static final String ZMQ_ADDRESS = "tcp://*:%d".formatted(ZMQ_PORT);

    @Test
    void notificationTest(TestInfo info) {
        try (
                GenericContainer<?> btcd = new GenericContainer<>(DOCKER_IMAGE)
                        .withCommand(
                                "-regtest",
                                "-printtoconsole",
                                "-rpcallowip=172.17.0.0/16",
                                "-rpcbind=0.0.0.0",
                                "-rpcauth=%s".formatted(RPC_AUTH),
                                "-zmqpubhashtx=%s".formatted(ZMQ_ADDRESS),
                                "-zmqpubhashblock=%s".formatted(ZMQ_ADDRESS),
                                "-zmqpubrawblock=%s".formatted(ZMQ_ADDRESS),
                                "-zmqpubrawtx=%s".formatted(ZMQ_ADDRESS),
                                "-zmqpubsequence=%s".formatted(ZMQ_ADDRESS),
                                "-debug=zmq"
                        )
                        .withExposedPorts(RPC_PORT, P2P_PORT, ZMQ_PORT);
        ) {
            btcd.start();

            var proxy = createWalletProxy(getTestName(info), btcd);
            var zmq = proxy.getZmqNotifications();
            assertFalse(zmq.isEmpty());
            var pubHashTx = zmq.stream().filter(z -> "pubhashtx".equals(z.getType())).findFirst().orElseThrow();
            assertEquals(ZMQ_ADDRESS, pubHashTx.getAddress());
            var pubHashBlock = zmq.stream().filter(z -> "pubhashblock".equals(z.getType())).findFirst().orElseThrow();
            assertEquals(ZMQ_ADDRESS, pubHashBlock.getAddress());
            var pubRawBlock = zmq.stream().filter(z -> "pubrawblock".equals(z.getType())).findFirst().orElseThrow();
            assertEquals(ZMQ_ADDRESS, pubRawBlock.getAddress());
            var pubRawTx = zmq.stream().filter(z -> "pubrawtx".equals(z.getType())).findFirst().orElseThrow();
            assertEquals(ZMQ_ADDRESS, pubRawTx.getAddress());
            var pubSequence = zmq.stream().filter(z -> "pubsequence".equals(z.getType())).findFirst().orElseThrow();
            assertEquals(ZMQ_ADDRESS, pubSequence.getAddress());

            try (var subscriber = new NotificationSubscriber("localhost", btcd.getMappedPort(ZMQ_PORT))) {
                var latch = new CountDownLatch(5);
                subscriber.addHashTxListener((m) -> {
                    assertEquals(1, m.sequence());
                    var tx = proxy.getTransaction(m.hex(), null, null);
                    assertEquals(m.hex(), tx.getTxId());
                    latch.countDown();
                });
                subscriber.addRawTxListener((m) -> {
                    assertEquals(1, m.sequence());
                    var tx = proxy.decodeRawTransaction(m.hex(), null);
                    assertNotNull(tx);
                    latch.countDown();
                });
                subscriber.addSequenceListener((m) -> {
                    assertEquals(1, m.sequence());
                    var block = proxy.getBlock(m.hex());
                    assertNotNull(block);
                    assertEquals(SequenceType.BLOCKHASH_CONNECTED, m.type());
                    assertNull(m.mempoolSeq());
                    latch.countDown();
                });
                subscriber.addHashBlockListener((m) -> {
                    assertEquals(0, m.sequence());
                    var block = proxy.getBlock(m.hex());
                    assertNotNull(block);
                    latch.countDown();
                });
                subscriber.addRawBlockListener((m) -> {
                    assertEquals(0, m.sequence());
                    var script = proxy.decodeScript(m.hex());
                    assertNotNull(script);
                    latch.countDown();
                });

                subscriber.run();

                var address = proxy.getNewAddress(null, null);
                proxy.generateToAddress(1, address, null);

                var done = latch.await(10, TimeUnit.SECONDS);
                assertTrue(done);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @Disabled
    void localNodeTest() {
        try (var subscriber = new NotificationSubscriber("localhost", 28335)) {
            subscriber.addSequenceListener((m) -> {
                System.out.printf("type:%s, hex:%s, seq:%d, mempoolSeq:%d%n", m.type(), m.hex(), m.sequence(), m.mempoolSeq());
            });

            subscriber.run();
            Thread.sleep(1000000000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static BitcoinProxy createWalletProxy(String wallet, GenericContainer<?> bitcoinDaemon) {
        BitcoinProxy proxy = new BitcoinProxy(bitcoinDaemon.getHost(), bitcoinDaemon.getMappedPort(RPC_PORT), RPC_USER, RPC_PWD, wallet);
        proxy.createWallet(
                wallet,
                null,
                null,
                null,
                true,
                null,
                null,
                null);
        return proxy;
    }

    private static String getTestName(TestInfo info) {
        return info.getTestMethod().orElseThrow().getName();
    }
}