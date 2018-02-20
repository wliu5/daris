package daris.plugin.services;

import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDocMaker;

public class IDUtils {

    public static String rid(long uuid, Long schemaId, long assetId) {
        StringBuilder sb = new StringBuilder();
        sb.append(uuid).append(".");
        if (schemaId != null) {
            sb.append(schemaId).append(".");
        }
        sb.append(assetId);
        return sb.toString();
    }

    public static String rid(ServiceExecutor executor, long assetId) throws Throwable {
        return rid(serverUUID(executor), schemaID(executor), assetId);
    }

    public static String rid(ServiceExecutor executor, String assetId) throws Throwable {
        return rid(executor, assetId(executor, assetId));
    }

    public static long serverUUID(ServiceExecutor executor) throws Throwable {
        return executor.execute("server.uuid").longValue("uuid");
    }

    public static Long schemaID(ServiceExecutor executor) throws Throwable {
        return executor.execute("schema.self.describe").longValue("schema/@id", null);
    }

    public static long assetId(ServiceExecutor executor, String assetId) throws Throwable {
        if (assetId.startsWith("path=")) {
            return assetIdFromPath(executor, assetId.substring(5));
        } else {
            return Long.parseLong(assetId);
        }
    }

    public static long assetIdFromPath(ServiceExecutor executor, String assetPath) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("path", assetPath);
        return executor.execute("asset.identifier.get", dm.root()).longValue("id");
    }

    public static long assetIdFromCID(ServiceExecutor executor, String cid) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("cid", cid);
        return executor.execute("asset.identifier.get", dm.root()).longValue("id");
    }

    public static String cidFromAssetId(ServiceExecutor executor, String assetId) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("id", assetId);
        return executor.execute("asset.identifier.get", dm.root()).value("id/@cid");
    }

    public static String assetIdFromRID(String rid) {
        int idx = rid.lastIndexOf('.');
        return rid.substring(idx + 1);
    }

}
