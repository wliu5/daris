package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;

public class SvcUserDicomSendSettingsGet extends PluginService {

    public static final String SERVICE_NAME = "daris.user.dicom.send.settings.get";

    private Interface _defn;

    public SvcUserDicomSendSettingsGet() {
        _defn = new Interface();
    }

    @Override
    public Access access() {
        return ACCESS_ACCESS;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Gets the dicom send settings saved previously.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        XmlDoc.Element se = getDicomSendSettings(executor());
        if (se != null) {
            w.add(se);
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    static XmlDoc.Element getDicomSendSettings(ServiceExecutor executor) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("app", SvcUserDicomSendSettingsSet.APP);
        return executor.execute("user.self.settings.get", dm.root()).element("settings");
    }

}
