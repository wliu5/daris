package daris.essentials;

import java.util.Collection;
import java.util.Date;

import nig.mf.plugin.util.AssetUtil;
import arc.mf.plugin.*;
import arc.mf.plugin.PluginService.Interface;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcAssetsNoMembers extends PluginService {


	private Interface _defn;

	public SvcAssetsNoMembers() {
		_defn = new Interface();
		Interface.Element me = new Interface.Element("where", StringType.DEFAULT, "Predicate to select assets", 1, 1);
		_defn.add(me);
	}
	public String name() {
		return "nig.assets.collection.no-members";
	}

	public String description() {
		return "Find all collection assets with no direct members (no :related meta-data).";
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


		String where2 = args.value("where");

		String where = "asset is collection";
		if (where2!=null) {
			where += " and (" + where2 + ")";
		}
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("where", where);
		dm.add("size", "infinity");
		XmlDoc.Element r = executor().execute("asset.query", dm.root());
		Collection<String> ids = r.values("id");
		for (String id : ids) {
			XmlDoc.Element asset = AssetUtil.getAsset(executor(), null, id);
			XmlDoc.Element related = asset.element("asset/related");
			if (related==null) {
				w.add("id", id);
			}
		}
	}
}
