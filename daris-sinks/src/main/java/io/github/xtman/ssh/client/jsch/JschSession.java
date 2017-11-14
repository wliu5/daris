package io.github.xtman.ssh.client.jsch;

import java.io.IOException;

import io.github.xtman.ssh.client.Connection;

public class JschSession implements io.github.xtman.ssh.client.Session {

    private JschConnection _connection;
    private com.jcraft.jsch.ChannelExec _channel;

    JschSession(JschConnection connection, com.jcraft.jsch.ChannelExec channel) {
        _connection = connection;
        _channel = channel;

    }

    @Override
    public void close() throws IOException {
        _channel.disconnect();
    }

    @Override
    public Connection connection() {
        return _connection;
    }

    @Override
    public void execute(String command, String charset) throws Throwable {
        // TODO Auto-generated method stub

    }

    @Override
    public void execute(String command) throws Throwable {
        // TODO Auto-generated method stub

    }

}
