package daris.essentials;

import java.util.Collection;

import nig.mf.plugin.util.AssetUtil;
import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginService.Interface.Element;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.AssetType;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

/**
 * 
 * @author Neil Killeen
 *
 */
public class SvcAssetContentPrune extends PluginService {
	private Interface _defn;

	public SvcAssetContentPrune() {

		_defn = new Interface();
		_defn.add(new Element("where", StringType.DEFAULT, "Restrict selection of assets.  A clause selecting only assets with content will be automatically added.", 0, 1));
		_defn.add (new Element("prune", BooleanType.DEFAULT, "Actually prune assets (default false).   If false just list", 0, 1));
	}

	public String name() {
		return "nig.asset.content.prune";
	}

	public String description() {
		return "Finds and optionally prunes assets that have content and multiple versions. All sizes listed in the output are in bytes";
	}

	public Interface definition() {
		return _defn;
	}

	public Access access() {
		return ACCESS_ADMINISTER;
	}

	public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

		String where = args.value("where");
		Boolean prune = args.booleanValue("prune",  false);
		prune (executor(), where, prune, w);
	}
	
	private  void prune  (ServiceExecutor executor, String where, Boolean prune, XmlWriter w) throws Throwable {
	
		
		if (where==null) {
			where = "asset has content";
		} else {
		   where = where + " and (asset has content)";
		}
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("where", where);
		dm.add("size", "infinity");
		XmlDoc.Element r = executor.execute("asset.query", dm.root());
		Collection<String> ids = r.values("id");
		if (ids==null) return;
		//
		Integer recoverySize = 0;
		Integer totalSize = 0;
		Integer nAssets = 0;
		for (String id : ids) {
			XmlDoc.Element asset = AssetUtil.getAsset(executor, null, id);
			String versions = asset.value("asset/content/@versions");
			System.out.println("versions="+versions);
			Integer iversions = Integer.parseInt(versions);
			if (iversions>1) {
				String currentSizeS = asset.value("asset/content/size");
				String totalSizeS = asset.value("asset/content/@total-size");
				Integer cs = Integer.parseInt(currentSizeS);
				Integer ts = Integer.parseInt(totalSizeS);
				Integer rs = ts - cs;
				recoverySize += rs;
				totalSize += ts;
				nAssets++;
				w.add("id", new String[]{"current-size", currentSizeS, "total-size", ""+ts, "recovery-size", ""+rs}, id);
				if (prune) {
					dm = new XmlDocMaker("args");
					dm.add("id", id);
					executor.execute("asset.prune", dm.root());
				}
			}
		}
		w.add("number", new String[]{"total-size", ""+totalSize, "recovery-size", ""+""+recoverySize}, nAssets);

	}
}