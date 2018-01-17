package daris.plugin.services;

import java.util.Collection;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcRoleUserRemove extends PluginService {

    public static final String SERVICE_NAME = "daris.role-user.remove";

    private Interface _defn;

    public SvcRoleUserRemove() {
        _defn = new Interface();
        _defn.add(new Interface.Element("role", StringType.DEFAULT, "The role name.", 1, Integer.MAX_VALUE));
    }

    @Override
    public Access access() {
        return ACCESS_ADMINISTER;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Revoke permission from the role (user) so that the role is no longer a candicate of DaRIS role-user.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter arg3) throws Throwable {
        Collection<String> roles = args.values("role");
        removeProjectRoleUserCandidates(executor(), roles);
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    public static void removeProjectRoleUserCandidates(ServiceExecutor executor, Collection<String> roles)
            throws Throwable {
        if (roles != null) {
            for (String role : roles) {
                removeProjectRoleUserCandidate(executor, role);
            }
        }
    }

    public static void removeProjectRoleUserCandidate(ServiceExecutor executor, String role) throws Throwable {
        if (executor.execute("authorization.role.exists", "<args><role>" + role + "</role></args>", null, null)
                .booleanValue("exists")) {
            String desc = executor
                    .execute("authorization.role.describe", "<args><role>" + role + "</role></args>", null, null)
                    .value("role/description");
            if (desc != null) {
                int idx = desc.indexOf(SvcRoleUserAdd.PROJECT_ROLE_USER_CANDIDATE_TRAILING_MARK);
                if (idx >= 0) {
                    XmlDocMaker dm = new XmlDocMaker("args");
                    dm.add("role", role);
                    desc = desc.replace(SvcRoleUserAdd.PROJECT_ROLE_USER_CANDIDATE_TRAILING_MARK, "");
                    if (desc.isEmpty()) {
                        desc = role;
                    }
                    dm.add("description", desc);
                    executor.execute("authorization.role.modify", dm.root());
                }
            }
        }
        if (DictionaryUtils.dictionaryExists(executor,
                SvcRoleUserAdd.PROJECT_ROLE_USER_CANDIDATE_DICTIONARY)) {
            if (DictionaryUtils.dictionaryEntryExists(executor,
                    SvcRoleUserAdd.PROJECT_ROLE_USER_CANDIDATE_DICTIONARY, role, null)) {
                DictionaryUtils.removeDictionaryEntry(executor,
                        SvcRoleUserAdd.PROJECT_ROLE_USER_CANDIDATE_DICTIONARY, role, null);
            }
        }
    }

}
