package daris.plugin.services;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import nig.mf.pssd.Role;

public class SvcUserList extends PluginService {

    public static final String SERVICE_NAME = "daris.user.list";

    private Interface _defn;

    public SvcUserList() {
        _defn = new Interface();
        Interface.Element ie = new Interface.Element("authority", StringType.DEFAULT,
                "The authority of interest for users. Defaults to all.", 0, 1);
        ie.add(new Interface.Attribute("protocol", StringType.DEFAULT,
                "The protocol of the identity authority. If unspecified, defaults to federated user within the same type of repository.",
                0));
        _defn.add(ie);
        _defn.add(new Interface.Element("domain", StringType.DEFAULT,
                "The authentication domain for users. Defaults to all.", 0, 1));
        _defn.add(new Interface.Element("exclude-system-domain", BooleanType.DEFAULT,
                "Exclude users in 'system' domain?  Defaults to true.", 0, 1));
        _defn.add(new Interface.Element("count-projects", BooleanType.DEFAULT,
                "Counts the projects the user has access. Defaults to false.", 0, 1));
    }

    public String name() {
        return SERVICE_NAME;
    }

    public String description() {
        return "Gets a list of users that have been registered as users of the DaRIS PSSD model. Includes external authorities.";
    }

    public Interface definition() {
        return _defn;
    }

    public Access access() {
        return ACCESS_ACCESS;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

        XmlDoc.Element authority = args.element("authority");
        String domain = args.stringValue("domain");
        Boolean excludeSystemDomain = args.booleanValue("exclude-system-domain", true);
        if ("system".equals(domain) && excludeSystemDomain) {
            throw new IllegalArgumentException(
                    "Given argument domain 'system' contradicts with 'exclude-system-domain' argument.");
        }

        boolean countProjects = args.booleanValue("count-projects", false);

        // Find the users that have the daris:pssd.model.user role. This will
        // work well for LDAP domain users as it will only return the users who
        // have the role (efficiently)
        XmlDocMaker dm = new XmlDocMaker("args");
        if (authority != null) {
            dm.add(authority);
        }
        if (domain != null) {
            dm.add("domain", domain);
        }
        if (countProjects) {
            dm.add("permissions", new String[] { "levels", "1" }, true);
        }
        dm.add("role", new String[] { "type", "role" }, Role.modelUserRoleName());
        List<XmlDoc.Element> users = executor().execute("user.describe", dm.root()).elements("user");
        /*
         * output xml (reformatted)
         */
        if (users != null) {
            for (XmlDoc.Element user : users) {
                String d = user.value("@domain");
                if (!excludeSystemDomain || !"system".equals(d)) {
                    String name = getUserName(user);
                    String email = getUserEmail(user);
                    int nbProjects = 0;
                    if (countProjects) {
                        nbProjects = countUserProjects(executor(), user);
                    }
                    w.add("user",
                            new String[] { "id", user.value("@id"), "authority", user.value("@authority"), "@protocol",
                                    user.value("protocol"), "domain", user.value("@domain"), "name", name, "email",
                                    email, "nb-projects", countProjects ? Integer.toString(nbProjects) : null },
                            user.value("@user"));
                }
            }
        }

    }

    static Set<String> getUserProjects(ServiceExecutor executor, XmlDoc.Element user) throws Throwable {
        Collection<String> roleUsers = executor.execute(SvcRoleUserList.SERVICE_NAME).values("role-user");
        Set<String> projectIds = new HashSet<String>();
        if (user.elementExists("role")) {
            addProjects(executor, user, roleUsers, projectIds);
        }
        return projectIds;
    }

    static int countUserProjects(ServiceExecutor executor, XmlDoc.Element user) throws Throwable {
        Set<String> projectIds = getUserProjects(executor, user);
        if (projectIds != null) {
            return projectIds.size();
        }
        return 0;
    }

    static void addProjects(ServiceExecutor executor, XmlDoc.Element e, Collection<String> roleUsers,
            Set<String> projectIds) throws Throwable {
        Collection<String> roles = e.values("role[@type='role']");
        if (roles != null) {
            for (String role : roles) {
                if (roleUsers != null && roleUsers.contains(role)) {
                    XmlDocMaker dm = new XmlDocMaker("args");
                    dm.add("name", role);
                    dm.add("type", "role");
                    XmlDoc.Element ae = executor.execute("actor.describe", dm.root()).element("actor");
                    addProjects(executor, ae, roleUsers, projectIds);
                } else if (role.startsWith("daris:pssd.project.")) {
                    Pattern pattern = Pattern.compile("(\\.\\d+)+$");
                    Matcher matcher = pattern.matcher(role);
                    if (matcher.find()) {
                        int idx = matcher.start();
                        String projectId = role.substring(idx + 1);
                        projectIds.add(projectId);
                    }
                }
            }
        }
    }

    static String getUserName(XmlDoc.Element user) throws Throwable {
        String name = user.value("name");
        if (name == null) {
            String firstName = user.value("asset/meta/mf-user/name[@type='first']");
            String middleName = user.value("asset/meta/mf-user/name[@type='middle']");
            String lastName = user.value("asset/meta/mf-user/name[@type='last']");
            if (firstName != null || middleName != null || lastName != null) {
                StringBuilder sb = new StringBuilder();
                if (firstName != null) {
                    sb.append(firstName);
                }
                if (middleName != null) {
                    sb.append(" ").append(middleName);
                }
                if (lastName != null) {
                    sb.append(" ").append(lastName);
                }
                name = sb.toString().trim();
            }
        }
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return null;
    }

    static String getUserEmail(XmlDoc.Element user) throws Throwable {
        String email = user.value("e-mail");
        if (email == null) {
            if (user.elementExists("asset/meta/mf-user/email")) {
                email = user.value("asset/meta/mf-user/email");
            }
        }
        return email;
    }

}
