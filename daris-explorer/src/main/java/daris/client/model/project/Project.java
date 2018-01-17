package daris.client.model.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import daris.client.model.DataUse;
import daris.client.model.method.MethodRef;
import daris.client.model.object.DObject;

public class Project extends DObject {

    private DataUse _dataUse;
    private List<MethodRef> _methods;
    private List<ProjectUser> _users;
    private List<ProjectRoleUser> _roleUsers;

    public Project(Element oe) throws Throwable {
        super(oe);
        _dataUse = DataUse.fromString(oe.value("data-use"));
        if (oe.elementExists("method")) {
            List<XmlDoc.Element> mes = oe.elements("method");
            _methods = new ArrayList<MethodRef>(mes.size());
            for (XmlDoc.Element me : mes) {
                String mCid = me.value("id");
                String mName = me.value("name");
                String mDesc = me.value("description");
                MethodRef method = new MethodRef(mCid, mName, mDesc);
                _methods.add(method);
            }
        }
        if (oe.elementExists("user")) {
            List<XmlDoc.Element> mes = oe.elements("user");
            _users = new ArrayList<ProjectUser>(mes.size());
            for (XmlDoc.Element me : mes) {
                String domain = me.value("@domain");
                String user = me.value("@user");
                ProjectSpecificRole role = ProjectSpecificRole
                        .fromString(me.value("@role"));
                ProjectUser member = new ProjectUser(domain, user, role);
                _users.add(member);
            }
        }
        if (oe.elementExists("role-user")) {
            List<XmlDoc.Element> rmes = oe.elements("role-user");
            _roleUsers = new ArrayList<ProjectRoleUser>(rmes.size());
            for (XmlDoc.Element rme : rmes) {
                String member = rme.value("@name");
                ProjectSpecificRole role = ProjectSpecificRole
                        .fromString(rme.value("@role"));
                ProjectRoleUser roleMember = new ProjectRoleUser(member,
                        role);
                _roleUsers.add(roleMember);
            }
        }
    }

    public List<MethodRef> methods() {
        if (_methods == null) {
            return null;
        }
        return Collections.unmodifiableList(_methods);
    }

    public int numberOfMethods() {
        return _methods == null ? 0 : _methods.size();
    }

    public boolean hasMethods() {
        return _methods != null && !_methods.isEmpty();
    }

    public List<ProjectUser> users() {
        if (_users == null) {
            return null;
        }
        return Collections.unmodifiableList(_users);
    }

    public DataUse dataUse() {
        return _dataUse;
    }

    @Override
    public final Type type() {
        return DObject.Type.PROJECT;
    }

}
