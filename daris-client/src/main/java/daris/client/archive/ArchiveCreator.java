package daris.client.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;

import arc.archive.ArchiveOutput;
import arc.archive.ArchiveRegistry;
import arc.mf.client.archive.Archive;

public class ArchiveCreator implements Runnable {

    private Collection<File> _inputs;
    private File _baseDir;
    private File _outputArcFile;
    private int _clevel;
    private boolean _quiet;

    public ArchiveCreator(Collection<File> inputs, File baseDir, File outputArcFile, int clevel, boolean quiet) {
        _inputs = inputs;
        _baseDir = baseDir;
        _outputArcFile = outputArcFile;
        _clevel = clevel;
        _quiet = quiet;
    }

    public ArchiveCreator(ArchiveCreatorSettings settings) {
        this(settings.inputs(), settings.baseDir(), settings.outputArchiveFile(), settings.compressionLevel(),
                settings.quiet());
    }

    @Override
    public void run() {
        try {
            Archive.declareSupportForAllTypes();
            ArchiveOutput ao = ArchiveRegistry.createOutput(_outputArcFile, ArchiveType.getMimeType(_outputArcFile),
                    _clevel, null);
            try {
                for (File inputFile : _inputs) {
                    add(ao, inputFile);
                }
            } finally {
                ao.close();
            }
        } catch (Throwable e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            e.printStackTrace(System.err);
        }
    }

    private void add(ArchiveOutput ao, File inputFile) throws Throwable {

        String ename = inputFile.getAbsolutePath();
        String baseDirPath = _baseDir.getAbsolutePath();
        if (ename.startsWith(baseDirPath + File.separator) || ename.startsWith(baseDirPath + "/")) {
            ename = ename.substring(baseDirPath.length());
        }
        if (ename.startsWith(File.separator) || ename.startsWith("/")) {
            ename = ename.substring(1);
        }
        if (inputFile.isDirectory()) {
            Files.walkFileTree(inputFile.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        add(ao, file.toFile());
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
                public FileVisitResult visitFileFailed(Path file, IOException ioe) throws IOException {
                    ioe.printStackTrace(System.err);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            if (!_quiet) {
                System.out.print("Adding '" + ename + "' ...");
            }
            ao.add(null, ename, inputFile);
            if (!_quiet) {
                System.out.println("done");
            }
        }
    }

}
