package daris.plugin.services;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import arc.mf.plugin.PluginTask;
import arc.mf.plugin.ServerRoute;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.EmailAddressType;
import arc.mf.plugin.dtype.EnumType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcAssetReplicateDestroyedCheck extends arc.mf.plugin.PluginService {

    public static final String SERVICE_NAME = "daris.asset.replicate.destroyed.check";

    public static final int DEFAULT_STEP = 100;

    private Interface _defn;

    public SvcAssetReplicateDestroyedCheck() {
        _defn = new Interface();
        _defn.add(new Interface.Element("peer", StringType.DEFAULT,
                " The name of the remote peer (server). Run server.peer.list to see all connected peers.", 1, 1));
        _defn.add(new Interface.Element("dst", StringType.DEFAULT,
                " The root namespace on the remote peer (server). If not specified, check all assets on the remote peer.",
                0, 1));
        _defn.add(new Interface.Element("step", IntegerType.POSITIVE_ONE,
                "The size of the asset.query result set. Defaults to " + DEFAULT_STEP, 0, 1));
        _defn.add(new Interface.Element("email", EmailAddressType.DEFAULT,
                "Set to true to list the assets that are missing or differ on the peer system. Defaults to false.", 0,
                1));
        _defn.add(new Interface.Element("action", new EnumType(new String[] { "get-id", "count" }),
                "Action to take when locally destroyed replicas are found. Defaults to count."));
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
        return "Checks the replica assets on the remote peers to find the ones already destroyed locally.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String peer = args.value("peer");
        ServerRoute peerRoute = new ServerRoute(
                executor().execute("server.peer.status", "<args><name>" + peer + "</name></args>", null, null)
                        .value("peer/@uuid"));
        String dst = args.value("dst");
        int step = args.intValue("step", DEFAULT_STEP);

        String email = args.value("email");
        String action = args.stringValue("action", "count");

        long uuid = IDUtils.serverUUID(executor());
        Long schemaID = IDUtils.schemaID(executor());

        StringBuilder where = new StringBuilder();
        where.append("rid in '");
        where.append(uuid);
        if (schemaID != null) {
            where.append(".").append(schemaID);
        }
        where.append("'");
        if (dst != null) {
            where.append(" and namespace>='").append(dst).append("'");
        }

        long idx = 1;
        boolean complete = false;
        Summary summary = new Summary();
        do {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("where", where);
            dm.add("idx", idx);
            dm.add("size", step);
            dm.add("include-destroyed", true);
            dm.add("action", "get-rid");
            PluginTask.checkIfThreadTaskAborted();
            XmlDoc.Element re = executor().execute(peerRoute, "asset.query", dm.root());
            check(executor(), re, uuid, schemaID, peerRoute, summary, "get-id".equals(action) ? w : null);
            idx += step;
            complete = re.booleanValue("cursor/total/@complete");
        } while (!complete);
        w.add("destroyed", summary.destroyed);
        w.add("total", summary.total);

        if (email != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("         Peer(" + peerRoute.target() + "): ").append(peer).append("\n");
            sb.append("      DST Namespace: ").append(dst).append("\n");
            sb.append("      Total Checked: ").append(summary.total).append("\n");
            sb.append("          Destoryed: ").append(summary.destroyed).append("\n");
            SvcAssetReplicateCheck.sendEmail(executor(), "Replicate Destroyed Check Result", sb.toString(), email);
        }
    }

    private class Summary {
        long total = 0;
        long destroyed = 0;
        long synchronised = 0;
        
        // TODO
    }

    private void check(ServiceExecutor executor, XmlDoc.Element re, long uuid, Long schemaID, ServerRoute peerRoute,
            Summary summary, XmlWriter w) throws Throwable {

        long count = re.longValue("cursor/count");
        if (count == 0) {
            return;
        }

        List<XmlDoc.Element> rides = re.elements("rid");
        if (rides == null || rides.isEmpty()) {
            return;
        }

        XmlDocMaker dm = new XmlDocMaker("args");
        for (XmlDoc.Element ride : rides) {
            String rid = ride.value();
            dm.add("id", rid);
        }
        PluginTask.checkIfThreadTaskAborted();
        List<XmlDoc.Element> ees = executor.execute("asset.exists", dm.root()).elements("exists");

        assert count == ees.size();
        summary.total += count;

        Set<String> destroyed = new LinkedHashSet<String>();
        for (XmlDoc.Element ee : ees) {
            boolean exists = ee.booleanValue();
            String rid = ee.value("@id");
            if (!exists) {
                summary.destroyed++;
                destroyed.add(rid);
            }
        }

        if (w != null) {
            if (!destroyed.isEmpty()) {
                for (XmlDoc.Element ride : rides) {
                    String rid = ride.value();
                    if (destroyed.contains(rid)) {
                        w.add("id", new String[] { "destroyed", "true" }, ride.value("@id"));
                    }
                }
            }
        }
        PluginTask.threadTaskCompleted(count);
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
