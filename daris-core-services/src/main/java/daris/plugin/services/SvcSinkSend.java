package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;

public class SvcSinkSend extends PluginService {

    public static final String SERVICE_NAME = "daris.sink.send";

    private Interface _defn;

    public SvcSinkSend() {
        _defn = new Interface();
        
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
        return "Send to specified sink.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        // TODO Auto-generated method stub

    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
