package daris.client.model.project.messages;

import java.util.ArrayList;
import java.util.List;

import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlWriter;
import arc.mf.object.ObjectMessage;
import daris.client.model.object.DObject;
import daris.client.model.project.Project;

public class ProjectUserList extends ObjectMessage<List<Project.User>> {

    private String _cid;
    private String _proute;

    public ProjectUserList(String cid, String proute) {

        _cid = cid;
        _proute = proute;
    }

    public ProjectUserList(Project o) {

        this(o.id(), o.proute());
    }

    @Override
    protected void messageServiceArgs(XmlWriter w) {

        if (_proute != null) {
            w.add("cid", new String[] { "proute", _proute }, _cid);
        } else {
            w.add("cid", _cid);
        }
        w.add("include", "user");
    }

    @Override
    protected String messageServiceName() {

        return "daris.project.user.list";
    }

    @Override
    protected List<Project.User> instantiate(XmlElement xe) throws Throwable {

        if (xe != null) {
            List<XmlElement> ues = xe.elements("user");
            if (ues != null) {
                List<Project.User> us = new ArrayList<Project.User>(ues.size());
                for (XmlElement ue : ues) {
                    us.add(new Project.User(ue));
                }
                if (!us.isEmpty()) {
                    return us;
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
