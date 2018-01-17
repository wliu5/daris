package daris.plugin.services;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.BooleanType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcRoleUserDescribe extends PluginService {

    public static final String SERVICE_NAME = "daris.role-user.describe";

    public static final String PROJECT_ROLE_PREFIX = "daris:pssd.project.";

    private Interface _defn;

    public SvcRoleUserDescribe() {
        _defn = new Interface();
        _defn.add(new Interface.Element("list-projects", BooleanType.DEFAULT,
                "List the (local only) projects to which the role-user(s) have access? Defaults to false.", 0, 1));

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
        return "Returns information on role-users (and the projects they have access to).";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outptus, XmlWriter w) throws Throwable {
        boolean listProjects = args.booleanValue("list-projects", false);
        List<String> roleUsers = SvcRoleUserList.getRoleUsers(executor());
        if (roleUsers != null) {
            for (String roleUser : roleUsers) {
                describeRoleUser(executor(), roleUser, listProjects, w);
            }
        }
    }

    static void describeRoleUser(ServiceExecutor executor, String roleUser, boolean listProjects, XmlWriter w)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("type", "role");
        dm.add("name", roleUser);
        if (listProjects) {
            dm.add("levels", "infinity");
        }
        XmlDoc.Element ae = executor.execute("actor.describe", dm.root()).element("actor");

        if (ae != null) {
            w.push("role-user", new String[] { "id", ae.value("@id"), "name", roleUser });
        }
        // list associated projects
        if (listProjects) {
            Map<String, SimpleEntry<String, String>> userRoleAndDataUses = getProjectUserRoleAndDataUse(ae);
            if (userRoleAndDataUses != null) {
                Set<String> projects = userRoleAndDataUses.keySet();
                for (String project : projects) {
                    SimpleEntry<String, String> entry = userRoleAndDataUses.get(project);
                    String projectUserRole = entry.getKey();
                    String projectDataUse = entry.getValue();
                    w.add("project", new String[] { "role", projectUserRole, "data-use", projectDataUse }, project);
                }
            }
        }
        w.pop();
    }

    static Map<String, SimpleEntry<String, String>> getProjectUserRoleAndDataUse(XmlDoc.Element actor)
            throws Throwable {

        Map<String, Set<String>> projectRoles = getProjectRoles(actor);
        if (projectRoles != null) {
            Map<String, SimpleEntry<String, String>> userRoleAndDataUses = new HashMap<String, SimpleEntry<String, String>>();
            Set<String> projects = projectRoles.keySet();
            for (String project : projects) {
                Set<String> roles = projectRoles.get(project);
                String projectUserRole = getProjectUserRole(roles);
                String projectDataUse = getProjectDataUse(roles);
                userRoleAndDataUses.put(project, new SimpleEntry<String, String>(projectUserRole, projectDataUse));
            }
            if (!userRoleAndDataUses.isEmpty()) {
                return userRoleAndDataUses;
            }
        }
        return null;
    }

    static String getProjectUserRole(Set<String> roles) {
        if (roles != null) {
            if (roles.contains("admin")) {
                return "project-administrator";
            } else if (roles.contains("subject.admin")) {
                return "subject-administrator";
            } else if (roles.contains("member")) {
                return "member";
            } else if (roles.contains("guest")) {
                return "guest";
            }
        }
        return null;
    }

    static String getProjectDataUse(Set<String> roles) {
        if (roles != null) {
            if (roles.contains("subject.use.extended")) {
                return "extended";
            } else if (roles.contains("subject.use.specific")) {
                return "specific";
            } else if (roles.contains("subject.use.unspecified")) {
                return "unspecified";
            }
        }
        return null;
    }

    static Map<String, Set<String>> getProjectRoles(XmlDoc.Element actor) throws Throwable {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        getProjectRoles(actor, map);
        return map;
    }

    static void getProjectRoles(XmlDoc.Element e, Map<String, Set<String>> map) throws Throwable {
        if (e.nameEquals("role") && e.hasValue()) {
            String role = e.value();
            if (role != null && role.startsWith(PROJECT_ROLE_PREFIX) && role.matches(".*\\d+$")) {
                SimpleEntry<String, String> entry = parseProjectRoles(role);
                if (entry != null) {
                    String project = entry.getKey();
                    Set<String> roles = map.get(project);
                    if (roles == null) {
                        roles = new HashSet<String>();
                    }
                    roles.add(entry.getValue());
                    map.put(project, roles);
                }
            }
        }
        List<XmlDoc.Element> ses = e.elements();
        if (ses != null) {
            for (XmlDoc.Element se : ses) {
                getProjectRoles(se, map);
            }
        }
    }

    private static SimpleEntry<String, String> parseProjectRoles(String projectRole) {
        int i = projectRole.length() - 1;
        while (i >= 0) {
            char c = projectRole.charAt(i);
            if (c != '.' && !Character.isDigit(c)) {
                String project = projectRole.substring(i + 2);
                String roleType = projectRole.substring(PROJECT_ROLE_PREFIX.length(), i + 1);
                return new SimpleEntry<String, String>(project, roleType);
            }
            i--;
        }
        return null;
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
