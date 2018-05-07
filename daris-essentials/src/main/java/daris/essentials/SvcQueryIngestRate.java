package daris.essentials;

import java.util.Collection;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcQueryIngestRate extends PluginService {

	public static enum StorageUnit {
		MB, GB, TB, PB;
		// Method to convert an amount of storage in bytes into the relevant unit attached to the object instance
		public XmlDoc.Element convert(long storage, String purpose) throws Throwable{
			double divMB = 1.0e6;
			double divGB = 1.0e9;
			double divTB = 1.0e12;
			double divPB = 1.0e15;
			double quota=0;
			String[] attributes = new String[2];
			switch(this){
			case MB:
				quota = storage / divMB;
				break;
			case GB:
				quota = storage / divGB;
				break;
			case TB:
				quota = storage / divTB;
				break;
			case PB:
				quota = storage / divPB;
				break;
			}
			attributes[0] = "units";
			attributes[1] = this.toString();
			XmlDocMaker dm = new XmlDocMaker("root");
			dm.add(purpose,attributes, quota);
			return dm.root().element(purpose);
		}

		/**
		 * Convert the storage amount to a value with the current units
		 * 
		 * @param storage
		 * @return
		 * @throws Throwable
		 */
		public String convertToString (long storage) throws Throwable {
			XmlDoc.Element r = convert (storage, "value");
			String value = r.value();
			String units = r.value("@units");
			return value + " " + units;
		}

		// Sets the storage units on the object instance
		public static StorageUnit fromString(String s, StorageUnit defaultValue){
			if(s!=null){
				StorageUnit[] vs = values();
				for(int i=0;i<vs.length;i++){
					if(vs[i].name().equalsIgnoreCase(s)){
						return vs[i];
					}
				}
			}
			return defaultValue;
		}
	}

	private Interface _defn;

	public SvcQueryIngestRate() {
		_defn = new Interface();
		Interface.Element me = new Interface.Element("where", StringType.DEFAULT, "Predicate to select assets", 1, 1);
		_defn.add(me);
		me = new Interface.Element("year", StringType.DEFAULT, "The calendar year of interest (no default)", 1, Integer.MAX_VALUE);
		_defn.add(me);
		me = new Interface.Element("month", StringType.DEFAULT, "The calendar month(s) of interest (defaults to all). Use 3 letters: Jan, Feb, Mar etc", 0, Integer.MAX_VALUE);
		_defn.add(me);
	}

	public String name() {
		return "nig.query.ingest.rate";
	}

	public String description() {
		return "Specialised service to list storage data ingest rates (i.e. by looking at ctime) per month for the selected assets.";
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
		Collection<String> years = args.values("year");				
		Collection<String> months = args.values("month");				
		//
		for (String year : years) {
			Double totalSize = 0.0;
			for (int i=0;i<12;i++) {
				if (monthIncluded(i, months)) {
					PluginTask.checkIfThreadTaskAborted();
					XmlDocMaker dm = new XmlDocMaker("args");
					String where = where2 + " and " + makeWhere(i, year);
					String my = makeMonth (i, year);
					dm.add("where", where);
					dm.add("action", "sum");
					dm.add("xpath", "content/size");
					XmlDoc.Element r = executor().execute("asset.query", dm.root());
					w.push("ingest");
					w.add("month",  my);
					String nAssets = r.value("value/@nbe");
					String bytes = r.value("value");
					//
					Double td = 0.0;
					if (bytes!=null) td = Double.parseDouble(bytes);
					totalSize += td;
					//
					w.add("size", new String[]{"n", nAssets, "bytes", bytes}, bytesToGBytes (bytes));
					w.pop();
				}
			}
			w.add("total-size", new String[]{"unit", "bytes"},  totalSize);
		}
	}


	private Boolean monthIncluded (int im, Collection<String> months) {
		if (months==null) return true;
		for (String month : months) {
		   if (month.equalsIgnoreCase("Jan")) {
			   if (im==0) return true;
		   } else if (month.equalsIgnoreCase("Feb")) {
			   if (im==1) return true;
		   } else if (month.equalsIgnoreCase("Mar")) {
			   if (im==2) return true;
		   } else if (month.equalsIgnoreCase("Apr")) {
			   if (im==3) return true;
		   } else if (month.equalsIgnoreCase("May")) {
			   if (im==4) return true;
		   } else if (month.equalsIgnoreCase("Jun")) {
			   if (im==5) return true;
		   } else if (month.equalsIgnoreCase("Jul")) {
			   if (im==6) return true;
		   } else if (month.equalsIgnoreCase("Aug")) {
			   if (im==7) return true;
		   } else if (month.equalsIgnoreCase("Sep")) {
			   if (im==8) return true;
		   } else if (month.equalsIgnoreCase("Oct")) {
			   if (im==9) return true;
		   } else if (month.equalsIgnoreCase("Nov")) {
			   if (im==10) return true;
		   } else if (month.equalsIgnoreCase("Dec")) {
			   if (im==11) return true;
		   } 
		}
		return false;
	}
	private String makeWhere (int i, String year) throws Throwable {
		if (i==0) {
			return "ctime>='01-Jan-" + year + " 00:00:00' and ctime<='31-Jan-" + year + " 24:00:00'";
		} else if (i==1) {
			return "ctime>='01-Feb-" + year + " 00:00:00' and ctime<='29-Feb-" + year + " 24:00:00'";    // Allow for leap year
		} else if (i==2) {
			return "ctime>='01-Mar-" + year + " 00:00:00' and ctime<='31-Mar-" + year + " 24:00:00'";
		} else if (i==3) {
			return "ctime>='01-Apr-" + year + " 00:00:00' and ctime<='30-Apr-" + year + " 24:00:00'";
		} else if (i==4) {
			return "ctime>='01-May-" + year + " 00:00:00' and ctime<='31-May-" + year + " 24:00:00'";
		} else if (i==5) {
			return "ctime>='01-Jun-" + year + " 00:00:00' and ctime<='30-Jun-" + year + " 24:00:00'";
		} else if (i==6) {
			return "ctime>='01-Jul-" + year + " 00:00:00' and ctime<='31-Jul-" + year + " 24:00:00'";
		} else if (i==7) {
			return "ctime>='01-Aug-" + year + " 00:00:00' and ctime<='31-Aug-" + year + " 24:00:00'";
		} else if (i==8) {
			return "ctime>='01-Sep-" + year + " 00:00:00' and ctime<='30-Sep-" + year + " 24:00:00'";
		} else if (i==9) {
			return "ctime>='01-Oct-" + year + " 00:00:00' and ctime<='31-Oct-" + year + " 24:00:00'";
		} else if (i==10) {
			return "ctime>='01-Nov-" + year + " 00:00:00' and ctime<='30-Nov-" + year + " 24:00:00'";
		} else if (i==11) {
			return "ctime>='01-Dec-" + year + " 00:00:00' and ctime<='31-Dec-" + year + " 24:00:00'";
		} else {
			return null;
		}
	}


	private String makeMonth (int i, String year) throws Throwable {
		if (i==0) {
			return "Jan-" + year;
		} else if (i==1) {
			return "Feb-" + year;
		} else if (i==2) {
			return "Mar-" + year;
		} else if (i==3) {
			return "Apr-" + year;
		} else if (i==4) {
			return "May-" + year;
		} else if (i==5) {
			return "Jun-" + year;
		} else if (i==6) {
			return "Jul-" + year;
		} else if (i==7) {
			return "Aug-" + year;
		} else if (i==8) {
			return "Sep-" + year;
		} else if (i==9) {
			return "Oct-" + year;
		} else if (i==10) {
			return "Nov-" + year;
		} else if (i==11) {
			return "Dec-" + year;
		} else {
			return null;
		}
	}


	private String bytesToGBytes (String bytes) throws Throwable {
		if (bytes==null) return "0 GB";
		Long s = Long.parseLong(bytes);
		StorageUnit su = StorageUnit.fromString("GB", StorageUnit.GB); 
		return su.convertToString(s);
	}

}
