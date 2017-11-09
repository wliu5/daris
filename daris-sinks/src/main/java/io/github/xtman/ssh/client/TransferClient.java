package io.github.xtman.ssh.client;

import java.io.Closeable;

public interface TransferClient extends Closeable {

    Connection connection();

    boolean verbose();

}
