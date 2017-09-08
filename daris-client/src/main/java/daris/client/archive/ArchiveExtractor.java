package daris.client.archive;

import java.io.File;

import arc.archive.ArchiveInput;
import arc.archive.ArchiveRegistry;
import arc.mf.client.archive.Archive;
import arc.mime.NamedMimeType;
import arc.streams.StreamCopy;

public class ArchiveExtractor implements Runnable {

    private File _arcFile;
    private File _outputDir;
    private boolean _overwrite;
    private boolean _quiet;

    public ArchiveExtractor(File arcFile, File outputDir, boolean overwrite, boolean quiet) {
        _arcFile = arcFile;
        _outputDir = outputDir;
        _overwrite = overwrite;
        _quiet = quiet;
    }

    public ArchiveExtractor(File arcFile, File outputDir) {
        this(arcFile, outputDir, false, false);
    }

    public ArchiveExtractor(ArchiveExtractorSettings settings) {
        this(settings.inputArchiveFile(), settings.outputDirectory(), settings.overwrite(), settings.quiet());
    }

    @Override
    public void run() {
        try {
            Archive.declareSupportForAllTypes();
            ArchiveInput ai = ArchiveRegistry.createInput(_arcFile,
                    new NamedMimeType(ArchiveType.getMimeType(_arcFile)));
            try {
                ArchiveInput.Entry entry = null;
                while ((entry = ai.next()) != null) {
                    String ename = entry.name();
                    if (ename.startsWith("/")) {
                        ename = ename.substring(1);
                    }
                    File of = _outputDir == null ? new File(ename) : new File(_outputDir, ename);
                    if (entry.isDirectory()) {
                        of.mkdirs();
                    } else {
                        of.getParentFile().mkdirs();
                        boolean exists = of.exists();
                        if (!_quiet) {
                            System.out.print("Extracting to '" + of.getPath() + "' ...");
                        }
                        if (_overwrite || !exists) {
                            of.createNewFile();
                            StreamCopy.copy(entry.stream(), of);
                            if (!_quiet) {
                                System.out.println("done.");
                            }
                        } else {
                            if (!_quiet) {
                                System.out.println("exists. Skipped.");
                            }
                        }
                    }
                    ai.closeEntry();
                }
            } finally {
                ai.close();
            }
        } catch (Throwable e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            e.printStackTrace(System.err);
        }
    }

}
