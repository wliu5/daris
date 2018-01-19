package daris.client.model.project.messages;

import java.util.List;

import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlWriter;
import arc.mf.object.Null;
import arc.mf.object.ObjectMessage;
import daris.client.model.object.DObject;
import daris.client.model.project.Project;

public class ProjectUserSet extends ObjectMessage<Null> {

    private List<Project.RoleUser> _roleUsers;
    private List<Project.User> _users;
    private String _cid;

    public ProjectUserSet(String cid, List<Project.User> users, List<Project.RoleUser> roleUsers) {
        _cid = cid;
        _users = users;
        _roleUsers = roleUsers;
    }

    public ProjectUserSet(Project project) {
        _cid = project.id();
        _users = project.users();
        _roleUsers = project.roleUsers();
    }

    @Override
    protected void messageServiceArgs(XmlWriter w) {
        w.add("cid", _cid);
        ProjectUserAdd.addServiceArgs(w, _users);
        ProjectRoleUserAdd.addServiceArgs(w, _roleUsers);
    }

    @Override
    protected String messageServiceName() {
        return "daris.project.user.set";
    }

    @Override
    protected Null instantiate(XmlElement xe) throws Throwable {
        return new Null();
    }

    @Override
    protected String objectTypeName() {
        return DObject.Type.project.toString();
    }

    @Override
    protected String idToString() {
        return _cid;
    }

}
