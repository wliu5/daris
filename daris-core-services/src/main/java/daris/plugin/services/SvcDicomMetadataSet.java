package daris.plugin.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.AttributeTagAttribute;
import com.pixelmed.dicom.DicomDictionary;
import com.pixelmed.dicom.DicomInputStream;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.ValueRepresentation;

import arc.archive.ArchiveInput;
import arc.archive.ArchiveOutput;
import arc.archive.ArchiveRegistry;
import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginTask;
import arc.mf.plugin.PluginThread;
import arc.mf.plugin.dtype.AssetType;
import arc.mf.plugin.dtype.EnumType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.mf.plugin.dtype.XmlDocType;
import arc.mime.NamedMimeType;
import arc.streams.SizedInputStream;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcDicomMetadataSet extends PluginService {

    public static final String SERVICE_NAME = "daris.dicom.metadata.set";

    public static final int DEFAULT_COMPRESSION_LEVEL = 6;

    public static final int MAX_PIXEL_DATA_VALUE_LENGTH_USE_MEMORY_STREAM = 104857600;

    private static class DicomElement {

        private AttributeTag _tag;
        private byte[] _vrBytes;
        private String _action;
        private List<Object> _values;

        private DicomElement(AttributeTag tag, byte[] vrBytes, String action, List<Object> values) {
            _tag = tag;
            _vrBytes = vrBytes;
            _action = action;
            _values = values;
        }

        AttributeTag tag() {
            return _tag;
        }

        String vr() {
            return new String(_vrBytes);
        }

        String action() {
            return _action;
        }

        void setAttributeValues(Attribute attr) throws Throwable {
            // boolean anonymize = "anonymize".equalsIgnoreCase(_action);
            attr.removeValues();

            byte[] vrBytes = attr.getVR();
            String vr = new String(vrBytes);
            assert vr.equals(vr());

            String tagStr = String.format("%04x%04x", attr.getTag().getGroup(), attr.getTag().getElement());
            if (_values != null) {
                for (Object v : _values) {
                    if (ValueRepresentation.isApplicationEntityVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isAgeStringVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isAttributeTagVR(vrBytes)) {
                        ((AttributeTagAttribute) attr).addValue((AttributeTag) v);
                    } else if (ValueRepresentation.isCodeStringVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isDateVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isDecimalStringVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isDateTimeVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isFloatSingleVR(vrBytes)) {
                        attr.addValue((Float) v);
                    } else if (ValueRepresentation.isFloatDoubleVR(vrBytes)) {
                        attr.addValue((Double) v);
                    } else if (ValueRepresentation.isIntegerStringVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isLongStringVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isLongTextVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isOtherByteVR(vrBytes)) {
                        throw new IllegalArgumentException(
                                "Setting OB element[@tag='" + tagStr + "'] is not supported.");
                    } else if (ValueRepresentation.isOtherDoubleVR(vrBytes)) {
                        throw new IllegalArgumentException(
                                "Setting OD element[@tag='" + tagStr + "'] is not supported.");
                    } else if (ValueRepresentation.isOtherFloatVR(vrBytes)) {
                        throw new IllegalArgumentException(
                                "Setting OF element[@tag='" + tagStr + "'] is not supported.");
                    } else if (ValueRepresentation.isOtherWordVR(vrBytes)) {
                        throw new IllegalArgumentException(
                                "Setting OW element[@tag='" + tagStr + "'] is not supported.");
                    } else if (ValueRepresentation.isPersonNameVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isShortStringVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isSignedLongVR(vrBytes)) {
                        attr.addValue((Integer) v);
                    } else if (ValueRepresentation.isSequenceVR(vrBytes)) {
                        throw new IllegalArgumentException(
                                "Setting SQ element[@tag='" + tagStr + "'] is not supported.");
                    } else if (ValueRepresentation.isSignedShortVR(vrBytes)) {
                        attr.addValue((Short) v);
                    } else if (ValueRepresentation.isShortTextVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isTimeVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isUniqueIdentifierVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else if (ValueRepresentation.isUnsignedLongVR(vrBytes)) {
                        attr.addValue((Integer) v);
                    } else if (ValueRepresentation.isUnsignedShortVR(vrBytes)) {
                        attr.addValue((Short) v);
                    } else if (ValueRepresentation.isUnknownVR(vrBytes)) {
                        throw new IllegalArgumentException(
                                "Setting UN element[@tag='" + tagStr + "'] is not supported.");
                    } else if (ValueRepresentation.isUnlimitedTextVR(vrBytes)) {
                        attr.addValue(String.valueOf(v));
                    } else {
                        throw new Exception("Unknown VR: " + vr + " for element[@tag='" + tagStr + "']");
                    }
                }
            }
        }

        static DicomElement parse(XmlDoc.Element ele, DicomDictionary dict) throws Throwable {
            String tagStr = ele.value("@tag");
            AttributeTag tag = new AttributeTag(Integer.parseInt(tagStr.substring(0, 4), 16),
                    Integer.parseInt(tagStr.substring(4, 8), 16));
            byte[] vrBytes = dict.getValueRepresentationFromTag(tag);
            String vr = new String(vrBytes);
            String action = ele.stringValue("@action", "merge");
            if ("anonymize".equalsIgnoreCase(action) || "remove".equalsIgnoreCase(action)) {
                return new DicomElement(tag, vrBytes, action, null);
            }
            Collection<String> stringValues = ele.values("value");
            if (stringValues == null || stringValues.isEmpty()) {
                throw new IllegalArgumentException("Missing element[@tag='" + tagStr + "']/value");
            }
            List<Object> values = new ArrayList<Object>(stringValues.size());
            for (String sv : stringValues) {
                if (ValueRepresentation.isApplicationEntityVR(vrBytes)) {
                    if (sv.length() > 16) {
                        throw new IllegalArgumentException(
                                "Invalid Application Entity value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isAgeStringVR(vrBytes)) {
                    if (!sv.matches("^[0-9]{3}[DWMY]{1}$")) {
                        throw new IllegalArgumentException(
                                "Invalid Age String value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isAttributeTagVR(vrBytes)) {
                    if (!sv.matches("^[a-fA-F0-9]{8}$")) {
                        throw new IllegalArgumentException(
                                "Invalid Attribute Tag value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(new AttributeTag(Integer.parseInt(sv.substring(0, 4), 16),
                            Integer.parseInt(sv.substring(4, 8), 16)));
                } else if (ValueRepresentation.isCodeStringVR(vrBytes)) {
                    if (sv.length() > 16) {
                        throw new IllegalArgumentException("Invalid length of Code String value: " + sv
                                + " for element[@tag='" + tagStr + "']. Expects maximum length of 16 bytes.");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isDateVR(vrBytes)) {
                    if (!sv.matches("^[0-9]{8}$")) {
                        throw new IllegalArgumentException(
                                "Invalid Date value:" + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isDecimalStringVR(vrBytes)) {
                    if (sv.length() > 16) {
                        throw new IllegalArgumentException(
                                "Invalid Decimal String value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    try {
                        Double.parseDouble(sv);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid Decimal String value: " + sv + " for element[@tag='" + tagStr + "']", e);
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isDateTimeVR(vrBytes)) {
                    if (sv.length() > 26 || !sv.matches("[0-9\\.&\\-+\\ ]+")) {
                        throw new IllegalArgumentException(
                                "Invalid Date Time value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isFloatSingleVR(vrBytes)) {
                    try {
                        values.add(Float.parseFloat(sv));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid Float value: " + sv + " for element[@tag='" + tagStr + "']", e);
                    }
                } else if (ValueRepresentation.isFloatDoubleVR(vrBytes)) {
                    try {
                        values.add(Double.parseDouble(sv));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid Float Double value: " + sv + " for element[@tag='" + tagStr + "']", e);
                    }
                } else if (ValueRepresentation.isIntegerStringVR(vrBytes)) {
                    if (sv.length() > 12) {
                        throw new IllegalArgumentException(
                                "Invalid Integer String value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    try {
                        Integer.parseInt(sv);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid Integer String value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isLongStringVR(vrBytes)) {
                    if (sv.length() > 64) {
                        throw new IllegalArgumentException(
                                "Invalid Long String value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isLongTextVR(vrBytes)) {
                    values.add(sv);
                } else if (ValueRepresentation.isOtherByteVR(vrBytes)) {
                    throw new IllegalArgumentException("Setting OB element[@tag='" + tagStr + "'] is not supported.");
                } else if (ValueRepresentation.isOtherDoubleVR(vrBytes)) {
                    throw new IllegalArgumentException("Setting OD element[@tag='" + tagStr + "'] is not supported.");
                } else if (ValueRepresentation.isOtherFloatVR(vrBytes)) {
                    throw new IllegalArgumentException("Setting OF element[@tag='" + tagStr + "'] is not supported.");
                } else if (ValueRepresentation.isOtherWordVR(vrBytes)) {
                    throw new IllegalArgumentException("Setting OW element[@tag='" + tagStr + "'] is not supported.");
                } else if (ValueRepresentation.isPersonNameVR(vrBytes)) {
                    if (sv.length() > 64) {
                        throw new IllegalArgumentException(
                                "Invalid Person Name value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isShortStringVR(vrBytes)) {
                    if (sv.length() > 16) {
                        throw new IllegalArgumentException(
                                "Invalid Short String value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isSignedLongVR(vrBytes)) {
                    try {
                        values.add(Integer.parseInt(sv));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid Signed Long value: " + sv + " for element[@tag='" + tagStr + "']", e);
                    }
                } else if (ValueRepresentation.isSequenceVR(vrBytes)) {
                    throw new IllegalArgumentException("Setting SQ element[@tag='" + tagStr + "'] is not supported.");
                } else if (ValueRepresentation.isSignedShortVR(vrBytes)) {
                    try {
                        values.add(Short.parseShort(sv));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid Signed Short value: " + sv + " for element[@tag='" + tagStr + "']", e);
                    }
                } else if (ValueRepresentation.isShortTextVR(vrBytes)) {
                    values.add(sv);
                } else if (ValueRepresentation.isTimeVR(vrBytes)) {
                    if (!sv.matches("\\d{2}(\\d{2}(\\d{2}(\\.\\d{1,6})?)?)?")) {
                        throw new IllegalArgumentException(
                                "Invalid Time value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                    values.add(sv);
                } else if (ValueRepresentation.isUniqueIdentifierVR(vrBytes)) {
                    if (sv.length() > 64 || !sv.matches("^\\d+(\\.\\d+)*$")) {
                        throw new IllegalArgumentException(
                                "Invalid Unique Identifier value: " + sv + " for element[@tag='" + tagStr + "']");
                    }
                } else if (ValueRepresentation.isUnsignedLongVR(vrBytes)) {
                    try {
                        values.add(Integer.parseUnsignedInt(sv));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid Unsigned Long: " + sv + " for element[@tag='" + tagStr + "']", e);
                    }
                } else if (ValueRepresentation.isUnsignedShortVR(vrBytes)) {
                    try {
                        values.add(Integer.parseUnsignedInt(sv));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid Unsigned Short: " + sv + " for element[@tag='" + tagStr + "']", e);
                    }
                } else if (ValueRepresentation.isUnknownVR(vrBytes)) {
                    throw new IllegalArgumentException("Setting UN element[@tag='" + tagStr + "'] is not supported.");
                } else if (ValueRepresentation.isUnlimitedTextVR(vrBytes)) {
                    values.add(sv);
                } else {
                    throw new Exception("Unknown VR: " + vr + " for element[@tag='" + tagStr + "']");
                }
            }
            return new DicomElement(tag, vrBytes, action, values);
        }

        static List<DicomElement> parse(List<XmlDoc.Element> es) throws Throwable {
            if (es != null && !es.isEmpty()) {
                DicomDictionary dict = new DicomDictionary();
                List<DicomElement> des = new ArrayList<DicomElement>(es.size());
                for (XmlDoc.Element e : es) {
                    DicomElement de = parse(e, dict);
                    des.add(de);
                }
                return des;
            }
            return null;
        }
    }

    private Interface _defn;

    public SvcDicomMetadataSet() {
        _defn = new Interface();
        _defn.add(new Interface.Element("id", AssetType.DEFAULT, "Asset id of the DICOM series.", 0, 1));
        _defn.add(new Interface.Element("cid", AssetType.DEFAULT, "Citeable id of the DICOM series.", 0, 1));
        Interface.Element ele = new Interface.Element("element", XmlDocType.DEFAULT, "DICOM element to edit.", 1,
                Integer.MAX_VALUE);
        ele.add(new Interface.Attribute("tag", new StringType(Pattern.compile("^[a-fA-F0-9]{8}$")), "The element tag",
                1));
        ele.add(new Interface.Attribute("action", new EnumType(new String[] { "merge", "anonymize", "remove" }),
                "The action to take. Defaults to merge. If set to remove, element value will be ignored.", 0));
        ele.add(new Interface.Element("value", StringType.DEFAULT,
                "Value of the element. Ignored if action is remove or annoymize.", 0, Integer.MAX_VALUE));
        _defn.add(ele);
        _defn.add(new Interface.Element("clevel", new IntegerType(0, 9), "Compression level. Defaults to 6.", 0, 1));

    }

    @Override
    public Access access() {
        return ACCESS_MODIFY;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Modify the metadata headers of the DICOM files for the specified DICOM series asset.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter arg3) throws Throwable {
        String cid = args.value("cid");
        String id = args.value("id");

        final List<DicomElement> des = DicomElement.parse(args.elements("element"));
        final int clevel = args.intValue("clevel", DEFAULT_COMPRESSION_LEVEL);

        if (id == null && cid == null) {
            throw new IllegalArgumentException("Expects either id or cid argument. Found none.");
        }
        if (id != null && cid != null) {
            throw new IllegalArgumentException("Expects either id or cid argument. Found both.");
        }

        XmlDocMaker dm = new XmlDocMaker("args");
        if (id != null) {
            dm.add("id", id);
        } else {
            dm.add("cid", cid);
        }
        PluginService.Outputs outputs = new PluginService.Outputs(1);
        XmlDoc.Element ae = executor().execute("asset.get", dm.root(), null, outputs).element("asset");
        final PluginService.Output output = outputs.output(0);

        if (id == null) {
            id = ae.value("@id");
        }

        if (!ae.elementExists("meta/mf-dicom-series")) {
            throw new IllegalArgumentException(
                    "No mf-dicom-series metadata found on asset " + id + ". Not a valid DICOM series(dataset).");
        }

        XmlDoc.Element ce = ae.element("content");
        if (ce == null || output == null) {
            throw new IllegalArgumentException(
                    "Asset " + id + " does not have content. Not a valid DICOM series(dataset).");
        }

        String ctype = output.mimeType();
        if (ctype == null) {
            ctype = ce.value("type");
        }
        final NamedMimeType mimeType = new NamedMimeType(ctype);
        if (!ArchiveRegistry.isAnArchive(ctype)) {
            throw new Exception("Content (type: " + ctype + ") of asset " + id + " is not an archive.");
        }
        final PipedInputStream pis = new PipedInputStream();
        final PipedOutputStream pos = new PipedOutputStream(pis);
        PluginThread.executeAsync(SERVICE_NAME, new Runnable() {
            @Override
            public void run() {
                try {
                    ArchiveInput ai = ArchiveRegistry
                            .createInput(new SizedInputStream(output.stream(), output.length()), mimeType);
                    try {
                        ArchiveOutput ao = ArchiveRegistry.createOutput(pos, mimeType.name(), clevel, null);
                        try {
                            ArchiveInput.Entry e = null;
                            while ((e = ai.next()) != null) {
                                try {
                                    // modify the the source entry and add to
                                    // the new archive
                                    processEntry(e, des, ao);
                                } finally {
                                    ai.closeEntry();
                                }
                            }
                        } finally {
                            ao.close();
                        }
                    } finally {
                        ai.close();
                    }
                } catch (Throwable e) {
                    e.printStackTrace(System.out);
                }
            }
        });
        dm = new XmlDocMaker("args");
        dm.add("id", id);
        executor().execute("asset.set", dm.root(),
                new PluginService.Inputs(new PluginService.Input(pis, -1, mimeType.name(), null)), null);
    }

    private static void processEntry(ArchiveInput.Entry e, List<DicomElement> des, ArchiveOutput ao) throws Throwable {
        if (e.isDirectory()) {
            ao.addDirectory(e.name());
            return;
        }
        DicomInputStream din = new DicomInputStream(e.stream());
        AttributeList al = new AttributeList();
        try {
            al.read(din);
            for (DicomElement de : des) {
                if (de.action().equalsIgnoreCase("remove")) {
                    al.remove(de.tag());
                } else {
                    Attribute attr = al.get(de.tag());
                    if (attr == null) {
                        attr = al.putNewAttribute(de.tag());
                    }
                    de.setAttributeValues(attr);
                }
            }
            Attribute pixelData = al.getPixelData();
            long pixelDataValueLength = pixelData == null ? 0 : pixelData.getVL();
            if (pixelDataValueLength >= 0 && pixelDataValueLength <= MAX_PIXEL_DATA_VALUE_LENGTH_USE_MEMORY_STREAM) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    al.write(baos, TransferSyntax.ExplicitVRLittleEndian, true, true);
                    byte[] bytes = baos.toByteArray();
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    try {
                        ao.add("application/dicom", e.name(), bais, bytes.length);
                    } finally {
                        bais.close();
                    }
                } finally {
                    baos.close();
                }
            } else {
                File tf = PluginTask.createTemporaryFile(e.name());
                try {
                    al.write(tf, TransferSyntax.ExplicitVRLittleEndian, true, true);
                    ao.add("application/dicom", e.name(), tf);
                } finally {
                    PluginTask.deleteTemporaryFile(tf);
                }
            }
        } finally {
            din.close();
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
