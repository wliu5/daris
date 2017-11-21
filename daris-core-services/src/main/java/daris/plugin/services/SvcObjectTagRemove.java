package daris.plugin.services;

import java.util.Collection;
import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

//TODO test
public class SvcObjectTagRemove extends PluginService {

    public static final String SERVICE_NAME = "daris.object.tag.remove";

    private Interface _defn;

    public SvcObjectTagRemove() {
        _defn = new Interface();
        SvcObjectTagAdd.addToDefn(_defn);
    }

    @Override
    public Access access() {
        return ACCESS_MODIFY;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Remove tag from DaRIS objects.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String tag = args.value("tag");
        if (args.elementExists("id")) {
            Collection<String> ids = args.values("id");
            for (String id : ids) {
                removeTag(executor(), id, tag);
            }
        } else if (args.elementExists("cid")) {
            List<XmlDoc.Element> cides = args.elements("cid");
            for (XmlDoc.Element cide : cides) {
                String cid = cide.value();
                boolean recursive = cide.booleanValue("@recursive", false);
                removeTag(cid, tag, recursive, executor());
            }
        } else {
            throw new IllegalArgumentException("Missing id or cid.");
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    static void removeTag(ServiceExecutor executor, String assetId, String tag) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("id", assetId);
        dm.add("name", tag);
        executor.execute("asset.tag.remove", dm.root());
    }

    static void removeTag(String cid, String tag, boolean recursive, ServiceExecutor executor) throws Throwable {
        StringBuilder sb = new StringBuilder("cid='").append(cid).append("'");
        if (recursive) {
            sb.append(" or cid starts with '").append(cid).append("'");
        }

        XmlDocMaker dm = new XmlDocMaker("args");
        dm = new XmlDocMaker("args");
        dm.add("where", sb.toString());
        dm.add("action", "pipe");
        dm.push("service", new String[] { "name", "asset.tag.remove" });
        dm.add("name", tag);
        dm.pop();
        executor.execute("asset.query", dm.root());
    }
}
