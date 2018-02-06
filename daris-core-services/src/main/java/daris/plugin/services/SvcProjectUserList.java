package daris.plugin.services;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServerRoute;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.AssetType;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.EnumType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import daris.plugin.model.DataUse;
import daris.plugin.model.ProjectRole;
import daris.plugin.model.UserSelf;

public class SvcProjectUserList extends PluginService {

    public static final String SERVICE_NAME = "daris.project.user.list";

    private Interface _defn;

    public SvcProjectUserList() {

        _defn = new Interface();
        _defn.add(new Interface.Element("id", AssetType.DEFAULT, "The asset id of the project.", 0, 1));
        Interface.Element cid = new Interface.Element("cid", CiteableIdType.DEFAULT, "The citeable id of the project.",
                0, 1);
        cid.add(new Interface.Attribute("proute", CiteableIdType.DEFAULT,
                "In afederation, must specify the route to the peer that manages this citable id. If not supplied, the object is assumed to be local.",
                0));
        _defn.add(cid);
        _defn.add(new Interface.Element("include", new EnumType(new String[] { "user", "role-user", "all" }),
                "Type of user to include. Defaults to all.", 0, 1));

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
        return "List users of the specified project.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {

        String assetId = args.value("id");
        String cid = args.value("cid");
        if (assetId == null && cid == null) {
            throw new IllegalArgumentException("Missing cid or id.");
        }
        if (assetId != null && cid != null) {
            throw new IllegalArgumentException("Expecting cid or id. Found both.");
        }
        if (assetId != null) {
            cid = AssetUtils.getCiteableId(executor(), assetId);
        }
        String proute = args.value("cid/@proute");
        ServerRoute sroute = proute == null ? null : new ServerRoute(proute);

        String include = args.stringValue("include", "all");

        if (include.equals("all") || include.equals("user")) {
            List<XmlDoc.Element> ues = getProjectUsers(executor(), sroute, cid);
            if (ues != null) {
                for (XmlDoc.Element ue : ues) {
                    String authority = ue.value("@authority");
                    String protocol = ue.value("@protocol");
                    String domain = ue.value("@domain");
                    String user = ue.value("@user");
                    String userId = ue.value("@id");
                    String fullName = ue.value("name");
                    String email = ue.value("e-mail");

                    ProjectRole.Type roleType = null;
                    DataUse dataUse = null;
                    Set<String> roles = filterValues(ue.values("role[@type='role']"),
                            ProjectRole.PROJECT_SPECIFIC_ROLE_PREFIX, "." + cid);

                    if (roles == null) {
                        if (UserSelf.isAdmin(executor())) {
                            roleType = ProjectRole.Type.PROJECT_ADMINISTRATOR;
                        } else {
                            continue;
                        }
                    } else {
                        roleType = ProjectRole.Type.roleTypeFrom(roles, cid);
                        dataUse = (roleType == ProjectRole.Type.PROJECT_ADMINISTRATOR
                                || roleType == ProjectRole.Type.SUBJECT_ADMINISTRATOR) ? null
                                        : DataUse.dataUseFrom(roles, cid);
                    }
                    w.add("user",
                            new String[] { "domain", domain, "user", user, "id", userId, "authority", authority,
                                    "protocol", protocol, "name", fullName, "email", email, "role", roleType.toString(),
                                    "data-use", dataUse == null ? null : dataUse.toString() });
                }
            }
        }

        if (include.equals("all") || include.equals("role-user")) {
            Set<String> rus = getProjectRoleUsers(executor(), sroute, cid);
            if (rus != null) {
                for (String ru : rus) {
                    XmlDoc.Element ae = executor().execute(sroute, "actor.describe",
                            "<args><type>role</type><name>" + ru + "</name></args>", null, null).element("actor");
                    String actorId = ae.value("@id");
                    Set<String> roles = filterValues(ae.values("role"), ProjectRole.PROJECT_SPECIFIC_ROLE_PREFIX,
                            "." + cid);
                    ProjectRole.Type roleType = ProjectRole.Type.roleTypeFrom(roles, cid);
                    DataUse dataUse = (roleType == ProjectRole.Type.PROJECT_ADMINISTRATOR
                            || roleType == ProjectRole.Type.SUBJECT_ADMINISTRATOR) ? null
                                    : DataUse.dataUseFrom(roles, cid);
                    w.add("role-user", new String[] { "name", ru, "id", actorId, "role", roleType.toString(),
                            "data-use", dataUse == null ? null : dataUse.toString() });
                }
            }
        }
    }

    static Set<String> filterValues(Collection<String> values, String prefix, String suffix) {
        if (values != null && !values.isEmpty() && !(prefix == null && suffix == null)) {
            Set<String> result = new LinkedHashSet<String>();
            for (String value : values) {
                boolean suffixMatch = suffix == null || value.endsWith(suffix);
                boolean prefixMatch = prefix == null || value.startsWith(prefix);
                if (prefixMatch && suffixMatch) {
                    result.add(value);
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        return null;
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    public static List<XmlDoc.Element> getProjectUsers(ServiceExecutor executor, ServerRoute sroute, String cid)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("role", new String[] { "type", "role" }, ProjectRole.guestRoleName(cid));
        dm.add("permissions", true);
        return executor.execute(sroute, "user.describe", dm.root()).elements("user");
    }

    public static Set<String> getProjectRoleUsers(ServiceExecutor executor, ServerRoute sroute, String cid)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("type", "role");
        dm.add("role", new String[] { "type", "role" }, ProjectRole.guestRoleName(cid));
        Collection<String> roles = executor.execute(sroute, "actors.granted", dm.root()).values("actor/@name");
        if (roles == null || roles.isEmpty()) {
            return null;
        }

        List<String> candidates = SvcRoleUserList.getRoleUsers(executor);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Set<String> result = new LinkedHashSet<String>();
        for (String role : roles) {
            if (role.startsWith("daris:pssd.project.")) {
                continue;
            }
            if (candidates.contains(role)) {
                result.add(role);
            }
        }
        if (!result.isEmpty()) {
            return result;
        }
        return null;
    }

}
