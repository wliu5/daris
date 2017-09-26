package daris.plugin.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import nig.mf.plugin.pssd.dicom.Role;

public class SvcDicomOnsendUserList extends PluginService {

    private static final String DEFAULT_AUTH_DOMAIN = "dicom";

    private static final String NAME_SUFFIX = "DARIS_ONSEND";

    private static final String DICOM_INGEST_ROLE = Role.dicomIngestRoleName();

    private Interface _defn;

    public SvcDicomOnsendUserList() {
        _defn = new Interface();
        _defn.add(new Interface.Element("dicom", StringType.DEFAULT,
                "The authentication domain of the DICOM proxy users. Defaults to " + DEFAULT_AUTH_DOMAIN, 0, 1));
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
        return "List the dicom proxy users (AE titles) that can be used for onsending dicom data.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        String domain = args.value("domain");
        if (domain == null) {
            domain = findDicomAuthenticationDomain(executor());
            if (domain == null) {
                domain = DEFAULT_AUTH_DOMAIN;
            }
        }
        List<String> users = findDicomOnsendUsers(executor(), domain);
        if (users != null) {
            for (String user : users) {
                w.add("user", new String[] { "domain", domain }, user);
            }
        }
    }

    private static List<String> findDicomOnsendUsers(ServiceExecutor executor, String domain) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("role", new String[] { "type", "role" }, DICOM_INGEST_ROLE);
        dm.add("domain", domain);
        dm.add("size", "infinity");
        XmlDoc.Element re = executor.execute("user.describe", dm.root());
        List<XmlDoc.Element> ues = re.elements("user");
        if (ues != null && !ues.isEmpty()) {
            List<String> users = new ArrayList<String>();
            for (XmlDoc.Element ue : ues) {
                String name = ue.value("name");
                String user = ue.value("@user");
                if (name.endsWith(NAME_SUFFIX)) {
                    users.add(user);
                }
            }
            if (!users.isEmpty()) {
                return users;
            }
        }
        return null;
    }

    private static String findDicomAuthenticationDomain(ServiceExecutor executor) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("type", "dicom");
        XmlDoc.Element re = executor.execute("network.describe", dm.root());
        Collection<String> domains = re.values("service/arg[@name='authentication.domain']");
        if (domains != null && !domains.isEmpty()) {
            return new TreeSet<String>(domains).first();
        }
        return null;
    }

    @Override
    public String name() {
        return "daris.dicom.onsend.user.list";
    }

}
