package daris.plugin.model;

import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDocMaker;

public class UserSelf {

    public static final boolean hasRole(ServiceExecutor executor, String role, String roleType) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("role", new String[] { "type", roleType }, role);
        return executor.execute("actor.self.have", dm.root()).booleanValue("role");
    }

    public static final boolean hasPermission(ServiceExecutor executor, String resource, String resourceType,
            String resourceAccess) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.push("perm");
        dm.add("resource", new String[] { "type", resourceType }, resource);
        dm.add("access", resourceAccess);
        dm.pop();
        return executor.execute("actor.self.have", dm.root()).booleanValue("role");
    }

    public static final boolean isAdmin(ServiceExecutor executor) throws Throwable {
        return hasRole(executor, "system-administrator", "role");
    }

}
