package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;

//TODO test
public class SvcObjectTagRemove extends PluginService {

    @Override
    public Access access() {
        return ACCESS_MODIFY;
    }

    @Override
    public Interface definition() {
        // TODO Auto-generated method stub
        return null;
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
