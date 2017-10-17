package io.github.xtman.ssh.client;

import java.util.Base64;

import ch.ethz.ssh2.ConnectionInfo;

public class SshUtils {

    public static String getHostKey(String host, int port, String algorithm) throws Throwable {
        ch.ethz.ssh2.Connection conn = new ch.ethz.ssh2.Connection(host, port);
        conn.setServerHostKeyAlgorithms(new String[] { algorithm });
        String hostKey = null;
        try {
            ConnectionInfo info = conn.connect();
            hostKey = new String(Base64.getEncoder().encode(info.serverHostKey), "UTF-8");
        } finally {
            conn.close();
        }
        return hostKey;
    }

    public static String getRSAHostKey(String host, int port) throws Throwable {
        return getHostKey(host, port, "ssh-rsa");
    }

}
