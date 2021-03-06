package nig.mf.mr.client.upload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.apache.commons.io.FileUtils;

import nig.compress.ZipUtil;
import nig.iio.siemens.MBCRawUploadUtil;
import nig.mf.MimeTypes;
import nig.mf.client.util.ClientConnection;
import nig.mf.client.util.AssetUtil;
import nig.util.DateUtil;
import arc.mf.client.ServerClient;
import arc.streams.StreamCopy;
import arc.streams.StreamCopy.AbortCheck;
import arc.xml.XmlDoc;
import arc.xml.XmlStringWriter;




public class MBCMRUpload {

	public static final String FILE_NAME_PREFIX = "meas_";

	//
	private static final String RAW_STUDY_DOC_TYPE = "daris:siemens-raw-mr-study";
	private static final String RAW_SERIES_DOC_TYPE = "daris:siemens-raw-mr-series ";

	// This is the NAS host as seen from the NAS host as the MR client is run on
	// that host
	private static final String DEFAULT_SRC_PATH = "/home/meduser/MR/Raw";
	private static final String DEFAULT_LOGGER_PATH = "/home/meduser/MR/Raw_Archive_Upload_Logs";

	// Only one study type as it may hold both PET and CT DataSets
	private static final String[] PSSD_STUDY_TYPES = {
		"Magnetic Resonance Imaging", "Quality Assurance", "Unspecified" };

	// The authenticating security token must be made with :app == to this string.
	// The token must also hold the appropriate permissions (roles daris:pssd.object.admin and daris:pssd.model.user)
	//	to allow it to access the PSSD Project or namespace into which it is uploading data.
	private static final String TOKEN_APP = "MBIC-MR-Raw-Upload";

	// This class sets the defaults for arguments that can be passed in
	// to the main program
	private static class Options {

		public String  path = null;
		public String  logpath = null;
		public boolean chksum = true;
		public boolean delete = true;
		public boolean expire = false;		
		public boolean logger = true;
		public String  id = null;
		public boolean decrypt = true;
		public String sleep = null;
		public String subjectFindMethod = "name";

		//
		public void print () {
			System.out.println("path          = " + path);
			System.out.println("find-method   = " + subjectFindMethod);
			System.out.println("no-chksum     = " + !chksum);
			System.out.println("no-delete     = " + !delete);
			System.out.println("expire        = " + expire);
			System.out.println("no-log        = " + !logger);
			System.out.println("logpath       = " + logpath);
			System.out.println("id            = " + id);
			System.out.println("no-decrypt    = " + !decrypt);
			if (sleep!=null) System.out.println("sleep         = " + sleep + " minutes");
		}
	}


	public static final String HELP_ARG = "-help";
	public static final String PATH_ARG = "-path";
	public static final String LOGPATH_ARG = "-logpath";
	public static final String NOLOG_ARG = "-no-log";
	public static final String NOCHKSUM_ARG = "-no-chksum";
	public static final String NODELETE_ARG = "-no-delete";
	public static final String EXPIRE_ARG = "-expire";
	public static final String ID_ARG = "-id";
	public static final String DECRYPT_ARG = "-no-decrypt";
	public static final String SLEEP_ARG = "-sleep";
	public static final String FIND_ARG = "-find-method";
	public static final String DEST_ARG = "-dest";  // Consumed by wrapper



	/**
	 * 
	 * the main function of this command line tool.
	 * 
	 * @param args
	 * 
	 */
	public static void main(String[] args) throws Throwable {

		// Parse user inputs
		Options ops = new Options();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase(HELP_ARG)) {
				printHelp();
				System.exit(0);
			} else if (args[i].equalsIgnoreCase(PATH_ARG)) {
				ops.path = args[++i];
			} else if (args[i].equalsIgnoreCase(LOGPATH_ARG)) {
				ops.logpath = args[++i];
			} else if (args[i].equalsIgnoreCase(FIND_ARG)) {
				ops.subjectFindMethod = args[++i];
				if (!(ops.subjectFindMethod.equals("name") ||
						ops.subjectFindMethod.equals("id") ||
						ops.subjectFindMethod.equals("name+id") ||
						ops.subjectFindMethod.equals("name+dob"))) {
					throw new Exception("Illegal value '"
							+ ops.subjectFindMethod
							+ "' for argument -find-method");
				}
			} else if (args[i].equalsIgnoreCase(SLEEP_ARG)) {
				ops.sleep = args[++i];
			} else if (args[i].equalsIgnoreCase(NOCHKSUM_ARG)) {
				ops.chksum = false;
			} else if (args[i].equalsIgnoreCase(NODELETE_ARG)) {
				ops.delete = false;
			} else if (args[i].equalsIgnoreCase(EXPIRE_ARG)) {
				ops.expire = true;
			} else if (args[i].equalsIgnoreCase(NOLOG_ARG)) {
				ops.logger = false;
			} else if (args[i].equalsIgnoreCase(ID_ARG)) {
				ops.id = args[++i];
			} else if (args[i].equalsIgnoreCase(DECRYPT_ARG)) {
				ops.decrypt = false;
			} else if (args[i].equalsIgnoreCase(DEST_ARG)) {
				// Consumed by wrapper
				i++;
			} else {
				System.err.println("MBCMRUpload: error: unexpected argument = " + args[i]);
				printHelp();
				System.exit(1);	 
			}
		}
		//
		if (ops.path == null) ops.path = DEFAULT_SRC_PATH;
		MBCRawUploadUtil.checkPath (ops.path);

		if (ops.id==null) {
			System.err.println("MBCMRUpload: you must specify the -id argument.");
		}

		// Create logger
		PrintWriter logger = MBCRawUploadUtil.createFileLogger(ops.logger, ops.logpath, DEFAULT_LOGGER_PATH);


		if (!ops.chksum) {
			if (ops.delete){
				MBCRawUploadUtil.log (logger, "*** Disabling deletion of input as check sum has been turned off");
				ops.delete = false;
			}
		}
		//
		ops.print();

		// Have a sleep
		if (ops.sleep!=null) {
			MBCRawUploadUtil.log (logger, "\nSleeping for " + ops.sleep + " minutes");
			Float s = Float.parseFloat(ops.sleep);
			if (s<0) throw new Exception ("Sleep period must be positive");
			s *= 60.0f * 1000.0f;                       // milli seconds
			Integer s2 = s.intValue();
			Thread.sleep(s2);
			MBCRawUploadUtil.log (logger, "   Waking from sleep");
		}

		// Upload data
		MBCRawUploadUtil.log (logger, "");
		upload(logger, ops);
		if (logger!=null) {
			logger.flush();
			logger.close();
		}


	}


	private static MFSession openMFSession (Options ops) throws Throwable {
		ConnectionSettings settings = new ConnectionSettings(null);
		//
		boolean useHttp = false;
		boolean encrypt = false;
		String host = ClientConnection.getProperty("mf.host");
		//
		String p = ClientConnection.getProperty("mf.port");
		int port = Integer.parseInt(p);
		//
		String transport = ClientConnection.getProperty("mf.transport");
		if (transport.equalsIgnoreCase("TCPIP")) {
			useHttp = false;
			encrypt = false;
		} else if (transport.equalsIgnoreCase("HTTP")) {
			useHttp = true;
			encrypt = false;
		} else if (transport.equalsIgnoreCase("HTTPS")) {
			useHttp = true;
			encrypt = true;
		} else {
			throw new Exception("Unexpected transport: " + transport + ", expected one of [tcpip,http,https]");
		}
		String token = ClientConnection.getProperty("mf.token");
		String domain = ClientConnection.getProperty("mf.domain");
		String user = ClientConnection.getProperty("mf.user");
		String password = ClientConnection.getProperty("mf.password");
		//
		if (!domain.equals("") && !user.equals("") && !password.equals("")) {
			settings.setServer(host, port, useHttp, encrypt);
		}
		if (!token.equals("")) {
			settings.setToken(token);
			settings.setApp(TOKEN_APP);
		}
		settings.setUserCredentials(domain, user, password);

		// Generate session object and start keep alive pings
		MFSession session = new MFSession(settings);
		session.startPingServerPeriodically(60000);   // ms 
		return session;
	}

	private static void upload (PrintWriter logger, Options ops) throws Throwable {

		// This class keeps the session alive.  One can also fetch a ServerClient.Connection
		// object for occasional use.
		MFSession session = openMFSession (ops);
		try {

			// Check CID is for this server
			if (ops.id!=null) {
				ServerClient.Connection cxn = session.connect();
				nig.mf.pssd.client.util.CiteableIdUtil.checkCIDIsForThisServer(cxn, ops.id, true);
				cxn.close();
			}


			// Iterate over all files in directory or upload given
			File path = new File(ops.path);
			if (path.isDirectory()) {
				File[] files = path.listFiles();
				if (files.length> 0) { 
					// Handle one more directory layer if present
					for (int i=0; i<files.length; i++) {
						if (files[i].isDirectory()) {
							MBCRawUploadUtil.log (logger,"Descending into directory : " + files[i].getAbsolutePath().toString());

							File[] mrFiles = files[i].listFiles();
							if (mrFiles.length> 0) { 
								for (int j=0; j<mrFiles.length; j++) {
									uploadFile (session, mrFiles[j], ops, logger);
								}
							}

							// See if we can delete the directory as well (if no longer holds any files)
							if (ops.delete) {
								mrFiles = files[i].listFiles();
								if (mrFiles.length==0) {
									MBCRawUploadUtil.log (logger,"     Deleting directory : " + files[i].getAbsolutePath().toString());
									MBCRawUploadUtil.deleteFile(files[i], logger);		
								} else {
									MBCRawUploadUtil.log (logger,"*** Directory : " + files[i].getAbsolutePath().toString() + " is not empty; cannot delete");
								}
							}
						} else {
							uploadFile (session, files[i], ops, logger);
						}
					}
				} else {
					MBCRawUploadUtil.log (logger,"*** No files to upload in : " + path.toString());
				}

			} else {
				uploadFile (session, path, ops, logger);
			}
		} finally {
			session.stopPingServerPeriodically();
		}
	}


	private static void uploadFile (MFSession session, File path, Options ops,  PrintWriter logger) throws Throwable {

		MBCRawUploadUtil.log (logger, "");
		MBCRawUploadUtil.log (logger, "Processing file " + path.toString());

		// Parse directory name
		MRMetaData pm = new MRMetaData (path);
		try {
			pm.parse();
		} catch (Throwable t) {
			MBCRawUploadUtil.log (logger, "   *** Failed to parse file into meta-data - skipping file");
			MBCRawUploadUtil.log (logger, "   ***    with error " + t.getMessage() + "'");
			return;
		}
		pm.print();
		if (logger!=null) {
			pm.printToWriter(logger);
		}

		// Filter out everything but raw data files
		String ext = pm.getExtension();
		if (!ext.equalsIgnoreCase("dat")) {
			MBCRawUploadUtil.log (logger, "   *** File not a raw Siemens MR file - skipping");
			return;
		}

		// Parse subject find method (values checked to be correct earlier)
		MBCRawUploadUtil.SUBJECT_FIND_METHOD subjectFindMethod = null;
		if (ops.subjectFindMethod.equals("name")) {
			subjectFindMethod = MBCRawUploadUtil.SUBJECT_FIND_METHOD.NAME;
		} else if (ops.subjectFindMethod.equals("name+dob")) {
			subjectFindMethod = MBCRawUploadUtil.SUBJECT_FIND_METHOD.NAME_DOB;
		} else if (ops.subjectFindMethod.equals("id")) {
			subjectFindMethod = MBCRawUploadUtil.SUBJECT_FIND_METHOD.ID;
		} else if (ops.subjectFindMethod.equals("name+id")) {
			subjectFindMethod = MBCRawUploadUtil.SUBJECT_FIND_METHOD.NAME_ID;
		}

		// See if we can find the subject.  Null if we didn't find it or multiples
		ServerClient.Connection cxn = session.connect();
		String subjectID = MBCRawUploadUtil.findSubjectAsset (cxn, null, ops.id, false,
				subjectFindMethod, pm.getFirstName(), pm.getLastName(), pm.getID(), pm.getDOB(), logger);
		cxn.close();

		// Upload
		if (subjectID != null) {
			createPSSDAssets (session, path, pm, subjectID, ops, logger);
		} else {
			// Skip uploading this one as there is no Subject and this client does not create Subjects
		}
	}


	private static void createPSSDAssets (MFSession session, File file, MRMetaData pm, String subjectCID,
			Options ops, PrintWriter logger) throws Throwable {


		// Look for MR raw STudy associated with this Patient
		String rawStudyCID = findRawStudy  (session, pm, null, subjectCID);

		// Create Study if needed
		if (rawStudyCID==null) {
			rawStudyCID = createRawStudy (session, pm, subjectCID);
			MBCRawUploadUtil.log (logger, "  Created raw PSSD Study = " + rawStudyCID);
		} else {
			MBCRawUploadUtil.log (logger, "  Found raw PSSD Study = " + rawStudyCID);
		}

		// Look for extant MR raw DataSets
		String rawDataSetCID = findRawSeries  (session, pm, file, rawStudyCID);

		MBCRawUploadUtil.log (logger, "  Uploading file");
		long tsize = FileUtils.sizeOf(file);
		MBCRawUploadUtil.log (logger, "     File size = " + FileUtils.byteCountToDisplaySize(tsize));


		// Create asset for raw MR data file. Skip if pre-exists
		Boolean chkSumsMatch = false;
		if (rawDataSetCID==null) {
			rawDataSetCID = createRawSeries (session, file, pm, rawStudyCID, ops, logger);
		} else {
			MBCRawUploadUtil.log (logger, "  Found existing raw DataSet ID = " + rawDataSetCID);
			if (ops.chksum) {

				// Compute check sum
				String chkSumDisk = null;
				if (ops.chksum) {
					// Get chksum from disk
					MBCRawUploadUtil.log (logger, "  Computing disk file check sum");
					chkSumDisk = ZipUtil.getCRC32(file, 16);
					MBCRawUploadUtil.log (logger, "  Finished computing disk file check sum");
				}
				//
				ServerClient.Connection cxn = session.connect();
				chkSumsMatch = compareCheckSums (cxn, ops, logger, rawDataSetCID, chkSumDisk, false);
				cxn.close();
				//
				if (chkSumsMatch) {
					MBCRawUploadUtil.log (logger, "  Checksums match so skipping file");
					if (ops.delete) {
						MBCRawUploadUtil.deleteFile(file, logger);		
					}
				} else {
					MBCRawUploadUtil.log (logger, "  *** Checksums do not match - the source file will not be destroyed - you must resolve this discrepancy.");
				}
			}
		}
	}


	private static Boolean compareCheckSums(ServerClient.Connection cxn, Options ops, PrintWriter logger, 
			String rawDataSetCID, String chkSumDisk, Boolean destroy) throws Throwable {
		MBCRawUploadUtil.log (logger, "  Validating checksum");

		// Get chksum from asset
		String chkSumAsset = MBCRawUploadUtil.getCheckSum (cxn, null, rawDataSetCID);
		Boolean chkSumsMatch = false;
		if (chkSumDisk.equalsIgnoreCase(chkSumAsset)) {
			MBCRawUploadUtil.log(logger, "     Checksums match");	
			chkSumsMatch = true;
		} else {
			chkSumsMatch = false;
			MBCRawUploadUtil.log (logger, "    Checksums do not match. Checksums are:");	
			MBCRawUploadUtil.log (logger, "       Input file      = " + chkSumDisk);
			MBCRawUploadUtil.log (logger, "       Mediaflux asset = " + chkSumAsset);
			//
			if (destroy) {
				AssetUtil.destroy(cxn, null, rawDataSetCID);
				MBCRawUploadUtil.log (logger, "      Destroyed Mediaflux asset " + rawDataSetCID);
			}
		}
		return chkSumsMatch;
	}


	/**
	 * FInd the raw Study in either the DICOM or PSSD data models
	 * 
	 * @param cxn
	 * @param pm
	 * @param patientAssetID
	 * @param subjectCID
	 * @return
	 * @throws Throwable
	 */
	private static String findRawStudy (MFSession session, MRMetaData pm, String patientAssetID, String subjectCID) throws Throwable {
		// FInds by date only as there is no study 'UID'. Only UIDs for Series (data sets).
		XmlStringWriter w = new XmlStringWriter();
		String query = null;
		query = "model='om.pssd.study' and cid starts with '" + subjectCID + "'";	
		w.add("action", "get-cid");
		query += " and xpath(" + RAW_STUDY_DOC_TYPE + "/date)='" + 
				DateUtil.formatDate(pm.getDate(), false, false) + "'";
		w.add("where", query);
		XmlDoc.Element r = session.execute("asset.query", w.document(), null, null);
		if (r==null) return null;
		return r.value("cid");
	}

	/**
	 * Create the raw study as a DICOM or PSSD data model asset
	 * 
	 * @param cxn
	 * @param pm
	 * @param subjectCID
	 * @param domain
	 * @param user
	 * @return
	 * @throws Throwable
	 */
	private static String createRawStudy(MFSession session,
			MRMetaData pm,  String subjectCID) throws Throwable {

		// Create a study with siemens doc attached
		XmlStringWriter w = new XmlStringWriter();

		// Find the ExMethod executing the Method that created this Subject
		ServerClient.Connection cxn = session.connect();
		String exMethodCID = MBCRawUploadUtil.findExMethod(cxn, subjectCID);
		w.add("pid", exMethodCID);

		// FInd the step in the Method for this Study type
		Vector<String> studyTypes = new Vector<String>();
		for (int i = 0; i < PSSD_STUDY_TYPES.length; i++) {
			studyTypes.add(PSSD_STUDY_TYPES[i]);
		}
		String step = MBCRawUploadUtil.getFirstMethodStep(cxn, exMethodCID,
				studyTypes);
		cxn.close();
		//
		w.add("step", step);


		// Add the meta-data
		w.push("meta");
		w.push(RAW_STUDY_DOC_TYPE);
		//
		String date = DateUtil.formatDate(pm.getDate(), false, false);
		w.add("date",date);
		w.add("frame-of-reference", pm.getFoR());
		w.push("ingest");
		w.add("date", "now");
		w.pop();
		//
		w.pop();
		w.pop();
		w.add("name", "Raw Siemens MR");

		//
		XmlDoc.Element r = null;
		w.add("description", "Raw Siemens data");
		r = session.execute("om.pssd.study.create", w.document(), null, null);
		return r.value("id");
	}




	private static String findRawSeries(MFSession session, MRMetaData pm, File file,
			String sid) throws Throwable {

		// The parent ID could be repository, project, subject or study
		// Look for DataSet with given file name. It's the easiest way to
		// establish the DataSet already exists
		XmlStringWriter w = new XmlStringWriter();
		String name = file.getName(); // This is the name of the asset.
		String query =  "type='siemens-raw-mr/series' and model='om.pssd.dataset' and cid starts with '"
				+ sid + "' and xpath(daris:pssd-object/name)='" + name
				+ "' and asset has content";
		w.add("action", "get-cid");

		// We just establish that there is content and that the meta-data is the
		// same
		w.add("where", query);
		w.add("pdist", "0");
		XmlDoc.Element r = session.execute("asset.query", w.document(), null, null);
		if (r == null)
			return null;
		Collection<String> cids = r.values("cid");
		if (cids == null)
			return null;

		// What to do if we find multiples... It probably means something has
		// been uploaded wrongly and needs to be remedied (could be ok in a test environment).
		if (cids.size() > 1) {
			throw new Exception(
					"Multiple Raw Siemens DataSets with name '"
							+ name
							+ "' and parent '"
							+ sid
							+ "' were found in the repository. This is most likely an error and needs to be remedied.");
		}
		return r.value("cid");
	}





	private static String createRawSeries(MFSession session, File path, MRMetaData pm, String rawStudyCID,
			Options ops, PrintWriter logger) throws Throwable {
		// Create a data set with siemens doc attached
		XmlStringWriter w = new XmlStringWriter();
		w.add("pid", rawStudyCID);

		w.push("meta");
		w.push(RAW_SERIES_DOC_TYPE);

		// This is just a date, no time. Its the same date that we store on
		// the raw Study
		String date = DateUtil.formatDate(pm.getDate(), false, false);
		w.add("date", date);
		w.add("modality", "MR");
		w.add("description", "Siemens RAW MR file");
		//
		if (ops.expire) {
			Calendar c = Calendar.getInstance(); 
			Date t = pm.getDate();
			c.setTime(t); 
			c.add(Calendar.YEAR, 1);
			w.add("date-expire", c.getTime());
		}

		//
		w.push("ingest");
		w.add("date", "now");
		w.pop();
		//
		w.pop();
		w.pop();		

		// DataSet mime type
		w.add("type", MimeTypes.MR_RAW_SERIES_MIME_TYPE);

		// Prepare correct stream depending on check sum status
		final InputStream in = ops.chksum ? new CheckedInputStream(new BufferedInputStream(new FileInputStream(path)), new CRC32())  : new BufferedInputStream(new FileInputStream(path));

		// Upload
		w.add("description", "Raw Siemens DataSet");
		w.add("filename", path.getName()); // Original filename
		w.add("name", path.getName()); // Original filename
		XmlDoc.Element r = null;
		String cid = null;
		try {
			long t1 = System.nanoTime();

			r = session.execute("om.pssd.dataset.primary.create", w.document(),
					new arc.mf.client.ServerClient.GeneratedInput(null, path.getAbsolutePath(), path.length()) {

				@Override
				protected void copyTo(OutputStream out, AbortCheck ac) throws Throwable {
					StreamCopy.copy(in, out, ac);
				}
			}, null);
			long t2 = System.nanoTime();
			//
			if (r==null) {
				throw new Exception ("Failed to create PSSD DataSet");
			}
			//
			cid = r.value("id");
			MBCRawUploadUtil.log (logger, "  Created raw PSSD DataSet = " + cid);
			double tSec = (double)(t2-t1) / (double)1000000000;
			//
			long tsize = FileUtils.sizeOf(path);
			double rBytes = (double)tsize / tSec;
			double rMBytes = rBytes / 1000000;
			MBCRawUploadUtil.log (logger, "  Approximate upload rate = " + rMBytes + " MB/sec");
			//
			ServerClient.Connection cxn = session.connect();
			XmlDoc.Element asset = AssetUtil.getMeta(cxn, null, cid);
			cxn.close();

			if (ops.chksum) {
				MBCRawUploadUtil.log (logger, "  Validating checksum");
				long csum = ((CheckedInputStream) in).getChecksum().getValue();
				long assetCsum = asset.longValue("asset/content/csum[@base='10']");
				if (csum == assetCsum) {
					MBCRawUploadUtil.log(logger, "     Checksums match");	
					//
					// Destroy input file if requested
					if (ops.delete) {
						MBCRawUploadUtil.deleteFile(path, logger);		
					}
				} else {
					MBCRawUploadUtil.log (logger, "    Checksums do not match. Checksums are:");	
					MBCRawUploadUtil.log (logger, "       Input file      = " + csum);
					MBCRawUploadUtil.log (logger, "       Mediaflux asset = " + assetCsum);
					//
					if (ops.delete) {
						cxn = session.connect();
						AssetUtil.destroy(cxn, null, cid);
						cxn.close();
						MBCRawUploadUtil.log (logger, "      Destroyed Mediaflux asset " + cid);
					}
				}
			}

		} finally {
			in.close();
		}
		return cid;
	}




	/**
	 * 
	 * prints the help information for this command line tool.
	 * 
	 * @param os
	 * 
	 */
	private static void printHelp() {
		System.out.println("MBCPETUpload");
		System.out.println("");
		System.out.println("Synopsis:");
		System.out.println("   Uploads raw Siemens PET/CT files to Mediaflux.  Assets are associated");
		System.out.println("   with pre-existing DICOM patient assets holding the DICOM images.");
		System.out.println("");
		System.out.println("Usage:");
		System.out.println("   " + MBCMRUpload.class.getName() + " [options..]");
		System.out.println("");
		System.out.println("");
		System.out.println("Java Properties:");
		System.out.println("    -mf.host      [Required]: The name or IP address of the Mediaflux host.");
		System.out.println("    -mf.port      [Required]: The server port number.");
		System.out.println("    -mf.token     [Optional]: The security token to authenticate with (preferred).");
		System.out.println("    -mf.user      [Optional]: The logon user (if no token).");
		System.out.println("    -mf.domain    [Optional]: The logon domain (if no token).");
		System.out.println("    -mf.password  [Optional]: The logon user's (obfuscated) password (if no token).");
		System.out.println("    -mf.transport [Optional]: Required if the port number is non-standard.");
		System.out.println("                              One of [HTTP, HTTPS, TCPIP]. By default the");
		System.out.println("                              following transports are inferred from the port:");
		System.out.println("                                80    = HTTP");
		System.out.println("                                443   = HTTPS");
		System.out.println("                                other = TCPIP");
		System.out.println("");
		System.out.println("Options:");
		System.out.println("   " + HELP_ARG + "          Displays this help.");
		System.out.println("   " + PATH_ARG + "          The path for the directory holding the data or a single file. Default is " + DEFAULT_SRC_PATH);
		System.out
		.println("   "
				+ FIND_ARG
				+ "   Method to find pre-existing subjects; one of 'name' (default), ''name+dob', 'id', or 'name+id'");
		System.out.println("   " + NOLOG_ARG + "        Disables writing any log file.");
		System.out.println("   " + LOGPATH_ARG + "       Specify the directory for log files to be written in. Default is " + DEFAULT_LOGGER_PATH);
		System.out.println("   " + NOCHKSUM_ARG + "     Disables check sum validation of uploaded file");
		System.out.println("   " + NODELETE_ARG + "     Disables the deletion of the input file after check sum validation. Files will only be deleted if the option to compare checksums is enabled.");
		System.out.println("   " + EXPIRE_ARG + "        Specifies that meta-data is to be attached to the file with an expiry data of 1 year after acquisition");
		System.out.println("   " + ID_ARG + "            Specifies the PSSD data model (DaRIS) should be used and that this is the citeable ID that the Study should be associated with. Can be depth 2 (the repository), 3 (a Project) or 4 (a Subject).");
		System.out.println("   " + DECRYPT_ARG + "    Specifies the password should not be decrypted.");
		System.out.println("   " + SLEEP_ARG +   "         Specifies the amount of time (minutes) to wait before trying to upload data.Some early parsing/checking happens before the sleep is activated.");
		System.out.println("");
		System.out.println("");
	}

}

