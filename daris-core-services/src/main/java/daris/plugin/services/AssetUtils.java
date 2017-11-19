package daris.plugin.services;

import java.util.AbstractMap.SimpleEntry;

import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;

public class AssetUtils {

    static SimpleEntry<String, String> getAssetIdentifiers(ServiceExecutor executor, String id, String cid)
            throws Throwable {
        if (id == null && cid == null) {
            throw new IllegalArgumentException("Missing argument cid or id.");
        }
        if (id != null && cid != null) {
            throw new IllegalArgumentException("Expect argument id or cid, but not both.");
        }
        if (id != null) {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("id", id);
            cid = executor.execute("asset.identifier.get", dm.root()).value("id/@cid");
            if (cid == null) {
                // NOTE: every daris/pssd object should have a cid (except for
                // attachments...)
                throw new Exception("Asset " + id + " does not have cid. Not a valid DaRIS object.");
            }
        }
        if (cid != null) {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("cid", cid);
            id = executor.execute("asset.identifier.get", dm.root()).value("id");
        }
        return new SimpleEntry<String, String>(id, cid);
    }

    static SimpleEntry<String, String> getAssetIdentifiers(ServiceExecutor executor, XmlDoc.Element args)
            throws Throwable {
        String id = args.value("id");
        String cid = args.value("cid");
        if (id == null && cid == null) {
            throw new IllegalArgumentException("Missing argument cid or id.");
        }
        if (id != null && cid != null) {
            throw new IllegalArgumentException("Expects argument cid or id, but not both.");
        }
        return getAssetIdentifiers(executor, id, cid);
    }

    static XmlDoc.Element getAssetMeta(ServiceExecutor executor, String id, String cid) throws Throwable {
        if (id != null && cid != null) {
            throw new IllegalArgumentException("Expects cid or id, but not both.");
        }
        XmlDocMaker dm = new XmlDocMaker("args");
        if (id != null) {
            dm.add("id", id);
        } else {
            dm.add("cid", cid);
        }
        return executor.execute("asset.get", dm.root()).element("asset");
    }

    static boolean assetExists(ServiceExecutor executor, String id) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("id", id);
        return executor.execute("asset.exists", dm.root()).booleanValue("exists");
    }

}
