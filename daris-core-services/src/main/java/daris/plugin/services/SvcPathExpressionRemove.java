package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;
import nig.mf.plugin.pssd.Project;

//TODO test
public class SvcPathExpressionRemove extends PluginService {

    public static final String SERVICE_NAME = "daris.path.expression.remove";

    private Interface _defn;

    public SvcPathExpressionRemove() {
        _defn = new Interface();
        _defn.add(new Interface.Element("name", StringType.DEFAULT, "Name of the expression.", 1, 1));
        _defn.add(new Interface.Element("project", CiteableIdType.DEFAULT,
                "The citeable id of the project. If specified, the expression will be removed from the project specific dictionary: "
                        + Project.PROJECT_SPECIFIC_DICTIONARY_NAMESPACE_PREFIX + "{project}:"
                        + SvcPathExpressionAdd.DICTIONARY_NAME,
                0, 1));
    }

    @Override
    public Access access() {
        return ACCESS_ADMINISTER;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Remove path expression from dictionary: " + SvcPathExpressionAdd.GLOBAL_DICTIONARY
                + " or project specific dictionary: " + Project.PROJECT_SPECIFIC_DICTIONARY_NAMESPACE_PREFIX
                + "{project}:" + SvcPathExpressionAdd.DICTIONARY_NAME;
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String name = args.value("name");
        String project = args.value("project");
        String dictionary = project == null ? SvcPathExpressionAdd.GLOBAL_DICTIONARY
                : SvcPathExpressionAdd.projectSpecificDictionary(project);

        if (DictionaryUtils.dictionaryExists(executor(), dictionary)) {
            if (DictionaryUtils.dictionaryEntryExists(executor(), dictionary, name, null)) {
                DictionaryUtils.removeDictionaryEntry(executor(), dictionary, name, null);
            }
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
