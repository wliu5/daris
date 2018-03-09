package daris.plugin.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TransferSyntax;

import arc.archive.ArchiveInput;
import arc.archive.ArchiveRegistry;
import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginTask;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.AssetType;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.EnumType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.PasswordType;
import arc.mf.plugin.dtype.StringType;
import arc.mf.plugin.dtype.XmlDocType;
import arc.mime.MimeType;
import arc.mime.NamedMimeType;
import arc.streams.SizedInputStream;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import daris.util.FileUtils;
import nig.mf.plugin.pssd.services.SvcDicomSend;
import nig.mf.plugin.pssd.services.SvcDicomSend.ElementName;
import nig.mf.plugin.pssd.services.SvcDicomSend.ValueReference;
import nig.mf.pssd.CiteableIdUtil;

public abstract class SvcDicomSshSend extends PluginService {

    public static final String SERVICE_NAME = "daris.dicom.ssh.send";

    public static final String SERVICE_SCP_PUT = "daris.scp.put";

    public static final String SERVICE_SFTP_PUT = "daris.sftp.put";

    public static final String DEFAULT_AE_TITLE = "DaRIS";

    protected Interface defn;

    public SvcDicomSshSend() {
        defn = new Interface();
        defn.add(new Interface.Element("id", AssetType.DEFAULT,
                "The asset id of the dicom patient/study/series to be sent.", 0, Integer.MAX_VALUE));

        defn.add(new Interface.Element("cid", CiteableIdType.DEFAULT,
                "The citeable id of the dicom patient/study/series to be sent.", 0, Integer.MAX_VALUE));

        defn.add(new Interface.Element("where", StringType.DEFAULT,
                "A query to find the DICOM datasets/series to send.", 0, 1));

        Interface.Element override = new Interface.Element("override", XmlDocType.DEFAULT,
                "The dicom elements to override.", 0, 1);
        Interface.Element ee = new Interface.Element("element", XmlDocType.DEFAULT, "The dicom element to override", 0,
                Integer.MAX_VALUE);
        ee.add(new Interface.Attribute("name", new EnumType(ElementName.stringValues()),
                "The name of the dicom element. Should not specified if 'tag' is specified.", 0));
        ee.add(new Interface.Attribute("tag", new StringType(Pattern.compile("[0-9a-fA-F]{8}")),
                "The tag of the dicom element. Should not specified if 'name' is specified.", 0));
        ee.add(new Interface.Attribute("anonymize", BooleanType.DEFAULT,
                "Anonymize the element. Defaults to false. If it is set to true, the element value will be ignored.",
                0));
        ee.add(new Interface.Element("value", StringType.DEFAULT,
                "The new value of the dicom element. It will be ignored if 'anonymize' is set to true. 'value' and 'value-reference' arguments should not be specified at the same time.",
                0, 1));
        ee.add(new Interface.Element("value-reference", new EnumType(ValueReference.stringValues()),
                "The new value refers to meta data of parent subject/study. It will be ignored if 'anonymize' is set to true. 'value' and 'value-reference' arguments should not be specified at the same time.",
                0, 1));
        override.add(ee);
        defn.add(override);

        /*
         * ssh server args
         */
        defn.add(new Interface.Element("protocol", new EnumType(new String[] { "scp", "sftp" }),
                "The transfer protocol. Defaults to scp.", 0, 1));
        defn.add(new Interface.Element("host", StringType.DEFAULT, "SSH server host address.", 1, 1));
        defn.add(new Interface.Element("port", new IntegerType(0, 65535), "SSH server port. Defaults 22.", 0, 1));
        defn.add(new Interface.Element("user", StringType.DEFAULT, "SSH user name.", 1, 1));
        Interface.Element password = new Interface.Element("password", PasswordType.DEFAULT, "SSH user's password.", 0,
                1);
        password.add(new Interface.Attribute("type", new EnumType(new String[] { "value", "reference" }),
                "value or reference to a secure wallet entry. If the latter, key for the entry is specified. Defaults to value.",
                0));
        defn.add(password);

        Interface.Element privateKey = new Interface.Element("private-key", PasswordType.DEFAULT,
                "SSH user's private key.", 0, 1);
        privateKey.add(new Interface.Attribute("type", new EnumType(new String[] { "value", "reference" }),
                "value or reference to a secure wallet entry. If the latter, key for the entry is specified. Defaults to value.",
                0));
        defn.add(privateKey);

        Interface.Element passphrase = new Interface.Element("passphrase", PasswordType.DEFAULT,
                "Passphrase for the private key. Ignored if no private-key is specified.", 0, 1);
        passphrase.add(new Interface.Attribute("type", new EnumType(new String[] { "value", "reference" }),
                "value or reference to a secure wallet entry. If the latter, key for the entry is specified. Defaults to value.",
                0));
        defn.add(passphrase);

        defn.add(new Interface.Element("directory", StringType.DEFAULT, "Remote destination directory.", 0, 1));

        // defn.add(new Interface.Element("dicom-dir-path-pattern",
        // StringType.DEFAULT,
        // "DICOM directory path pattern.", 0, 1));

        defn.add(new Interface.Element("dicom-filename-by",
                new EnumType(new String[] { "SOPInstanceUID", "InstanceNumber" }),
                "Name the dicom files by SOPInstanceUID or InstanceNumber. Defaults to SOPInstanceUID", 0, 1));
    }

    protected abstract String sshTransferService();

    @Override
    public Access access() {
        return ACCESS_ACCESS;
    }

    @Override
    public Interface definition() {
        return this.defn;
    }

    @Override
    public String description() {
        return "Transfer DICOM data to the specified remote ssh server via scp or sftp protocol.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        /*
         * ssh server connection details
         */
        String service = sshTransferService();
        if (executor().execute("system.service.exists", "<args><service>" + service + "</service></args>", null, null)
                .booleanValue("exists") == false) {
            throw new UnsupportedOperationException(
                    "Service: " + service + " does not exists. Please install daris-ssh-plugin package first.");
        }

        /*
         * dicom data to send
         */
        Collection<String> ids = args.values("id");
        Collection<String> cids = args.values("cid");
        String where = args.value("where");
        if ((ids == null || ids.isEmpty()) && (cids == null || cids.isEmpty()) && where == null) {
            Exception ex = new IllegalArgumentException("Argument 'id', 'cid' or 'where' is required.");
            throw ex;
        }
        String dicomFilenameBy = args.stringValue("dicom-filename-by", "SOPInstanceUID");
        Set<String> datasetAssetIds = new TreeSet<String>();
        if (ids != null) {
            PluginTask.checkIfThreadTaskAborted();
            for (String id : ids) {
                SvcDicomSend.addByAssetId(executor(), id, datasetAssetIds);
            }
        }
        if (cids != null) {
            PluginTask.checkIfThreadTaskAborted();
            for (String cid : cids) {
                SvcDicomSend.addByCiteableId(executor(), cid, datasetAssetIds);
            }
        }
        if (where != null) {
            PluginTask.checkIfThreadTaskAborted();
            SvcDicomSend.addByQuery(executor(), where, datasetAssetIds);
        }

        Map<AttributeTag, Object> override = null;
        if (args.elementExists("override")) {
            override = SvcDicomSend.parseOverriddenElements(args.element("override"));
        }

        Progress progress = new Progress();
        if (datasetAssetIds.isEmpty()) {
            // No dicom data is specified.
            w.push("sent");
            w.add("number-of-datasets", new String[] { "total", String.valueOf(progress.totalDatasets) },
                    progress.processedDatasets);
            w.add("number-of-files", new String[] { "total", String.valueOf(progress.totalFiles) },
                    progress.processedFiles);
            w.pop();
            return;
        }
        progress.totalDatasets = datasetAssetIds.size();

        for (String datasetAssetId : datasetAssetIds) {
            File tmpDir = PluginTask.createTemporaryDirectory();
            try {
                sendDicomDataset(executor(), service, args, datasetAssetId, override, tmpDir, dicomFilenameBy,
                        progress);
            } finally {
                FileUtils.deleteDirectory(tmpDir.toPath());
            }
        }
    }

    private static void sendDicomDataset(ServiceExecutor executor, String service, XmlDoc.Element args, String assetId,
            Map<AttributeTag, Object> override, File tmpDir, String dicomFilenameBy, Progress progress)
            throws Throwable {
        PluginTask.checkIfThreadTaskAborted();
        extractDicomDataset(executor, assetId, override, tmpDir, dicomFilenameBy, progress);
        PluginTask.checkIfThreadTaskAborted();
        sendDirectory(executor, service, args, tmpDir);
        progress.processedDatasets++;
    }

    private static void extractDicomDataset(ServiceExecutor executor, String assetId,
            Map<AttributeTag, Object> override, File baseDir, String dicomFilenameBy, Progress progress)
            throws Throwable {
        PluginService.Outputs outputs = new PluginService.Outputs(1);
        XmlDoc.Element ae = executor.execute("asset.get", "<args><id>" + assetId + "</id></args>", null, outputs)
                .element("asset");
        PluginService.Output output = outputs.output(0);

        assert output != null;

        String cid = ae.value("cid");
        int seriesSize = ae.intValue("meta/mf-dicom-series/size");
        progress.totalFiles += seriesSize;

        String studyAssetId = null;
        String studyCID = null;
        if (cid != null) {
            // has cid: pssd data
            studyCID = CiteableIdUtil.getParentId(cid);
        } else {
            // has no cid: pss data
            studyAssetId = ae.value("related[@type='container']/to");
        }

        assert studyAssetId != null || studyCID != null;

        String ctype = ae.value("content/type");
        long csize = output.length() >= 0 ? output.length() : ae.longValue("content/size");

        assert ctype != null;

        MimeType mtype = new NamedMimeType(ctype);

        assert ArchiveRegistry.isAnArchive(mtype);
        ArchiveInput ai = ArchiveRegistry.createInput(new SizedInputStream(output.stream(), csize), mtype);
        try {
            ArchiveInput.Entry e = null;
            int idx = 1;
            while ((e = ai.next()) != null) {
                try {
                    if (!e.isDirectory()) {
                        editDicomFile(studyCID, idx, e.stream(), e.size(), override, baseDir, dicomFilenameBy);
                        progress.processedFiles++;
                        idx++;
                        PluginTask.threadTaskCompletedOneOf(progress.totalFiles);
                    }
                } finally {
                    ai.closeEntry();
                }
            }
        } finally {
            ai.close();
        }
    }

    private static void editDicomFile(String studyCID, int idx, InputStream in, long length,
            Map<AttributeTag, Object> override, File dir, String dicomFilenameBy) throws Throwable {
        AttributeList list = new AttributeList();
        list.read(new DicomInputStream(in));
        Attribute mediaStorageSOPClassUIDAttr = list.get(TagFromName.MediaStorageSOPClassUID);
        String mediaStorageSOPClassUID = null;
        if (mediaStorageSOPClassUIDAttr != null) {
            mediaStorageSOPClassUID = mediaStorageSOPClassUIDAttr.getSingleStringValueOrNull();
        }
        Attribute mediaStorageSOPInstanceUIDAttr = list.get(TagFromName.MediaStorageSOPInstanceUID);
        String mediaStorageSOPInstanceUID = null;
        if (mediaStorageSOPInstanceUIDAttr != null) {
            mediaStorageSOPInstanceUID = mediaStorageSOPInstanceUIDAttr.getSingleStringValueOrNull();
        }
        // String implementationClassUID =
        // list.get(TagFromName.ImplementationClassUID).getSingleStringValueOrNull();
        // String implementationVersionName =
        // list.get(TagFromName.ImplementationVersionName).getSingleStringValueOrNull();
        /*
         * Cleanup
         */
        list.removeGroupLengthAttributes();
        list.removeMetaInformationHeaderAttributes();
        list.remove(TagFromName.DataSetTrailingPadding);
        list.correctDecompressedImagePixelModule();
        list.insertLossyImageCompressionHistoryIfDecompressed();
        if (mediaStorageSOPClassUID != null && mediaStorageSOPInstanceUID != null) {
            FileMetaInformation.addFileMetaInformation(list, mediaStorageSOPClassUID, mediaStorageSOPInstanceUID,
                    TransferSyntax.ExplicitVRLittleEndian, DEFAULT_AE_TITLE);
        } else {
            FileMetaInformation.addFileMetaInformation(list, TransferSyntax.ExplicitVRLittleEndian, DEFAULT_AE_TITLE);
        }
        // Put the new tag in place
        if (override != null && !override.isEmpty()) {
            for (AttributeTag aTag : override.keySet()) {
                Object ov = override.get(aTag);
                // NOTE: null values are ignored
                if (ov != null && (ov instanceof String)) {
                    String aValue = (String) ov;
                    Attribute attr = list.get(aTag);
                    if (attr != null) {
                        attr.setValue(aValue);
                    } else {
                        list.putNewAttribute(aTag).addValue(aValue);
                    }
                }
            }
        }

        File parentDir = dir;
        /*
         * patient dir
         */
        StringBuilder sb = new StringBuilder();
        String patientId = Attribute.getSingleStringValueOrDefault(list, TagFromName.PatientID, "")
                .replaceAll("[^ \\w.-]", " ").replaceAll("\\ {2,}", " ").trim();
        String patientName = Attribute.getSingleStringValueOrDefault(list, TagFromName.PatientName, "")
                .replaceAll("[^ \\w.-]", " ").replaceAll("\\ {2,}", " ").trim();
        if (patientId.isEmpty() && patientName.isEmpty()) {
            sb.append(CiteableIdUtil.getSubjectId(studyCID));
        } else {
            if (!patientId.isEmpty()) {
                sb.append(patientId);
            }
            if (!patientName.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(patientName);
            }
        }
        if (sb.length() > 0) {
            parentDir = new File(dir, sb.toString());
            parentDir.mkdir();
        }

        /*
         * study dir
         */
        sb = new StringBuilder();
        String studyDescription = Attribute.getSingleStringValueOrDefault(list, TagFromName.StudyDescription, "")
                .replaceAll("[^ \\w.-]", " ").replaceAll("\\ {2,}", " ").trim();
        if (!studyDescription.isEmpty()) {
            sb.append(studyDescription);
        } else {
            if (studyCID != null) {
                sb.append(studyCID);
            } else {
                String studyInstanceUID = Attribute.getSingleStringValueOrNull(list, TagFromName.StudyInstanceUID);
                if (studyInstanceUID != null) {
                    sb.append(studyInstanceUID);
                }
            }
        }
        if (sb.length() > 0) {
            parentDir = new File(parentDir, sb.toString());
            parentDir.mkdirs();
        }

        /*
         * series dir
         */
        String seriesDescription = Attribute.getSingleStringValueOrDefault(list, TagFromName.SeriesDescription, "")
                .replaceAll("[^ \\w.-]", " ").replaceAll("\\ {2,}", " ").trim();

        if (!seriesDescription.isEmpty()) {
            parentDir = new File(parentDir, seriesDescription);
        } else {
            String seriesInstanceUID = Attribute.getSingleStringValueOrNull(list, TagFromName.SeriesInstanceUID);
            parentDir = new File(parentDir, seriesInstanceUID);
        }
        parentDir.mkdirs();

        /*
         * dicom file name
         */
        String dicomFilename = null;
        if (dicomFilenameBy.equals("SOPInstanceUID")) {
            dicomFilename = Attribute.getSingleStringValueOrDefault(list, TagFromName.SOPInstanceUID,
                    String.format("%05d", idx));
        } else {
            dicomFilename = String.format("%05d",
                    Attribute.getSingleIntegerValueOrDefault(list, TagFromName.InstanceNumber, idx));
        }
        dicomFilename += ".dcm";

        File f = new File(parentDir, dicomFilename);
        list.write(new FileOutputStream(f), TransferSyntax.ExplicitVRLittleEndian, true, true);
    }

    private static void sendDirectory(ServiceExecutor executor, String service, XmlDoc.Element args, File dir)
            throws Throwable {
        File[] files = dir.listFiles();
        for (File f : files) {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add(args.element("host"));
            if (args.elementExists("port")) {
                dm.add(args.element("port"));
            }
            if (args.elementExists("user")) {
                dm.add(args.element("user"));
            }
            if (args.elementExists("password")) {
                dm.add(args.element("password"));
            }
            if (args.elementExists("private-key")) {
                dm.add(args.element("private-key"));
            }
            if (args.elementExists("passphrase")) {
                dm.add(args.element("passphrase"));
            }
            if (args.elementExists("directory")) {
                dm.add(args.element("directory"));
            }
            dm.add("url", new String[] { "unarchive", "false" }, f.toURI().toURL().toString());
            executor.execute(service, dm.root());
        }
    }

    private static class Progress {
        long totalDatasets = 0;
        long totalFiles = 0;
        long processedDatasets = 0;
        long processedFiles = 0;
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    @Override
    public boolean canBeAborted() {
        return true;
    }

}
