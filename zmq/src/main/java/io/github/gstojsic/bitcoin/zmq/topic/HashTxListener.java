package io.github.gstojsic.bitcoin.zmq.topic;

public interface HashTxListener {
    void onMessage(HashTxMessage message);
}