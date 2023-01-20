package io.github.gstojsic.bitcoin.zmq.topic;

import java.util.HexFormat;

public record HashBlockMessage(long sequence, byte[] hashBlock) {
    private static final HexFormat hex = HexFormat.of();

    public String hex() {
        return hex.formatHex(hashBlock);
    }
}