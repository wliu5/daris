package daris.client.archive;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class ArchiveCreatorSettings {

    private Set<File> _inputs;
    private File _baseDir;
    private File _outputArcFile;
    private int _clevel;
    private boolean _quiet;

    public ArchiveCreatorSettings() {
        _inputs = new LinkedHashSet<File>();
        _clevel = 6;
        _quiet = false;
    }

    public ArchiveCreatorSettings setBaseDir(File baseDir) {
        _baseDir = baseDir;
        return this;
    }

    public ArchiveCreatorSettings setOutputArchiveFile(File oaf) {
        _outputArcFile = oaf;
        return this;
    }

    public ArchiveCreatorSettings setCompressionLevel(int clevel) {
        if (clevel < 0) {
            _clevel = 0;
        } else if (clevel > 9) {
            _clevel = 9;
        } else {
            _clevel = clevel;
        }
        return this;
    }

    public ArchiveCreatorSettings setQuiet(boolean quiet) {
        _quiet = quiet;
        return this;
    }

    public Set<File> inputs() {
        return Collections.unmodifiableSet(_inputs);
    }

    public File baseDir() {
        return _baseDir;
    }

    public File outputArchiveFile() {
        return _outputArcFile;
    }

    public int compressionLevel() {
        return _clevel;
    }

    public boolean quiet() {
        return _quiet;
    }

    public ArchiveCreatorSettings addInput(File f) {
        _inputs.add(f);
        return this;
    }

    public void validate() throws IllegalArgumentException {
        if (_outputArcFile == null) {
            throw new IllegalArgumentException("Missing output file argument.");
        }
        if (_inputs.isEmpty()) {
            throw new IllegalArgumentException("Missing input file(s).");
        }
    }

}
