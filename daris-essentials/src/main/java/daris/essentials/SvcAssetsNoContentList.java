package daris.essentials;

import java.util.Collection;
import java.util.Date;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcAssetsNoContentList extends PluginService {


	private Interface _defn;

	public SvcAssetsNoContentList() {
		_defn = new Interface();
		_defn.add(new Interface.Element("cid", CiteableIdType.DEFAULT, "Parent CID(s) to check.", 1, Integer.MAX_VALUE));
		Interface.Element me = new Interface.Element("model", StringType.DEFAULT, "A model type to filter assets by.", 0, 1);
		Interface.Attribute at = new Interface.Attribute("text", StringType.DEFAULT, "A string for the email representin the model name (e.g. DataSets for om.pssd.dataset') - to avoid mime-cast subsititutions.", 0);
		me.add(at);
		_defn.add(me);
		_defn.add(new Interface.Element("type", StringType.DEFAULT, "A mime type to filter assets by.", 0, 1));
		_defn.add(new Interface.Element("email",StringType.DEFAULT, "E-mail address to send results to.", 0, 1));
		_defn.add(new Interface.Element("days", IntegerType.DEFAULT, "Look back time in days. Defaults to all time.", 0, 1));

	}
	public String name() {
		return "nig.assets.nocontent.list";
	}

	public String description() {
		return "Find empty (no content) assets under a parent CID and send an email (only if it finds some empty assets).";
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


		XmlDoc.Element model = args.element("model");
		String modelName = model.value();
		String modelText = model.value("@text");
		if (modelText==null) {
			modelText = modelName;
		}
		//
		String type = args.value("type");
		String email = args.value("email");
		Collection<String> cids = args.values("cid");
		String days = args.value("days");
		//
		StringBuilder sb = new StringBuilder();
		Boolean some = false;
		for (String cid : cids) {
			String text = null;
			XmlDocMaker dm = new XmlDocMaker("args");
			String where = "cid starts with '" + cid + "' and asset hasno content";
			if (days!=null) {
				where +=  " and (mtime>='NOW-"+days+"DAY' or ctime>='NOW-" + days + "DAY')";
			}
			if (model!=null) {
				where += " and model='" + modelName + "'";
				text = " of model type '" + modelText + "'";
			}
			if (type!=null) {
				where += " and type='" + type + "'";
				if (text!=null) {
					text += " and mime type '" + type + "'";
				} else {
					text = " of mime type '" + type + "'";
				}
			}
			dm.add("where", where);
			dm.add("size", "infinity");
			dm.add("action", "get-cid");
			XmlDoc.Element r = executor().execute("asset.query", dm.root());
			if (r!=null) {
				Collection<String> ids = r.values("cid");
				if (ids!=null) {
					sb.append("CID " + cid + " contains " + ids.size() + " empty assets " + text + " - investigate \n");
					w.add("cid", new String[]{"number-empty-assets", ""+ids.size()}, cid);
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
			dm.add("subject", date.toString() + " : empty assets detected");
			dm.add("to", email);
			executor().execute("mail.send", dm.root());
		}
	}
}
