package daris.client.model.project;

public class ProjectRoleUser {

    private String _roleName;
    private ProjectSpecificRole _role;

    public ProjectRoleUser(String roleName, ProjectSpecificRole role) {
        _roleName = roleName;
        _role = role;
    }

    public String name() {
        return _roleName;
    }

    public ProjectSpecificRole role() {
        return _role;
    }

}
