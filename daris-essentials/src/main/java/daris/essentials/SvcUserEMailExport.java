package daris.essentials;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Vector;

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
		_defn.add(new Interface.Element("email",StringType.DEFAULT, "E-Mail the CSV report to this destination (default none).", 0, Integer.MAX_VALUE));
		_defn.add(new Interface.Element("role",StringType.DEFAULT, "The user must hold this role (wihc must be of type 'role') to be included. If you provide more than one, the user must hold at least one role in the list to be included.", 0, Integer.MAX_VALUE));
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
		String sendToEMail = args.value("email");
		Collection<String> roles = args.values("role");

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
		Vector<String> emailsUsed = new Vector<String>();     // List of emails already found. 
		// Not easy to use a HashSet for uniqueness because we need more than just the email
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
						Boolean use = email!=null && ((useEnabledOnly&&enabled) || !useEnabledOnly) && !alreadyUsed(emailsUsed, email);
						if (use) {
							if (!hasRole (executor(), roles, userName)) use = false;

						}
						if (use) {
							emailsUsed.add(email);
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
		String mimeType = "text/csv";
		if (outputs!=null) {
			outputs.output(0).setData(PluginTask.deleteOnCloseInputStream(csvFile), csvFile.length(), mimeType);
		}

		// Send via email
		if (sendToEMail!=null) {
			String uuid = executor().execute(null, "server.uuid").value("uuid");
			XmlDocMaker dm = new XmlDocMaker("args");
			dm.add("name", "mail.from");
			String from = executor().execute(null, "server.property.get", dm.root()).value("property");
			// 
			FileInputStream fis = new FileInputStream(csvFile);
			PluginService.Input in = new PluginService.Input(fis,csvFile.length(), mimeType, null);
			PluginService.Inputs ins = new PluginService.Inputs(in);
			//
			dm = new XmlDocMaker("args");
			dm.push("attachment");
			dm.add("name", "Mediaflux Emails");
			dm.add("type", mimeType);
			dm.pop();
			dm.add("to", sendToEMail);
			dm.add("subject", "Email List from server" + uuid);
			dm.add("from", from);
			executor().execute("mail.send",dm.root(),ins,null);
			fis.close();
		}


		csvFile.delete();
	}

	private Boolean hasRole (ServiceExecutor executor, Collection<String> roles, String userName) throws Throwable {
		if (roles==null) return true;
		Boolean has = false;
		for (String role : roles) {
			XmlDocMaker dm = new XmlDocMaker("args");
			dm.add("name", userName);
			dm.add("role", new String[]{"type", "role"}, role);
			dm.add("type", "user");
			XmlDoc.Element r = executor.execute("actor.have", dm.root());
			if (r.booleanValue("actor/role")) has = true;
		}
		return has;
	}

	private Boolean alreadyUsed (Vector<String> usedEMails, String email) {
		for (String usedEMail : usedEMails) {
			if (email.equals(usedEMail)) return true;
		}
		return false;
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
