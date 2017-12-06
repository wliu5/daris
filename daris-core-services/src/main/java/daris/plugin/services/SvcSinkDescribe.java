package daris.plugin.services;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlWriter;

public class SvcSinkDescribe extends PluginService {

    public static final String SERVICE_NAME = "daris.sink.describe";

    private Interface _defn;

    public SvcSinkDescribe() {
        _defn = new Interface();
        _defn.add(new Interface.Element("name", StringType.DEFAULT, "Sink name.", 0, Integer.MAX_VALUE));
    }

    @Override
    public Access access() {
        return ACCESS_ACCESS;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Describes one or more data sinks.";
    }

    @Override
    public void execute(XmlDoc.Element args, Inputs i, Outputs o, XmlWriter w) throws Throwable {
        Set<String> names = new LinkedHashSet<String>();
        if (args.elementExists("name")) {
            names.addAll(args.values("name"));
        } else {
            Collection<String> sinks = executor().execute("sink.list").values("sink");
            if (sinks != null) {
                names.addAll(sinks);
            }
        }
        if (names.isEmpty()) {
            return;
        }
        XmlDoc.Element re = executor().execute("sink.type.describe");
        for (String name : names) {
            XmlDoc.Element se = executor()
                    .execute("sink.describe", "<args><name>" + name + "</name></args>", null, null).element("sink");
            XmlDoc.Element te = re.element("sink[@type='" + se.value("destination/type") + "']");
            describeSink(se, te, w);
        }
    }

    private void describeSink(XmlDoc.Element se, XmlDoc.Element te, XmlWriter w) throws Throwable {
        String name = se.value("@name");
        String type = se.value("destination/type");
        w.push("sink", new String[] { "type", type, "name", name });
        List<XmlDoc.Element> taes = te.elements("arg");
        if (taes != null) {
            for (XmlDoc.Element tae : taes) {
                String argName = tae.value();
                String argType = tae.value("@type");
                String argDescription = tae.value("@description");
                String argValue = se.value("destination/arg[@name='" + argName + "']");
                describeSinkArgument(argName, argType, argDescription, argValue, w);
            }
        }
        w.pop();
    }

    private void describeSinkArgument(String argName, String argType, String argDescription, String argValue,
            XmlWriter w) throws Throwable {

        boolean admin = false;
        boolean text = false;
        boolean time = false;
        boolean optional = false;
        boolean mutable = false;
        String pattern = null;
        String defaultValue = null;
        String enumValues = null;
        String xor = null;
        argDescription = argDescription == null ? null : argDescription.trim();
        String description = argDescription;

        if (argDescription != null && !argDescription.isEmpty() && argDescription.matches(".*\\{\\{.*\\}\\}$")) {
            int idx1 = argDescription.indexOf("{{");
            int idx2 = argDescription.lastIndexOf("}}");
            if (idx1 >= 0 && idx2 > idx1) {
                description = argDescription.substring(0, idx1);
                String[] ps = argDescription.substring(idx1 + 2, idx2).split(",");
                for (String p : ps) {
                    p = p.trim();
                    if ("admin".equals(p)) {
                        admin = true;
                    } else if ("text".equals(p)) {
                        if (argType.equals("string") || argType.equals("password")) {
                            text = true;
                        }
                    } else if ("time".equals(p)) {
                        if (argType.equals("date")) {
                            time = true;
                        }
                    } else if ("optional".equals(p)) {
                        optional = true;
                    } else if ("mutable".equals(p)) {
                        mutable = true;
                    } else if (p != null && p.startsWith("pattern=")) {
                        if (argType.equals("string")) {
                            pattern = p.substring(8);
                        }
                    } else if (p != null && p.startsWith("default=")) {
                        defaultValue = p.substring(8);
                    } else if (p != null && p.startsWith("enum=")) {
                        enumValues = join(p.substring(5).split("\\|"), ",");
                    } else if (p != null && p.startsWith("xor")) {
                        xor = join(p.substring(4).split("\\|"), ",");
                    }
                }
            }
        }

        w.push("arg", new String[] { "type", argType, "name", argName, "optional", optional ? "true" : null, "mutable",
                mutable ? "true" : null, "admin", admin ? "true" : null, "text", text ? "true" : null, "time",
                time ? "true" : null, "default", defaultValue, "pattern", pattern, "enum", enumValues, "xor", xor });
        if (description != null) {
            w.add("description", description);
        }
        if (argValue != null) {
            w.add("value", argValue);
        }
        w.pop();
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    private static String join(String[] tokens, String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

}
