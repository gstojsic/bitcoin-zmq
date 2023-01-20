package io.github.gstojsic.bitcoin.zmq.topic;

import java.util.HexFormat;

public record SequenceMessage(long sequence, byte[] hash, SequenceType type, Long mempoolSeq) {
    private static final HexFormat hex = HexFormat.of();

    public String hex() {
        return hex.formatHex(hash);
    }
}