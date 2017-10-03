package daris.plugin.services;

import java.util.Collection;
import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.EnumType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.mf.plugin.dtype.XmlDocType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcUserDicomSendSettingsSet extends PluginService {

    public static final String SERVICE_NAME = "daris.user.dicom.send.settings.set";

    public static final String APP = "daris.dicom.send";

    private Interface _defn;

    public SvcUserDicomSendSettingsSet() {
        _defn = new Interface();
        _defn.add(new Interface.Element("action", new EnumType(new String[] { "merge", "replace", "remove" }),
                "Action to take. Defaults to merge", 0, 1));

        _defn.add(new Interface.Element("calling-ae-title", StringType.DEFAULT, "Title of calling application entity.",
                0, 1));

        Interface.Element calledAE = new Interface.Element("called-ae", XmlDocType.DEFAULT, "Called application entity",
                0, 1);
        calledAE.add(new Interface.Attribute("name", StringType.DEFAULT,
                "Unique name for the application entity within DaRIS. Note: it can be the same as AE title, or it can be set to something more meaningful.",
                1));
        calledAE.add(new Interface.Element("title", StringType.DEFAULT, "AE title.", 1, 1));
        calledAE.add(new Interface.Element("host", StringType.DEFAULT, "AE host.", 1, 1));
        calledAE.add(new Interface.Element("port", new IntegerType(0, 65535), "AE port.", 1, 1));
        calledAE.add(
                new Interface.Element("ssl", BooleanType.DEFAULT, "Is transported secured? Defaults to false.", 0, 1));
        calledAE.add(new Interface.Element("description", StringType.DEFAULT, "Description about the DICOM AE.", 0, 1));
        _defn.add(calledAE);
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
        return "Manage DICOM send settings in user's settings.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        String action = args.stringValue("action", "merge");
        if (action.equals("remove")) {
            removeDicomSendSettings(executor());
            return;
        } else if (action.equals("replace")) {
            if (!args.elementExists("calling-ae-title") && !args.elementExists("called-ae")) {
                removeDicomSendSettings(executor());
                return;
            }
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("app", SvcUserDicomSendSettingsSet.APP);
            dm.push("settings");
            if (args.elementExists("calling-ae-title")) {
                dm.add(args.element("calling-ae-title"));
            }
            if (args.elementExists("called-ae")) {
                dm.add(args.element("called-ae"));
            }
            dm.pop();
            executor().execute("user.self.settings.set", dm.root());
        } else {
            if (!args.elementExists("calling-ae-title") && !args.elementExists("called-ae")) {
                return;
            }
            XmlDoc.Element se = SvcUserDicomSendSettingsGet.getDicomSendSettings(executor());
            Collection<String> callingAETitles = se == null ? null : se.values("calling-ae-title");
            List<XmlDoc.Element> calledAEs = se == null ? null : se.elements("called-ae");
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("app", SvcUserDicomSendSettingsSet.APP);
            dm.push("settings");
            String callingAETitle = args.value("calling-ae-title");
            if (callingAETitle != null) {
                dm.add("calling-ae-title", callingAETitle);
            }
            if (callingAETitles != null) {
                for (String aet : callingAETitles) {
                    if (callingAETitle == null || !aet.equals(callingAETitle)) {
                        dm.add("calling-ae-title", aet);
                    }
                }
            }
            String calledAEName = args.value("called-ae/@name");
            if (args.elementExists("called-ae")) {
                dm.add(args.element("called-ae"));
            }
            if (calledAEs != null) {
                for (XmlDoc.Element calledAE : calledAEs) {
                    if (calledAEName == null || !calledAEName.equals(calledAE.value("@name"))) {
                        dm.add(calledAE);
                    }
                }
            }
            dm.pop();
            executor().execute("user.self.settings.set", dm.root());
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    static void removeDicomSendSettings(ServiceExecutor executor) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("app", SvcUserDicomSendSettingsSet.APP);
        executor.execute("user.self.settings.remove", dm.root());
    }

}
