package daris.plugin.services;

import java.util.Collection;
import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcUserDicomSendCallingAETitleRemove extends PluginService {

    public static final String SERVICE_NAME = "daris.user.dicom.send.calling-ae-title.remove";

    private Interface _defn;

    public SvcUserDicomSendCallingAETitleRemove() {
        _defn = new Interface();
        _defn.add(new Interface.Element("title", StringType.DEFAULT, "Calling AE title to remove from user's settings.",
                1, 1));

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
        return "Remove calling AE title from user's settings.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        String title = args.value("title");
        XmlDoc.Element se = SvcUserDicomSendSettingsGet.getDicomSendSettings(executor());
        if (se != null) {
            Collection<String> callingAETitles = se.values("calling-ae-title");
            if (callingAETitles != null && callingAETitles.contains(title)) {
                List<XmlDoc.Element> callingAETs = se.elements("calling-ae-title");
                for (XmlDoc.Element callingAET : callingAETs) {
                    if (title.equals(callingAET.value())) {
                        se.remove(callingAET);
                    }
                }
                XmlDocMaker dm = new XmlDocMaker("args");
                dm.add("app", SvcUserDicomSendSettingsSet.APP);
                if (se.hasSubElements()) {
                    dm.push("settings");
                    dm.add(se,false);
                    dm.pop();
                    executor().execute("user.self.settings.set", dm.root());
                } else {
                    executor().execute("user.self.settings.remove", dm.root());
                }
            }
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
