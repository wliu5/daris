package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcUserDicomSendCalledAEAdd extends PluginService {

    public static final String SERVICE_NAME = "daris.user.dicom.send.called-ae.add";

    private Interface _defn;

    public SvcUserDicomSendCalledAEAdd() {
        _defn = new Interface();
        SvcDicomSendCalledAEAdd.addToDefn(_defn);
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
        return "Add DICOM AE to user's settings.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        String name = args.value("name");
        XmlDoc.Element se = SvcUserDicomSendSettingsGet.getDicomSendSettings(executor());
        if (SvcDicomSendCalledAEAdd.aeAssetExists(executor(), name)) {
            throw new IllegalArgumentException("DICOM application entity with name: '" + name + "' already exists.");
        }
        if (se != null && se.elementExists("called-ae[@name='" + name + "']")) {
            throw new IllegalArgumentException("DICOM application entity with name: '" + name + "' already exists.");
        }
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("app", SvcUserDicomSendSettingsSet.APP);
        dm.push("settings");
        if (se != null) {
            dm.add(se, false);
        }
        dm.push("called-ae", new String[] { "name", name });
        dm.add(args.element("title"));
        dm.add(args.element("host"));
        dm.add(args.element("port"));
        if (args.elementExists("ssl")) {
            dm.add(args.element("ssl"));
        }
        if (args.elementExists("description")) {
            dm.add(args.element("description"));
        }
        dm.pop();
        dm.pop();
        executor().execute("user.self.settings.set", dm.root());
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    static boolean aeSettingsExists(ServiceExecutor executor, String name) throws Throwable {
        XmlDoc.Element se = SvcUserDicomSendSettingsGet.getDicomSendSettings(executor);
        return se != null && se.elementExists("called-ae[@name='" + name + "']");
    }

}
