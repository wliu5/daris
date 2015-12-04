package daris.client.settings;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import arc.xml.XmlDoc;
import arc.xml.XmlWriter;
import daris.client.model.task.DownloadCollisionPolicy;
import daris.client.model.task.DownloadOptions;
import daris.client.model.task.DownloadOptions.Parts;
import daris.client.model.transcode.Transcode;

public class DownloadSettings {

    private static final String _defaultDirectory = System
            .getProperty("user.home") + File.separator + "Downloads"
            + File.separator + "DaRIS";

    public static String getDefaultDirectory() {
        File dir = new File(_defaultDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    private boolean _alwaysAsk = true;
    private boolean _recursive = true;
    private boolean _decompress = false;
    private Parts _parts = Parts.all;
    private Map<String, Transcode> _transcodes = null;
    private DownloadCollisionPolicy _collisionPolicy;
    private String _directory = getDefaultDirectory();

    public DownloadSettings(XmlDoc.Element de) throws Throwable {
        _alwaysAsk = de.booleanValue("alwasy-ask", true);
        _recursive = de.booleanValue("recursive", true);
        _decompress = de.booleanValue("decompress", false);
        _parts = Parts.fromString(de.value("parts"), Parts.all);
        _collisionPolicy = DownloadCollisionPolicy.fromString(
                de.value("collision-policy"),
                DownloadCollisionPolicy.OVERWRITE);
        _directory = de.stringValue("directory", getDefaultDirectory());
        if (!(new File(_directory).exists())) {
            _directory = getDefaultDirectory();
        }
    }

    public DownloadSettings() {
        _alwaysAsk = true;
        _recursive = true;
        _decompress = false;
        _collisionPolicy = DownloadCollisionPolicy.OVERWRITE;
        _directory = getDefaultDirectory();
    }

    public boolean alwaysAsk() {
        return _alwaysAsk;
    }

    public boolean recursive() {
        return _recursive;
    }

    public boolean decompress() {
        return _decompress;
    }

    public Parts parts() {
        return _parts;
    }

    public DownloadCollisionPolicy collisionPolicy() {
        return _collisionPolicy;
    }

    public String directory() {
        return _directory;
    }

    public Set<Transcode> transcodes() {
        if (_transcodes == null || _transcodes.isEmpty()) {
            return null;
        } else {
            return new HashSet<Transcode>(_transcodes.values());
        }
    }

    public void addTranscode(Transcode transcode) {
        if (_transcodes == null) {
            _transcodes = new HashMap<String, Transcode>();
        }
        _transcodes.put(transcode.fromMimeType, transcode);
    }

    public boolean hasTranscodes() {
        return _transcodes != null && !_transcodes.isEmpty();
    }

    public Transcode transcodeFor(String fromMimeType) {
        if (hasTranscodes()) {
            return _transcodes.get(fromMimeType);
        } else {
            return null;
        }
    }

    public boolean hasTranscodeFor(String fromMimeType) {
        return hasTranscodes() && _transcodes.containsKey(fromMimeType);
    }

    public void save(XmlWriter w) throws Throwable {
        w.push("download");
        w.add("always-ask", _alwaysAsk);
        w.add("recursive", _recursive);
        w.add("decompress", _decompress);
        w.add("parts", _parts);
        w.add("collision-policy", _collisionPolicy);
        w.add("directory", _directory);
        w.pop();
    }

    public void set(DownloadOptions downloadOptions) {
        _recursive = downloadOptions.recursive();
        _decompress = downloadOptions.decompress();
        if (downloadOptions.hasTranscodes()) {
            Set<Transcode> transcodes = downloadOptions.transcodes();
            for (Transcode transcode : transcodes) {
                addTranscode(transcode);
            }
        }
        _collisionPolicy = downloadOptions.collisionPolicy();
        _directory = downloadOptions.directory();
    }

}
