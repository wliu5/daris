package daris.plugin.asset.path;

import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import nig.mf.pssd.client.util.CiteableIdUtil;

public class AssetPathUtils {

    static String getRelatedMetadataValue(ServiceExecutor executor, XmlDoc.Element ae, String type, String xpath,
            String defaultValue) {
        try {
            String assetId = ae.value("related[@type='" + type + "']/to");
            if (assetExists(executor, assetId)) {
                XmlDoc.Element rae = getAssetMeta(executor, assetId);
                return rae.stringValue(xpath, defaultValue);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean assetExists(ServiceExecutor executor, String assetId) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("id", assetId);
        return executor.execute("asset.exists", dm.root()).booleanValue("exists", false);
    }

    static XmlDoc.Element getAssetMeta(ServiceExecutor executor, String assetId) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("id", assetId);
        return executor.execute("asset.get", dm.root()).element("asset");
    }

    static String getParentMetadataValue(ServiceExecutor executor, XmlDoc.Element ae, int depth, String xpath,
            String defaultValue) {
        try {
            String cid = ae.value("cid");
            if (cid != null) {
                String pid = CiteableIdUtil.getParentId(cid, depth);
                if (assetExists(pid, executor)) {
                    XmlDoc.Element pae = getAssetMeta(pid, executor);
                    return pae.stringValue(xpath, defaultValue);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean assetExists(String cid, ServiceExecutor executor) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("cid", cid);
        return executor.execute("asset.exists", dm.root()).booleanValue("exists", false);
    }

    static XmlDoc.Element getAssetMeta(String cid, ServiceExecutor executor) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("cid", cid);
        return executor.execute("asset.get", dm.root()).element("asset");
    }

    static String getMetadataValue(ServiceExecutor executor, String assetId, String expr) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("id", assetId);
        dm.add("expr", expr);
        return executor.execute("asset.path.generate", dm.root()).value("path");
    }

}
