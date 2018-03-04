package daris.essentials;

import java.util.Collection;
import java.util.Date;
import java.util.Vector;

import nig.mf.plugin.util.AssetUtil;
import nig.mf.pssd.plugin.util.CiteableIdUtil;
import nig.mf.pssd.plugin.util.DistributedAssetUtil;
import arc.mf.plugin.*;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcReplicateCheck extends PluginService {


	private Interface _defn;


	public SvcReplicateCheck() {
		_defn = new Interface();
		_defn.add(new Interface.Element("peer",StringType.DEFAULT, "Name of peer that objects have been replicated to.", 1, 1));
		_defn.add(new Interface.Element("where",StringType.DEFAULT, "Query predicate to restrict the selected assets on the local host. If unset, all assets are considered. If there is more than one where clause, then the second and subsequent clauses are evaluated (linearly) against the result set of the first where clause (a post filter).", 0, Integer.MAX_VALUE));
		_defn.add(new Interface.Element("size",IntegerType.DEFAULT, "Limit the accumulation loop to this number of assets per iteration (if too large, the host may run out of virtual memory).  Defaults to 5000.", 0, 1));
		_defn.add(new Interface.Element("dst", StringType.DEFAULT, "The destination parent namespace. If supplied (use '/' for root namespace), assets will actually be replicated (one at a time; not efficient). The default is no replication.", 0, 1));
		_defn.add(new Interface.Element("check-asset", BooleanType.DEFAULT, "Check modification time (see if primary has been modified after the replica) and base 10 checksum (if has content) of existing replicas (default false) as well as their existence (hugely slows the process if activated).", 0, 1));
		_defn.add(new Interface.Element("use-indexes", BooleanType.DEFAULT, "Turn on or off the use of indexes in the query. Defaults to true.", 0, 1));
		_defn.add(new Interface.Element("debug", BooleanType.DEFAULT, "Write some stuff in the log. Default to false.", 0, 1));
		_defn.add(new Interface.Element("include-destroyed", BooleanType.DEFAULT, "Include soft destroyed assets (so don't include soft destroy selection in the where predicate. Default to false.", 0, 1));
		_defn.add(new Interface.Element("list", BooleanType.DEFAULT, "List all the IDs of assets to be replicated. Default to false.", 0, 1));
		_defn.add(new Interface.Element("rep-inc", IntegerType.DEFAULT, "When debug is true, messages are written to the server log. This parameter specifies the increment to report that assets have been replicated.  Defaults to 1000. ", 0, 1));
		_defn.add(new Interface.Element("throw-on-fail", BooleanType.DEFAULT, "By default, service will fail if it fails to replicate any asset. Set to false to not fail and wirte a message in the mediaflux-server log instead for every asset that fails to replciate.", 0, 1));
		_defn.add(new Interface.Element("related", IntegerType.DEFAULT, "Specifies the number of levels of related assets (primary relationship) to be replicated.  Has no impact on the find part of the service, just the replicatioh. Defaults to 0. Specify infinity to traverse all relationships.", 0, 1));
	}
	public String name() {
		return "nig.replicate.check";
	}

	public String description() {
		return "Lists assets (both primaries, and replicas from other hosts) that haven't been replicated to the remote peer (primary algorithm is to look for the replica by asset id). You can abort during the accumulation phase once per size chunk and also during actual replication of assets (between each asset). Assets can also be actually replicated rather than just listed by specifying the destination root namespace.";
	}

	public Interface definition() {
		return _defn;
	}

	public Access access() {
		return ACCESS_ADMINISTER;
	}

	public int executeMode() {
		return EXECUTE_LOCAL;
	}

	public boolean canBeAborted() {

		return true;
	}

	public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

		// Init
		int[] idx = new int[]{1};
		int[] count = new int[]{0};
		Date date = new Date();
		String dateTime = date.toString();     // Just used to tag message in log file

		// Get inputs
		Collection<String> wheres = args.values("where");
		String peer = args.value("peer");
		String size = args.stringValue("size", "5000");
		String dst = args.value("dst");
		Boolean checkAsset = args.booleanValue("check-asset", false);
		Boolean useIndexes = args.booleanValue("use-indexes", true);
		Boolean dbg = args.booleanValue("debug", false);
		Boolean list = args.booleanValue("list", false);
		Boolean includeDestroyed = args.booleanValue("include-destroyed", false);
		Boolean throwOnFail = args.booleanValue("throw-on-fail", true);
		Integer repInc = args.intValue("rep-inc", 1000);
		Integer related = args.intValue("related", 0);

		// Find route to peer. Exception if can't reach and build in extra checks to make sure we are 
		// being very safe
		ServerRoute sr = DistributedAssetUtil.findPeerRoute(executor(), peer);
		if (sr==null) {
			throw new Exception("Failed to generated the ServerRoute for the remote host");
		}
		String uuidLocal = serverUUID(executor(), null);
		if (sr.target().equals(uuidLocal)) {
			throw new Exception ("Remote peer UUID appears to be the same as the local host (" + uuidLocal + ") - cannot proceed");
		}

		// Iterate through cursor and build list of assets 
		boolean more = true;
		Vector<String> assetIDs = new Vector<String>();
		String schemaID = schemaID(executor());

		if (schemaID!=null) {
			w.add("schema-ID", schemaID);
		}
		w.add("uuid-local", uuidLocal);
		while (more) {
			more = find (executor(),  schemaID, dateTime, wheres, peer, sr, uuidLocal, size, 
					assetIDs, checkAsset, useIndexes, dbg,  list, includeDestroyed, idx, count, w);
			if (dbg) {
				log(dateTime, "nig.replicate.check : checking for abort \n");
			}
			PluginTask.checkIfThreadTaskAborted();
		}

		// Replicate one at a time
		w.add("total-checked", count[0]);
		w.add("total-to-replicate", assetIDs.size());
		if (dbg) {
			log(dateTime, "   nig.replicate.check : total checked = " + count[0]);
			log(dateTime, "   nig.replicate.check : total to replicate = " + assetIDs.size());
		}
		if (dst!=null) {
			if (dbg) {
				log(dateTime,"Starting replication of " + assetIDs.size() + " assets");
			}
			int c = 1;
			int nRep = 0;
			for (String id : assetIDs) {
				// Check for abort
				PluginTask.checkIfThreadTaskAborted();

				// Somebody may have destroyed the asset since we made the list
				// so check it's still there
				if (AssetUtil.exists(executor(), id, false)) {

					// Print out stuff
					if (dbg) {
						int rem = c % repInc; 
						if (c==1 || rem==1) {
							log(dateTime, "nig.replicate.check: replicating asset # " + c);
						}
					}

					// Replicate
					XmlDocMaker dm = new XmlDocMaker("args");
					dm.add("id", id);
					dm.add("cmode", "push");
					dm.add("dst", dst);
					dm.push("peer");
					dm.add("name", peer);
					dm.pop();
					dm.add("related", related);
					dm.add("update-doc-types", false);
					dm.add("update-models", false);
					dm.add("allow-move", true);
					dm.add("locally-modified-only", false);             // Allows us to replicate foreign assets (that are already replicas on our primary)
					if (includeDestroyed) dm.add("include-destroyed", true);

					try {
						executor().execute("asset.replicate.to", dm.root());
						nRep++;
					} catch (Throwable t) {
						if (throwOnFail) {
							throw new Exception(t);
						} else {
							String cid = CiteableIdUtil.idToCid (executor(), id);
							w.add("id", new String[]{"cid", cid, "status", "error-sending"}, id);
							log(dateTime, "Failed to send asset " + id + " with error " + t.getMessage());
						}
					}
					c++;
				}
			}
			w.add("total-replicated", nRep);
			if (dbg) {
				log(dateTime, "   nig.replicate.check : total replicated = " + nRep);
			}
		}
	}

	private void log (String dateTime, String message) {
		System.out.println(dateTime + " : " + message);
	}

	private String serverUUID(ServiceExecutor executor, String proute) throws Throwable {

		XmlDoc.Element r = executor.execute(new ServerRoute(proute), "server.uuid");
		return r.value("uuid");
	}

	private boolean find (ServiceExecutor executor, String schemaID, String dateTime, Collection<String> wheres, String peer, ServerRoute sr, String uuidLocal, String size, 
			Vector<String> assetList, Boolean checkAsset,  Boolean useIndexes, Boolean dbg, Boolean list,
			Boolean includeDestroyed, int[] idx, int[] count, XmlWriter w)	throws Throwable {

		// Find local  assets  with the given query. We work through the cursor else
		// we may run out of memory
		if (dbg) log(dateTime, "nig.replicate.check : find assets on primary in chunk starting with idx = " + idx[0]);
		XmlDocMaker dm = new XmlDocMaker("args");

		if (includeDestroyed) {
			if (wheres!=null) {
				boolean first = true;
				for (String where : wheres) {
					if (first)  {
						String where2 = where += " and ((asset has been destroyed) or (asset has not been destroyed))";
						first = false;
						dm.add("where", where2);						
					} else {
						dm.add("where", where);
					}
				}
			} else {
				dm.add("where",  "((asset has been destroyed) or (asset has not been destroyed))");
			}
			dm.add("include-destroyed", true);			
		} else {
			if (wheres!=null) {
				for (String where : wheres) {
					dm.add("where", where);
				}
			}
		}


		dm.add("idx", idx[0]);
		dm.add("size", size);
		dm.add("pdist", 0);
		dm.add("action", "get-meta");
		dm.add("use-indexes", useIndexes);
		if (dbg) {
			System.out.println("Primary query = " + dm.root());
		}
		XmlDoc.Element r = executor().execute("asset.query", dm.root());
		if (r==null) return false;  
		Collection<XmlDoc.Element> assets = r.elements("asset");
		if (assets==null) return false;
		count[0] += assets.size();

		// Get the cursor and increment for next time
		XmlDoc.Element cursor = r.element("cursor");
		boolean more = !(cursor.booleanValue("total/@complete"));
		if (more) {
			Integer next = cursor.intValue("next");
			idx[0] = next;
		}

		// See if the replicas exist on the peer - make a list of rids to find
		dm = new XmlDocMaker("args");	
		for (XmlDoc.Element asset : assets) {
			// Get the asset id, and the rid (asset may already be a replica from elsewhere)
			String id = asset.value("@id");

			// If the asset is already a replica, its rid remains the same
			// when replicated to another peer
			String rid = asset.value("rid");    

			// If primary, set expected rid on remote peer else retain extant rid from foreign system
			String rid2 = setRID (id, rid, schemaID, uuidLocal);
			dm.add("rid", rid2);
		}

		// Now check if they exist
		if (dbg) {
			log(dateTime, "   nig.replicate.check : checking if " + assets.size() + " assets exist on DR");
		}
		XmlDoc.Element r2 = executor.execute(sr, "asset.exists", dm.root());
		if (r2==null) return more;
		Collection<XmlDoc.Element> results = r2.elements("exists");


		// Create a list of assets that don't have replicas that we want to replicate
		if (dbg) {
			log(dateTime, "   nig.replicate.check : iterate through " + results.size() + " results and build list for replication.");
		}

		// Iterate through the remote peer call to asset.exists results. Each asset checked has a return value
		Integer n = null;
		for (XmlDoc.Element result : results) {

			// Fetch the rid and pull out the id
			String rid = result.value("@rid");

			// Now, if the rid is from a foreign server, the id in the rid (uuid.id) is foreign also
			// Therefore, if the asset is a foreign replica, we must fetch it's true primary
			// ID on the local server
			String primaryID = null;
			if (!rid.equals(uuidLocal)) {
				dm = new XmlDocMaker("args");
				dm.add("rid", rid);
				r = executor.execute ("asset.exists", dm.root());
				primaryID = r.value("exists/@id");
				if (primaryID==null) {
					// This should not occur because we know the asset exists
					throw new Exception("Failed to find primary asset ID for foreign replica with rid="+rid);
				}
			} else {
				String[] t = rid.split("\\.");
				if (n==null) n = t.length;           // They are all the same length
				primaryID = t[n-1];
			}
			
			// Now take action depending on if the asset exists on the DR or not
			String cid = CiteableIdUtil.idToCid(executor, primaryID);               // May be null
			if (result.booleanValue()==false) {
				if (list) {
					XmlDoc.Element asset = AssetUtil.getAsset(executor, null, primaryID);
					String type = asset.value("asset/type");
					String csum = asset.value("asset/content/csum[@base='10']");    // May be null (no content)
					if (type!=null && type.equals("content/unknown")) type = null;
					//
		            XmlDoc.Element csize = asset.element("asset/content/size");
					String csizeV = csize.value();
					String csizeU = csize.value("@h");
					String t = null;
					if (csize!=null) {
						t = csizeV +" " +csizeU;
					}
					//
		            w.add("id", new String[]{"exists", "false", "cid", cid, "type", type, "csum-base10", csum, "csize", t},  primaryID);
				}
				assetList.add(primaryID);
			} else {

				// The asset exists as a replica, but perhaps it's been modified.
				// Very time consuming...
				if (checkAsset) {
					// See if the primary has been modified since the replica was made
					XmlDoc.Element asset = AssetUtil.getAsset(executor, null, primaryID);
					Date mtime = asset.dateValue("asset/mtime");
					String type = asset.value("asset/type");
					String csum = asset.value("asset/content/csum[@base='10']");


					// Use id overload e.g. "asset.get :id rid=1004.123455"
					dm = new XmlDocMaker("args");
					dm.add("id","rid="+rid);
					XmlDoc.Element remoteAsset = executor.execute(sr, "asset.get", dm.root());

					Date mtimeRep = remoteAsset.dateValue("asset/mtime");
					String cidRep = remoteAsset.value("asset/cid");            // Same for primary and replica
					String csumRep = remoteAsset.value("asset/content/csum[@base='10']");
					if (dbg) {
						if (mtime!=null && mtimeRep!=null) {
							log(dateTime, "      nig.replicate.check : mtimes=" + mtime + ", " + mtimeRep);
						} else {			
							log(dateTime, "      nig.replicate.check : mtimes are unexpectedly null for asset " + rid);
						}
						if (csum!=null && csumRep!=null) {
							log(dateTime, "      nig.replicate.check : check sums (base 10) =" + csum + ", " + csumRep);
						}
					}
					if (csum!=null) {
						if (csumRep!=null) {
							if (mtime.after(mtimeRep) || !csum.equals(csumRep)) {
								w.add("id", new String[]{"type", type, "exists", "true", "cid", cidRep, "mtime-primary", mtime.toString(), "mtime-replica", mtimeRep.toString(),
										"csum-base10-primary", csum, "csum-base10-replica", csumRep},  primaryID);
								assetList.add(primaryID);	
							}
						} else {
							w.add("id", new String[]{"type", type, "exists", "true", "cid", cidRep, "mtime-primary", mtime.toString(), "mtime-replica", mtimeRep.toString(),
									"csum-base10-primary", csum, "csum-base10-replica", "missing"},  primaryID);
							assetList.add(primaryID);	
						}
					} else {
						if (mtime.after(mtimeRep)) {
							w.add("id", new String[]{"type", type, "exists", "true", "cid", cidRep, "mtime-primary", mtime.toString(), "mtime-replica", mtimeRep.toString()},
									primaryID);
							assetList.add(primaryID);	
						}
					}
				}
			}
		}
		//
		return more;
	}


	static public String schemaID (ServiceExecutor executor) throws Throwable {
		XmlDoc.Element r = executor.execute("schema.self.describe");

		// Fetch the schema ID.  If null, we are in the primary schema
		String iSchema = r.value("schema/@id");

		// TBD return actual value once schema code settles
		return null;
	}

	/**
	 *  Set the expected RID on the DR server. If the asset is in a numbered schema
	 *     that has to be accounted for.
	 *     
	 * @param id
	 * @param rid
	 * @param schemaID
	 * @param uuidLocal
	 * @return
	 * @throws Throwable
	 */
	static public String setRID (String id, String rid, String schemaID, String uuidLocal) throws Throwable  {
		if (rid!=null) return rid;
		if (schemaID==null) {

			// No numbered schema (primary schema)
			return uuidLocal + "." + id;
		} else {
			// We are in a numbered schema
			return uuidLocal + "." + schemaID + "." + id;
		}
	}
}
