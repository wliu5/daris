package io.github.xtman.ssh.client.jsch;

import java.io.ByteArrayInputStream;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

import io.github.xtman.ssh.client.KeyTools;
import io.github.xtman.ssh.client.KeyType;

public class JschKeyTools extends io.github.xtman.ssh.client.KeyTools {

    @Override
    public byte[] getServerHostKey(KeyType type, String host, int port) throws Throwable {
        return KeyTools.getPublicKeyBytes(getServerHostKey(host, port, type));
    }

    @Override
    public String getServerHostKey(String host, int port, KeyType type) throws Throwable {

        JSch jsch = new JSch();

        // set the algorithm/key type
        JSch.setConfig("server_host_key", type.publicKeyTypeName());

        // in-memory buffer & stream to store the remote host key.
        byte[] knownHosts = new byte[8192];
        ByteArrayInputStream in = new ByteArrayInputStream(knownHosts);

        // set the in-memory stream as key repository
        jsch.setKnownHosts(in);

        // create a session where user name is nobody, it can be anything except
        // null. Because it will fail to authenticate anyway. The remote host
        // key will be added to repository before the authentication.

        com.jcraft.jsch.Session session = jsch.getSession("nobody", host, port);
        // accept unknown key
        session.setConfig("StrictHostKeyChecking", "no");

        try {
            // connect to remote host. It will do host key exchange first then
            // authentication.
            session.connect();
        } catch (JSchException e) {
            // authentication fails but it is ok.
        } finally {
            if (session.isConnected()) {
                // by any chance it is connected, disconnect it.
                session.disconnect();
            }
        }
        return session.getHostKey().getKey();
    }

}
