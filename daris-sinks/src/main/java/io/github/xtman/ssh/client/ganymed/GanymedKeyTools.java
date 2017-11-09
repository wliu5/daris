package io.github.xtman.ssh.client.ganymed;

import java.util.Base64;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.ConnectionInfo;
import io.github.xtman.ssh.client.KeyType;

public class GanymedKeyTools extends io.github.xtman.ssh.client.KeyTools {

    @Override
    public byte[] getServerHostKey(KeyType type, String host, int port) throws Throwable {
        Connection cxn = new ch.ethz.ssh2.Connection(host, port);
        cxn.setServerHostKeyAlgorithms(new String[] { type.publicKeyTypeName() });
        try {
            ConnectionInfo cxnInfo = cxn.connect(null, GanymedConnection.CONNECT_TIMEOUT_MILLISECS,
                    GanymedConnection.KEY_EXCHANGE_TIMEOUT_MILLISECS);
            return cxnInfo.serverHostKey;
        } finally {
            cxn.close();
        }
    }

    @Override
    public String getServerHostKey(String host, int port, KeyType type) throws Throwable {
        byte[] b = getServerHostKey(type, host, port);
        if (b != null) {
            return new String(Base64.getEncoder().encode(b), "UTF-8");
        }
        return null;
    }

}
