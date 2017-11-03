package daris.plugin.asset.path1;

import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDoc;

public interface Compilable {

    String source();

    String compile(ServiceExecutor executor, XmlDoc.Element assetMeta);

}
