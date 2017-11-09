package io.github.xtman.ssh.client;

public interface ScpPutClient extends PutClient {

    boolean compress();

    boolean preserveModificationTime();

}
