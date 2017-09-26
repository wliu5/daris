package daris.plugin.services;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcCollectionTranscodeList extends PluginService {

    public static final String SERVICE_NAME = "daris.collection.transcode.list";

    private Interface _defn;

    public SvcCollectionTranscodeList() {
        _defn = new Interface();
        _defn.add(new Interface.Element("cid", CiteableIdType.DEFAULT, "The citeable id of the root/parent object.", 1,
                1));
        _defn.add(new Interface.Element("where", StringType.DEFAULT,
                "the query to filter/find the objects to be included.", 0, 1));
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
        return "List the available transcodes for the objects in the specified collection.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String cid = args.value("cid");
        String where = args.value("where");
        listTranscodes(executor(), cid, where, w);
    }

    public static void listTranscodes(ServiceExecutor executor, String cid, String where, XmlWriter w)
            throws Throwable {
        Map<String, Set<String>> transcodes = transcodesFor(executor, cid, where);
        if (transcodes != null && !transcodes.isEmpty()) {
            Set<String> fromTypes = transcodes.keySet();
            for (String fromType : fromTypes) {
                w.push("transcode", new String[] { "from", fromType });
                Set<String> toTypes = transcodes.get(fromType);
                if (toTypes != null && !toTypes.isEmpty()) {
                    for (String toType : toTypes) {
                        w.add("to", toType);
                    }
                }
                w.pop();
            }
        }
    }

    public static Map<String, Set<String>> transcodesFor(ServiceExecutor executor, String cid, String where)
            throws Throwable {
        SortedSet<String> types = SvcCollectionTypeList.listTypes(executor, cid,
                where == null ? "asset has content" : ("(" + where + ") and asset has content"));
        if (types != null) {
            return transcodesFor(executor, types);
        } else {
            return null;
        }
    }

    public static Map<String, Set<String>> transcodesFor(ServiceExecutor executor, Collection<String> fromTypes)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        for (String fromType : fromTypes) {
            dm.add("from", fromType);
        }
        XmlDoc.Element re = executor.execute("asset.transcode.describe", dm.root());
        List<XmlDoc.Element> tes = re.elements("transcode");
        if (tes != null && !tes.isEmpty()) {
            Map<String, Set<String>> transcodes = new TreeMap<String, Set<String>>();
            for (XmlDoc.Element te : tes) {
                String from = te.value("from");
                String to = te.value("to");
                Set<String> toTypes = transcodes.get(from);
                if (toTypes == null) {
                    toTypes = new TreeSet<String>();
                    transcodes.put(from, toTypes);
                }
                toTypes.add(to);
            }
            return transcodes;
        }
        return null;
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
