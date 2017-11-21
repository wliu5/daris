package daris.plugin.services;

import java.util.Collection;
import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.AssetType;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import nig.mf.plugin.pssd.Project;
import nig.mf.pssd.CiteableIdUtil;

// TODO test

public class SvcObjectTagAdd extends PluginService {

    public static final String SERVICE_NAME = "daris.object.tag.add";
    public static final String DICTIONARY_NAME = "daris.object.tag";

    private Interface _defn;

    public SvcObjectTagAdd() {
        _defn = new Interface();
        addToDefn(_defn);
    }

    static void addToDefn(Interface defn) {
        defn.add(new Interface.Element("id", AssetType.DEFAULT, "Asset id of the object.", 0, Integer.MAX_VALUE));

        Interface.Element cid = new Interface.Element("cid", CiteableIdType.DEFAULT, "Citeable id of the object.", 0,
                Integer.MAX_VALUE);
        cid.add(new Interface.Attribute("recursive", BooleanType.DEFAULT, "Include descendants. Defautls to false.",
                0));
        defn.add(cid);

        defn.add(new Interface.Element("tag", StringType.DEFAULT, "Tag name.", 1, 1));
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
        return "Add tag to the specified object.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String tag = args.value("tag");
        if (args.elementExists("id")) {
            Collection<String> ids = args.values("id");
            for (String id : ids) {
                addTag(executor(), id, tag);
            }
        } else if (args.elementExists("cid")) {
            List<XmlDoc.Element> cides = args.elements("cid");
            for (XmlDoc.Element cide : cides) {
                String cid = cide.value();
                boolean recursive = cide.booleanValue("@recursive", false);
                addTag(cid, tag, recursive, executor());
            }
        } else {
            throw new IllegalArgumentException("Missing id or cid.");
        }
    }

    static void addTag(ServiceExecutor executor, String assetId, String tag) throws Throwable {
        String cid = AssetUtils.getCiteableId(executor, assetId);
        if (cid == null) {
            throw new IllegalArgumentException("Asset " + assetId + " is not a valid DaRIS object. No cid is found.");
        }
        addTag(cid, tag, false, executor);
    }

    static void addTag(String cid, String tag, boolean recursive, ServiceExecutor executor) throws Throwable {
        String projectCid = CiteableIdUtil.getProjectId(cid);
        String dictionaryNS = Project.projectSpecificDictionaryNamespaceOf(projectCid);
        String dictionary = dictionaryNS + ":" + DICTIONARY_NAME;
        if (!DictionaryUtils.dictionaryExists(executor, dictionary)) {
            DictionaryUtils.createDictionary(executor, dictionary, null);
        }
        if (!DictionaryUtils.dictionaryEntryExists(executor, dictionary, tag, null)) {
            DictionaryUtils.addDictionaryEntry(executor, dictionary, tag, null);
        }

        StringBuilder sb = new StringBuilder("cid='").append(cid).append("'");
        if (recursive) {
            sb.append(" or cid starts with '").append(cid).append("'");
        }

        XmlDocMaker dm = new XmlDocMaker("args");
        dm = new XmlDocMaker("args");
        dm.add("where", sb.toString());
        dm.add("action", "pipe");
        dm.push("service", new String[] { "name", "asset.tag.add" });
        dm.push("tag");
        dm.add("name", new String[] { "dictionary", dictionary }, tag);
        dm.pop();
        dm.pop();
        executor.execute("asset.query", dm.root());
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
