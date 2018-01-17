package daris.client.model.project;

public class ProjectUser {

    private String _domain;
    private String _user;
    private ProjectSpecificRole _role;

    public ProjectUser(String domain, String user, ProjectSpecificRole role) {
        _domain = domain;
        _user = user;
        _role = role;
    }

    public String domain() {
        return _domain;
    }

    public String user() {
        return _user;
    }

    public ProjectSpecificRole role() {
        return _role;
    }

}
