package daris.client.download;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import arc.xml.XmlDoc;
import arc.xml.XmlStringWriter;
import daris.client.download.DataDownloadOptions.Parts;
import daris.client.session.MFSession;
import daris.client.task.Task;
import daris.client.task.TaskConsumer;
import daris.client.util.LoggingUtils;

public class DataDownload implements Runnable {

    public static final String APP_NAME = "daris-download";

    public static final int DEFAULT_PAGE_SIZE = 1000;

    private MFSession _session;
    private String _where;
    private DataDownloadOptions _options;
    private Logger _logger;
    private int _nbWorkers;
    private ExecutorService _threadPool;
    private int _size;
    private BlockingQueue<Task> _queue;
    private List<Future<?>> _futures;

    public DataDownload(MFSession session, Collection<String> cids, DataDownloadOptions options) {
        this(session, constructQuery(cids, options), options);
    }

    public DataDownload(MFSession session, String where, DataDownloadOptions options) {
        _session = session;
        _where = where;
        _options = options;
        _logger = options.logging() ? createLogger() : null;
        _nbWorkers = options.numberOfWorkers();
        _threadPool = Executors.newFixedThreadPool(_nbWorkers);
        _size = DEFAULT_PAGE_SIZE;
        _queue = new LinkedBlockingQueue<Task>();
        _futures = new ArrayList<Future<?>>(_nbWorkers);
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < _nbWorkers; i++) {
                _futures.add(_threadPool.submit(new TaskConsumer(_queue, _logger)));
            }
            int idx = 1;
            int remaining = Integer.MAX_VALUE;
            XmlDoc.Element re = null;
            while (remaining > 0) {
                XmlStringWriter w = new XmlStringWriter();
                w.add("where", _where);
                w.add("count", true);
                w.add("idx", idx);
                w.add("size", _size);
                w.add("action", "get-id");
                re = _session.execute("asset.query", w.document(), null, null);
                remaining = re.intValue("cursor/remaining", 0);
                Collection<String> ids = re.values("id");
                if (ids != null) {
                    for (String id : ids) {
                        _queue.put(new AssetDownloadTask(id, _options, _session, _logger));
                    }
                }
                idx += _size;
            }
            for (int i = 0; i < _nbWorkers; i++) {
                _queue.put(new daris.client.task.PoisonTask());
            }
            _threadPool.shutdown();
            while (!_threadPool.awaitTermination(5000, TimeUnit.SECONDS)) {
                for (Future<?> f : _futures) {
                    System.out.println(f.isDone());
                }
            }
            return;
        } catch (Throwable e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LoggingUtils.logError(_logger, e);
        }
    }

    private static String constructQuery(Collection<String> cids, DataDownloadOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        boolean first = true;
        for (String cid : cids) {
            if (!first) {
                sb.append(" or ");
            } else {
                first = false;
            }
            sb.append("(");
            sb.append("cid='").append(cid).append("'");
            if (options.recursive()) {
                sb.append(" or cid starts with '").append(cid).append("'");
            }
            sb.append(")");
        }
        sb.append(")");
        if (options.filter() != null) {
            sb.append("and (").append(options.filter()).append(")");
        }
        if (options.datasetOnly()) {
            sb.append("and (model='om.pssd.dataset')");
        }
        if (options.parts() == Parts.CONTENT) {
            sb.append("and (asset has content)");
        }
        return sb.toString();
    }

    public static Logger createLogger() {

        Logger logger = Logger.getLogger(APP_NAME);
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        /*
         * add file handler
         */
        try {
            FileHandler fileHandler = new FileHandler("%t/" + APP_NAME + ".%g.log", 5000000, 2);
            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new Formatter() {

                @Override
                public String format(LogRecord record) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(new Date(record.getMillis())).append(" ");
                    sb.append("[thread: ").append(record.getThreadID()).append("] ");
                    sb.append(record.getLevel().getName()).append(" ");
                    sb.append(record.getMessage());
                    sb.append("\n");
                    return sb.toString();
                }
            });
            logger.addHandler(fileHandler);
        } catch (Throwable e) {
            System.err.println("Warning: failed to create daris-download.*.log file in system temporary directory.");
            e.printStackTrace(System.err);
            return null;
        }
        return logger;
    }

}
