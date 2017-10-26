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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void execute(Element arg0, Inputs arg1, Outputs arg2, XmlWriter arg3) throws Throwable {
        // TODO Auto-generated method stub

    }

    @Override
    public String name() {
        // TODO Auto-generated method stub
        return null;
    }

}
