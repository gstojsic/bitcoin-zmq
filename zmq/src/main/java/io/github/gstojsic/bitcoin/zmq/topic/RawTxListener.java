package io.github.gstojsic.bitcoin.zmq.topic;

public interface RawTxListener {
    void onMessage(RawTxMessage message);
}