package nig.mf.plugin.pssd;

import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDocMaker;

public class ApplicationProperty {

    public static final String APPLICATION_NAME = Application.DARIS;

    public static void set(ServiceExecutor executor, String name, String value)
            throws Throwable {
        // create the property
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("ifexists", "ignore");
        dm.push("property", new String[] { "app", APPLICATION_NAME, "name",
                name });
        executor.execute("application.property.create", dm.root());

        // set the property value
        dm = new XmlDocMaker("args");
        dm.add("property",
                new String[] { "app", APPLICATION_NAME, "name", name }, value);
        executor.execute("application.property.set", dm.root());
    }

    public static String get(ServiceExecutor executor, String name)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("app", APPLICATION_NAME);
        dm.add("name", name);
        return executor.execute("application.property.describe", dm.root())
                .value("property/value");
    }

    public static void destroy(ServiceExecutor executor, String name)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("property",
                new String[] { "app", APPLICATION_NAME, "name", name });
        executor.execute("application.property.destroy", dm.root());
    }

    public static boolean exists(ServiceExecutor executor, String name)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("property", new String[] { "app", APPLICATION_NAME }, name);
        return executor.execute("application.property.exists", dm.root())
                .booleanValue("exists");
    }

    public static class TemporaryNamespace {

        public static final String TMP_NAMESPACE_PROPERTY = "namespace.tmp";

        public static String get(ServiceExecutor executor) throws Throwable {
            if (!ApplicationProperty.exists(executor, TMP_NAMESPACE_PROPERTY)) {
                String namespace = Application.defaultNamespace(executor)
                        + "/tmp";
                set(executor, namespace);
                return namespace;
            } else {
                return ApplicationProperty
                        .get(executor, TMP_NAMESPACE_PROPERTY);
            }
        }

        public static void set(ServiceExecutor executor, String namespace)
                throws Throwable {
            ApplicationProperty
                    .set(executor, TMP_NAMESPACE_PROPERTY, namespace);
        }

    }



}
