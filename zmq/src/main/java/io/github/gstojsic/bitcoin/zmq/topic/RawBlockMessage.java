package io.github.gstojsic.bitcoin.zmq.topic;

import java.util.HexFormat;

public record RawBlockMessage(long sequence, byte[] rawBlock) {
    private static final HexFormat hex = HexFormat.of();

    public String hex() {
        return hex.formatHex(rawBlock);
    }
}