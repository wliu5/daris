package daris.client.cli;

import java.io.File;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import daris.client.download.DataDownload;
import daris.client.download.DataDownloadOptions;
import daris.client.download.DataDownloadOptions.Parts;
import daris.client.pssd.CiteableIdUtils;
import daris.client.session.ConnectionSettings;
import daris.client.session.MFSession;

public class DataDownloadCLI {

    public static void main(String[] args) throws Throwable {

        if (args == null || args.length == 0) {
            showHelp();
            System.exit(1);
        }

        ConnectionSettings connectionSettings = new ConnectionSettings(null);
        Set<String> cids = new LinkedHashSet<String>();
        DataDownloadOptions options = new DataDownloadOptions();
        try {
            for (int i = 0; i < args.length;) {
                if (args[i].equals("--help") || args[i].equals("-h")) {
                    showHelp();
                    System.exit(0);
                } else if (args[i].equals("--mf.host")) {
                    connectionSettings.setServerHost(args[i + 1]);
                    i += 2;
                } else if (args[i].equals("--mf.port")) {
                    try {
                        int port = Integer.parseInt(args[i + 1]);
                        if (port <= 0 || port > 65535) {
                            throw new IllegalArgumentException("Invalid --mf.port: " + args[i + 1]);
                        }
                        connectionSettings.setServerPort(port);
                        i += 2;
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("Invalid --mf.port: " + args[i + 1], e);
                    }
                } else if (args[i].equals("--mf.transport")) {
                    connectionSettings.setServerTransport(args[i + 1]);
                    i += 2;
                } else if (args[i].equals("--mf.auth")) {
                    String[] parts = args[i + 1].split(",");
                    if (parts != null && parts.length == 3) {
                        connectionSettings.setUserCredentials(parts[0], parts[1], parts[2]);
                        i += 2;
                    } else {
                        throw new IllegalArgumentException("Invalid --mf.auth: " + args[i + 1]);
                    }
                } else if (args[i].equals("--mf.token")) {
                    connectionSettings.setToken(args[i + 1]);
                    i += 2;
                } else if (args[i].equals("--mf.sid")) {
                    connectionSettings.setSessionKey(args[i + 1]);
                    i += 2;
                } else if (args[i].equals("--no-recursive")) {
                    options.setRecursive(false);
                    i++;
                } else if (args[i].equals("--parts")) {
                    options.setParts(DataDownloadOptions.Parts.fromString(args[i + 1], Parts.CONTENT));
                    i += 2;

                } else if (args[i].equals("--include-attachments")) {
                    options.setIncludeAttachments(true);
                    i++;
                } else if (args[i].equals("--no-decompress")) {
                    options.setDecompress(false);
                    i++;
                } else if (args[i].equals("--filter")) {
                    if (options.filter() != null) {
                        throw new IllegalArgumentException("--filter has already been specified.");
                    }
                    String filter = args[i + 1];
                    options.setFilter(filter);
                    i += 2;
                } else if (args[i].equals("--dataset-only")) {
                    options.setDatasetOnly(true);
                    i++;
                } else if (args[i].equals("--output-dir")) {
                    File outputDir = new File(args[i + 1]);
                    if (!outputDir.exists()) {
                        throw new IllegalArgumentException(
                                "output directory: " + outputDir.getAbsolutePath() + " does not exist.");
                    }
                    options.setOutputDir(outputDir);
                    i += 2;
                } else if (args[i].equals("--transcodes")) {
                    if (options.hasTranscodes()) {
                        throw new IllegalArgumentException("--transcodes has already been specified.");
                    }
                    String transcodes = args[i + 1];
                    parseTranscodes(options, transcodes);
                    i += 2;
                } else if (args[i].equals("--no-overwrite")) {
                    options.setOverwrite(false);
                    i++;
                } else if (args[i].equals("--nb-workers")) {
                    try {
                        int nbWorkers = Integer.parseInt(args[i + 1]);
                        if (nbWorkers < 1) {
                            throw new IllegalArgumentException("Invalid --nb-workers: " + args[i + 1]);
                        }
                        options.setNumberOfWorkers(nbWorkers);
                        i += 2;
                    } catch (Throwable e) {
                        throw new IllegalArgumentException("Invalid --nb-workers: " + args[i + 1], e);
                    }
                } else if (args[i].equals("--logging")) {
                    options.setLogging(true);
                    i++;
                } else {
                    String cid = args[i];
                    if (!CiteableIdUtils.isCID(cid)) {
                        throw new IllegalArgumentException(cid + " is not a valid citeable id.");
                    }
                    cids.add(cid);
                    i++;
                }
            }

            connectionSettings.validate();

            if (cids.isEmpty()) {
                throw new IllegalArgumentException("No object cid is specified.");
            }
            MFSession session = new MFSession(connectionSettings);
            try {
                new DataDownload(session, cids, options).run();
            } finally {
                session.discard();
            }
        } catch (Throwable ex) {
            ex.printStackTrace(System.err);
            showHelp();
            System.exit(1);
        }

    }

    static Logger createLogger() throws Throwable {
        Logger logger = Logger.getLogger("daris-download");
        logger.setLevel(Level.ALL);
        logger.setUseParentHandlers(false);
        /*
         * add file handler
         */
        try {
            FileHandler fileHandler = new FileHandler("%t/.daris-download.%g.log", 5000000, 2);
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
            System.err.println("Warning: failed to create .daris-download.*.log file.");
            e.printStackTrace(System.err);
        }
        /*
         * console handler
         */
        StreamHandler consoleHandler = new StreamHandler(System.out, new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getMessage() + "\n";
            }
        }) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                super.flush();
            }
        };
        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);
        return logger;
    }

    private static void parseTranscodes(DataDownloadOptions options, String transcodes) {
        if (transcodes == null || transcodes.trim().isEmpty()) {
            return;
        }
        String[] pairs = transcodes.split(";");
        for (String pair : pairs) {
            String[] types = pair.split(":");
            options.addTranscode(types[0], types[1]);
        }
    }

    private static void showHelp() {
        // @formatter:off
        System.out.println();
        System.out.println("USAGE: ");
        System.out.println("    " + DataDownload.APP_NAME + " <options> <cid>");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("    --mf.host <host>                    The Mediaflux server host.");
        System.out.println("    --mf.port <port>                    The Mediaflux server port.");
        System.out.println("    --mf.transport <transport>          The Mediaflux server transport, can be http, https or tcp/ip.");
        System.out.println("    --mf.auth <domain,user,password>    The Mediaflux user authentication deatils.");
        System.out.println("    --mf.token <token>                  The Mediaflux secure identity token.");
        System.out.println("    --mf.sid <sid>                      The Mediaflux session id.");
        System.out.println("    --output-dir <dir>                  The output directory. If not specified, defatuls to current working directory.");
        System.out.println("    --no-overwrite                      Do not overwrite files if it exists.");
        System.out.println("    --no-recursive                      Do not download recurisvely.");
        System.out.println("    --parts <content|meta|all>          Parts to download. Defaults to content.");
        System.out.println("    --include-attachments               Include attachments.");
        System.out.println("    --no-decompress                     Do not decompress/extract archive.");
        System.out.println("    --filter <query>                    A filter query to find the objects to download.");
        System.out.println("    --dataset-only                      Downloads only datasets.");
        System.out.println("    --transcodes <from:to;from:to>      Apply transcodes before downloading. e.g. --transcodes dicom/series:nifti/series");
        System.out.println("    --nb-workers <n>                    Number of worker threads. Defaults to 1.");
        System.out.println("    --logging                           Turn on logging.");
        System.out.println("    --help                              Display help information.");
        System.out.println();
        // @formatter:on
    }

}
