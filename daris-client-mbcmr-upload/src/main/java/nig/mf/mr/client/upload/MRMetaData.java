package nig.mf.mr.client.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Date;

import nig.io.LittleEndianDataInputStream;
import nig.util.DateUtil;

/**
 * Siemens VB15-VB17 RaidFile structure
 * @author nebk
 *
 */
public class MRMetaData {
	

	private File _file = null;
	private String _firstName = null;
	private String _lastName = null;
	private String _patientID = null;
	private String _fileName = null;
	private Date _dateAndTime = null;
	private String _ext = null;

	public MRMetaData (File file) throws Throwable {
		_file = file;
		_fileName = _file.getName();
	}
	
	
	public String getFileName  () {return _fileName;};
	public String getFirstName() {return _firstName;};
	public String getName() {
		if (_firstName!=null) {
			return _firstName + " " + _lastName;
		} else {
			return _lastName;
		}
	}
	public String getLastName() {return _lastName;};
	public String getID () {return _patientID;};
	public Date getAcquisitionDateTime () {return _dateAndTime;};
	public String getExtension () {return _ext;};


	public void parse () throws Throwable {
		
		// Get file extension (should be .dat)
		String[] parts = _fileName.split("\\.");
		int l = parts.length;
		_ext = parts[l-1];
		
		// Read file
		InputStream in = new FileInputStream(_file);

		// JVM Assumes Big Endian. Need to flip.
		LittleEndianDataInputStream din = new LittleEndianDataInputStream(in);

		Integer nProtHeaderLen = din.readInt();
		Integer nNrProtocols = din.readInt();
		System.out.println("nProtocols="+nNrProtocols);
		for (int i=0; i<nNrProtocols; i++) {

			// Protocol Name
			byte[] pcProtName = new byte[32];
			int idx = 0;
			while (true) {
				din.read(pcProtName, idx, 1);
				if (pcProtName[idx]==0) break;
				idx++;
			}
			String protocolName = new String(pcProtName, 0, idx);
			System.out.println("Protocol Name='"+protocolName +"'");
			
			// Read the protocol buffer
			Integer protocolLength = din.readInt();
			byte[] header = new byte[protocolLength];
			din.read(header, 0, protocolLength);
			
			// Convert to a string
			String stringHeader = new String(header);
			
//			PrintWriter p = new PrintWriter(protocolName + ".txt");
//			p.write(stringHeader);
			//
			if (protocolName.equals("Config")) {
				
				// Find things of interest
				_patientID = find ("PatientID", stringHeader);
				String patientName = find ("tPatientName", stringHeader);
				parseName (patientName);
				//
				parseDate (find ("FrameOfReference", stringHeader));
			}
//			p.close();
		}

		din.close();
		if (_patientID==null || _lastName==null || _dateAndTime==null) {
			throw new Exception("Failed to extract all of patient ID, patient Name and Date from header.");
		}
	}

	private String find (String thing, String header) throws Throwable {
		int idx = header.indexOf(thing);
		if (idx<=0) return null;
		//
		int start = header.indexOf("{", idx);
		int end = header.indexOf("}", start);
		if (start<=0 || end<=0 || end<=start) {
			return null;
		}
		
		// Items are surrounded by '" '
		return header.substring(start+3, end-3);
	}

	private void parseDate (String date) throws Throwable {
		// Of the form  1.3.12.2.1107.5.2.34.18978.1.20140718161933906.0.0.0
		if (date==null) return;
		String[] parts = date.split("\\.");
		int l = parts.length;
		if (l==14) {
			String t = parts[10];
			String t2 = t.substring(0,13);
			_dateAndTime = DateUtil.dateFromString(t2, "yyyyMMddHHmmss");
		}
	}

	private void parseName (String name) {
		String[] parts = name.split("^");
		int l = parts.length;
		if (l>=2) {
			_firstName = parts[0];
			_lastName = parts[l-1];
		} else {
			_firstName = null;
			_lastName = name;
		}
	}
	public void print () throws Throwable {
		System.out.println("  File name           = " + _fileName);
		if (_firstName!=null && _lastName!=null) {
			System.out.println("  Patient name        = " + _firstName + " " + _lastName);
		} else if (_lastName!=null) {
			System.out.println("  Patient (last) name = " + _lastName);
		}
		if (_patientID != null) {
			System.out.println("  Patient ID = " + _patientID);
		}
		if (_dateAndTime != null) {
			String tDate = DateUtil.formatDate(_dateAndTime, true, false);
			System.out.println("  Date = " + tDate);
		}
	}


	public void printToWriter (PrintWriter writer) {
	}
}
