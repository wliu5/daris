package daris.plugin.services;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

public class SvcAssetReplicateDestroyedCheck extends arc.mf.plugin.PluginService {

    public static final String SERVICE_NAME = "daris.asset.replicate.destroyed.check";

    public static final int DEFAULT_STEP = 5000;

    public static final int MAX_RESULT_ELEMENTS = 1000;

    public static final String NOTIFICATION_EMAIL_SUBJECT = "DaRIS replicate destroyed check result";

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
                "Action to take when locally destroyed replicas are found. Defaults to count.", 0, 1));
        _defn.add(new Interface.Element("use-indexes", BooleanType.DEFAULT,
                "If true, then use available indexes. If false, then perform linear searching. Defaults to true.", 0,
                1));
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
        boolean useIndexes = args.booleanValue("use-indexes", true);

        long idx = 1;
        boolean complete = false;
        Summary summary = new Summary(peer, peerRoute, dst);
        do {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("where", where);
            dm.add("idx", idx);
            dm.add("size", step);
            dm.add("include-destroyed", true);
            dm.add("action", "get-rid");
            dm.add("use-indexes", useIndexes);
            PluginTask.checkIfThreadTaskAborted();
            XmlDoc.Element re = executor().execute(peerRoute, "asset.query", dm.root());
            check(executor(), re, uuid, schemaID, peerRoute, summary, action);
            idx += step;
            complete = re.booleanValue("cursor/total/@complete");
        } while (!complete);
        if (summary.haveResult()) {
            w.add(summary.result(), false);
        }
        w.add("destroyed", summary.destroyed);
        w.add("total", summary.total);

        if (email != null) {
            sendEmail(executor(), summary, email);
        }
    }

    private class Summary {
        final long startTime;
        final String peer;
        final ServerRoute peerRoute;
        final String dstNamespace;
        final AtomicLong total = new AtomicLong(0);
        final AtomicLong destroyed = new AtomicLong(0);
        private AtomicInteger _nbElements = new AtomicInteger(0);

        private int _maxElements = MAX_RESULT_ELEMENTS;

        private XmlDocMaker _dm;

        Summary(String peer, ServerRoute peerRoute, String dstNamespace) {
            this(peer, peerRoute, dstNamespace, MAX_RESULT_ELEMENTS);
        }

        Summary(String peer, ServerRoute peerRoute, String dstNamespace, int maxElements) {
            this.startTime = System.currentTimeMillis();
            this.peer = peer;
            this.peerRoute = peerRoute;
            this.dstNamespace = dstNamespace;
            _maxElements = maxElements;
        }

        public void add(String element, String[] attrs, String value) throws Throwable {
            if (_dm == null) {
                _dm = new XmlDocMaker("result");
            }
            int nbElements = _nbElements.getAndIncrement();
            if (nbElements <= _maxElements) {
                _dm.add(element, attrs, value);
            }
        }

        public XmlDoc.Element result() {
            return _dm == null ? null : _dm.root();
        }

        public boolean haveResult() {
            return result() != null && result().elementExists("id");
        }

        public boolean isResultComplete() throws Throwable {
            XmlDoc.Element re = result();
            if (re != null) {
                int n = re.count("id");
                return n > 0 && n == destroyed.get();
            }
            return false;
        }
    }

    private void check(ServiceExecutor executor, XmlDoc.Element re, long uuid, Long schemaID, ServerRoute peerRoute,
            Summary summary, String action) throws Throwable {

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
        summary.total.getAndAdd(count);

        Set<String> destroyed = new LinkedHashSet<String>();
        for (XmlDoc.Element ee : ees) {
            boolean exists = ee.booleanValue();
            String rid = ee.value("@id");
            if (!exists) {
                summary.destroyed.getAndIncrement();
                destroyed.add(rid);
            }
        }

        if ("get-id".equalsIgnoreCase(action)) {
            if (!destroyed.isEmpty()) {
                for (XmlDoc.Element ride : rides) {
                    String rid = ride.value();
                    if (destroyed.contains(rid)) {
                        summary.add("id", new String[] { "destroyed", "true" }, ride.value("@id"));
                    }
                }
            }
        }
        PluginTask.threadTaskCompleted(count);
    }

    static void sendEmail(ServiceExecutor executor, Summary summary, String email) throws Throwable {
        StringBuilder sb = new StringBuilder();
        String startTime = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z").format(new Date(summary.startTime));
        sb.append(String.format("%32s: %s\n", "Execution started", startTime));
        sb.append(String.format("%32s: %s\n", "Peer(" + summary.peerRoute.target() + ")", summary.peer));
        if (summary.dstNamespace != null) {
            sb.append(String.format("%32s: %s\n", "Peer namespace", summary.dstNamespace));
        }
        sb.append(String.format("%32s: %s\n", "Destroyed locally", summary.destroyed.get()));
        sb.append(String.format("%32s: %s\n", "Total checked", summary.total.get()));

        XmlDoc.Element re = summary.result();
        if (re != null) {
            List<XmlDoc.Element> ides = re.elements("id");
            if (ides != null && !ides.isEmpty()) {
                sb.append("\n").append("Replica assets destroyed locally");
                if (!summary.isResultComplete()) {
                    sb.append("(incomplete)");
                }
                sb.append(":\n");
                for (XmlDoc.Element ide : ides) {
                    SvcAssetReplicateCheck.saveXmlElementAsText(sb, ide, 4, 4);
                }
            }
        }
        SvcAssetReplicateCheck.sendEmail(executor, NOTIFICATION_EMAIL_SUBJECT + " [" + startTime + "]", sb.toString(),
                email);
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
