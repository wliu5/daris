package daris.plugin.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;

public class SvcTmpDirDestroy extends PluginService {

    public static String SERVICE_NAME = "daris.tmp.directory.destroy";

    private Interface _defn;

    public SvcTmpDirDestroy() {
        _defn = new Interface();
        _defn.add(new Interface.Element("path", StringType.DEFAULT, "The (absolute) path to the directory.", 1, 1));
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
        return "Deletes the specified temporary directory.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        Path dir = Paths.get(args.value("path"));
        Files.deleteIfExists(dir);
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
