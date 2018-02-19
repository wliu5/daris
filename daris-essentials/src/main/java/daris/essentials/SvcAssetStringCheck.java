package daris.essentials;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcAssetStringCheck extends PluginService {


	private Interface _defn;
	private Integer idx_ = 1;
	private Integer count_ = 0;


	public SvcAssetStringCheck() {
		_defn = new Interface();
		_defn.add(new Interface.Element("where",StringType.DEFAULT, "Query predicate to restrict the selected assets on the local host. YOu shoujld include the component to only finds assets with the xpath of interest set.", 1, 1));
		_defn.add(new Interface.Element("xpath",StringType.DEFAULT, "The xpath of interest.", 1, 1));
		_defn.add(new Interface.Element("size",IntegerType.DEFAULT, "Limit the accumulation loop to this number of assets per iteration (if too large, the VM may run out of virtual memory).  Defaults to 10000.", 0, 1));
	}
	public String name() {
		return "nig.asset.string.check";
	}

	public String description() {
		return "Checks assets for specific XPATHs holding non-ASCII unicode values.";
	}

	public Interface definition() {
		return _defn;
	}

	public Access access() {
		return ACCESS_ADMINISTER;
	}

	public int executeMode() {
		return EXECUTE_LOCAL;
	}

	public boolean canBeAborted() {

		return true;
	}

	public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

		// Get inputs
		String where = args.value("where");
		String size = args.stringValue("size", "10000");
		String xpath = args.value("xpath");

		// Iterate through cursor and build list of assets 
		boolean more = true;
		Vector<String> assetIDs = new Vector<String>();
		while (more) {
			more = find (executor(),  where, size, xpath, assetIDs,  w);
			PluginTask.checkIfThreadTaskAborted();
		}
		w.add("total-checked", count_);
	}

	private boolean find (ServiceExecutor executor,  String where,  String size,  String xpath, Vector<String> assetList, 
			XmlWriter w)
					throws Throwable {

		// Find local  assets  with the given query. We work through the cursor else
		// we may run out of memory
		XmlDocMaker dm = new XmlDocMaker("args");
		if (where!=null) dm.add("where", where);
		dm.add("idx", idx_);
		dm.add("size", size);
		dm.add("pdist", 0);
		dm.add("action", "get-value");
		dm.add("xpath", "id");
		dm.add("xpath", xpath);
		XmlDoc.Element r = executor().execute("asset.query", dm.root());
		if (r==null) return false;  
		Collection<XmlDoc.Element> assets = r.elements("asset");
		if (assets==null) return false;
		count_ += assets.size();

		// Get the cursor and increment for next time
		XmlDoc.Element cursor = r.element("cursor");
		boolean more = !(cursor.booleanValue("total/@complete"));
		if (more) {
			Integer next = cursor.intValue("next");
			idx_ = next;
		}

		// Test
		for (XmlDoc.Element asset : assets) {
			List<XmlDoc.Element> t = asset.elements("value");
			String id = t.get(0).value();
			String text = t.get(1).value();
			if (text.matches("\\A\\p{ASCII}*\\z")) {
				w.add("id", new String[]{"text", text, "ascii", "true"}, id);
			} else {
				w.add("id", new String[]{"text", text, "ascii", "false"}, id);
			}
		}
		//
		return more;

	}
}
