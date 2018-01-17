package daris.client.model.project.messages;

import java.util.List;

import arc.mf.client.util.ListUtil;
import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlWriter;
import arc.mf.object.Null;
import arc.mf.object.ObjectMessage;
import daris.client.model.object.DObject;
import daris.client.model.project.Project;

public class ProjectUserAdd extends ObjectMessage<Null> {

    private String _cid;
    private List<Project.User> _users;

    public ProjectUserAdd(String cid, List<Project.User> users) {
        _cid = cid;
        _users = users;
    }

    public ProjectUserAdd(String cid, Project.User user) {
        this(cid, ListUtil.list(user));
    }

    @Override
    protected void messageServiceArgs(XmlWriter w) {

        w.add("cid", _cid);
        assert _users != null;
        assert !_users.isEmpty();
        addServiceArgs(w, _users);
    }

    static void addServiceArgs(XmlWriter w, List<Project.User> users) {
        if (users != null) {
            for (Project.User user : users) {
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
