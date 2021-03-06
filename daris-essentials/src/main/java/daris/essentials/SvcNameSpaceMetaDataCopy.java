package daris.essentials;

import java.util.Collection;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcNameSpaceMetaDataCopy extends PluginService {


	private Interface _defn;

	public SvcNameSpaceMetaDataCopy() {
		_defn = new Interface();
		Interface.Element me = new Interface.Element("from", StringType.DEFAULT, "The parent namespace to copy from.", 1, 1);
		me.add(new Interface.Attribute("proute", CiteableIdType.DEFAULT,
				"In a federation, specifies the route to the peer that manages this namespace.  If not supplied, then the namespace will be assumed to be local.", 0));
		_defn.add(me);
		//
		me = new Interface.Element("to", StringType.DEFAULT, "The parent namespace to copy to. The parent part of this must pre-exist.  E.g. if the from tree is '/a/b/c/d', 'from=/a/b'  and 'to=/x/y/z' then /x/y must exist and the result will be 'x/y/z/c/d' that is 'b' is renamed to 'z' in this process (create=true).", 1, 1);
		me.add(new Interface.Attribute("proute", CiteableIdType.DEFAULT,
				"In a federation, specifies the route to the peer that manages this namespace.  If not supplied, then the namespace will be assumed to be local.", 0));
		_defn.add(me);
		//
		_defn.add(new Interface.Element("create", BooleanType.DEFAULT, "By default this service expects all the recipient namespaces to pre-exist. Set to true to create the child namespaces as required. If false and namespace does not exist, that namespace is skipped."+ 
				" If they don't they are skipped.  Set this to true to create any missing namespaces.", 0, 1));
		_defn.add(new Interface.Element("list", BooleanType.DEFAULT, "List all namespaces traversed (defaults to false).", 0, 1));
		_defn.add(new Interface.Element("recurse", BooleanType.DEFAULT, "By default this service will recurse down the namespace tree. Set to false to take the top level only.", 0, 1));
	}

	public String name() {
		return "nig.namespace.metadata.copy";
	}

	public String description() {
		return "Specialised service to recursively copy (set) namespace meta-data and template meta-data from one namespace root to another. For example to c opy from namespace parent /CAPIM (from) to new parent  /projects/proj-CAPIM-101.3.1 (to).";
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

		String fromParent = args.value("from");
		String fromRoute = args.value("from/@proute");              // Local if null
		String toParent = args.value("to");
		String toRoute = args.value("to/@proute");                  // Local if null
		Boolean create = args.booleanValue("create", false);
		Boolean list = args.booleanValue("list", false);
		Boolean recurse = args.booleanValue("recurse", true);

		// See if the peer is reachable. Kind of clumsy as it fails silently with no exception
		// if you just try and access an unreachable peer
		ServerRoute srFrom = new ServerRoute(fromRoute);
		if (srFrom!=null) {
			XmlDoc.Element r = executor().execute(srFrom, "server.uuid");
			if (r.element("uuid")==null) {
				throw new Exception ("Can't reach remote 'from'  peer " + fromRoute);
			}
		}
		ServerRoute srTo = new ServerRoute(toRoute);
		if (srTo!=null) {
			XmlDoc.Element r = executor().execute(srTo, "server.uuid");
			if (r.element("uuid")==null) {
				throw new Exception ("Can't reach remote 'to' peer " + toRoute);
			}
		}

		// Recursively create namespaces and set meta-data
		copy (executor(), create, srFrom, srTo, fromParent, toParent, fromParent, toParent, list, recurse, w);
	}


	private void createNameSpace (ServiceExecutor executor, ServerRoute sr, String nameSpace) throws Throwable {
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("namespace", nameSpace);
		executor.execute(sr, "asset.namespace.create", dm.root());
	}

	private String replaceRoot (String from, String fromRoot, String toRoot) throws Throwable {
		// Replace the fromRoot component of from by the toRoot
		int nF = fromRoot.length();
		if (nF<=0) {
			throw new Exception ("The parent 'from' path has zero length");
		}
		String t = from.substring(nF+1);
		return  toRoot + "/" + t;
	}



	private void copy (ServiceExecutor executor, Boolean create, ServerRoute srFrom, ServerRoute srTo, String fromNS, String toNS, 
			String fromParent, String toParent, Boolean list, Boolean recurse, XmlWriter w) throws Throwable {


		// Check abort
		PluginTask.checkIfThreadTaskAborted();


		// Get asset
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("namespace",fromNS);
		XmlDoc.Element asset = executor().execute(srFrom, "asset.namespace.describe", dm.root());

		// Create namespace as needed/requested	
		if (!assetNameSpaceExists(executor(), srTo, toNS)) {
			if (create) {
				if (list) {
					w.add("to", new String[]{"from", fromNS, "created", "true"}, toNS);
				}
				createNameSpace (executor(), srTo, toNS);
			} else {
				if (list) {
					w.add("to", new String[]{"from", fromNS, "created", "false"}, toNS);
				}
				return;
			}
		} else {
			if (list) {
				w.add("to", new String[]{"from", fromNS, "pre-exists", "true"}, toNS);
			}
		}
		
		
		// Set namespace meta-data 
		XmlDoc.Element meta = asset.element("namespace/asset-meta");
		boolean some = false;
		if (meta!=null) {
			dm = new XmlDocMaker("args");
			dm.add("namespace", toNS);
			dm.push("asset-meta");
			Collection<XmlDoc.Element> els = meta.elements();
			if (els!=null) {
				some = true;
				for (XmlDoc.Element el : els) {
					dm.add(el);
				}
			}
			dm.pop();
		}
		if (some) {
			executor().execute(srTo, "asset.namespace.asset.meta.set", dm.root());
		}

		// Template
		XmlDoc.Element template = asset.element("namespace/template");
		some = false;
		if (template!=null) {
			dm = new XmlDocMaker("args");
			dm.add("namespace", toNS);
			dm.push("template");
			Collection<XmlDoc.Element> els = template.elements();
			if (els!=null) {
				some = true;
				for (XmlDoc.Element el : els) {
					dm.add(el);
				}
			}
			dm.pop();
		}
		if (some) {
			executor().execute(srTo, "asset.namespace.template.set", dm.root());
		}
		if (!recurse) return;
		
		// Now descend into the children namespaces	
		dm = new XmlDocMaker("args");
		dm.add("namespace", fromNS);
		XmlDoc.Element r = executor().execute(srFrom, "asset.namespace.list", dm.root());
		if (r==null) return;
		XmlDoc.Element pathEl = r.element("namespace");
		if (pathEl==null) return;
		String path = pathEl.value("@path");
		Collection<String> nss = pathEl.values("namespace");
		if (nss==null) return;
		if (nss.size()==0) return;
		//
		for (String ns : nss) {
			String fns = path + "/" + ns;
			String tns = replaceRoot (fns, fromParent, toParent);
			copy (executor(), create, srFrom, srTo, fns, tns, fromParent, toParent, list, recurse, w);
		}
	}


	private Boolean assetNameSpaceExists(ServiceExecutor executor, ServerRoute sr, String namespace) throws Throwable {

		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("namespace", namespace);
		XmlDoc.Element r = executor.execute(sr, "asset.namespace.exists", dm.root());
		return r.booleanValue("exists");
	}

}
