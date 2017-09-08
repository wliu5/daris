package daris.client.archive;

import java.io.File;

public class ArchiveExtractorSettings {
    private File _arcFile;
    private File _outputDir;
    private boolean _overwrite = false;
    private boolean _quiet = false;

    public ArchiveExtractorSettings() {

    }

    public ArchiveExtractorSettings setInputArchiveFile(File iaf) {
        _arcFile = iaf;
        return this;
    }

    public ArchiveExtractorSettings setOutputDirectory(File outputDir) {
        _outputDir = outputDir;
        return this;
    }

    public ArchiveExtractorSettings setOverwrite(boolean overwrite) {
        _overwrite = overwrite;
        return this;
    }

    public ArchiveExtractorSettings setQuiet(boolean quiet) {
        _quiet = quiet;
        return this;
    }

    public File inputArchiveFile() {
        return _arcFile;
    }

    public File outputDirectory() {
        return _outputDir;
    }

    public boolean overwrite() {
        return _overwrite;
    }

    public boolean quiet() {
        return _quiet;
    }

    public void validate() throws IllegalArgumentException {
        if (_arcFile == null) {
            throw new IllegalArgumentException("Missing input archive file.");
        }
    }

}
