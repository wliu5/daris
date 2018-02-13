package daris.plugin.services;

import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginThread;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcCollectionSummaryGet extends PluginService {

    public static final String SERVICE_NAME = "daris.collection.summary.get";

    private Interface _defn;

    public SvcCollectionSummaryGet() {
        _defn = new Interface();
        _defn.add(new Interface.Element("cid", CiteableIdType.DEFAULT, "The citeable id of the root/parent object.", 0,
                1));
        _defn.add(new Interface.Element("where", StringType.DEFAULT,
                "the query to filter/find the objects to be included.", 0, 1));
        _defn.add(new Interface.Element("async", BooleanType.DEFAULT,
                "Execute the queries asynchronously. It may improve the performance. Defaults to false.", 0, 1));
    }

    @Override
    public Access access() {
        return ACCESS_ACCESS;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Returns the summary of the specified collection.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        String cid = args.value("cid");
        String where = args.value("where");
        if (cid == null && where == null) {
            throw new IllegalArgumentException("Missing cid and where argument.");
        }
        boolean async = args.booleanValue("async", false);
        if (async) {
            executeAsync(cid, where, w);
        } else {
            executeSync(executor(), cid, where, w);
        }
    }

    private static void executeSync(ServiceExecutor executor, String cid, String where, XmlWriter w) throws Throwable {
        String objectQuery = objectQuery(cid, where);
        String attachmentQuery = attachmentQuery(objectQuery);
        String datasetQuery = datasetQuery(objectQuery);
        String dicomDatasetQuery = dicomDatasetQuery(datasetQuery);

        XmlDocMaker dm = new XmlDocMaker("args");
        dm.push("service", new String[] { "name", "asset.query" });
        dm.add("where", objectQuery);
        dm.add("action", "count");
        dm.pop();
        dm.push("service", new String[] { "name", "asset.query" });
        dm.add("where", objectQuery);
        dm.add("xpath", "content/size");
        dm.add("action", "sum");
        dm.pop();
        dm.push("service", new String[] { "name", "asset.query" });
        dm.add("where", attachmentQuery);
        dm.add("xpath", "content/size");
        dm.add("action", "sum");
        dm.pop();
        dm.push("service", new String[] { "name", "asset.query" });
        dm.add("where", datasetQuery);
        dm.add("xpath", "content/size");
        dm.add("action", "sum");
        dm.pop();
        dm.push("service", new String[] { "name", "asset.query" });
        dm.add("where", dicomDatasetQuery);
        dm.add("xpath", "content/size");
        dm.add("action", "sum");
        dm.pop();
        dm.push("service", new String[] { "name", SvcCollectionTranscodeList.SERVICE_NAME });
        dm.add("cid", cid);
        if (where != null) {
            dm.add("where", where);
        }
        dm.pop();

        List<XmlDoc.Element> res = executor.execute("service.execute", dm.root()).elements("reply/response");
        List<XmlDoc.Element> tes = res.get(5).elements("transcode");

        long nbo = res.get(0).longValue("value", 0);
        long nbc = res.get(1).longValue("value/@nbe", 0);
        long csize = res.get(1).longValue("value", 0);
        long nba = res.get(2).longValue("value/@nbe", 0);
        long asize = res.get(2).longValue("value", 0);
        long size = csize + asize;
        long nbd = res.get(3).longValue("value/@nbe", 0);
        long dsize = res.get(3).longValue("value", 0);
        long nbdd = res.get(4).longValue("value/@nbe", 0);
        long ddsize = res.get(4).longValue("value", 0);

        w.add("number-of-objects", nbo);
        w.add("number-of-attachments", nba);
        w.add("number-of-datasets", nbd);
        w.add("number-of-dicom-datasets", nbdd);
        w.add("total-content-size", new String[] { "nbe", Long.toString(nbc) }, csize);
        w.add("total-attachment-size", new String[] { "nbe", Long.toString(nba) }, asize);
        w.add("total-dataset-size", new String[] { "nbe", Long.toString(nbd) }, dsize);
        w.add("total-dicom-dataset-size", new String[] { "nbe", Long.toString(nbdd) }, ddsize);
        w.add("total-size", size);
        if (tes != null) {
            for (XmlDoc.Element te : tes) {
                w.add(te);
            }
        }
    }

    private static void executeAsync(final String cid, final String where, XmlWriter w) throws Throwable {
        final String objectQuery = objectQuery(cid, where);
        final String attachmentQuery = attachmentQuery(objectQuery);
        final String datasetQuery = datasetQuery(objectQuery);
        final String dicomDatasetQuery = dicomDatasetQuery(datasetQuery);

        Worker countObjects = new Worker() {
            @Override
            protected Element execute(ServiceExecutor executor) throws Throwable {
                XmlDocMaker dm = new XmlDocMaker("args");
                dm.add("where", objectQuery);
                dm.add("action", "count");
                return executor.execute("asset.query", dm.root());
            }
        };

        Worker sumContentSize = new Worker() {
            @Override
            protected Element execute(ServiceExecutor executor) throws Throwable {
                XmlDocMaker dm = new XmlDocMaker("args");
                dm.add("where", objectQuery);
                dm.add("action", "sum");
                dm.add("xpath", "content/size");
                return executor.execute("asset.query", dm.root());
            }
        };

        Worker sumAttachmentSize = new Worker() {
            @Override
            protected Element execute(ServiceExecutor executor) throws Throwable {
                XmlDocMaker dm = new XmlDocMaker("args");
                dm.add("where", attachmentQuery);
                dm.add("action", "sum");
                dm.add("xpath", "content/size");
                return executor.execute("asset.query", dm.root());
            }
        };

        Worker sumDatasetSize = new Worker() {
            @Override
            protected Element execute(ServiceExecutor executor) throws Throwable {
                XmlDocMaker dm = new XmlDocMaker("args");
                dm.add("where", datasetQuery);
                dm.add("action", "sum");
                dm.add("xpath", "content/size");
                return executor.execute("asset.query", dm.root());
            }
        };

        Worker sumDicomDatasetSize = new Worker() {
            @Override
            protected Element execute(ServiceExecutor executor) throws Throwable {
                XmlDocMaker dm = new XmlDocMaker("args");
                dm.add("where", dicomDatasetQuery);
                dm.add("action", "sum");
                dm.add("xpath", "content/size");
                return executor.execute("asset.query", dm.root());
            }
        };

        Worker listTranscodes = new Worker() {
            @Override
            protected Element execute(ServiceExecutor executor) throws Throwable {
                XmlDocMaker dm = new XmlDocMaker("args");
                if (cid != null) {
                    dm.add("cid", cid);
                }
                if (where != null) {
                    dm.add("where", where);
                }
                return executor.execute(SvcCollectionTranscodeList.SERVICE_NAME, dm.root());
            }
        };

        PluginThread.executeAsync(SERVICE_NAME + " " + "count objects", countObjects);
        PluginThread.executeAsync(SERVICE_NAME + " " + "sum content size", sumContentSize);
        PluginThread.executeAsync(SERVICE_NAME + " " + "sum attachment size", sumAttachmentSize);
        PluginThread.executeAsync(SERVICE_NAME + " " + "sum dataset size", sumDatasetSize);
        PluginThread.executeAsync(SERVICE_NAME + " " + "sum dicom dataset size", sumDicomDatasetSize);
        PluginThread.executeAsync(SERVICE_NAME + " " + "list transcodes", listTranscodes);

        long nbo = 0;
        long nbc = 0;
        long csize = 0;
        long nba = 0;
        long asize = 0;
        long size = 0;
        long nbd = 0;
        long dsize = 0;
        long nbdd = 0;
        long ddsize = 0;
        List<XmlDoc.Element> tes = null;

        while (true) {
            if (countObjects.finished()) {
                if (countObjects.failed()) {
                    throw countObjects.thrown();
                }
                if (countObjects.succeeded()) {
                    nbo = countObjects.result().longValue("value", 0);
                }
            }
            if (sumContentSize.finished()) {
                if (sumContentSize.failed()) {
                    throw sumContentSize.thrown();
                }
                if (sumContentSize.succeeded()) {
                    nbc = sumContentSize.result().longValue("value/@nbe", 0);
                    csize = sumContentSize.result().longValue("value", 0);
                }
            }
            if (sumAttachmentSize.finished()) {
                if (sumAttachmentSize.failed()) {
                    throw sumAttachmentSize.thrown();
                }
                if (sumAttachmentSize.succeeded()) {
                    nba = sumAttachmentSize.result().longValue("value/@nbe", 0);
                    asize = sumAttachmentSize.result().longValue("value", 0);
                }
            }
            if (sumDatasetSize.finished()) {
                if (sumDatasetSize.failed()) {
                    throw sumDatasetSize.thrown();
                }
                if (sumDatasetSize.succeeded()) {
                    nbd = sumDatasetSize.result().longValue("value/@nbe", 0);
                    dsize = sumDatasetSize.result().longValue("value", 0);
                }
            }
            if (sumDicomDatasetSize.finished()) {
                if (sumDicomDatasetSize.failed()) {
                    throw sumDicomDatasetSize.thrown();
                }
                if (sumDicomDatasetSize.succeeded()) {
                    nbdd = sumDicomDatasetSize.result().longValue("value/@nbe", 0);
                    ddsize = sumDicomDatasetSize.result().longValue("value", 0);
                }
            }
            if (listTranscodes.finished()) {
                if (listTranscodes.failed()) {
                    throw listTranscodes.thrown();
                }
                if (listTranscodes.succeeded()) {
                    tes = listTranscodes.result().elements("transcode");
                }
            }
            if (countObjects.finished() && sumContentSize.finished() && sumAttachmentSize.finished()
                    && sumDatasetSize.finished() && sumDicomDatasetSize.finished() && listTranscodes.finished()) {
                break;
            } else {
                Thread.sleep(100);
            }
        }

        size = csize + asize;

        w.add("number-of-objects", nbo);
        w.add("number-of-attachments", nba);
        w.add("number-of-datasets", nbd);
        w.add("number-of-dicom-datasets", nbdd);
        w.add("total-content-size", new String[] { "nbe", Long.toString(nbc) }, csize);
        w.add("total-attachment-size", new String[] { "nbe", Long.toString(nba) }, asize);
        w.add("total-dataset-size", new String[] { "nbe", Long.toString(nbd) }, dsize);
        w.add("total-dicom-dataset-size", new String[] { "nbe", Long.toString(nbdd) }, ddsize);
        w.add("total-size", size);
        if (tes != null) {
            for (XmlDoc.Element te : tes) {
                w.add(te);
            }
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    private static String objectQuery(String cid, String where) {
        StringBuilder sb = new StringBuilder();
        if (cid != null) {
            sb.append("(cid='" + cid + "' or cid starts with '" + cid + "')");
            if (where != null) {
                sb.append(" and (").append(where).append(")");
            }
        } else {
            sb.append("(").append(where).append(") and ");
            sb.append("daris:pssd-object has value");
        }
        return sb.toString();
    }

    private static String attachmentQuery(String objectQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append("related to{attached-to} (");
        sb.append(objectQuery);
        sb.append(")");
        return sb.toString();
    }

    private static String datasetQuery(String objectQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(objectQuery);
        sb.append(") and model='om.pssd.dataset'");
        return sb.toString();
    }

    private static String dicomDatasetQuery(String datasetQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append(datasetQuery);
        sb.append(" and mf-dicom-series has value");
        return sb.toString();
    }

    private static abstract class Worker implements Runnable {

        private boolean _submitted = false;
        private Boolean _succeeded = null;
        private XmlDoc.Element _re = null;
        private Throwable _thrown = null;;

        @Override
        public void run() {
            try {
                _submitted = true;
                _re = execute(PluginThread.serviceExecutor());
                _succeeded = true;
            } catch (Throwable e) {
                _thrown = e;
                _succeeded = false;
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public boolean finished() {
            return _submitted && _succeeded != null;
        }

        public boolean succeeded() {
            return _submitted && _succeeded == true;
        }

        public boolean failed() {
            return _submitted && _succeeded == false;
        }

        protected abstract XmlDoc.Element execute(ServiceExecutor executor) throws Throwable;

        public XmlDoc.Element result() {
            return _re;
        }

        public Throwable thrown() {
            return _thrown;
        }
    }

}
