package daris.client.model.project.messages;

import java.util.ArrayList;
import java.util.List;

import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlWriter;
import arc.mf.object.ObjectMessage;
import daris.client.model.object.DObject;
import daris.client.model.project.Project;

public class ProjectRoleUserList extends ObjectMessage<List<Project.RoleUser>> {

    private String _cid;
    private String _proute;

    public ProjectRoleUserList(String cid, String proute) {

        _cid = cid;
        _proute = proute;
    }

    public ProjectRoleUserList(Project o) {

        this(o.id(), o.proute());
    }

    @Override
    protected void messageServiceArgs(XmlWriter w) {

        if (_proute != null) {
            w.add("cid", new String[] { "proute", _proute }, _cid);
        } else {
            w.add("cid", _cid);
        }
        w.add("include", "role-user");
    }

    @Override
    protected String messageServiceName() {

        return "daris.project.user.list";
    }

    @Override
    protected List<Project.RoleUser> instantiate(XmlElement xe) throws Throwable {

        if (xe != null) {
            List<XmlElement> rues = xe.elements("role-user");
            if (rues != null) {
                List<Project.RoleUser> rus = new ArrayList<Project.RoleUser>(rues.size());
                for (XmlElement rue : rues) {
                    rus.add(new Project.RoleUser(rue));
                }
                if (!rus.isEmpty()) {
                    return rus;
                }
            }
        }
        return null;
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
