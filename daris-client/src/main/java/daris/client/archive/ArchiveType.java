package daris.client.archive;

import java.io.File;

public enum ArchiveType {
    ZIP("application/zip"), AAR("application/arc-archive"), JAR("application/java-archive");

    private String _mimeType;

    ArchiveType(String mimeType) {
        _mimeType = mimeType;
    }

    public String mimeType() {
        return _mimeType;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    public String fileExtension() {
        return name().toLowerCase();
    }

    public static ArchiveType fromExtension(String ext) {
        ArchiveType[] vs = values();
        for (ArchiveType v : vs) {
            if (v.fileExtension().equalsIgnoreCase(ext)) {
                return v;
            }
        }
        return null;
    }

    public static ArchiveType fromMimeType(String mimeType) {
        ArchiveType[] vs = values();
        for (ArchiveType v : vs) {
            if (v.mimeType().equals(mimeType)) {
                return v;
            }
        }
        return null;
    }

    public static ArchiveType getArchiveType(String fileName) {
        if (fileName != null) {
            int idx = fileName.lastIndexOf('.');
            if (idx != -1) {
                String ext = fileName.substring(idx + 1);
                return fromExtension(ext);
            }
        }
        return null;
    }

    public static ArchiveType getArchiveType(File file) {
        return getArchiveType(file.getName());
    }

    public static String getMimeType(String fileName) {
        ArchiveType at = getArchiveType(fileName);
        if (at != null) {
            return at.mimeType();
        }
        return null;
    }

    public static String getMimeType(File file) {
        if (file != null) {
            return getMimeType(file.getName());
        }
        return null;
    }

    public static boolean isArchiveFile(File f) {
        return getArchiveType(f) != null;
    }
}
