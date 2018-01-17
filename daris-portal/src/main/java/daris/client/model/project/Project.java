package daris.client.model.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import arc.mf.client.util.ActionListener;
import arc.mf.client.util.ObjectUtil;
import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlWriter;
import arc.mf.model.authentication.Authority;
import arc.mf.model.authentication.Domain;
import arc.mf.model.authentication.DomainRef;
import arc.mf.model.authentication.UserRef;
import arc.mf.object.Null;
import arc.mf.object.ObjectMessageResponse;
import daris.client.model.IDUtil;
import daris.client.model.method.MethodRef;
import daris.client.model.object.DObject;
import daris.client.model.object.DObjectRef;
import daris.client.model.object.messages.DObjectCreate;
import daris.client.model.object.messages.DObjectUpdate;
import daris.client.model.project.messages.ProjectCreate;
import daris.client.model.project.messages.ProjectMetadataDescribe;
import daris.client.model.project.messages.ProjectUpdate;
import daris.client.model.project.messages.ProjectUserSet;

public class Project extends DObject {

    public static final String SPECIFIC_ADMINISTRATOR_ROLE_NAME_ROOT = "daris:pssd.project.admin";
    public static final String SPECIFIC_SUBJECT_ADMINISTRATOR_ROLE_NAME_ROOT = "daris:pssd.project.subject.admin";
    public static final String SPECIFIC_MEMBER_ROLE_NAME_ROOT = "daris:pssd.project.member";
    public static final String SPECIFIC_GUEST_ROLE_NAME_ROOT = "daris:pssd.project.guest";

    public static final String CID_ROOT_NAME_DICTIONARY = "daris:pssd.project.cid.rootnames";

    public static final String CID_ROOT_NAME_DEFAULT = "pssd.project";

    public static final String ASSET_NAMESPACE_DICTIONARY = "daris:pssd.project.asset.namespaces";

    private DataUse _dataUse;
    private List<MethodRef> _methods;
    private List<Project.User> _users;
    private List<Project.RoleUser> _roleUsers;

    public Project() {

        this(null, null, null, null);
    }

    public Project(String id, String proute, String name, String description) {
        super(id, proute, name, description, false, 0, false);
        _cidRootName = Project.CID_ROOT_NAME_DEFAULT;
    }

    public Project(XmlElement oe) throws Throwable {

        super(oe);

        /*
         * data-use
         */
        _dataUse = DataUse.parse(oe.value("data-use"));
        /*
         * methods
         */
        List<XmlElement> mthdes = oe.elements("method");
        if (mthdes != null) {
            if (!mthdes.isEmpty()) {
                _methods = new Vector<MethodRef>(mthdes.size());
                for (XmlElement mde : mthdes) {
                    _methods.add(new MethodRef(mde.value("id"), mde.value("name"), mde.value("description")));
                }
            }
        }
        /*
         * members
         */
        List<XmlElement> ues = oe.elements("user");
        if (ues != null) {
            if (!ues.isEmpty()) {
                _users = new ArrayList<Project.User>(ues.size());
                for (XmlElement ue : ues) {
                    _users.add(new Project.User(ue));
                }
            }
        }
        /*
         * role-members
         */
        List<XmlElement> rues = oe.elements("role-user");
        if (rues != null) {
            if (!rues.isEmpty()) {
                _roleUsers = new ArrayList<Project.RoleUser>(rues.size());
                for (XmlElement rue : rues) {
                    _roleUsers.add(new Project.RoleUser(rue));
                }
            }
        }
    }

    public List<MethodRef> methods() {

        return _methods;
    }

    public void setMethods(List<MethodRef> methods) {

        _methods = methods;
    }

    public boolean hasMethods() {

        if (_methods == null) {
            return false;
        }
        return !_methods.isEmpty();
    }

    public void setNamespace(String namespace) {
        super.setNamespace(namespace);
    }

    public DataUse dataUse() {

        return _dataUse;
    }

    public void setDataUse(DataUse dataUse) {

        _dataUse = dataUse;
    }

    private String _cidRootName;

    public String cidRootName() {
        return _cidRootName;
    }

    public void setCidRootName(String cidRootName) {
        _cidRootName = cidRootName;
    }

    public List<Project.User> users() {

        return _users;
    }

    public void addUser(Project.User pu) {

        if (_users == null) {
            _users = new ArrayList<Project.User>();
        }
        int index = -1;
        for (int i = 0; i < _users.size(); i++) {
            Project.User user = _users.get(i);
            if (user.user().equals(pu.user())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            _users.remove(index);
        }
        _users.add(pu);
    }

    public void removeUser(Project.User pu) {

        if (pu == null) {
            return;
        }
        if (_users != null) {
            Project.User rm = null;
            for (Project.User user : _users) {
                if (user.user().equals(pu.user())) {
                    rm = user;
                    break;
                }
            }
            if (rm != null) {
                _users.remove(rm);
            }
        }
    }

    public List<Project.RoleUser> roleUsers() {

        return _roleUsers;
    }

    public void addRoleUser(Project.RoleUser pru) {

        if (_roleUsers == null) {
            _roleUsers = new ArrayList<Project.RoleUser>();
        }
        int index = -1;
        for (int i = 0; i < _roleUsers.size(); i++) {
            Project.RoleUser ru = _roleUsers.get(i);
            if (ru.name().equals(pru.name())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            _roleUsers.remove(index);
        }
        _roleUsers.add(pru);
    }

    public void removeRoleUser(Project.RoleUser pru) {

        if (pru == null) {
            return;
        }
        if (_roleUsers != null) {
            Project.RoleUser rm = null;
            for (Project.RoleUser ru : _roleUsers) {
                if (ru.name().equals(pru.name())) {
                    rm = ru;
                    break;
                }
            }
            if (rm != null) {
                _roleUsers.remove(rm);
            }
        }
    }

    @Override
    public DObject.Type type() {

        return DObject.Type.project;
    }

    @Override
    protected DObjectCreate objectCreateMessage(DObjectRef po) {

        return new ProjectCreate(this);
    }

    @Override
    public void createServiceArgs(XmlWriter w) {

        super.createServiceArgs(w);
        if (namespace() != null) {
            w.add("namespace", namespace());
        }
        w.add("data-use", _dataUse);
        if (_methods != null) {
            for (MethodRef m : _methods) {
                w.push("method");
                w.add("id", m.id());
                w.pop();
            }
        }
        if (_users != null) {
            for (Project.User user : _users) {
                w.push("user");
                if (user.user().domain().authority() != null && user.user().domain().authority().name() != null) {
                    if (user.user().domain().authority().protocol() != null) {
                        w.add("authority", new String[] { "protocol", user.user().domain().authority().protocol() },
                                user.user().domain().authority().name());
                    } else {
                        w.add("authority", user.user().domain().authority().name());
                    }
                }
                w.add("domain", user.user().domain());
                w.add("user", user.user().name());
                w.add("role", user.role());
                if (user.dataUse() != null) {
                    w.add("data-use", user.dataUse());
                }
                w.pop();
            }
        }
        if (_roleUsers != null) {
            for (Project.RoleUser roleUser : _roleUsers) {
                w.push("role-user");
                w.add("name", roleUser.name());
                w.add("role", roleUser.role());
                if (roleUser.dataUse() != null) {
                    w.add("data-use", roleUser.dataUse());
                }
                w.pop();
            }
        }
        if (_cidRootName != null) {
            w.add("cid-root-name", _cidRootName);
        }
    }

    @Override
    protected DObjectUpdate objectUpdateMessage() {

        return new ProjectUpdate(this);
    }

    @Override
    public void updateServiceArgs(XmlWriter w) {

        super.updateServiceArgs(w);
        // w.add("namespace", _nameSpace);
        w.add("data-use", _dataUse);
        if (_methods != null) {
            for (MethodRef m : _methods) {
                w.push("method");
                w.add("id", m.id());
                w.pop();
            }
        }
        if (_users != null) {
            for (Project.User user : _users) {
                w.push("user");
                if (user.user().domain().authority() != null && user.user().domain().authority().name() != null) {
                    if (user.user().domain().authority().protocol() != null) {
                        w.add("authority", new String[] { "protocol", user.user().domain().authority().protocol() },
                                user.user().domain().authority().name());
                    } else {
                        w.add("authority", user.user().domain().authority().name());
                    }
                }
                w.add("domain", user.user().domain());
                w.add("user", user.user().name());
                w.add("role", user.role());
                if (user.dataUse() != null) {
                    w.add("data-use", user.dataUse());
                }
                w.pop();
            }
        }
        if (_roleUsers != null) {
            for (Project.RoleUser roleUser : _roleUsers) {
                w.push("role-user");
                w.add("name", roleUser.name());
                w.add("role", roleUser.role());
                if (roleUser.dataUse() != null) {
                    w.add("data-use", roleUser.dataUse());
                }
                w.pop();
            }
        }
    }

    public boolean hasUsers() {

        if (_users != null) {
            return !_users.isEmpty();
        }
        return false;
    }

    public boolean hasRoleUsers() {

        if (_roleUsers != null) {
            return !_roleUsers.isEmpty();
        }
        return false;
    }

    public boolean hasMembersOrRoleMembers() {

        return hasUsers() || hasRoleUsers();
    }

    public boolean hasAdminUser() {

        if (hasUsers()) {
            for (Project.User user : _users) {
                if (user.role().equals(ProjectRole.PROJECT_ADMINISTRATOR)) {
                    return true;
                }
            }
        }
        if (hasRoleUsers()) {
            for (Project.RoleUser roleUser : _roleUsers) {
                if (roleUser.role().equals(ProjectRole.PROJECT_ADMINISTRATOR)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void commitUsers(ObjectMessageResponse<Null> rh) {

        new ProjectUserSet(this).send(rh);
    }

    public void commitRoleUsers(ObjectMessageResponse<Null> rh) {

        new ProjectUserSet(this).send(rh);
    }

    /**
     * Set the meta data definition of a project object (to be created).
     * 
     * @param o
     *            the project to be created.
     */
    public static void setMetaForEdit(final Project o, final ActionListener al) {

        new ProjectMetadataDescribe().send(new ObjectMessageResponse<XmlElement>() {

            @Override
            public void responded(XmlElement metaForEdit) {

                o.setMetaForEdit(metaForEdit);
                al.executed(true);
            }
        });
    }

    public static String adminRoleFromId(String cid) {
        String projectCid = IDUtil.getProjectId(cid);
        if (projectCid == null) {
            return null;
        }
        return Project.SPECIFIC_ADMINISTRATOR_ROLE_NAME_ROOT + "." + projectCid;
    }

    public static String subjectAdminRoleFromId(String cid) {
        String projectCid = IDUtil.getProjectId(cid);
        if (projectCid == null) {
            return null;
        }
        return Project.SPECIFIC_SUBJECT_ADMINISTRATOR_ROLE_NAME_ROOT + "." + projectCid;
    }

    public static String memberRoleFromeId(String cid) {
        String projectCid = IDUtil.getProjectId(cid);
        if (projectCid == null) {
            return null;
        }
        return Project.SPECIFIC_MEMBER_ROLE_NAME_ROOT + "." + projectCid;
    }

    public static String guestRoleFromeId(String cid) {
        String projectCid = IDUtil.getProjectId(cid);
        if (projectCid == null) {
            return null;
        }
        return SPECIFIC_GUEST_ROLE_NAME_ROOT + "." + projectCid;
    }

    public static class User implements Comparable<User> {

        private UserRef _user;
        private ProjectRole _role;
        private DataUse _dataUse;

        public User(XmlElement ue) {

            long userId = -1;
            try {
                userId = ue.longValue("@id");
            } catch (Throwable e) {
            }
            String authorityName = ue.value("@authority");
            String authorityProtocol = ue.value("@protocol");
            Authority authority = authorityName == null ? Domain.AUTHORITY_MEDIAFLUX
                    : new Authority(authorityProtocol, authorityName);
            DomainRef domain = new DomainRef(authority, ue.value("@domain"), Domain.Type.LOCAL, null);
            _user = new UserRef(userId, domain, ue.value("@user"));
            String userEmail = ue.value("email");
            if (userEmail != null) {
                _user.setEmail(userEmail);
            }
            String userName = ue.value("@name");
            if (userName != null) {
                _user.setPersonName(userName);
            }
            _role = ProjectRole.parse(ue.value("@role"));
            _dataUse = DataUse.parse(ue.value("@data-use"));
        }

        public User(UserRef user, ProjectRole role, DataUse dataUse) {

            _user = user;
            assert role != null;
            _role = role;
            if (_role.equals(ProjectRole.PROJECT_ADMINISTRATOR) || _role.equals(ProjectRole.SUBJECT_ADMINISTRATOR)) {
                _dataUse = null;
            } else {
                _dataUse = dataUse;
            }
        }

        public UserRef user() {

            return _user;
        }

        public ProjectRole role() {

            return _role;
        }

        public DataUse dataUse() {

            return _dataUse;
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && (o instanceof Project.User)) {
                Project.User pu = (Project.User) o;
                return _user.equals(pu.user()) && _role.equals(pu.role()) && ObjectUtil.equals(_dataUse, pu.dataUse());
            }
            return false;
        }

        public boolean isSameUser(Project.User pu) {

            if (pu == null) {
                return false;
            }
            return _user.equals(pu.user());
        }

        @Override
        public String toString() {

            String s = ":member";
            if (_user.domain().authority() != null && _user.domain().authority().name() != null) {
                s += " -authority \"" + _user.domain().authority().name() + "\"";
            }
            s += " -domain \"" + _user.domain() + "\"";
            s += " -user \"" + _user.name() + "\"";
            s += " -role \"" + _role.toString() + "\"";
            if (_dataUse != null) {
                s += " -data-use \"" + _dataUse.toString() + "\"";
            }
            return s;
        }

        public String toHTML() {

            String html = "<table><thead><tr><th align=\"center\" colspan=\"2\">Member</th></tr><thead>";
            html += "<tbody>";

            if (_user.domain().authority() != null && _user.domain().authority().name() != null) {
                html += "<tr><td><b>authority:</b></td><td>" + _user.domain().authority().name() + "</td></tr>";
            }

            html += "<tr><td><b>domain:</b></td><td>" + _user.domain() + "</td></tr>";
            html += "<tr><td><b>user:</b></td><td>" + _user.name() + "</td></tr>";
            html += "<tr><td><b>role:</b></td><td>" + _role + "</td></tr>";
            if (_dataUse != null) {
                html += "<tr><td><b>dataUse:</b></td><td>" + _dataUse + "</td></tr>";
            }
            html += "</tbody></table>";
            return html;
        }

        @Override
        public int compareTo(User o) {

            if (o == null) {
                return 1;
            }
            if (_role.ordinal() > o.role().ordinal()) {
                return 1;
            }
            if (_role.ordinal() < o.role().ordinal()) {
                return -1;
            }
            return _user.actorName().compareTo(o.user().actorName());
        }

        public void setRole(ProjectRole role) {

            _role = role;
        }

        public void setDataUse(DataUse dataUse) {

            if (_role.equals(ProjectRole.PROJECT_ADMINISTRATOR) || _role.equals(ProjectRole.SUBJECT_ADMINISTRATOR)) {
                _dataUse = null;
            } else {
                _dataUse = dataUse;
            }
        }

    }

    public static class RoleUser implements Comparable<RoleUser> {

        private long _roleId;
        private String _roleName;
        private ProjectRole _role;
        private DataUse _dataUse;

        public RoleUser(XmlElement rue) {
            try {
                _roleId = rue.longValue("@id");
            } catch (Throwable e) {

            }
            _roleName = rue.value("@name");

            _role = ProjectRole.parse(rue.value("@role"));
            _dataUse = DataUse.parse(rue.value("@data-use"));
        }

        public RoleUser(String roleName, long roleId, ProjectRole projectRole, DataUse dataUse) {

            _roleName = roleName;
            _roleId = roleId;
            _role = projectRole;
            _dataUse = dataUse;
        }

        public String name() {

            return _roleName;
        }

        public long id() {
            return _roleId;
        }

        public ProjectRole role() {

            return _role;
        }

        public void setRole(ProjectRole role) {

            _role = role;
        }

        public DataUse dataUse() {

            return _dataUse;
        }

        public void setDataUse(DataUse dataUse) {

            if (_role.equals(ProjectRole.PROJECT_ADMINISTRATOR) || _role.equals(ProjectRole.SUBJECT_ADMINISTRATOR)) {
                _dataUse = null;
            } else {
                _dataUse = dataUse;
            }
        }

        @Override
        public String toString() {

            String s = ":role-user -id " + id() + " -name " + name() + " -role " + _role;
            if (_dataUse != null) {
                s += " -data-use " + _dataUse;
            }
            return s;
        }

        public String toHTML() {

            String html = "<table><thead><tr><th align=\"center\" colspan=\"2\">Role Member</th></tr><thead>";
            html += "<tbody>";
            html += "<tr><td><b>id:</b></td><td>" + id() + "</td></tr>";
            html += "<tr><td><b>name:</b></td><td>" + name() + "</td></tr>";
            html += "<tr><td><b>role:</b></td><td>" + _role + "</td></tr>";
            if (_dataUse != null) {
                html += "<tr><td><b>data-use:</b></td><td>" + _dataUse + "</td></tr>";
            }
            html += "</tbody></table>";
            return html;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o instanceof RoleUser) {
                RoleUser ru = (RoleUser) o;
                return _roleName.equals(ru.name()) && _role.equals(ru.role())
                        && ObjectUtil.equals(_dataUse, ru.dataUse());
            }
            return false;
        }

        @Override
        public int compareTo(RoleUser o) {
            if (o == null) {
                return 1;
            }
            return String.CASE_INSENSITIVE_ORDER.compare(_roleName, o.name());
        }
    }

}
