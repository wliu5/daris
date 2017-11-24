package io.github.xtman.ssh.client.jsch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;

import io.github.xtman.ssh.client.ConnectionBuilder;
import io.github.xtman.ssh.client.ConnectionDetails;
import io.github.xtman.ssh.client.PutClient;
import io.github.xtman.ssh.client.ScpPutClient;
import io.github.xtman.ssh.client.Session;
import io.github.xtman.ssh.client.SftpClient;

public class JschConnection implements io.github.xtman.ssh.client.Connection {

    public static final String IMPL = "jsch";

    private ConnectionDetails _cxnDetails;
    private JSch _jsch;
    private com.jcraft.jsch.Session _jschSession;
    private int _maxSessions;
    private boolean _verbose;

    private Collection<com.jcraft.jsch.Channel> _channels;

    public JschConnection(ConnectionDetails cxnDetails, int maxSessions, boolean verbose) throws Throwable {
        _cxnDetails = cxnDetails;
        _maxSessions = maxSessions;
        _verbose = verbose;

        _channels = Collections.synchronizedCollection(new HashSet<com.jcraft.jsch.Channel>());

        _jsch = new JSch();
        _jsch.setKnownHosts(new ByteArrayInputStream(new byte[8192]));
        String hostKey = _cxnDetails.hostKey();
        if (hostKey != null) {
            _jsch.getHostKeyRepository()
                    .add(new HostKey(_cxnDetails.host(), Base64.getDecoder().decode(hostKey.getBytes())), userInfo());
        }
        /*
         * added user's identity (private & public keys)
         */
        if (_cxnDetails.privateKey() != null) {
            _jsch.addIdentity(_cxnDetails.username() + "_identity", _cxnDetails.privateKey().getBytes(),
                    _cxnDetails.publicKey() != null ? _cxnDetails.publicKey().getBytes() : null,
                    _cxnDetails.passphrase() != null ? _cxnDetails.passphrase().getBytes() : null);
        }

        /*
         * connect
         */
        _jschSession = _jsch.getSession(_cxnDetails.username(), _cxnDetails.host(), _cxnDetails.port());
        _jschSession.setConfig("StrictHostKeyChecking", _cxnDetails.hostKey() != null ? "yes" : "no");
        _jschSession.setUserInfo(userInfo());
        if (_verbose) {
            System.out.print("opening connection to " + _cxnDetails.host() + ":" + _cxnDetails.port() + " ...");
        }
        _jschSession.connect();
        if (_verbose) {
            System.out.println("done");
        }
    }

    private com.jcraft.jsch.Channel openChannel(String type) throws Throwable {
        synchronized (_channels) {
            if (_maxSessions > 0) {
                while (_channels.size() >= _maxSessions) {
                    if (_verbose) {
                        System.out.println("Thread " + Thread.currentThread().getId() + ": reach max session limit "
                                + _maxSessions + ". Waiting...");
                    }
                    try {
                        _channels.wait();
                    } catch (Throwable e) {
                        throw new IOException(e);
                    }
                }
            }
            com.jcraft.jsch.Channel channel = _jschSession.openChannel(type);
            _channels.add(channel);
            return channel;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            clearChannels();
        } finally {
            if (_verbose) {
                System.out.print("closing connection to " + _cxnDetails.host() + ":" + _cxnDetails.port() + " ...");
            }
            _jschSession.disconnect();
            if (_verbose) {
                System.out.println("done");
            }
        }
    }

    @Override
    public ConnectionDetails connectionDetails() {
        return _cxnDetails;
    }

    @Override
    public int maxSessions() {
        return _maxSessions;
    }

    @Override
    public Session createSession() throws Throwable {
        com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) openChannel("exec");
        return new JschSession(this, channel);
    }

    @Override
    public JschSftpClient createSftpClient(String baseDir, int dirMode, int fileMode, String encoding)
            throws Throwable {
        com.jcraft.jsch.ChannelSftp channel = (com.jcraft.jsch.ChannelSftp) openChannel("sftp");
        return new JschSftpClient(this, channel, baseDir, dirMode, fileMode, encoding, verbose());
    }

    @Override
    public JschScpPutClient createScpPutClient(String baseDir, int dirMode, int fileMode, String encoding,
            boolean preserveModificationTime, boolean compress) throws Throwable {
        return new JschScpPutClient(this, (com.jcraft.jsch.ChannelExec) openChannel("exec"), baseDir, dirMode, fileMode,
                encoding, preserveModificationTime, compress, verbose());
    }

    private UserInfo userInfo() {
        return new UserInfo() {
            @Override
            public String getPassphrase() {
                return _cxnDetails.passphrase();
            }

            @Override
            public String getPassword() {
                return _cxnDetails.password();
            }

            @Override
            public boolean promptPassword(String message) {
                return _cxnDetails.password() != null;
            }

            @Override
            public boolean promptPassphrase(String message) {
                return _cxnDetails.passphrase() != null;
            }

            @Override
            public boolean promptYesNo(String message) {
                return false;
            }

            @Override
            public void showMessage(String message) {

            }
        };
    }

    @Override
    public boolean verbose() {
        return _verbose;
    }

    void removeChannel(com.jcraft.jsch.Channel channel) {
        synchronized (_channels) {
            _channels.remove(channel);
            _channels.notifyAll();
        }
    }

    private void clearChannels() {
        synchronized (_channels) {
            for (com.jcraft.jsch.Channel channel : _channels) {
                try {
                    if (!channel.isClosed()) {
                        channel.disconnect();
                    }
                } catch (Throwable e) {

                } finally {
                    _channels.remove(channel);
                    _channels.notifyAll();
                }
            }
        }
    }

    public static class Builder extends ConnectionBuilder {
        public Builder() {
            super(IMPL);
        }

        public Builder(String impl) {
            super(IMPL);
        }

        @Override
        public Builder setImplementation(String impl) {
            return this;
        }

        @Override
        public JschConnection build() throws Throwable {
            return (JschConnection) super.build();
        }
    }

    @Override
    public SftpClient createSftpClient(String baseDir, int dirMode, int fileMode) throws Throwable {
        return createSftpClient(baseDir, dirMode, fileMode, PutClient.DEFAULT_ENCODING);
    }

    @Override
    public SftpClient createSftpClient(String baseDir) throws Throwable {
        return createSftpClient(baseDir, PutClient.DEFAULT_DIRECTORY_MODE, PutClient.DEFAULT_FILE_MODE,
                PutClient.DEFAULT_ENCODING);
    }

    @Override
    public SftpClient createSftpClient() throws Throwable {
        return createSftpClient(PutClient.DEFAULT_BASE_DIRECTORY, PutClient.DEFAULT_DIRECTORY_MODE,
                PutClient.DEFAULT_FILE_MODE, PutClient.DEFAULT_ENCODING);
    }

    @Override
    public ScpPutClient createScpPutClient(String baseDir, int dirMode, int fileMode, String encoding)
            throws Throwable {
        return createScpPutClient(baseDir, dirMode, fileMode, encoding, false, false);
    }

    @Override
    public ScpPutClient createScpPutClient(String baseDir, int dirMode, int fileMode) throws Throwable {
        return createScpPutClient(baseDir, dirMode, fileMode, PutClient.DEFAULT_ENCODING, false, false);
    }

    @Override
    public ScpPutClient createScpPutClient(String baseDir) throws Throwable {
        return createScpPutClient(baseDir, PutClient.DEFAULT_DIRECTORY_MODE, PutClient.DEFAULT_FILE_MODE,
                PutClient.DEFAULT_ENCODING, false, false);
    }

    @Override
    public ScpPutClient createScpPutClient() throws Throwable {
        return createScpPutClient(PutClient.DEFAULT_BASE_DIRECTORY, PutClient.DEFAULT_DIRECTORY_MODE,
                PutClient.DEFAULT_FILE_MODE, PutClient.DEFAULT_ENCODING, false, false);
    }
}
