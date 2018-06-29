package daris.essentials;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;

import arc.mf.plugin.*;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcUserEMailExport extends PluginService {


	private Interface _defn;
	private static final String CSV_DELIMITER = ",";


	public SvcUserEMailExport() {
		_defn = new Interface();
		_defn.add(new Interface.Element("enabled-only",BooleanType.DEFAULT, "Only search enabled accounts (default true).", 0, 1));
		_defn.add(new Interface.Element("exclude",StringType.DEFAULT, "Exclude this domain.", 0, Integer.MAX_VALUE));
	}
	public String name() {
		return "nig.user.email.export";
	}

	public String description() {
		return "Find email addresses from user accounts and optionally export to a CSV file.";
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

		return false;
	}

	@Override
	public int minNumberOfOutputs() {
		return 0;
	}

	@Override
	public int maxNumberOfOutputs() {
		return 1;
	}


	public void execute(XmlDoc.Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {

		Boolean  useEnabledOnly  = args.booleanValue("enabled-only", true);
		Collection<String> excludes = args.values("exclude");

		// FInd the domains		
		XmlDoc.Element r = executor().execute("authentication.domain.describe");
		Collection<XmlDoc.Element> domains = r.elements("domain");

		// CSV temporary file and Headers
		File csvFile = PluginTask.createTemporaryFile(".csv");
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(csvFile)));
		{
			StringBuilder sb = new StringBuilder();
			sb.append("User,AccountEnabled,eMail");
			sb.append(CSV_DELIMITER);
			out.println(sb.toString());	
		}


		// Iterate through domains
		for (XmlDoc.Element domain : domains) {
			String domainName = domain.value("@name");
			if (keepDomain (domainName, excludes)) {
				// Describe domain to get details
				String type = domain.value("@type");
				String authority = domain.value("@authority");
				String protocol = domain.value("@protocol");

				// Find the users and iterate
				XmlDocMaker dm = new XmlDocMaker("args");
				dm.add("domain", domainName);
				if (authority!=null) {
					dm.add("authority", new String[]{"protocol", protocol}, authority);
				}
				r = executor().execute("authentication.user.describe", dm.root());
				Collection<XmlDoc.Element> users = r.elements("user");
				if (users!=null) {
					for (XmlDoc.Element user : users) {
						Boolean enabled = user.booleanValue("@enabled", true);
						String email = user.value("e-mail");
						String userName = domainName + ":" + user.value();
						Boolean use = email!=null && ((useEnabledOnly&&enabled) || !useEnabledOnly);

						if (use) {
							w.add("email", new String[]{"user", userName, "enabled", Boolean.toString(enabled)}, email);
							//
							StringBuilder sb = new StringBuilder();
							sb.append(userName);
							sb.append(CSV_DELIMITER);
							sb.append(Boolean.toString(enabled));
							sb.append(CSV_DELIMITER);
							sb.append(email);
							out.println(sb.toString());			
						}
					}
				}
			}
		}
		out.close();

		// Write CSV file
		if (outputs!=null) {
			outputs.output(0).setData(PluginTask.deleteOnCloseInputStream(csvFile), csvFile.length(), "text/csv");
		}
		csvFile.delete();
	}


	private Boolean keepDomain (String domain, Collection<String> excludes) {
		if (excludes==null) return true;
		for (String exclude : excludes) {
			if (domain.equals(exclude)) return false;
		}
		return true;
	}

	private XmlDoc.Element describeDomain (ServiceExecutor executor, String domain) throws Throwable {
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("domain", domain);
		return executor.execute("authentication.domain.describe", dm.root());
	}
}
