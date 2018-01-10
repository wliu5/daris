package daris.essentials;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcQueryFileDistribution extends PluginService {

	private static String[] units = {"KB", "MB", "GB", "TB", "PB"};
	private static Integer cursorSize_ = 10000;
	
	private class ValHolder {
		private Double min_;
		private Double max_;
		private Long n_;
		public ValHolder (Double min, Double max, Long n) {
			min_ = min;
			max_ = max;
			n_ = n;
		}
		public void set (Double min, Double max, Long n) {
			min_ = min;
			max_ = max;
			n_ = n;
		}
		Double min () {return min_;};
		Double max () {return max_;};
		Long n () {return n_;};		
	}
	

	private Interface _defn;

	public SvcQueryFileDistribution() {
		_defn = new Interface();
		Interface.Element me = new Interface.Element("where", StringType.DEFAULT, "Predicate to select assets", 1, 1);
		_defn.add(me);
	}

	public String name() {
		return "nig.file.size.distribution";
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

		// Parse
		String where = args.value("where") + " and asset has content";
		
		// Set logarithmic bin width. We use a doubling of size 
		Double logBinWidth = Math.log10(2.0);
		//
		Integer nAssets = count (executor(), where);
		
		// Generate Histogram container
		HashMap<Integer,Long> bins = new HashMap<Integer, Long>();
			
		// Iterate through assets and accumulate
		Boolean more = true;
		ValHolder vh = new ValHolder(1.0E15, -1.0, 0L);
		AtomicInteger idx = new AtomicInteger(1);
		while (more) {
			more = accumulate (executor(), nAssets, where, logBinWidth, bins, idx, vh, w);
		}
		w.add("number-assets", vh.n());
		w.add("minimum-file-size", vh.min());
		w.add("maximum-file-size", vh.max());
		
		// FInd maximum bin from HashMap (not sorted)
		Integer maxBin = 0;
		Set<Integer> keySet = bins.keySet();
		for (Integer key : keySet) {
			if (key>maxBin) maxBin = key;
		}	
		
		// Print histogram
		Long humanSize = 1L;
		int group = 0;
		String unit = units[0];
		for (int i=10; i<=maxBin; i++) {
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


	private Boolean accumulate (ServiceExecutor executor,  Integer nAssets, String where, Double logBinWidth, HashMap<Integer,Long> bins, 
			AtomicInteger idx, ValHolder vh, XmlWriter w) throws Throwable {
		PluginTask.checkIfThreadTaskAborted();
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("where", where);
		dm.add("idx", idx.intValue());
		dm.add("size", cursorSize_);
		dm.add("action", "get-value");
		dm.add("xpath", "content/size");
		XmlDoc.Element r = executor.execute("asset.query", dm.root());
		Collection<XmlDoc.Element> assets = r.elements("asset");
		if (assets==null) return false;
		//
		XmlDoc.Element cursor = r.element("cursor");
		boolean more = !(cursor.booleanValue("total/@complete"));
		if (more) {
			idx.set(cursor.intValue("next"));
		}
		// Accumulate into logarithmic histogram
		for (XmlDoc.Element asset : assets) {
			Double size = asset.doubleValue("value");
			Double ls = Math.log10(size);
			Integer iBin = setBin (ls, logBinWidth);
			if (bins.containsKey(iBin)) {
				Long n = bins.get(iBin) + 1;
				bins.put(iBin, n);
			} else {
				bins.put(iBin, 1L);
			}
			// Update
			vh.set(Math.min(vh.min(), size), Math.max(vh.max(), size), (1L+vh.n()));
			
			// Sanity check as  these may be very long lived executions.
			if (vh.n()>nAssets) {
				throw new Exception ("The accumulation loop has found too many assets - abandoning");
			}
		}
		return more;
	}

	private int setBin (Double logValue, Double logBinWidth) {
		// We arrange the bins so that any size < 1024 is in the bottom bin [10]
		// Then the bin increments by one per power of 2 (1024 [10], 2048 [11] etc)
		int idx = (int)(1.0 + Math.floor(logValue / logBinWidth));
		if (idx<10) idx = 10;
		return idx;		
	}
	
	
	private Integer count (ServiceExecutor executor, String where) throws Throwable {
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("where", where);
		dm.add("action", "count");
		XmlDoc.Element r = executor.execute("asset.query", dm.root());
		return r.intValue("value");
	}
}
