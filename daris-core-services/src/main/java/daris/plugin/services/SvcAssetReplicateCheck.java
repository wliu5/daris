package daris.plugin.services;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    public static final int DEFAULT_STEP = 5000;

    public static final int MAX_RESULT_ELEMENTS = 1000;

    public static final String NOTIFICATION_EMAIL_SUBJECT = "DaRIS replicate check result";

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
        _defn.add(new Interface.Element("use-indexes", BooleanType.DEFAULT,
                "If true, then use available indexes. If false, then perform linear searching. Defaults to true."));
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
        boolean useIndexes = args.booleanValue("use-indexes", true);

        long uuid = IDUtils.serverUUID(executor());
        Long schemaID = IDUtils.schemaID(executor());

        long idx = 1;
        boolean complete = false;
        Summary summary = new Summary(where, peer, peerRoute);
        do {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("where", where);
            dm.add("idx", idx);
            dm.add("size", step);
            dm.add("include-destroyed", includeDestroyed);
            if (compare) {
                dm.add("action", "get-meta");
            }
            dm.add("use-indexes",useIndexes);
            PluginTask.checkIfThreadTaskAborted();
            XmlDoc.Element re = executor().execute("asset.query", dm.root());
            check(executor(), re, uuid, schemaID, peerRoute, summary, action);
            idx += step;
            complete = re.booleanValue("cursor/total/@complete");
        } while (!complete);

        XmlDoc.Element re = summary.result();
        if (re != null && re.elementExists("id")) {
            w.addAll(re.elements("id"));
        }
        w.add("missing", summary.missing);
        long differ = summary.differ.get();
        w.add("replicated", new String[] { "differ", differ > 0 ? Long.toString(differ) : null }, summary.replicated);
        w.add("total", summary.total);

        if (email != null) {
            sendEmail(executor(), summary, email);
        }
    }

    private class Summary {
        public final String where;
        public final String peer;
        public final ServerRoute peerRoute;
        public final long startTime;

        AtomicLong total = new AtomicLong(0);
        AtomicLong replicated = new AtomicLong(0);
        AtomicLong missing = new AtomicLong(0);
        AtomicLong differ = new AtomicLong(0);

        private XmlDocMaker _dm;
        private AtomicInteger _nbElements = new AtomicInteger(0);
        private int _maxElements = MAX_RESULT_ELEMENTS;

        public Summary(String where, String peer, ServerRoute peerRoute, int maxElements) {
            this.where = where;
            this.peer = peer;
            this.peerRoute = peerRoute;
            _maxElements = maxElements;
            this.startTime = System.currentTimeMillis();
        }

        public Summary(String where, String peer, ServerRoute peerRoute) {
            this(where, peer, peerRoute, MAX_RESULT_ELEMENTS);
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

        public boolean hasReplicatedAssetsDiffer() {
            return differ.get() > 0;
        }

        public boolean isResultComplete() throws Throwable {
            XmlDoc.Element re = result();
            if (re != null) {
                int n = re.count("id");
                return n > 0 && n == differ.get() + missing.get();
            }
            return false;
        }
    }

    private void check(ServiceExecutor executor, XmlDoc.Element re, long uuid, Long schemaID, ServerRoute peerRoute,
            Summary summary, String action) throws Throwable {
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
        summary.total.getAndAdd(count);

        for (XmlDoc.Element ee : ees) {
            boolean exists = ee.booleanValue();
            String assetId = IDUtils.assetIdFromRID(ee.value("@rid"));
            if (exists) {
                summary.replicated.getAndIncrement();
                XmlDoc.Element ae = re.element("asset[@id='" + assetId + "']");
                if (ae != null) {
                    // compare
                    PluginTask.checkIfThreadTaskAborted();
                    XmlDoc.Element rae = executor.execute(peerRoute, "asset.get",
                            "<args><id>" + ee.value("@id") + "</id></args>", null, null).element("asset");
                    String differ = differ(ae, rae);
                    if (differ != null) {
                        summary.differ.getAndIncrement();
                        if ("get-id".equalsIgnoreCase(action)) {
                            summary.add("id", new String[] { "differ", differ, "cid",
                                    re.value("asset[@id='" + assetId + "']/cid") }, assetId);
                        }
                    }
                }
            } else {
                summary.missing.getAndIncrement();
                if ("get-id".equalsIgnoreCase(action)) {
                    String cid = re.value("asset[@id='" + assetId + "']/cid");
                    if (cid == null) {
                        cid = IDUtils.cidFromAssetId(executor, assetId);
                    }
                    summary.add("id", new String[] { "missing", "true", "cid", cid }, assetId);
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

    static void sendEmail(ServiceExecutor executor, Summary summary, String email) throws Throwable {
        StringBuilder sb = new StringBuilder();
        String startTime = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z").format(new Date(summary.startTime));
        sb.append(String.format("%32s: %s\n", "Execution started", startTime));
        sb.append(String.format("%32s: %s\n", "Selection query", summary.where));
        sb.append(String.format("%32s: %s\n", "Peer(" + summary.peerRoute.target() + ")", summary.peer));
        sb.append(String.format("%32s: %s\n", "Replicated", summary.replicated));
        sb.append(String.format("%32s: %s\n", "Missing", summary.missing));
        sb.append(String.format("%32s: %s\n", "Total", summary.total));
        if (summary.hasReplicatedAssetsDiffer()) {
            sb.append(String.format("%32s: %s\n", "Differ", summary.differ));
        }

        XmlDoc.Element re = summary.result();
        if (re != null) {
            List<XmlDoc.Element> ides = re.elements("id");
            if (ides != null && !ides.isEmpty()) {
                sb.append("\n").append("Assets not replicated or replicas differ");
                if (!summary.isResultComplete()) {
                    sb.append("(incomplete)");
                }
                sb.append(":\n");
                for (XmlDoc.Element ide : ides) {
                    saveXmlElementAsText(sb, ide, 4, 4);
                }
            }
        }
        sendEmail(executor, NOTIFICATION_EMAIL_SUBJECT + " [" + startTime + "]", sb.toString(), email);
    }

    static void saveXmlElementAsText(StringBuilder sb, XmlDoc.Element e, int indent, int tabSize) throws Throwable {
        sb.append(String.format("%" + indent + "s:%s", "", e.name()));
        List<XmlDoc.Attribute> attrs = e.attributes();
        if (attrs != null) {
            for (XmlDoc.Attribute attr : attrs) {
                sb.append(" -").append(attr.name()).append(" \"").append(attr.value()).append("\"");
            }
        }
        if (e.value() != null) {
            sb.append(" \"").append(e.value()).append("\"");
        }
        sb.append("\n");
        if (e.hasSubElements()) {
            List<XmlDoc.Element> ses = e.elements();
            for (XmlDoc.Element se : ses) {
                saveXmlElementAsText(sb, se, indent + tabSize, tabSize);
            }
        }
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
