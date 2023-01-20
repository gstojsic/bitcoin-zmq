package io.github.gstojsic.bitcoin.zmq.topic;

public interface SequenceListener {
    void onMessage(SequenceMessage message);
}