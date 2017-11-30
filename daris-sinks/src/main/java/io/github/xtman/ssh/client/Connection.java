package io.github.xtman.ssh.client;

import java.io.Closeable;

public interface Connection extends Closeable {

    public static final int DEFAULT_MAX_CHANNELS = 0;

    public static final String DEFAULT_REMOTE_BASE_DIRECTORY = ".";

    public static final String DEFAULT_ENCODING = "UTF-8";

    ConnectionDetails connectionDetails();

    int maxChannels();

    boolean verbose();

    Executor createExecutor(String remoteBaseDir, String encoding) throws Throwable;

    ScpClient createScpClient(String remoteBaseDir, String encoding, Integer dirMode, Integer fileMode,
            boolean compress, boolean preserve) throws Throwable;

    SftpClient createSftpClient(String remoteBaseDir, String encoding, Integer dirMode, Integer fileMode,
            boolean compress, boolean preserve) throws Throwable;

    Executor createExecutor() throws Throwable;

    ScpClient createScpClient() throws Throwable;

    SftpClient createSftpClient() throws Throwable;

}
