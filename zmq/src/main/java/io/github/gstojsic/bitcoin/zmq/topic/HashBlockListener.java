package io.github.gstojsic.bitcoin.zmq.topic;

public interface HashBlockListener {
    void onMessage(HashBlockMessage message);
}