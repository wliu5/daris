package daris.plugin.services;

import java.io.File;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginTask;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;

public class SvcTmpDirCreate extends PluginService {

    public static String SERVICE_NAME = "daris.tmp.directory.create";

    private Interface _defn;

    public SvcTmpDirCreate() {
        _defn = new Interface();
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
        return "Creates and returns a path to a temporary directory.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        File dir = PluginTask.createTemporaryDirectory();
        w.add("path", dir.getAbsolutePath());
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
