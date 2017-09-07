package daris.client.download;

import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;

import arc.archive.ArchiveExtractor;
import arc.archive.ArchiveInput;
import arc.archive.ArchiveRegistry;
import arc.mf.client.ServerClient;
import arc.mf.client.archive.Archive;
import arc.mime.NamedMimeType;
import arc.streams.LongInputStream;
import arc.streams.StreamCopy;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlStringWriter;
import daris.client.download.DataDownloadOptions.Parts;
import daris.client.pssd.CiteableIdUtils;
import daris.client.session.MFSession;
import daris.client.task.AbstractTask;
import daris.client.task.Loggable;
import daris.client.util.XmlUtils;

public class AssetDownloadTask extends AbstractTask {

    private String _assetId;
    private DataDownloadOptions _options;

    public AssetDownloadTask(String assetId, DataDownloadOptions options, MFSession session, Logger logger) {
        super(session, logger);
        _assetId = assetId;
        _options = options;
    }

    @Override
    public void execute(MFSession session) throws Throwable {
        downloadAsset(session, this, _assetId, _options);
    }

    @Override
    public String type() {
        return "asset-download";
    }

    private static void downloadAsset(MFSession session, Loggable log, String assetId, DataDownloadOptions options)
            throws Throwable {
        XmlStringWriter w = new XmlStringWriter();
        w.add("id", assetId);
        w.add("lock", true);
        XmlDoc.Element ae = session.execute("asset.get", w.document(), null, null).element("asset");
        try {
            downloadAsset(session, log, ae, options);
        } finally {
            w = new XmlStringWriter();
            w.add("id", assetId);
            session.execute("asset.unlock", w.document(), null, null);
        }
    }

    private static void downloadAsset(MFSession session, Loggable log, XmlDoc.Element ae, DataDownloadOptions options)
            throws Throwable {
        File objectDir = createObjectDirectory(options.outputDir(), ae);
        if (options.includeAttachments()) {
            Collection<String> attachments = ae.values("related[@type='attachment']/to");
            if (attachments != null && !attachments.isEmpty()) {
                for (String attachment : attachments) {
                    downloadAttachment(session, log, ae, attachment, objectDir, options);
                }
            }
        }
        if (options.parts() == Parts.META || options.parts() == Parts.ALL) {
            downloadMeta(session, log, ae, objectDir, options);
        }
        if (options.parts() == Parts.CONTENT || options.parts() == Parts.ALL) {
            String mimeType = ae.value("type");
            if (ae.elementExists("content")) {
                if (options.hasTranscode(mimeType)) {
                    transcodeContent(session, log, ae, objectDir, options);
                } else {
                    downloadContent(session, log, ae, objectDir, options);
                }
            }
        }
    }

    private static String titleFor(XmlDoc.Element ae) throws Throwable {
        String cid = ae.value("cid");
        String assetId = ae.value("@id");
        if (cid != null) {
            String objectType = ae.value("meta/daris:pssd-object/type");
            if (objectType != null) {
                return objectType + " " + cid;
            }
        }
        return "asset " + assetId;
    }

    private static File createObjectDirectory(File outputDir, XmlDoc.Element ae) throws Throwable {
        StringBuilder sb = new StringBuilder();
        String cid = ae.value("cid");
        String assetId = ae.value("@id");
        if (cid == null) {
            sb.append(File.separator).append(assetId).toString();
        } else {
            String projectCID = CiteableIdUtils.getProjectCID(cid);
            sb.append(File.separator);
            sb.append(projectCID);
            if (!CiteableIdUtils.isProjectCID(cid)) {
                String subjectCID = CiteableIdUtils.getSubjectCID(cid);
                sb.append(File.separator);
                sb.append(subjectCID);
                if (!(CiteableIdUtils.isSubjectCID(cid) || CiteableIdUtils.isExMethodCID(cid))) {

                    String studyCID = CiteableIdUtils.getStudyCID(cid);
                    sb.append(File.separator);
                    sb.append(studyCID);
                    if (!CiteableIdUtils.isStudyCID(cid)) {
                        sb.append(File.separator);
                        sb.append(cid);
                    }
                }
            }
        }
        File objectDir = new File(outputDir, sb.toString());
        if (!objectDir.exists()) {
            objectDir.mkdirs();
        }
        return objectDir;
    }

    private static File createMetaFile(File objectDir, XmlDoc.Element ae) throws Throwable {
        String cid = ae.value("cid");
        String assetId = ae.value("@id");
        String fileName = new StringBuilder(cid == null ? assetId : cid).append(".meta.xml").toString();
        File file = new File(objectDir, fileName);
        return file;
    }

    private static File createAttachmentDirectory(File objectDir, XmlDoc.Element aae) throws Throwable {
        String attachmentDirName = "attachment-" + aae.value("@id");
        File attachmentDir = new File(objectDir, attachmentDirName);
        if (!attachmentDir.exists()) {
            attachmentDir.mkdirs();
        }
        return attachmentDir;
    }

    private static File createContentDirectory(File objectDir, XmlDoc.Element ae) throws Throwable {
        String type = ae.value("type");
        if (type != null) {
            File contentDir = new File(objectDir, type.replace('/', '_'));
            if (!contentDir.exists()) {
                contentDir.mkdirs();
            }
            return contentDir;
        } else {
            return objectDir;
        }
    }

    private static File createContentFile(File contentDir, XmlDoc.Element ae) throws Throwable {
        String cid = ae.value("cid");
        String assetId = ae.value("@id");
        String ext = ae.value("content/type/@ext");
        String contentFileName = ae.value("meta/daris:pssd-filename/original");
        if (contentFileName == null) {
            contentFileName = cid == null ? assetId : cid;
            if (ext != null) {
                contentFileName = contentFileName + "." + ext;
            }
        }
        File contentFile = new File(contentDir, contentFileName);
        return contentFile;
    }

    private static File createTranscodedDirectory(File objectDir, String type) throws Throwable {
        File transcodedDir = new File(objectDir, type.replace('/', '_'));
        transcodedDir.mkdirs();
        return transcodedDir;
    }

    private static File createTranscodedFile(File transcodedDir, XmlDoc.Element ae, String toType, String toExt)
            throws Throwable {
        String cid = ae.value("cid");
        String assetId = ae.value("@id");
        String ext = ae.value("content/type/@ext");
        String fileName = ae.value("meta/daris:pssd-filename/original");

        if (fileName == null) {
            fileName = cid == null ? assetId : cid;
        }
        if (ext != null && fileName.endsWith("." + ext)) {
            fileName = fileName.substring(0, fileName.length() - ext.length() - 1);
        }
        if (!fileName.endsWith("." + toExt)) {
            fileName += "." + toExt;
        }

        File file = new File(transcodedDir, fileName);
        return file;
    }

    private static void downloadMeta(MFSession session, Loggable log, XmlDoc.Element ae, File objectDir,
            DataDownloadOptions options) throws Throwable {
        File file = createMetaFile(objectDir, ae);
        String cid = ae.value("cid");
        String assetId = ae.value("@id");
        XmlDoc.Element e;
        if (cid == null) {
            e = session.execute("asset.get", "<id>" + assetId + "</id>", null, null).element("asset");
        } else {
            e = session.execute("om.pssd.object.describe", "<id>" + cid + "</id>", null, null).element("object");
        }
        log.logInfo("writing file: " + file.getAbsolutePath());
        XmlUtils.saveToFile(e, file);
    }

    private static void downloadAttachment(MFSession session, final Loggable log, XmlDoc.Element ae,
            final String attachmentAssetId, File objectDir, DataDownloadOptions options) throws Throwable {

        /*
         * attachment asset meta
         */
        XmlDoc.Element aae = session.execute("asset.get", "<id>" + attachmentAssetId + "</id>", null, null)
                .element("asset");

        /*
         * attachment file
         */
        String attachmentFileName = aae.value("name");
        if (attachmentFileName == null) {
            attachmentFileName = attachmentAssetId;
            String ext = aae.value("content/type/@ext");
            if (ext != null) {
                attachmentFileName = attachmentFileName + "." + ext;
            }
        }
        File attachmentDir = createAttachmentDirectory(objectDir, aae);
        final File attachmentFile = new File(attachmentDir, attachmentFileName);
        if (attachmentFile.exists() && !options.overwrite()) {
            log.logInfo("skipping attachment file: " + attachmentFile.getAbsolutePath());
            return;
        }

        /*
         * output handler
         */
        ServerClient.OutputConsumer output = new ServerClient.OutputConsumer() {
            @Override
            protected void consume(Element re, LongInputStream is) throws Throwable {
                log.logInfo("writing file: " + attachmentFile.getAbsolutePath());
                StreamCopy.copy(is, attachmentFile);
            }
        };
        log.logInfo("downloading attachment file: " + attachmentFile.getAbsolutePath());
        session.execute("asset.content.get", "<id>" + attachmentAssetId + "</id>", null, output);
    }

    private static void downloadContent(MFSession session, final Loggable logger, final XmlDoc.Element ae,
            final File objectDir, final DataDownloadOptions options) throws Throwable {
        String assetId = ae.value("@id");
        final String title = titleFor(ae);
        final String ctype = ae.value("content/type");
        final File contentDir = createContentDirectory(objectDir, ae);

        ServerClient.OutputConsumer output = new ServerClient.OutputConsumer() {
            @Override
            protected void consume(Element re, LongInputStream is) throws Throwable {
                Archive.declareSupportForAllTypes();
                try {
                    if (ArchiveRegistry.isAnArchive(ctype) && options.decompress()) {
                        ArchiveInput ai = ArchiveRegistry.createInput(is, new NamedMimeType(ctype));
                        logger.logInfo("extracting content archive of " + title);
                        try {
                            ArchiveExtractor.extract(ai, contentDir, true, options.overwrite(), false);
                            // ArchiveInput.discardToEndOfStream(is);
                        } finally {
                            ai.close();
                        }
                    } else {
                        File contentFile = createContentFile(contentDir, ae);
                        if (!contentFile.exists() || options.overwrite()) {
                            logger.logInfo("writing file: " + contentFile.getAbsolutePath());
                            StreamCopy.copy(is, contentFile);
                        } else {
                            logger.logInfo("skipping file: " + contentFile.getAbsolutePath());
                        }
                    }
                } finally {
                    is.close();
                }
            }
        };
        logger.logInfo("downloading content of " + title);
        session.execute("asset.content.get", "<id>" + assetId + "</id>", null, output);
    }

    private static void transcodeContent(MFSession session, final Loggable logger, final XmlDoc.Element ae,
            File objectDir, final DataDownloadOptions options) throws Throwable {
        String assetId = ae.value("@id");
        String type = ae.value("type");
        final String toType = options.transcode(type);
        final String atype = options.decompress() ? "aar" : "zip";
        final String title = titleFor(ae);
        final File transcodedDir = createTranscodedDirectory(objectDir, toType);
        logger.logInfo("created directory: " + transcodedDir);
        XmlStringWriter w = new XmlStringWriter();
        w.add("id", assetId);
        w.add("atype", atype);
        w.push("transcode");
        w.add("from", type);
        w.add("to", toType);
        w.pop();

        ServerClient.OutputConsumer output = new ServerClient.OutputConsumer() {
            @Override
            protected void consume(Element re, LongInputStream is) throws Throwable {
                Archive.declareSupportForAllTypes();
                try {
                    if (options.decompress()) {
                        ArchiveInput ai = ArchiveRegistry.createInput(is, new NamedMimeType("application/arc-archive"));
                        logger.logInfo("extracting transcoded content of " + title);
                        try {
                            ArchiveExtractor.extract(ai, transcodedDir, true, options.overwrite(), false);
                            // ArchiveInput.discardToEndOfStream(is);
                        } finally {
                            ai.close();
                        }
                    } else {
                        File transcodedFile = createTranscodedFile(transcodedDir, ae, toType, atype);
                        if (!transcodedFile.exists() || options.overwrite()) {
                            logger.logInfo("writing file: " + transcodedFile.getAbsolutePath());
                            StreamCopy.copy(is, transcodedFile);
                        } else {
                            logger.logInfo("skipping file: " + transcodedFile.getAbsolutePath());
                        }
                    }
                } finally {
                    is.close();
                }
            }
        };
        logger.logInfo("transcoding " + title + " from " + type + " to " + toType);
        session.execute("asset.transcode", w.document(), null, output);
    }

}
