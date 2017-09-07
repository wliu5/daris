package daris.plugin.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.EnumType;
import arc.mf.plugin.dtype.StringType;
import arc.streams.StreamCopy;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;
import daris.util.FileUtils;

public class SvcTmpDirFileUpload extends PluginService {

    public static final String SERVICE_NAME = "daris.tmp.directory.file.upload";

    private Interface _defn;

    public SvcTmpDirFileUpload() {
        _defn = new Interface();
        _defn.add(new Interface.Element("directory", StringType.DEFAULT, "Path to the temporary directory.", 1, 1));
        _defn.add(new Interface.Element("path", StringType.DEFAULT, "Relative destination path for the uploaded file.",
                1, 1));
        _defn.add(new Interface.Element("if-exists", new EnumType(new String[] { "ignore", "overwrite", "error" }),
                "Action to take if the file already exists in the temporary directory. Defaults overwrite.", 0, 1));
        _defn.add(new Interface.Element("cleanup-on-error", BooleanType.DEFAULT,
                "Clean up the temporary directory if error occurs during the upload. Defaults to false.", 0, 1));
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
        return "Upload file to the specified temporary directory.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        boolean cleanupOnError = args.booleanValue("cleanup-on-error", false);

        Path dir = Paths.get(args.value("directory"));
        if (!Files.exists(dir)) {
            throw new FileNotFoundException("'file:" + dir + "' does not exist.");
        }
        Path path = Paths.get(dir.toString(), args.value("path"));

        File file = path.toFile();

        try {
            if (Files.exists(path)) {
                String ifExists = args.stringValue("if-exists", "overwrite");
                if ("ignore".equalsIgnoreCase(ifExists)) {
                    inputs.input(0).close();
                    return;
                } else if ("error".equalsIgnoreCase(ifExists)) {
                    throw new Exception("'file:" + path + "' already exists.");
                }
            } else {
                Path parentPath = path.getParent();
                if (!Files.exists(parentPath)) {
                    Files.createDirectories(parentPath);
                }
            }
            PluginService.Input input = inputs.input(0);
            try {
                StreamCopy.copy(input.stream(), file);
            } finally {
                input.close();
            }
        } catch (Throwable e) {
            if (cleanupOnError) {
                System.out.println("Deleting temporary directory: " + dir);
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException ioe) {
                    System.err.println("Failed to delete directory: " + dir);
                    ioe.printStackTrace(System.err);
                }
            }
            throw e;
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    @Override
    public int minNumberOfInputs() {
        return 1;
    }

    @Override
    public int maxNumberOfInputs() {
        return 1;
    }
}
