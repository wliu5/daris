package daris.plugin.services;

import java.util.Collection;
import java.util.Date;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcDataSetsNoContentList extends PluginService {


	private Interface _defn;

	public SvcDataSetsNoContentList() {
		_defn = new Interface();
		_defn.add(new Interface.Element("cid",StringType.DEFAULT, "Parent CID to check.", 1, Integer.MAX_VALUE));
		_defn.add(new Interface.Element("email",StringType.DEFAULT, "E-mail address to send results to.", 0, 1));
	}
	public String name() {
		return "daris.datasets.nocontent.list";
	}

	public String description() {
		return "Find empty (no content) DataSets.";
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


		String email = args.value("email");
		Collection<String> cids = args.values("cid");
		//
		StringBuilder sb = new StringBuilder();
		Boolean some = false;
		for (String cid : cids) {
			XmlDocMaker dm = new XmlDocMaker("args");
			String where = "cid starts with '" + cid + "' and model='om.pssd.dataset' and asset hasno content";
			dm.add("where", where);
			dm.add("size", "infinity");
			dm.add("action", "get-cid");
			XmlDoc.Element r = executor().execute("asset.query", dm.root());
			if (r!=null) {
				Collection<String> ids = r.values("cid");
				if (ids!=null) {
					sb.append("CID " + cid + " contains " + ids.size() + " empty DataSets - investigate \n");
					w.add("cid", new String[]{"number-empty-datasets", ""+ids.size()}, cid);
					some = true;
				}				
			}
		}		
		//
		if (some && email!=null) {
			XmlDocMaker dm = new XmlDocMaker("args");
			dm.add("async", "true");
			dm.add("body", sb.toString());
			Date date = new Date();
			dm.add("subject", date.toString() + " : empty DataSets detected");
			dm.add("to", email);
			executor().execute("mail.send", dm.root());
		}
	}


}
