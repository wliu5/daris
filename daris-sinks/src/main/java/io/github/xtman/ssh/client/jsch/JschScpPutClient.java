package io.github.xtman.ssh.client.jsch;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import com.jcraft.jsch.ChannelExec;

import io.github.xtman.io.util.StreamUtils;
import io.github.xtman.ssh.client.Connection;
import io.github.xtman.util.PathUtils;

public class JschScpPutClient implements io.github.xtman.ssh.client.ScpPutClient {

    private JschConnection _connection;
    private ChannelExec _channel;

    private String _baseDir;
    private int _dirMode;
    private int _fileMode;
    private String _encoding;
    private boolean _preserveModificationTime;
    private boolean _compress;
    private boolean _verbose;

    private boolean _initialized = false;

    private InputStream _cin;
    private OutputStream _cout;

    JschScpPutClient(JschConnection connection, ChannelExec channel, String baseDir, int dirMode, int fileMode,
            String encoding, boolean preserveModificationTime, boolean compress, boolean verbose) {
        _connection = connection;
        _channel = channel;

        setBaseDirectory(baseDir);
        _dirMode = dirMode;
        _fileMode = fileMode;
        _encoding = encoding;
        _preserveModificationTime = preserveModificationTime;
        _compress = compress;
        _verbose = verbose;

        _initialized = false;
    }

    @Override
    public String encoding() {
        return _encoding;
    }

    @Override
    public void setEncoding(String encoding) {
        _encoding = encoding;
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

    public void put(InputStream in, long length, long lastModified, String dstPath) throws Throwable {

        /*
         * make sure it is initialized
         */
        initialize();

        /*
         * enter directory
         */
        dstPath = PathUtils.trimSlash(dstPath);
        String[] components = dstPath.split("/");
        if (components.length > 1) {
            for (int i = 0; i < components.length - 1; i++) {
                String p = components[i];
                pushDir(p);
            }
        }
        /*
         * send file
         */
        if (preserveModificationTime() && lastModified > 0) {
            // send mtime
            long mtime = lastModified / 1000;
            String cmd = String.format("T%d 0 %d 0\n", mtime, mtime);

            if (_verbose) {
                System.out.print("sending command: '" + cmd.substring(0, cmd.length() - 1) + "\\n' ... ");
            }
            _cout.write(cmd.getBytes(encoding()));
            _cout.flush();
            checkResponse(_cin);
            if (_verbose) {
                System.out.println("done");
            }
        }
        // send file mode, length and name
        String fileName = PathUtils.getLastComponent(dstPath);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("C%04o %d %s\n", fileMode(), length, fileName));
        String cmd = sb.toString();

        if (_verbose) {
            System.out.print("sending command: '" + cmd.substring(0, cmd.length() - 1) + "\\n' ... ");
        }
        _cout.write(cmd.getBytes(encoding()));
        _cout.flush();
        checkResponse(_cin);
        if (_verbose) {
            System.out.println("done");
        }

        if (_verbose) {
            System.out.print("sending file content ... ");
        }
        // send file content
        StreamUtils.copy(in, length, _cout);
        // _cout.flush();
        _cout.write(new byte[] { 0 });
        _cout.flush();
        checkResponse(_cin);
        if (_verbose) {
            System.out.println("done");
        }

        /*
         * back to base dir
         */
        if (components.length > 1) {
            for (int i = 0; i < components.length - 1; i++) {
                popDir();
            }
        }
    }

    @Override
    public void mkdirs(String dstPath) throws Throwable {
        initialize();
        if (dstPath != null && !dstPath.trim().isEmpty()) {
            dstPath = PathUtils.normalise(dstPath);
            dstPath = PathUtils.trimLeadingSlash(dstPath);
            String[] cs = dstPath.split("/");
            for (int i = 0; i < cs.length; i++) {
                pushDir(cs[i]);
            }
            for (int i = 0; i < cs.length; i++) {
                popDir();
            }
        }
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
            try {
                if (_cout != null) {
                    if (_verbose) {
                        System.out.print("closing output stream... ");
                    }
                    _cout.close();
                    if (_verbose) {
                        System.out.println("done");
                    }
                }
            } finally {
                if (_verbose) {
                    System.out.print("closing exec channel ... ");
                }
                _channel.disconnect();
                if (_verbose) {
                    System.out.println("done");
                }
            }
        } finally {
            _connection.removeChannel(_channel);
        }
    }

    @Override
    public boolean compress() {
        return _compress;
    }

    @Override
    public boolean preserveModificationTime() {
        return _preserveModificationTime;
    }

    @Override
    public void put(InputStream in, long length, String dstPath) throws Throwable {
        put(in, length, -1, dstPath);
    }

    @Override
    public void put(File f, String dstPath) throws Throwable {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        try {
            put(in, f.length(), f.lastModified(), dstPath);
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
    public void putDirectory(final Path dir, boolean self) throws Throwable {

        initialize();

        if (self) {
            pushDir(dir.getFileName().toString());
        }

        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                String dstPath = PathUtils.getRelativePath(file, dir);
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

        if (self) {
            popDir();
        }
    }

    private void initialize() throws Throwable {

        if (_initialized) {
            return;
        }

        // @formatter:off
        // -r    recursive 
        // -d    target should be a directory
        // -t    ???
        // -p    preserve mtime
        // -C    compress
        // @formatter:on

        StringBuilder sb = new StringBuilder("scp -r -d -t ");
        if (preserveModificationTime()) {
            sb.append("-p ");
        }
        if (compress()) {
            sb.append("-C ");
        }
        sb.append(baseDirectory()).append("\n");
        String cmd = sb.toString();

        _channel.setCommand(cmd);

        _cin = new BufferedInputStream(_channel.getInputStream(), 8192);
        _cout = new BufferedOutputStream(_channel.getOutputStream());

        /*
         * go to base dir
         */
        if (_verbose) {
            System.out.print(
                    "open exec channel by sending command: '" + cmd.substring(0, cmd.length() - 1) + "\\n' ... ");
        }
        _channel.connect();
        checkResponse(_cin);
        if (_verbose) {
            System.out.println("done");
        }

        _initialized = true;
    }

    /**
     * Enter the specified sub-directory.
     * 
     * @param dirName
     *            The name of the sub-directory.
     * @throws IOException
     */
    private void pushDir(String dirName) throws IOException {
        String cmd = String.format("D%04o 0 %s\n", directoryMode(), dirName);
        if (_verbose) {
            System.out.print("sending command: '" + cmd.substring(0, cmd.length() - 1) + "\\n' ... ");
        }
        _cout.write(cmd.getBytes(encoding()));
        _cout.flush();
        checkResponse(_cin);
        if (_verbose) {
            System.out.println("done");
        }
    }

    /**
     * Go back to parent directory. (cd ..)
     * 
     * @throws IOException
     */
    private void popDir() throws IOException {
        String cmd = "E\n";
        if (_verbose) {
            System.out.print("sending command: '" + cmd.substring(0, cmd.length() - 1) + "\\n' ... ");
        }
        _cout.write(cmd.getBytes(encoding()));
        _cout.flush();
        checkResponse(_cin);
        if (_verbose) {
            System.out.println("done");
        }
    }

    private static void checkResponse(InputStream in) throws IOException {
        final int b = in.read();
        // b may be
        // 0 for success,
        // 1 for error,
        // 2 for fatal error,

        if (b == -1) {
            // didn't receive any response
            throw new IOException("No response from remote server.");
        } else if (b != 0) {
            StringBuffer sb = new StringBuffer();
            int c = in.read();
            while (c > 0 && c != '\n') {
                sb.append((char) c);
                c = in.read();
            }
            if (b == 1) {
                throw new IOException("Remote server responded error(" + b + "): " + sb.toString());
            } else if (b == 2) {
                throw new IOException("Remote server responded fatal error(" + b + "): " + sb.toString());
            } else {
                throw new IOException("Remote server responded unknown error(" + b + "): " + sb.toString());
            }
        }
    }

}
