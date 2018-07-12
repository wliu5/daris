package daris.essentials;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import arc.dtype.DoubleType;
import arc.mf.plugin.*;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.FloatType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcQueryFileDistribution extends PluginService {

	private static String[] units = {"KB", "MB", "GB", "TB", "PB"};
	private static Integer cursorSize_ = 50000;
	private Long totalTime_;

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
		me = new Interface.Element("show-accum", BooleanType.DEFAULT, "Show some accumulation loop information (default false).", 0, 1);
		_defn.add(me);
		me = new Interface.Element("no-accum", BooleanType.DEFAULT, "Don't iterate and accumulate the histogram (default false), just do the query part.", 0, 1);
		_defn.add(me);
		me = new Interface.Element("size", IntegerType.DEFAULT, "Cursor size (default 50000)", 0, 1);
		_defn.add(me);
		me = new Interface.Element("block-size", FloatType.DEFAULT, "Block-size to compute wasted space. Default 4096 bytes", 0, 1);
		_defn.add(me);
	}

	public String name() {
		return "nig.query.file.size.distribution";
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
		Boolean showAccum = args.booleanValue("show-accum", false);
		Boolean noAccum = args.booleanValue("no-accum", false);
		Integer size = args.intValue("size", 50000);
		Double blockSize = args.doubleValue("block-size", 4096.0D);
		
		// Set logarithmic bin width. We use a doubling of size 
		Double logBinWidth = Math.log10(2.0);

		// Generate Histogram container
		HashMap<Integer,Long> bins = new HashMap<Integer, Long>();

		// Iterate through assets and accumulate
		Boolean more = true;
		ValHolder vh = new ValHolder(1.0E15, -1.0, 0L);
		ValHolder wasted = new ValHolder(1.0E15, -1.0, 0L);
		totalTime_ = 0L;
		AtomicInteger idx = new AtomicInteger(1);
		if (showAccum) w.push("accumulation");
		while (more) {
			more = accumulate (executor(), blockSize, noAccum, showAccum, where, size, logBinWidth, bins, idx, vh, wasted, w);
		}
		if (showAccum) {
			w.add("total-elapsed-time", totalTime_);
			w.pop();
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
		w.add("wasted-storage", new String[]{"uints", "bytes"},  wasted.max());
	}


	private Boolean accumulate (ServiceExecutor executor,  Double blockSize, Boolean noAccum, Boolean showAccum, String where, 
			Integer cursorSize, Double logBinWidth, HashMap<Integer,Long> bins, 
			AtomicInteger idx, ValHolder vh, ValHolder wastedSum, XmlWriter w) throws Throwable {
		if (showAccum) {
			w.push("cycle");
			w.add("start-index", idx.get());
		}
		Long time1 = System.currentTimeMillis();
		PluginTask.checkIfThreadTaskAborted();
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("where", where);
		dm.add("idx", idx.intValue());
		dm.add("size", cursorSize);
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
		
		if (!noAccum) {
			for (XmlDoc.Element asset : assets) {
				Double size = asset.doubleValue("value");
				
				// Just make  use of the  ValHolder container to hold the sum
				// of the wasted space (for storage with the given block size) 
				// in the max element
				Double wasted = size - Math.floor(size/blockSize)*blockSize;
				Double t = wastedSum.max() + wasted;
				wastedSum.set(0.0, t, 0L);
				//
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
			}
		}
		Long time2 = System.currentTimeMillis();
		totalTime_ = totalTime_ + (time2-time1);
		if (showAccum) {
			w.add("number-assets", assets.size());
			w.add("time-taken", time2-time1);
			w.pop();
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

}
