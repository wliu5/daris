package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;

public class SvcDicomSendCalledAEExists extends PluginService {

    public static final String SERVICE_NAME = "daris.dicom.send.called-ae.exists";

    private Interface _defn;

    public SvcDicomSendCalledAEExists() {
        _defn = new Interface();
        _defn.add(new Interface.Element("name", StringType.DEFAULT, "The name of the application entity.", 1, 1));
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
        return "Check if the application entity exists.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String name = args.value("name");
        if (SvcDicomSendCalledAEAdd.aeAssetExists(executor(), name)) {
            w.add("exists", new String[] { "name", name, "type", "public" }, true);
            return;
        }
        if (SvcUserDicomSendCalledAEAdd.aeSettingsExists(executor(), name)) {
            w.add("exists", new String[] { "name", name, "type", "private" }, true);
            return;
        }
        w.add("exists", new String[] { "name", name }, false);
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}