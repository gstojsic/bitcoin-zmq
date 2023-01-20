package io.github.gstojsic.bitcoin.zmq.topic;

import java.util.HexFormat;

public record HashTxMessage(long sequence, byte[] hash) {
    private static final HexFormat hex = HexFormat.of();

    public String hex() {
        return hex.formatHex(hash);
    }
}