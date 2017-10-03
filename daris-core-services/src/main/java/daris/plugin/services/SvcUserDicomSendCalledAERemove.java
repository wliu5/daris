package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcUserDicomSendCalledAERemove extends PluginService {

    public static final String SERVICE_NAME = "daris.user.dicom.send.called-ae.remove";

    private Interface _defn;

    public SvcUserDicomSendCalledAERemove() {
        _defn = new Interface();
        _defn.add(new Interface.Element("name", StringType.DEFAULT, "The name of the application entity.", 1, 1));
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
        return "Remove DICOM AE from user's settings.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        String name = args.value("name");
        XmlDoc.Element se = SvcUserDicomSendSettingsGet.getDicomSendSettings(executor());
        if (se == null || !se.elementExists("called-ae[@name='" + name + "']")) {
            return;
        }
        se.remove(se.element(("called-ae[@name='" + name + "']")));
        if (se.hasSubElements()) {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("app", SvcUserDicomSendSettingsSet.APP);
            dm.push("settings");
            dm.add(se, false);
            dm.pop();
            executor().execute("user.self.settings.set", dm.root());
        } else {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("app", SvcUserDicomSendSettingsSet.APP);
            executor().execute("user.self.settings.remove", dm.root());
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
