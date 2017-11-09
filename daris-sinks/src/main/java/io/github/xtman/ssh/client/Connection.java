package io.github.xtman.ssh.client;

import java.io.Closeable;

public interface Connection extends Closeable {

    public static final int DEFAULT_MAX_SESSIONS = 0;

    ConnectionDetails connectionDetails();

    int maxSessions();

    boolean verbose();

    Session createSession() throws Throwable;

    SftpClient createSftpClient(String baseDir, int dirMode, int fileMode, String encoding) throws Throwable;

    SftpClient createSftpClient(String baseDir, int dirMode, int fileMode) throws Throwable;

    SftpClient createSftpClient(String baseDir) throws Throwable;

    SftpClient createSftpClient() throws Throwable;

    ScpPutClient createScpPutClient(String baseDir, int dirMode, int fileMode, String encoding,
            boolean preserveModificationTime, boolean compress) throws Throwable;

    ScpPutClient createScpPutClient(String baseDir, int dirMode, int fileMode, String encoding) throws Throwable;

    ScpPutClient createScpPutClient(String baseDir, int dirMode, int fileMode) throws Throwable;

    ScpPutClient createScpPutClient(String baseDir) throws Throwable;

    ScpPutClient createScpPutClient() throws Throwable;

}
