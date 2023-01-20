package io.github.gstojsic.bitcoin.zmq.topic;

/**
 * Sequence types received in a sequence zmq message. See <a href="https://github.com/bitcoin/bitcoin/blob/master/doc/zmq.md">bitcoin docs</a>
 */
public enum SequenceType {

    /**
     * Blockhash connected (received as ASCII 'C')
     */
    BLOCKHASH_CONNECTED,

    /**
     * Blockhash disconnected (received as ASCII 'D')
     */
    BLOCKHASH_DISCONNECTED,

    /**
     * Transactionhash removed from mempool for non-block inclusion reason (received as ASCII 'R')
     */
    TRANSACTIONHASH_REMOVED,

    /**
     * Transactionhash added mempool (received as ASCII 'A')
     */
    TRANSACTIONHASH_ADDED
}