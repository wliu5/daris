package io.github.xtman.ssh.client.ganymed;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.xtman.ssh.client.Connection;

public class GanymedSession implements io.github.xtman.ssh.client.Session {

    public static final String DEFAULT_ENCODING = "UTF-8";

    private GanymedConnection _connection;
    private ch.ethz.ssh2.Session _session;

    GanymedSession(GanymedConnection connection, ch.ethz.ssh2.Session session) {
        _connection = connection;
        _session = session;
    }

    public int exitStatus() {
        return _session.getExitStatus();
    }

    public InputStream inputStream() {
        return _session.getStdout();
    }

    public OutputStream outputStream() {
        return _session.getStdin();
    }

    public void close() throws IOException {
        if (_session != null) {
            try {
                _session.close();
            } finally {
                _connection.removeSession(_session);
            }
        }
    }

    @Override
    public Connection connection() {
        return _connection;
    }

    @Override
    public void execute(String command, String charsetName) throws Throwable {
        _session.execCommand(command, charsetName);
    }

    @Override
    public void execute(String command) throws Throwable {
        _session.execCommand(command);
    }

}
