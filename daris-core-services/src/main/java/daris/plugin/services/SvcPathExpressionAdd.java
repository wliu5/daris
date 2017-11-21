package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;
import nig.mf.plugin.pssd.Project;

//TODO test
public class SvcPathExpressionAdd extends PluginService {

    public static final String DICTIONARY_NAME = "daris.path.expression";

    public static final String GLOBAL_DICTIONARY = "daris:" + DICTIONARY_NAME;

    public static final String SERVICE_NAME = "daris.path.expression.add";

    private Interface _defn;

    public SvcPathExpressionAdd() {
        _defn = new Interface();
        _defn.add(new Interface.Element("name", StringType.DEFAULT, "Name of the expression.", 1, 1));
        _defn.add(new Interface.Element("expr", StringType.DEFAULT,
                "The expression to generate output/destination path when exporting a daris object.", 1, 1));
        _defn.add(
                new Interface.Element("project", CiteableIdType.DEFAULT,
                        "The citeable id of the project. If specified, the expression will be added to the project specific dictionary: "
                                + Project.PROJECT_SPECIFIC_DICTIONARY_NAMESPACE_PREFIX + "{project}:" + DICTIONARY_NAME,
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
        return "Add path expression to dictionary: " + GLOBAL_DICTIONARY + " or project specific dictionary: "
                + Project.PROJECT_SPECIFIC_DICTIONARY_NAMESPACE_PREFIX + "{project}:" + DICTIONARY_NAME;
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String name = args.value("name");
        String expr = args.value("expr");
        String project = args.value("project");
        String dictionary = project == null ? GLOBAL_DICTIONARY : projectSpecificDictionary(project);

        if (!DictionaryUtils.dictionaryExists(executor(), dictionary)) {
            DictionaryUtils.createDictionary(executor(), dictionary,
                    "Expressions to generate output/destination path when export data from DaRIS.");
        }

        if (!DictionaryUtils.dictionaryEntryExists(executor(), dictionary, name, expr)) {
            if (DictionaryUtils.dictionaryEntryExists(executor(), dictionary, name, null)) {
                DictionaryUtils.removeDictionaryEntry(executor(), dictionary, name, null);
            }
            DictionaryUtils.addDictionaryEntry(executor(), dictionary, name, expr);
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    static String projectSpecificDictionary(String projectCid) {
        return Project.projectSpecificDictionaryNamespaceOf(projectCid) + ":" + DICTIONARY_NAME;
    }

}
