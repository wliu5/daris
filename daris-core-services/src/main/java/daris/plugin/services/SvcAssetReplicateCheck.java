package daris.plugin.services;

import java.util.Collection;
import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginTask;
import arc.mf.plugin.ServerRoute;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.EmailAddressType;
import arc.mf.plugin.dtype.EnumType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import daris.util.StringUtils;

public class SvcAssetReplicateCheck extends PluginService {

    public static final String SERVICE_NAME = "daris.asset.replicate.check";

    public static final int DEFAULT_STEP = 100;

    private Interface _defn;

    public SvcAssetReplicateCheck() {
        _defn = new Interface();
        _defn.add(new Interface.Element("where", StringType.DEFAULT, "Query to select the source assets to check.", 1,
                1));
        _defn.add(new Interface.Element("include-destroyed", BooleanType.DEFAULT,
                "Include assets that have been marked as destroyed? Defaults to true.", 0, 1));
        _defn.add(new Interface.Element("peer", StringType.DEFAULT,
                " The name of the remote peer (server). Run server.peer.list to see all connected peers.", 1, 1));
        _defn.add(new Interface.Element("step", IntegerType.POSITIVE_ONE,
                "The size of the asset.query result set. Defaults to " + DEFAULT_STEP, 0, 1));
        _defn.add(new Interface.Element("compare", BooleanType.DEFAULT,
                "Set to true to compare mtime, checksum. Defaults to false.", 0, 1));
        _defn.add(new Interface.Element("email", EmailAddressType.DEFAULT,
                "Set to true to list the assets that are missing or differ on the peer system. Defaults to false.", 0,
                1));
        _defn.add(new Interface.Element("action", new EnumType(new String[] { "count", "get-id" }),
                "Action to take when found assets that are not replicated. Defaults to 'count'. Set to 'get-cid' to list the assets.",
                0, 1));
    }

    @Override
    public Access access() {
        return ACCESS_ADMINISTER;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Checks if the assets in the specified source collection have been replicated to the remote peer.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {

        String where = args.value("where");
        String peer = args.value("peer");
        ServerRoute peerRoute = new ServerRoute(
                executor().execute("server.peer.status", "<args><name>" + peer + "</name></args>", null, null)
                        .value("peer/@uuid"));
        int step = args.intValue("step", DEFAULT_STEP);
        boolean includeDestroyed = args.booleanValue("include-destroyed", true);
        String action = args.stringValue("action", "count");
        boolean compare = args.booleanValue("compare", false);
        String email = args.value("email");

        long uuid = IDUtils.serverUUID(executor());
        Long schemaID = IDUtils.schemaID(executor());

        long idx = 1;
        boolean complete = false;
        Summary summary = new Summary();
        do {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("where", where);
            dm.add("idx", idx);
            dm.add("size", step);
            dm.add("include-destroyed", includeDestroyed);
            if (compare) {
                dm.add("action", "get-meta");
            }
            PluginTask.checkIfThreadTaskAborted();
            XmlDoc.Element re = executor().execute("asset.query", dm.root());
            check(executor(), re, uuid, schemaID, peerRoute, summary, "get-id".equals(action) ? w : null);
            idx += step;
            complete = re.booleanValue("cursor/total/@complete");
        } while (!complete);
        w.add("missing", summary.missing);
        w.add("replicated", new String[] { "differ", summary.differ > 0 ? Long.toString(summary.differ) : null },
                summary.replicated);
        w.add("total", summary.total);

        if (email != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("     Where: ").append(where).append("\n");
            sb.append("Peer(" + peerRoute.target() + "): ").append(peer).append("\n");
            sb.append("Replicated: ").append(summary.replicated).append("\n");
            sb.append("   Missing: ").append(summary.missing).append("\n");
            sb.append("     Total: ").append(summary.total).append("\n");
            if (summary.differ > 0) {
                sb.append("    Differ: ").append(summary.differ).append("\n");
            }
            sendEmail(executor(), "Replicate Check Result", sb.toString(), email);
        }
    }

    private class Summary {
        long total = 0;
        long replicated = 0;
        long missing = 0;
        long differ = 0;
    }

    private void check(ServiceExecutor executor, XmlDoc.Element re, long uuid, Long schemaID, ServerRoute peerRoute,
            Summary summary, XmlWriter w) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        long count = re.longValue("cursor/count");
        if (count == 0) {
            return;
        }
        if (re.elementExists("asset")) {
            List<XmlDoc.Element> aes = re.elements("asset");
            for (XmlDoc.Element ae : aes) {
                dm.add("rid", IDUtils.rid(uuid, schemaID, ae.longValue("@id")));
            }
        } else if (re.elementExists("id")) {
            Collection<Long> assetIds = re.longValues("id");
            for (Long assetId : assetIds) {
                dm.add("rid", IDUtils.rid(executor, assetId));
            }
        }
        PluginTask.checkIfThreadTaskAborted();
        List<XmlDoc.Element> ees = executor.execute(peerRoute, "asset.exists", dm.root()).elements("exists");

        assert count == ees.size();
        summary.total += count;

        for (XmlDoc.Element ee : ees) {
            boolean exists = ee.booleanValue();
            String assetId = IDUtils.assetIdFromRID(ee.value("@rid"));
            if (exists) {
                summary.replicated++;
                XmlDoc.Element ae = re.element("asset[@id='" + assetId + "']");
                if (ae != null) {
                    // compare
                    PluginTask.checkIfThreadTaskAborted();
                    XmlDoc.Element rae = executor.execute(peerRoute, "asset.get",
                            "<args><id>" + ee.value("@id") + "</id></args>", null, null).element("asset");
                    String differ = differ(ae, rae);
                    if (differ != null) {
                        summary.differ++;
                        if (w != null) {
                            w.add("id", new String[] { "differ", differ, "cid",
                                    re.value("asset[@id='" + assetId + "']/cid") }, assetId);
                        }
                    }
                }
            } else {
                summary.missing++;
                if (w != null) {
                    String cid = re.value("asset[@id='" + assetId + "']/cid");
                    if (cid == null) {
                        cid = IDUtils.cidFromAssetId(executor, assetId);
                    }
                    w.add("id", new String[] { "missing", "true", "cid", cid }, assetId);
                }
            }
        }
        PluginTask.threadTaskCompleted(count);
    }

    private String differ(XmlDoc.Element ae, XmlDoc.Element rae) throws Throwable {
        StringBuilder differ = new StringBuilder();
        if (ae.longValue("mtime/@millisec") != rae.longValue("mtime/@millisec")) {
            StringUtils.append(differ, ",", "mtime");
        }
        if (!ObjectUtils.equals(ae.value("cid"), rae.value("cid"))) {
            StringUtils.append(differ, ",", "cid");
        }
        if (!ObjectUtils.equals(ae.value("content/size"), rae.value("content/size"))) {
            StringUtils.append(differ, ",", "csize");
        }
        if (!ObjectUtils.equals(ae.value("content/csum"), rae.value("content/csum"))) {
            StringUtils.append(differ, ",", "csum");
        }
        return differ.length() > 0 ? differ.toString() : null;
    }

    static void sendEmail(ServiceExecutor executor, String subject, String body, String email) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        if (subject != null) {
            dm.add("subject", subject);
        }
        if (body != null) {
            dm.add("body", body);
        }
        dm.add("to", email);
        dm.add("async", false);
        executor.execute("mail.send", dm.root());
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    @Override
    public boolean canBeAborted() {
        return true;
    }

}
