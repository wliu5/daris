package daris.plugin.services;

import java.util.Collection;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcUserDicomSendCallingAETitleAdd extends PluginService {

    public static final String SERVICE_NAME = "daris.user.dicom.send.calling-ae-title.add";

    private Interface _defn;

    public SvcUserDicomSendCallingAETitleAdd() {
        _defn = new Interface();
        _defn.add(new Interface.Element("title", StringType.DEFAULT, "Calling AE title to add to user's settings.", 1,
                1));
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
        return "Add calling AE title to user's settings.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        String title = args.value("title");
        Collection<String> callingAETitles = executor().execute(SvcDicomSendCallingAETitleList.SERVICE_NAME)
                .values("calling-ae-title");
        if (callingAETitles != null && callingAETitles.contains(title)) {
            return;
        }
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("calling-ae-title", title);
        executor().execute(SvcUserDicomSendSettingsSet.SERVICE_NAME, dm.root());
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
