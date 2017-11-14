package io.github.xtman.ssh.client.jsch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.jcraft.jsch.SftpException;

import io.github.xtman.ssh.client.Connection;
import io.github.xtman.util.PathUtils;

public class JschSftpClient implements io.github.xtman.ssh.client.SftpClient {
    private JschConnection _connection;
    private com.jcraft.jsch.ChannelSftp _channel;

    private String _baseDir;
    private int _dirMode;
    private int _fileMode;
    private String _encoding;
    private boolean _verbose;

    JschSftpClient(JschConnection connection, com.jcraft.jsch.ChannelSftp channel, String baseDir, int dirMode,
            int fileMode, String encoding, boolean verbose) throws Throwable {
        _connection = connection;
        _channel = channel;

        setBaseDirectory(baseDir);
        _dirMode = dirMode;
        _fileMode = fileMode;
        _encoding = encoding;
        _verbose = verbose;

        _channel.connect();
        _channel.setFilenameEncoding(_encoding);
    }

    @Override
    public String encoding() {
        return _encoding;
    }

    @Override
    public void setEncoding(String encoding) {
        try {
            _channel.setFilenameEncoding(encoding);
            _encoding = encoding;
        } catch (SftpException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String baseDirectory() {
        return _baseDir;
    }

    @Override
    public void setBaseDirectory(String baseDir) {
        if (baseDir == null || baseDir.trim().isEmpty()) {
            _baseDir = DEFAULT_BASE_DIRECTORY;
        } else {
            baseDir = baseDir.trim();
            if (baseDir.startsWith("/") || baseDir.startsWith(".")) {
                _baseDir = baseDir;
            } else {
                _baseDir = PathUtils.join(".", baseDir);
            }
        }
    }

    @Override
    public int fileMode() {
        return _fileMode;
    }

    @Override
    public void setFileMode(int fileMode) {
        _fileMode = fileMode;
    }

    @Override
    public String fileModeString() {
        return String.format("%04o", _fileMode);
    }

    @Override
    public int directoryMode() {
        return _dirMode;
    }

    @Override
    public void setDirectoryMode(int dirMode) {
        _dirMode = dirMode;
    }

    @Override
    public String directoryModeString() {
        return String.format("%04o", _dirMode);
    }

    @Override
    public void mkdirs(String dstPath) throws Throwable {
        mkdirs(_channel, PathUtils.join(_baseDir, dstPath));
    }

    @Override
    public Connection connection() {
        return _connection;
    }

    @Override
    public boolean verbose() {
        return _verbose;
    }

    @Override
    public void close() throws IOException {
        try {
            _channel.disconnect();
        } finally {
            _connection.removeChannel(_channel);
        }
    }

    static boolean directoryExists(com.jcraft.jsch.ChannelSftp channel, String dstDirPath) throws Throwable {
        try {
            channel.stat(dstDirPath);
            return true;
        } catch (SftpException e) {
            if (e.id == com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            } else {
                throw e;
            }
        }
    }

    static void mkdirs(com.jcraft.jsch.ChannelSftp channel, String dstDirPath) throws Throwable {
        if (directoryExists(channel, dstDirPath)) {
            return;
        }
        String parentDirPath = PathUtils.getParent(dstDirPath);
        if (parentDirPath != null && !parentDirPath.equals("/") && !parentDirPath.equals(".")
                && !directoryExists(channel, parentDirPath)) {
            mkdirs(channel, parentDirPath);
        }
        channel.mkdir(dstDirPath);
    }

    @Override
    public void put(InputStream in, long length, String dstPath) throws Throwable {
        String path = PathUtils.join(_baseDir, dstPath);
        String dirPath = PathUtils.getParent(path);
        if (dirPath != null && !directoryExists(_channel, dirPath)) {
            mkdirs(_channel, dirPath);
        }
        _channel.put(in, path, null, com.jcraft.jsch.ChannelSftp.OVERWRITE);
    }

    @Override
    public void put(File f, String dstPath) throws Throwable {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        try {
            put(in, f.length(), dstPath);
        } finally {
            in.close();
        }
    }

    @Override
    public void put(Path f, String dstPath) throws Throwable {
        put(f.toFile(), dstPath);
    }

    @Override
    public void putDirectory(File dir) throws Throwable {
        putDirectory(dir, true);
    }

    @Override
    public void putDirectory(File dir, boolean self) throws Throwable {
        putDirectory(dir.toPath(), self);
    }

    @Override
    public void putDirectory(final Path dir) throws Throwable {
        putDirectory(dir, true);
    }

    @Override
    public void putDirectory(final Path dir, final boolean self) throws Throwable {
        final String dirName = dir.getFileName().toString();
        if (self) {
            mkdirs(dirName);
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                String dstPath = PathUtils.getRelativePath(file, dir);
                if (self) {
                    dstPath = PathUtils.join(dirName, dstPath);
                }
                try {
                    put(file, dstPath);
                } catch (Throwable e) {
                    if (e instanceof IOException) {
                        throw (IOException) e;
                    } else {
                        throw new IOException(e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException ioe) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException ioe) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
