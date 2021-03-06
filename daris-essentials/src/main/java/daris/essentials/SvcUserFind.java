package daris.essentials;

import java.util.Collection;
import java.util.Vector;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.IntegerType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcUserFind extends PluginService {


	private Interface _defn;


	public SvcUserFind() {
		_defn = new Interface();
		_defn.add(new Interface.Element("email",StringType.DEFAULT, "The user email.", 1, 1));
		_defn.add(new Interface.Element("domain",StringType.DEFAULT, "Limit the search to this authentication domain.", 0, Integer.MAX_VALUE));
		_defn.add(new Interface.Element("enabled-only",BooleanType.DEFAULT, "Only search enabled accounts (default true).", 0, 1));
	}
	public String name() {
		return "nig.user.find";
	}

	public String description() {
		return "Find user accounts with this email address.";
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

		String email = args.value("email");
		Boolean  useEnabledOnly  = args.booleanValue("enabled-only", true);

		// FInd the domains		
		Collection<String> domains = new Vector<String>();
		Collection<String> domains2 = args.values("domain");
		if (domains2!=null  && domains2.size()>0) {
			domains.addAll(domains2);
		} else {
			XmlDoc.Element r = executor().execute("authentication.domain.list");
			domains.addAll(r.values("domain"));
		}

		// Iterate through domains
		for (String domain : domains) {
			// Find the users
			XmlDocMaker dm = new XmlDocMaker("args");
			dm.add("domain", domain);
			XmlDoc.Element r = executor().execute("user.describe", dm.root());
			Collection<XmlDoc.Element> users = r.elements("user");

			for (XmlDoc.Element user : users) {
				Boolean enabled = user.booleanValue("@enabled", true);
				if (enabled==null) enabled = true;
				Boolean use = (useEnabledOnly&&enabled) || (!useEnabledOnly);
				String email2 = user.value("e-mail");
				String userName = user.value("@user");
				if (email2!=null && email2.equalsIgnoreCase(email) && use) {
					w.add("user", new String[]{"enabled", Boolean.toString(enabled)}, domain+":" + userName);
				}
			}
		}
	}
}
