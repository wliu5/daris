package io.github.xtman.ssh.client;

import java.io.Closeable;

public interface Session extends Closeable {

    Connection connection();

    void execute(String command, String charset) throws Throwable;

    void execute(String command) throws Throwable;

}
