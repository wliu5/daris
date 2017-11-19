package daris.plugin.services;

import java.util.Collection;
import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.AssetType;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import nig.mf.pssd.CiteableIdUtil;

public class SvcObjectTagAdd extends PluginService {

    public static final String SERVICE_NAME = "daris.object.tag.add";
    public static final String DICTIONARY_PREFIX = "daris:tags.";

    private Interface _defn;

    public SvcObjectTagAdd() {
        _defn = new Interface();
        _defn.add(new Interface.Element("id", AssetType.DEFAULT, "Asset id of the object.", 0, Integer.MAX_VALUE));

        Interface.Element cid = new Interface.Element("cid", AssetType.DEFAULT, "Citeable id of the object.", 0,
                Integer.MAX_VALUE);
        cid.add(new Interface.Attribute("recursive", BooleanType.DEFAULT, "Include descendants.", 0));
        _defn.add(cid);

        _defn.add(new Interface.Element("tag", StringType.DEFAULT, "Tag to apply.", 1, 1));
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
        String cid = executor.execute("asset.identifier.get", "<args><id>" + assetId + "</id></args>", null, null)
                .value("@cid");
        if (cid == null) {
            throw new IllegalArgumentException("Asset " + assetId + " is not a valid DaRIS object. No cid is found.");
        }
        addTag(cid, tag, false, executor);
    }

    static void addTag(String cid, String tag, boolean recursive, ServiceExecutor executor) throws Throwable {
        String projectCid = CiteableIdUtil.getProjectId(cid);
        String dictionary = DICTIONARY_PREFIX + projectCid;
        if (!DictionaryUtils.dictionaryExists(executor, dictionary)) {
            DictionaryUtils.createDictionary(executor, dictionary, null);
        }
        if (!DictionaryUtils.dictionaryEntryExists(executor, dictionary, tag, null)) {
            DictionaryUtils.addDictionaryEntry(executor, dictionary, tag, null);
        }
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.push("tag");
        dm.add("name", new String[] { "dictionary", dictionary }, tag);
        dm.pop();
        dm.add("cid", cid);
        if (recursive) {
            dm = new XmlDocMaker("args");
            dm.add("where", "cid starts with '" + cid + "'");
            dm.add("size", "infinity");
            dm.add("action", "get-cid");
            Collection<String> descendants = executor.execute("asset.query", dm.root()).values("cid");
            if (descendants != null) {
                for (String descendant : descendants) {
                    addTag(descendant, tag, false, executor);
                }
            }
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
