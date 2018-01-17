package daris.client.model.project.messages;

import java.util.List;

import arc.mf.client.util.ListUtil;
import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlWriter;
import arc.mf.object.Null;
import arc.mf.object.ObjectMessage;
import daris.client.model.object.DObject;
import daris.client.model.project.Project;

public class ProjectRoleUserAdd extends ObjectMessage<Null> {

    private String _cid;
    private List<Project.RoleUser> _roleUsers;

    public ProjectRoleUserAdd(String cid, Project.RoleUser roleUser) {
        this(cid, ListUtil.list(roleUser));
    }

    public ProjectRoleUserAdd(String cid, List<Project.RoleUser> roleUsers) {
        _cid = cid;
        _roleUsers = roleUsers;
    }

    @Override
    protected void messageServiceArgs(XmlWriter w) {

        w.add("cid", _cid);
        addServiceArgs(w, _roleUsers);
    }

    static void addServiceArgs(XmlWriter w, List<Project.RoleUser> roleUsers) {
        if (roleUsers != null) {
            for (Project.RoleUser roleUser : roleUsers) {
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

    @Override
    protected String messageServiceName() {

        return "daris.project.user.add";
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
