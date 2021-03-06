package nig.mf.plugin.pssd.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcDICOMUserCreate extends PluginService {

	private Interface _defn;

	public SvcDICOMUserCreate()  {
		_defn = new Interface();
		_defn.add(new Interface.Element("user", StringType.DEFAULT, "The DICOM username.", 1, 1));
		_defn.add(new Interface.Element("email", StringType.DEFAULT, "The email address to notify under some error handling conditions (e.g. data sent to wrong server).", 0, 1));

	}

	public Access access() {
		return ACCESS_MODIFY;
	}

	public Interface definition() {
		return _defn;
	}

	public String description() {
		return "Creates a standard  DICOM user and assigns the PSSD DICOM ingest role.  Each DICOM client AET must have a proxy DICOM user in the system.";
	}

	public String name() {
		return "om.pssd.dicom.user.create";
	}

	public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
		// Inputs
		String user = args.value("user");
		String email = args.value("email");

		// Create user
		XmlDocMaker dm = new XmlDocMaker("args");
		dm.add("domain", "dicom");
		dm.add("user", user);
		if (email!=null) dm.add("email", email);
		executor().execute("authentication.user.create", dm.root());

		// Grant role
		dm = new XmlDocMaker("args");
		dm.add("domain", "dicom");
		dm.add("user", user);
		dm.add("role", nig.mf.plugin.pssd.dicom.Role.dicomIngestRoleName());
		executor().execute("om.pssd.user.role.grant", dm.root());
	}
}
