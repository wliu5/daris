package daris.essentials;

import java.util.Collection;

import nig.mf.plugin.util.AssetUtil;
import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginTask;
import arc.mf.plugin.PluginService.Interface.Element;
import arc.mf.plugin.ServiceExecutor;
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
		_defn.add (new Element("list-all", BooleanType.DEFAULT, "List every asset found (true).  The default (false) is to just list the totals.", 0, 1));
	}

	public String name() {
		return "nig.asset.content.prune";
	}

	public String description() {
		return "Finds and optionally prunes DataSet assets that have content and multiple versions of content. All sizes listed in the output are in bytes. Is abortable and checked every asset.";
	}

	public Interface definition() {
		return _defn;
	}

	public Access access() {
		return ACCESS_ADMINISTER;
	}
	
	public boolean canBeAborted() {

		return true;
	}


	public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

		String where = args.value("where");
		Boolean prune = args.booleanValue("prune",  false);
		Boolean listAll = args.booleanValue("list-all",  false);
		prune (executor(), where, prune, listAll, w);
	}

	private  void prune  (ServiceExecutor executor, String where, Boolean prune, Boolean listAll, XmlWriter w) throws Throwable {


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
		Long recoverySize = 0L;
		Long totalSize = 0L;
		Integer nAssets = 0;
		for (String id : ids) {
			PluginTask.checkIfThreadTaskAborted();
			XmlDoc.Element asset = AssetUtil.getAsset(executor, null, id);
			String versions = asset.value("asset/content/@versions");
			if (versions!=null) {
				Integer iversions = Integer.parseInt(versions);
				if (iversions>1) {
					String currentSizeS = asset.value("asset/content/size");
					String totalSizeS = asset.value("asset/content/@total-size");
					Long cs = Long.parseLong(currentSizeS);
				    Long ts = Long.parseLong(totalSizeS);
					Long rs = ts - cs;
					recoverySize += rs;
					totalSize += ts;
					nAssets++;
					if (listAll) {
						w.add("id", new String[]{"current-size", currentSizeS, "total-size", ""+ts, "recovery-size", ""+rs}, id);
					}
					if (prune) {
						dm = new XmlDocMaker("args");
						dm.add("id", id);
						dm.add("keep-content", "true");
						executor.execute("asset.prune", dm.root());
					}
				}
			}
		}
		w.add("total-number", new String[]{"total-size", ""+totalSize, "total-recovery-size", ""+""+recoverySize}, nAssets);

	}
}