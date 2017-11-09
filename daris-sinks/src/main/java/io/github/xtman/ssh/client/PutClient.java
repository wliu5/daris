package io.github.xtman.ssh.client;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public interface PutClient extends TransferClient {

    public static final String DEFAULT_BASE_DIRECTORY = ".";

    public static final int DEFAULT_DIRECTORY_MODE = 0755;

    public static final int DEFAULT_FILE_MODE = 0644;

    public static final String DEFAULT_ENCODING = "UTF-8";

    String encoding();

    void setEncoding(String encoding);

    String baseDirectory();

    void setBaseDirectory(String baseDir);

    int fileMode();

    void setFileMode(int fileMode);

    String fileModeString();

    int directoryMode();

    void setDirectoryMode(int dirMode);

    String directoryModeString();

    void put(InputStream in, long length, String dstPath) throws Throwable;

    void put(File f, String dstPath) throws Throwable;

    void put(Path f, String dstPath) throws Throwable;

    void putDirectory(File dir) throws Throwable;

    void putDirectory(File dir, boolean self) throws Throwable;

    void putDirectory(Path dir) throws Throwable;
    
    void putDirectory(Path dir, boolean self) throws Throwable;

    void mkdirs(String dstPath) throws Throwable;

}
