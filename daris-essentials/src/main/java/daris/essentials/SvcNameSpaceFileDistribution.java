package daris.essentials;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcNameSpaceFileDistribution extends PluginService {

	private Integer idx_ = 1;
	private Integer size_ = 5000;
	private Double logBinWidth_ = 0.0;
	private Double minSize_ = 1.0E12;
	private Double maxSize_ = 0.0;
	private Integer nAssets_ = 0;
	private Integer maxBin_ = 0;
	//
	private static String[] units = {"KB", "MB", "GB", "TB", "PB"};

	private Interface _defn;

	public SvcNameSpaceFileDistribution() {
		_defn = new Interface();
		Interface.Element me = new Interface.Element("where", StringType.DEFAULT, "Predicate to select assets", 1, 1);
		_defn.add(me);
	}

	public String name() {
		return "nig.namespace.file.distribution";
	}

	public String description() {
		return "Specialised service to list the file size distribution for the selected assets.  Prints a histogram of size bin and number of files. Each bin shows the count for files up to that bin size. Filters out assets with no content.";
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

		// Initialise
		String where = args.value("where") + " and asset has content";
		init();
		
		// Set logarithmic bin width. We use a doubling of size 
		logBinWidth_ = Math.log10(2.0);

		// Generate Histogram container
		HashMap<Integer,Long> bins = new HashMap<Integer, Long>();
		
		
		// Iterate through assets and accumulate
		Boolean more = true;
		while (more) {
			more = accumulate (executor(), where, bins, w);
		}
		w.add("number-assets", nAssets_);
		w.add("minimum-file-size", minSize_);
		w.add("maximum-file-size", maxSize_);
		
		// FInd maximum bin from HashMap (not sorted)
		Set<Integer> keySet = bins.keySet();
		for (Integer key : keySet) {
			if (key>maxBin_) maxBin_ = key;
		}	
		
		// Print histogram
		Long humanSize = 1L;
		int group = 0;
		String unit = units[0];
		for (int i=10; i<=maxBin_; i++) {
			long actualSize = (long)Math.pow(2,i);
			
			// size takes values 1,2,4,8,16,32,64,128,256,512 and repeats
			// in groups for KB, MB, GB etc
			int j = i % 10;
			if (j==0) {
				humanSize = 1L;
				if (i!=10) {
					group++;
				}
			} else {
				humanSize *= 2;
			}
			
			// Set units
			if (group>=units.length) {
				// These would be large !
				unit = "??B";
			} else {
				unit = units[group];
			}
			
			// Print
			if (bins.containsKey(i)) {
				w.add("bin", new String[]{"actual-bin-size", ""+actualSize, "human-bin-size", ""+humanSize + unit}, bins.get(i));
			} else {
				w.add("bin", new String[]{"actual-bin-size", ""+actualSize, "human-bin-size", ""+humanSize + unit}, 0);
			}
		}
	}


	private void init () {
		idx_ = 1;
		size_ = 5000;
		logBinWidth_ = 0.0;
		minSize_ = 1.0E12;
		maxSize_ = 0.0;
		nAssets_ = 0;
		maxBin_ = 0;
	}
	private Boolean accumulate (ServiceExecutor executor, String where, HashMap<Integer,Long> bins, XmlWriter w) throws Throwable {

		PluginTask.checkIfThreadTaskAborted();
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("where", where);
		dm.add("idx", idx_);
		dm.add("size", size_);
		dm.add("action", "get-value");
		dm.add("xpath", "content/size");
		XmlDoc.Element r = executor.execute("asset.query", dm.root());
		Collection<XmlDoc.Element> assets = r.elements("asset");
		if (assets==null) return false;
		//
		XmlDoc.Element cursor = r.element("cursor");
		boolean more = !(cursor.booleanValue("total/@complete"));
		if (more) {
			idx_ = cursor.intValue("next");
		}
		// Accumulate into logarithmic histogram
		for (XmlDoc.Element asset : assets) {
			Double size = asset.doubleValue("value");
			Double ls = Math.log10(size);
			Integer iBin = setBin (ls);
			if (bins.containsKey(iBin)) {
				Long n = bins.get(iBin) + 1;
				bins.put(iBin, n);
			} else {
				bins.put(iBin, 1L);
			}
			nAssets_++;

			maxSize_ = Math.max(maxSize_, size);
			minSize_ = Math.min(minSize_, size);
		}
		return more;
	}

	int setBin (Double logValue) {
		// We arrange the bins so that any size < 1024 is in the bottom bin [10]
		// Then the bin increments by one per power of 2 (1024 [10], 2048 [11] etc)
		int idx = (int)(1.0 + Math.floor(logValue / logBinWidth_));
		if (idx<10) idx = 10;
		return idx;		
	}
}
