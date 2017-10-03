package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import daris.plugin.model.ModelRole;
import nig.mf.plugin.pssd.Application;

public class SvcDicomSendCalledAEAdd extends PluginService {

    public static final String SERVICE_NAME = "daris.dicom.send.called-ae.add";

    public static final String DOC_TYPE = "daris:dicom-application-entity";

    private Interface _defn;

    public SvcDicomSendCalledAEAdd() {
        _defn = new Interface();
        addToDefn(_defn);
    }

    static void addToDefn(Interface defn) {
        defn.add(new Interface.Element("name", StringType.DEFAULT,
                "Unique name for the application entity within DaRIS. Note: it can be the same as AE title, or it can be set to something more meaningful.",
                1, 1));
        defn.add(new Interface.Element("title", StringType.DEFAULT, "AE title.", 1, 1));
        defn.add(new Interface.Element("host", StringType.DEFAULT, "AE host.", 1, 1));
        defn.add(new Interface.Element("port", new IntegerType(0, 65535), "AE port.", 1, 1));
        defn.add(new Interface.Element("ssl", BooleanType.DEFAULT, "Is transported secured? Defaults to false.", 0, 1));
        defn.add(new Interface.Element("description", StringType.DEFAULT, "Description about the DICOM AE.", 0, 1));

    }

    @Override
    public Access access() {
        return ACCESS_ADMINISTER;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Add a public DICOM application entity.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String name = args.value("name");
        if (aeAssetExists(executor(), name) || SvcUserDicomSendCalledAEAdd.aeSettingsExists(executor(), name)) {
            throw new IllegalArgumentException("DICOM application entity with name: '" + name + "' already exists.");
        }
        String assetId = createDicomApplicationEntityAsset(executor(), args);
        w.add("id", assetId);
    }

    private static String createDicomApplicationEntityAsset(ServiceExecutor executor, XmlDoc.Element args)
            throws Throwable {
        String namespace = Application.defaultNamespace(executor) + "/dicom.send/called.application.entities";
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("namespace", new String[] { "create", "true" }, namespace);
        dm.push("meta");
        dm.push(DOC_TYPE);
        dm.add(args, false);
        dm.pop();
        dm.pop();
        dm.push("acl");
        dm.add("actor", new String[] { "type", "role" }, ModelRole.MODEL_USER_ROLE);
        dm.add("access", "read");
        dm.pop();
        dm.push("acl");
        dm.add("actor", new String[] { "type", "role" }, ModelRole.OBJECT_ADMIN_ROLE);
        dm.add("access", "read-write");
        dm.pop();
        return executor.execute("asset.create", dm.root()).value("id");
    }

    static boolean aeAssetExists(ServiceExecutor executor, String name) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("where", "xpath(" + DOC_TYPE + "/name)='" + name + "'");
        dm.add("size", 1);
        XmlDoc.Element re = executor.execute("asset.query", dm.root());
        return re.elementExists("id");
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
