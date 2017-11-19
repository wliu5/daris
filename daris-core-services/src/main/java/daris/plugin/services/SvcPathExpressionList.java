package daris.plugin.services;

import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
//TODO test
public class SvcPathExpressionList extends PluginService {

    public static final String SERVICE_NAME = "daris.path.expression.list";

    private Interface _defn;

    public SvcPathExpressionList() {
        _defn = new Interface();
        _defn.add(new Interface.Element("project", CiteableIdType.DEFAULT,
                "The citeable id of the project. If specified, include also project specific expressions for the project.",
                0, 1));
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
        return "List expressions to generate output/destination path when exporting daris data.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        listExpressions(executor(), SvcPathExpressionAdd.DICTIONARY, null, w);
        String project = args.value("project");
        if (project != null) {
            listExpressions(executor(), (SvcPathExpressionAdd.DICTIONARY + "." + project), project, w);
        }
    }

    static void listExpressions(ServiceExecutor executor, String dictionary, String project, XmlWriter w)
            throws Throwable {
        if (DictionaryUtils.dictionaryExists(executor, dictionary)) {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("dictionary", dictionary);
            dm.add("size", "infinity");
            List<XmlDoc.Element> ees = executor.execute("dictionary.entries.describe", dm.root()).elements("entry");
            if (ees != null) {
                for (XmlDoc.Element ee : ees) {
                    String name = ee.value("term");
                    String expr = ee.value("definition");
                    w.add("expression", new String[] { "name", name, "project", project }, expr);
                }
            }
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
