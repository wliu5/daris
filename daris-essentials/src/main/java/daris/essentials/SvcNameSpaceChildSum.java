package daris.essentials;

import java.util.Collection;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcNameSpaceChildSum extends PluginService {


	private Interface _defn;

	public SvcNameSpaceChildSum() {
		_defn = new Interface();
		_defn.add(new Interface.Element("namespace", StringType.DEFAULT, "If true, (default false) assets will actually be moved to the correct namespace (one at a time; not efficient) rather than just listed.", 0, 1));
	}
	public String name() {
		return "nig.namespace.child.sum";
	}

	public String description() {
		return "Lists the sums of content in all child namespaces in the local server.";
	}

	public Interface definition() {
		return _defn;
	}

	public Access access() {
		return ACCESS_ACCESS;
	}

	public int executeMode() {
		return EXECUTE_LOCAL;
	}

	public boolean canBeAborted() {

		return true;
	}

	public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

		String namespace = args.value("namespace");

		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("namespace", namespace);
		XmlDoc.Element r = executor().execute("asset.namespace.list", dm.root());
		if (r==null) return;
		Collection<String> nss = r.values("namespace/namespace");
		String path = r.value("namespace/@path");
		if (nss==null) return;
		w.push("namespace", new String[]{"path", namespace});
		for (String ns : nss) {
			String fullPath = path + "/" + ns;
			XmlDocMaker dm2 = new XmlDocMaker("args");
			dm2.add("where", "namespace>='" + fullPath + "'");
			dm2.add("action", "sum");
			dm2.add("xpath", "content/size");
			dm2.add("size", "infinity");
			dm2.add("pdist", 0);
			XmlDoc.Element r2 = executor().execute("asset.query", dm2.root());
			if (r2!=null) {
				String n = r2.value("value/@nbe");
				String s = r2.value("value");
				w.add("namespace", new String[]{"nbe", n, "sum-bytes", s}, ns);
			}
		}
		w.pop();
	}
}
