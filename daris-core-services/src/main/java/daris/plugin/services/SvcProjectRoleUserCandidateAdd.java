package daris.plugin.services;

import java.util.Collection;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcProjectRoleUserCandidateAdd extends PluginService {

    public static final String PROJECT_ROLE_USER_CANDIDATE_TRAILING_MARK = "[daris.project.role-user.candidate]";

    public static final String PROJECT_ROLE_USER_CANDIDATE_DICTIONARY = "daris:daris.project.role-user.candidate";

    public static final String SERVICE_NAME = "daris.project.role-user.candidate.add";

    private Interface _defn;

    public SvcProjectRoleUserCandidateAdd() {
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
        return "Add project role-user candidate.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter arg3) throws Throwable {
        /*
         * create dictionary if not exist
         */
        createDictionary(executor());

        /*
         * add roles to the dictionary
         */
        Collection<String> roles = args.values("role");
        addProjectRoleUserCandidates(executor(), roles);
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    public static void addProjectRoleUserCandidates(ServiceExecutor executor, Collection<String> roles)
            throws Throwable {
        if (roles != null) {
            for (String role : roles) {
                addProjectRoleUserCandidate(executor, role);
            }
        }
    }

    public static void addProjectRoleUserCandidate(ServiceExecutor executor, String role) throws Throwable {
        String desc = executor
                .execute("authorization.role.describe", "<args><role>" + role + "</role></args>", null, null)
                .value("role/description");
        if (desc == null || desc.indexOf(PROJECT_ROLE_USER_CANDIDATE_TRAILING_MARK) == -1) {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("role", role);
            if (desc == null) {
                desc = role + PROJECT_ROLE_USER_CANDIDATE_TRAILING_MARK;
            } else {
                desc += PROJECT_ROLE_USER_CANDIDATE_TRAILING_MARK;
            }
            dm.add("description", desc);
            executor.execute("authorization.role.modify", dm.root());
        }
        DictionaryUtils.addDictionaryEntry(executor, PROJECT_ROLE_USER_CANDIDATE_DICTIONARY, role, null);
    }

    static void createDictionary(ServiceExecutor executor) throws Throwable {
        if (!DictionaryUtils.dictionaryExists(executor, PROJECT_ROLE_USER_CANDIDATE_DICTIONARY)) {
            DictionaryUtils.createDictionary(executor, PROJECT_ROLE_USER_CANDIDATE_DICTIONARY,
                    "DaRIS project role user candicates.");
        }
    }

}
