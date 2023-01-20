package io.github.gstojsic.bitcoin.zmq.topic;

import java.util.HexFormat;

public record RawTxMessage(long sequence, byte[] rawTx) {
    private static final HexFormat hex = HexFormat.of();

    public String hex() {
        return hex.formatHex(rawTx);
    }
}