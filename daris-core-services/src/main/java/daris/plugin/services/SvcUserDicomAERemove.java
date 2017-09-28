package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcUserDicomAERemove extends PluginService {

    public static final String SERVICE_NAME = "daris.user.dicom.ae.remove";

    private Interface _defn;

    public SvcUserDicomAERemove() {
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
        XmlDoc.Element se = SvcUserDicomAEAdd.userSettingsOf(executor(), SvcUserDicomAEAdd.APP).element("settings");
        if (se == null || !se.elementExists("ae[@name='" + name + "']")) {
            return;
        }
        se.remove(se.element(("ae[@name='" + name + "']")));
        if (se.hasSubElements()) {
            System.out.println(2);
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("app", SvcUserDicomAEAdd.APP);
            dm.push("settings");
            dm.add(se, false);
            dm.pop();
            executor().execute("user.self.settings.set", dm.root());
        } else {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("app", SvcUserDicomAEAdd.APP);
            executor().execute("user.self.settings.remove", dm.root());
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
