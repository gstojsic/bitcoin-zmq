# bitcoin-zmq
bitcoin-zmq is a java library that allows java applications to process bitcoin node zmq notifications.

## requirements
The library requires a bitcoin node to connect to. For details on how to install a bitcoind node please refer
to the [Bitcoin documentation][bitcoinDoc]. Check this [page][zmq-docs] to see how to configure zmq on your bitcoin node. 

The project tests use [testcontainers][testcontainers] to run a bitcoin node in a docker container so docker needs to be installed to run
tests.

## usage
To use the library you have to add it as a dependency to your gradle project:
```gradle
implementation("io.github.gstojsic.bitcoin:zmq:1.0")
```
or maven project:
```maven
<dependency>
    <groupId>io.github.gstojsic.bitcoin</groupId>
    <artifactId>zmq</artifactId>
    <version>1.0</version>
</dependency>
```

A simple example on how to use the library in your project is as follows:
```java
try (var subscriber = new NotificationSubscriber("localhost", <<zmq port>>)) {
    subscriber.addSequenceListener((m) -> {
        //use m ... 
    });

    subscriber.run();
    //...
}
```
this assumes the bitcoin node is running on localhost on port zmq port.

## related projects
If you need to connect to your bitcoin node via JSON RPC in your java application, checkout [bitcoin-proxy][bitcoin-proxy]  

[bitcoinDoc]: https://github.com/bitcoin/bitcoin/tree/master/doc#setup
[testcontainers]: https://www.testcontainers.org/
[zmq-docs]: https://github.com/bitcoin/bitcoin/blob/master/doc/zmq.md
[bitcoin-proxy]: https://github.com/gstojsic/bitcoin-proxy