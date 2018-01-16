package daris.plugin.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcProjectRoleUserCandidateList extends PluginService {

    public static final String SERVICE_NAME = "daris.project.role-user.candidate.list";

    public static List<String> getProjectRoleUserCandidates(ServiceExecutor executor) throws Throwable {
        if (!DictionaryUtils.dictionaryExists(executor,
                SvcProjectRoleUserCandidateAdd.PROJECT_ROLE_USER_CANDIDATE_DICTIONARY)) {
            return null;
        }
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("dictionary", SvcProjectRoleUserCandidateAdd.PROJECT_ROLE_USER_CANDIDATE_DICTIONARY);
        dm.add("size", "infinity");
        Collection<String> terms = executor.execute("dictionary.entries.list", dm.root()).values("term");
        if (terms == null || terms.isEmpty()) {
            return null;
        }
        dm = new XmlDocMaker("args");
        for (String term : terms) {
            dm.add("role", term);
        }
        XmlDoc.Element re = executor.execute("authorization.role.exists", dm.root());
        List<String> roles = new ArrayList<String>();
        for (String term : terms) {
            if (re.booleanValue("exists[@role='" + term + "']")) {
                roles.add(term);
            }
        }
        if (roles.isEmpty()) {
            return null;
        }
        return roles;
    }

    public static String getActorId(ServiceExecutor executor, String role) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("type", "role");
        dm.add("name", role);
        return executor.execute("actor.describe", dm.root()).value("actor/@id");
    }

    private Interface _defn;

    public SvcProjectRoleUserCandidateList() {
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
        return "List all roles that can be granted as project role users.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        List<String> roles = getProjectRoleUserCandidates(executor());
        if (roles != null) {
            for (String role : roles) {
                String actorId = getActorId(executor(), role);
                w.add("role", new String[] { "id", actorId }, role);
            }
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }
}
