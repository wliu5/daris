package daris.plugin.services;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcCollectionTypeList extends PluginService {

    public static final String SERVICE_NAME = "daris.collection.type.list";

    private Interface _defn;

    public SvcCollectionTypeList() {
        _defn = new Interface();
        _defn.add(new Interface.Element("cid", CiteableIdType.DEFAULT, "The citeable id of the root/parent object.", 0,
                1));
        _defn.add(new Interface.Element("where", StringType.DEFAULT, "Additional query to find the matching objects.",
                0, 1));
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
        return "Enumerate MIME types of the objects in the specified collection.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String cid = args.value("cid");
        String where = args.value("where");
        if (cid == null && where == null) {
            throw new IllegalArgumentException("No cid or where is specified.");
        }
        SortedSet<String> types = listTypes(executor(), cid, where);
        if (types != null) {
            for (String type : types) {
                w.add("type", type);
            }
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    public static SortedSet<String> listTypes(ServiceExecutor executor, String cid, String where) throws Throwable {
        StringBuilder sb = new StringBuilder();
        if (cid != null) {
            sb.append("(cid='" + cid + "' or cid starts with '" + cid + "')");
        }
        if (where != null) {
            if (cid != null) {
                sb.append(" and (");
            }
            sb.append(where);
            if (cid != null) {
                sb.append(")");
            }
        }

        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("where", sb.toString());
        dm.add("action", "get-distinct-values");
        dm.add("xpath", "type");

        Collection<String> types = executor.execute("asset.query", dm.root()).values("value");
        if (types == null || types.isEmpty()) {
            return null;
        } else {
            return new TreeSet<String>(types);
        }
    }

}
