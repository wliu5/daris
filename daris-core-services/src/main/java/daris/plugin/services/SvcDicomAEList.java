package daris.plugin.services;

import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcDicomAEList extends PluginService {

    public static final String SERVICE_NAME = "daris.dicom.ae.list";

    private Interface _defn;

    public SvcDicomAEList() {
        _defn = new Interface();
        _defn.add(new Interface.Element("name-prefix", StringType.DEFAULT, "Name prefix filter.", 0, 1));
        _defn.add(new Interface.Element("title-prefix", StringType.DEFAULT, "AE title prefix filter.", 0, 1));
        _defn.add(new Interface.Element("host-prefix", StringType.DEFAULT, "AE host prefix filter.", 0, 1));
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
        return "List all public DICOM application entities.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String namePrefix = args.value("name-prefix");
        String titlePrefix = args.value("title-prefix");
        String hostPrefix = args.value("host-prefix");
        Boolean sslFilter = args.booleanValue("ssl", null);
        StringBuilder sb = new StringBuilder();
        sb.append(SvcDicomAEAdd.DOC_TYPE + " has value");
        if (namePrefix != null) {
            sb.append(" and ").append("xpath(" + SvcDicomAEAdd.DOC_TYPE + "/name) starts with '" + namePrefix + "'");
        }
        if (titlePrefix != null) {
            sb.append(" and ").append("xpath(" + SvcDicomAEAdd.DOC_TYPE + "/title) starts with '" + titlePrefix + "'");
        }
        if (hostPrefix != null) {
            sb.append(" and ").append("xpath(" + SvcDicomAEAdd.DOC_TYPE + "/host) starts with '" + hostPrefix + "'");
        }
        if (sslFilter != null) {
            sb.append(" and ").append("xpath(" + SvcDicomAEAdd.DOC_TYPE + "/ssl)=" + sslFilter);
        }

        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("where", sb.toString());
        dm.add("action", "get-value");
        dm.add("size", "infinity");
        dm.add("xpath", new String[] { "ename", "name" }, "meta/" + SvcDicomAEAdd.DOC_TYPE + "/name");
        dm.add("xpath", new String[] { "ename", "title" }, "meta/" + SvcDicomAEAdd.DOC_TYPE + "/title");
        dm.add("xpath", new String[] { "ename", "host" }, "meta/" + SvcDicomAEAdd.DOC_TYPE + "/host");
        dm.add("xpath", new String[] { "ename", "port" }, "meta/" + SvcDicomAEAdd.DOC_TYPE + "/port");
        dm.add("xpath", new String[] { "ename", "ssl" }, "meta/" + SvcDicomAEAdd.DOC_TYPE + "/ssl");
        dm.add("xpath", new String[] { "ename", "description" }, "meta/" + SvcDicomAEAdd.DOC_TYPE + "/description");

        XmlDoc.Element re = executor().execute("asset.query", dm.root());
        if (re.elementExists("asset")) {
            List<XmlDoc.Element> aes = re.elements("asset");
            if (aes != null) {
                for (XmlDoc.Element ae : aes) {
                    w.add("ae",
                            new String[] { "type", "public", "id", ae.value("@id"), "name", ae.value("name"), "title",
                                    ae.value("title"), "host", ae.value("host"), "port", ae.value("port"), "ssl",
                                    ae.value("ssl"), "description", ae.value("description") });
                }
            }
        }

        XmlDoc.Element se = SvcUserDicomAEAdd.userSettingsOf(executor(), SvcUserDicomAEAdd.APP).element("settings");
        if (se != null) {
            List<XmlDoc.Element> aes = se.elements("ae");
            if (aes != null) {
                for (XmlDoc.Element ae : aes) {
                    String name = ae.value("@name");
                    if (namePrefix != null && !name.startsWith(namePrefix)) {
                        continue;
                    }
                    String title = ae.value("title");
                    if (titlePrefix != null && !title.startsWith(titlePrefix)) {
                        continue;
                    }
                    String host = ae.value("host");
                    if (hostPrefix != null && !host.startsWith(hostPrefix)) {
                        continue;
                    }
                    String port = ae.value("port");
                    Boolean ssl = ae.booleanValue("ssl", false);
                    if (sslFilter != null && !sslFilter.equals(ssl)) {
                        continue;
                    }
                    String description = ae.value("description");
                    w.add("ae", new String[] { "type", "private", "name", name, "title", title, "host", host, "port",
                            port, "ssl", ssl == null ? null : Boolean.toString(ssl), "description", description });
                }
            }
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
