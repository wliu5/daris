package nig.mf.plugin.pssd;

import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDocMaker;

public class Application {

    public static final String DARIS = "daris";
    public static final String NAME = "PSSD";

    public static final String NAMESPACE_PROPERTY = "daris.namespace.default";

    public static final String META_NAMESPACE = "daris";
    public static final String DICT_NAMESPACE = "daris";

    public final static String defaultNamespace(ServiceExecutor executor) throws Throwable {
        if (propertyExists(executor, "daris", NAMESPACE_PROPERTY)) {
            String ns = getApplicationProperty(executor, "daris", NAMESPACE_PROPERTY);
            if (namespaceExists(executor, ns)) {
                return ns;
            } else {
                throw new Exception("The default namespace: " + ns + " does not exist.");
            }
        } else {
            if (namespaceExists(executor, "daris")) {
                createApplicationProperty(executor, "daris");
                return "daris";
            } else if (namespaceExists(executor, "pssd")) {
                createApplicationProperty(executor, "pssd");
                return "pssd";
            } else {
                throw new Exception("The default namespace: 'daris' does not exist.");
            }
        }
    }

    public static String defaultProjectNamespace(ServiceExecutor executor) throws Throwable {
        String ns = defaultNamespace(executor);
        if ("pssd".equals(ns) || ns.endsWith("pssd")) {
            return ns;
        } else {
            return ns + "/pssd";
        }
    }

    private static boolean propertyExists(ServiceExecutor executor, String app, String name) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("property", new String[] { "app", app }, name);
        return executor.execute("application.property.exists", dm.root()).booleanValue("exists", false);
    }

    private static boolean namespaceExists(ServiceExecutor executor, String namespace) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("namespace", namespace);
        return executor.execute("asset.namespace.exists", dm.root()).booleanValue("exists");
    }

    private static void createApplicationProperty(ServiceExecutor executor, String namespace) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.push("property", new String[] { "app", "daris", "name", NAMESPACE_PROPERTY });
        dm.add("value", namespace);
        dm.pop();
        executor.execute("application.property.create", dm.root());
    }

    private static String getApplicationProperty(ServiceExecutor executor, String app, String name) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("property", new String[] { "app", app }, name);
        return executor.execute("application.property.get", dm.root()).value("property");
    }
}
