package daris.plugin.services;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import arc.archive.ArchiveOutput;
import arc.archive.ArchiveRegistry;
import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginTask;
import arc.mf.plugin.PluginThread;
import arc.mf.plugin.dtype.EnumType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.mf.plugin.dtype.XmlDocType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcTmpDirConsume extends PluginService {

    public static final String SERVICE_NAME = "daris.tmp.directory.consume";

    private Interface _defn;

    public SvcTmpDirConsume() {
        _defn = new Interface();
        _defn.add(new Interface.Element("path", StringType.DEFAULT, "The (absolute) path to the temporary directory.",
                1, 1));
        _defn.add(new Interface.Element("atype", new EnumType(new String[] { "aar", "zip" }),
                "Archive type. aar or zip.", 0, 1));
        _defn.add(new Interface.Element("clevel", new IntegerType(0, 9),
                "Compression level for the archive. Defaults to 6. ", 0, 1));

        Interface.Element service = new Interface.Element("service", XmlDocType.DEFAULT,
                "The service to consume the temporary directory.", 1, 1);
        service.add(new Interface.Attribute("name", StringType.DEFAULT, "The name of the service.", 1));
        service.setIgnoreDescendants(true);
        _defn.add(service);
    }

    @Override
    public Access access() {
        return ACCESS_MODIFY;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Call the specified service and consume the specified temporary directory as service input (archive).";
    }

    @Override
    public void execute(Element args, Inputs inputs0, Outputs outputs0, XmlWriter w) throws Throwable {
        final Path dir = Paths.get(args.value("path"));
        if (Files.exists(dir)) {
            throw new IllegalArgumentException("'file:" + dir + "' does not exist.");
        }
        if (Files.isDirectory(dir)) {
            throw new IllegalArgumentException("'file:" + dir + "' is not a directory.");
        }
        XmlDoc.Element se = args.element("service");
        String serviceName = se.value("@name");
        String atype = args.stringValue("atype", "aar");
        final String ctype = "zip".equalsIgnoreCase(atype) ? "application/zip" : "application/arc-archive";
        final int clevel = args.intValue("clevel", 6);
        XmlDoc.Element serviceArgs = null;
        if (se.hasSubElements()) {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add(se, false);
            serviceArgs = dm.root();
        }
        final PipedInputStream pis = new PipedInputStream();
        final PipedOutputStream pos = new PipedOutputStream(pis);
        PluginThread.executeAsync(SERVICE_NAME, new Runnable() {
            @Override
            public void run() {
                try {
                    try {
                        ArchiveOutput ao = ArchiveRegistry.createOutput(pos, ctype, clevel, null);
                        try {
                            ao.addDirectory(dir.toString());
                        } finally {
                            ao.close();
                        }
                        PluginTask.threadTaskCompleted();
                    } finally {
                        pos.close();
                        // NOTE: No need to close pis (If do that, it will cause
                        // problem: corrupt archive).
                    }
                } catch (Throwable e) {
                    e.printStackTrace(System.out);
                    System.out.println("Deleting temporary directory: " + dir);
                    try {
                        Files.deleteIfExists(dir);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }

        });

        PluginService.Input input = new PluginService.Input(pis, -1, ctype, null);
        PluginService.Inputs inputs = new PluginService.Inputs(input);
        try {
            executor().execute(serviceName, serviceArgs, inputs, null);
        } catch (Throwable e) {
            System.out.println("Deleting temporary directory: " + dir);
            try {
                Files.deleteIfExists(dir);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            throw e;
        }

    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
