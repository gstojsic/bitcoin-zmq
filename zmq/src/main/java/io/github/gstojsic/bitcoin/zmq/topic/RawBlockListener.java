package io.github.gstojsic.bitcoin.zmq.topic;

public interface RawBlockListener {
    void onMessage(RawBlockMessage message);
}