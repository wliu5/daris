package daris.plugin.services;

import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDocMaker;

public class DictionaryUtils {

    public static void createDictionary(ServiceExecutor executor, String dictionary, String description)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("name", dictionary);
        if (description != null) {
            dm.add("description", description);
        }
        executor.execute("dictionary.create", dm.root());
    }

    public static boolean dictionaryExists(ServiceExecutor executor, String dictionary) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("name", dictionary);
        return executor.execute("dictionary.exists", dm.root()).booleanValue("exists");
    }

    public static boolean dictionaryEntryExists(ServiceExecutor executor, String dictionary, String term,
            String definition) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("dictionary", dictionary);
        dm.add("term", term);
        if (definition != null) {
            dm.add("definition", definition);
        }
        return executor.execute("dictionary.entry.exists", dm.root()).booleanValue("exists");
    }

    public static void addDictionaryEntry(ServiceExecutor executor, String dictionary, String term, String definition)
            throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("dictionary", dictionary);
        dm.add("term", term);
        if (definition != null) {
            dm.add("definition", definition);
        }
        executor.execute("dictionary.entry.add", dm.root());
    }

    public static void removeDictionaryEntry(ServiceExecutor executor, String dictionary, String term,
            String definition) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("dictionary", dictionary);
        dm.add("term", term);
        if (definition != null) {
            dm.add("definition", definition);
        }
        executor.execute("dictionary.entry.remove", dm.root());
    }

}
