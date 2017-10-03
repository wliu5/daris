package daris.plugin.services;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import arc.mf.plugin.PluginService;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDoc;
import arc.xml.XmlWriter;

public class SvcDicomSendCallingAETitleList extends PluginService {

    public static final String SERVICE_NAME = "daris.dicom.send.calling-ae-title.list";

    private Interface _defn;

    public SvcDicomSendCallingAETitleList() {
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
        return "List available DICOM calling AE titles. (Including registered DICOM onsend users and calling AE titles saved in user's settings.)";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        Set<String> callingAETs = new LinkedHashSet<String>();
        Collection<String> onsendUsers = executor().execute(SvcDicomOnsendUserList.SERVICE_NAME).values("user");
        if (onsendUsers != null) {
            callingAETs.addAll(onsendUsers);
        }
        XmlDoc.Element se = SvcUserDicomSendSettingsGet.getDicomSendSettings(executor());
        Collection<String> callingAETitles = se == null ? null : se.values("calling-ae-title");
        if (callingAETitles != null) {
            callingAETs.addAll(callingAETitles);
        }
        for (String aet : callingAETs) {
            w.add("calling-ae-title", aet);
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
