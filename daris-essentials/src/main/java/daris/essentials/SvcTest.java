package daris.essentials;

import java.util.Date;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.DoubleType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.LongType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcTest extends PluginService {


	private Interface _defn;
	private Integer idx_ = 1;
	private Integer count_ = 0;


	public SvcTest() {
		_defn = new Interface();
		_defn.add(new Interface.Element("where",StringType.DEFAULT, "Query predicate to restrict the selected assets on the local host. If unset, all assets are considered.", 0, 1));
		_defn.add(new Interface.Element("size", LongType.DEFAULT, "File size.", 0, 1));
		_defn.add(new Interface.Element("block-size", LongType.DEFAULT, "Block size", 0, 1));
		_defn.add(new Interface.Element("use-indexes", BooleanType.DEFAULT, "Turn on or off the use of indexes in the query. Defaults to true.", 0, 1));
		_defn.add(new Interface.Element("debug", BooleanType.DEFAULT, "Write some stuff in the log. Default to false.", 0, 1));
	}
	public String name() {
		return "nig.testing";
	}

	public String description() {
		return "Test service.";
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

		w.add("max-long", Long.MAX_VALUE);
		
		Long size = args.longValue("size");
		Long blockSize = args.longValue("block-size");
		w.add("size", size);
		w.add("block-size", blockSize);
		//
		Long f = size/blockSize;
		Long r = size % blockSize;
		if (r!=0) f++;
			
		Long wasted = (f * blockSize) - size;
		w.add("wasted", wasted);
		
		/*
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("name", "VicNode:nkilleen3");
		dm.add("type", "user");
		dm.add("role", new String[]{"type", "role"}, "VicNode:cryoem-operator");
		executor().execute("actor.grant", dm.root());
*/		
		/*
		

		// Init
		idx_ = 1;
		count_ = 0;
		Date date = new Date();
		String dateTime = date.toString();     // Just used to tag message in log file

		// Get inputs
		String where = args.value("where");
		String size = args.stringValue("size", "10000");
		Boolean useIndexes = args.booleanValue("use-indexes", true);
		Boolean dbg = args.booleanValue("debug", false);


		// Iterate through cursor and build list of assets 
		boolean more = true;
		while (more) {
			more = find (executor(),  dateTime, where,  size, useIndexes, 
					dbg, w);
			if (dbg) {
				log(dateTime, "nig.testing : checking for abort \n");
			}
			PluginTask.checkIfThreadTaskAborted();
		}
		w.add("total-counted", count_);
*/
	}

	private static void log (String dateTime, String message) {
		System.out.println(dateTime + " : " + message);
	}



	private boolean find (ServiceExecutor executor,  String dateTime, String where,  String size, 
			 Boolean useIndexes, Boolean dbg,
			XmlWriter w)	throws Throwable {

		// Find local  assets  with the given query. We work through the cursor else
		// we may run out of memory
		if (dbg) log(dateTime, "nig.testing : find assets on primary in chunk starting with idx = " + idx_);
		XmlDocMaker dm = new XmlDocMaker("args");
		if (where!=null) dm.add("where", where);
		dm.add("idx", idx_);
		dm.add("size", size);
		dm.add("pdist", 0);
		dm.add("use-indexes", useIndexes);
		XmlDoc.Element r = executor().execute("asset.query", dm.root());
		if (r==null) return false;  
		/*
		Collection<XmlDoc.Element> ids = r.elements("id");
		if (ids==null) {
			return false;
		}
		count_ += ids.size();
		*/

		// Get the cursor and increment for next time
		XmlDoc.Element cursor = r.element("cursor");
		boolean more = !(cursor.booleanValue("total/@complete"));
		count_ += cursor.intValue("count");
		if (more) {
			Integer next = cursor.intValue("next");
			idx_ = next;
		}
		//
		return more;
	}
}
