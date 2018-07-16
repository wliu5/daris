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
import arc.mf.plugin.dtype.LongType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcQueryFileDistribution extends PluginService {

	private static String[] units = {"KB", "MB", "GB", "TB", "PB"};
	private static Integer cursorSize_ = 50000;
	private Long totalTime_;

	private class ValHolder {
		public Double min_;
		public Double max_;
		public  Long n_;
		public Double sum_;
		public Double wastedSum_;
		public ValHolder (Double min, Double max, Long n, Double sum, Double wastedSum) {
			min_ = min;
			max_ = max;
			n_ = n;
			//
			sum_ = sum;
			wastedSum_ = wastedSum;
		}	
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
		me = new Interface.Element("block-size", LongType.DEFAULT, "Block-size to compute wasted space. Default 4096 bytes", 0, 1);
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
		Long blockSize = args.longValue("block-size", 4096);
		
		// Set logarithmic bin width. We use a doubling of size 
		Double logBinWidth = Math.log10(2.0);

		// Generate Histogram container
		HashMap<Integer,Long> bins = new HashMap<Integer, Long>();

		// Iterate through assets and accumulate
		Boolean more = true;
		ValHolder vh = new ValHolder(1.0E15, -1.0, 0L, 0.0, 0.0);
		totalTime_ = 0L;
		AtomicInteger idx = new AtomicInteger(1);
		if (showAccum) w.push("accumulation");
		while (more) {
			more = accumulate (executor(), blockSize, noAccum, showAccum, where, size, logBinWidth, bins, idx, vh, w);
		}
		if (showAccum) {
			w.add("total-elapsed-time", totalTime_);
			w.pop();
		}
		w.add("number-assets", vh.n_);
		w.add("minimum-file-size", vh.min_);
		w.add("maximum-file-size", vh.max_);
		w.add("sum", new String[]{"units", "bytes"},  vh.sum_);
		w.add("wasted-storage", new String[]{"units", "bytes"},  vh.wastedSum_);


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


	private Boolean accumulate (ServiceExecutor executor,  Long blockSize, Boolean noAccum, Boolean showAccum, String where, 
			Integer cursorSize, Double logBinWidth, HashMap<Integer,Long> bins, 
			AtomicInteger idx, ValHolder vh, XmlWriter w) throws Throwable {
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
				Long size = asset.longValue("value");
				
				// Just make  use of the  ValHolder container to hold the sum
				// of the wasted space (for storage with the given block size) 
				// in the max element. If a files is 1.8MB and the block size is
				// 1MB the wasted space is 0.2MB
				// blocksize - (size - floor(size))
				// 
				Long f = size/blockSize;
				Long rem = size % blockSize;
				if (rem!=0) f++;
					
				Long wasted = (f * blockSize) - size;
				// 
				Double t = vh.wastedSum_ + wasted;
				vh.wastedSum_ = t;
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
				vh.sum_ = vh.sum_ + size;
				vh.min_ = Math.min(vh.min_, size);
				vh.max_ = Math.max(vh.max_, size);
				vh.n_ = vh.n_ + 1;			
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
