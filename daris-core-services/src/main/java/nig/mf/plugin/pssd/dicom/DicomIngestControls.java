package nig.mf.plugin.pssd.dicom;

import java.util.Map;
import java.util.StringTokenizer;

import nig.mf.pssd.CiteableIdUtil;

/**
 * Ingestion controls for the NIG.DICOM engine These controls needs to match
 * what is in the DicomAssetHandlerFactory
 * 
 * @author Jason Lohrey
 *
 */
public class DicomIngestControls {

    /**
     * Don't identify.
     */
    public static final int ID_NONE = 0;
    
 
    /**
     * Uniquely identify patient by DICOM element (0x0010,0x0020).
     */
    public static final int ID_BY_PATIENT_ID = 1;

    /**
     * Uniquely identify patient by DICOM element (0x0010,0x0010).
     */
    public static final int ID_BY_PATIENT_FULL_NAME = 2;

    /**
     * Uniquely identify patient by first name from DICOM element
     * (0x0010,0x0010).
     */
    public static final int ID_BY_PATIENT_FIRST_NAME = 3;

    /**
     * Uniquely identify patient by last name from DICOM element
     * (0x0010,0x0010).
     */
    public static final int ID_BY_PATIENT_LAST_NAME = 4;

    /**
     * Uniquely identify by last name from DICOM element (0x0020,0x0010).
     */
    public static final int ID_BY_STUDY_ID = 5;

    /**
     * Uniquely identify by DICOM element ReferringPhysicianName
     * (0x0008,0x0090).
     */
    public static final int ID_BY_REFERRING_PHYSICIAN_NAME = 6;

    /**
     * Uniquely identify by DICOM element Performing Physician (0x0008,0x1050).
     */
    public static final int ID_BY_PERFORMING_PHYSICIAN = 7;

    /**
     * Uniquely identify by DICOM element ReferringPhysicianPhone
     * (0x0008,0x0094).
     */
    public static final int ID_BY_REFERRING_PHYSICIAN_PHONE = 8;

    /**
     * Uniquely identify by DICOM element RequestingPhysician (0x0032, 0x1032).
     */
    public static final int ID_BY_REQUESTING_PHYSICIAN = 9;


    
    public static class ExInvalidSetting extends Throwable {
        public ExInvalidSetting(String setting, String error) {
            super("NIG.DICOM: invalid argument [" + setting + "]: " + error);
        }
    }

    private int[] _cidElements;
    private String _cidPrefix;
    private int _minCidDepth;
    private Boolean _ignoreNonDigits;
    private String _ignoreBeforeLastDelim;
    private String _ignoreAfterLastDelim;
    private String _citableID;
    private String _findSubjectMethod;
    private Boolean _autoSubjectCreate;
    private Boolean _cloneFirstSubject;
    private String _subjectMetaService;
    private Boolean _ignoreModality;
    private Boolean _useEncryptedPatient;

    // Controls set subject name from DICOM Patient Name or ID
    // TBD Turn all these into an ENUM
    private Boolean _setSubjectNameFromFirst; // Set from first name
    private Boolean _setSubjectNameFromLast; // Set from last name
    private Boolean _setSubjectNameFromFull; // Set from full name
    private Boolean _setSubjectNameFromID; // Set from ID
    private Boolean _setSubjectNameFromIDandLast; // ID and last name
    
    private String _setSubjectNameFromIgnoreAfterLastDelim; // Ignore chars
                                                            // after (and incl)
                                                            // last delim
    private String _setSubjectNameFromIndexRange; // Select chars only in given
                                                  // range of indices (a start
                                                  // and end pair such as
                                                  // "0,11")
    private String _cidDirector;         // Allows us to direct the Study to the desired CID based on meta-data elements
    private String _cidDefault;          // A default CID to direct the data to if all other extraction methods fail.
    private String _cidIDFromProject;    // Look up the project by name via this DICOM element
    //
    private String _projectSelector;
    private Boolean _writeDICOMPatient;
    private Boolean _dropDoseReports; // Not needed for non-humans
    //
    private String _discardElementName;   // Some DICOM element to test whether this Study should be kept or discarded
    private String _discardElementValue;

    public DicomIngestControls() {
        _cidElements = null;
        _citableID = null;
        _autoSubjectCreate = false;
        _cloneFirstSubject = false;
        _findSubjectMethod = "name";
        _setSubjectNameFromFirst = false;
        _setSubjectNameFromLast = false;
        _setSubjectNameFromFull = false;
        _setSubjectNameFromIgnoreAfterLastDelim = null;
        _setSubjectNameFromID = false;
        _setSubjectNameFromIDandLast = false;
        _setSubjectNameFromIndexRange = null;

        // Minimum CID depth to be considered a CID..
        _minCidDepth = 3;
        _ignoreNonDigits = false;
        _ignoreBeforeLastDelim = null;
        _ignoreAfterLastDelim = null;
        _cidPrefix = null;

        _ignoreModality = false;
        _projectSelector = null;
        _writeDICOMPatient = false;
        _dropDoseReports = false;
        _useEncryptedPatient = false;
        //
        _discardElementName = null;
        _discardElementValue = null;
        //
        _cidDirector = null;
        _cidIDFromProject = null;
    }

    public String cidPrefix() {
        return _cidPrefix;
    }

    public int minCidDepth() {
        return _minCidDepth;
    }

    public int[] cidElements() {
        return _cidElements;
    }

    public Boolean ignoreNonDigits() {
        return _ignoreNonDigits;
    }

    public String ignoreBeforeLastDelim() {
        return _ignoreBeforeLastDelim;
    }

    public String ignoreAfterLastDelim() {
        return _ignoreAfterLastDelim;
    }

    public String citableID() {
        return _citableID;
    }

    public String findSubjectMethod() {
        return _findSubjectMethod;
    }

    public Boolean setSubjectNameFromID() {
        return _setSubjectNameFromID;
    }

    public Boolean setSubjectNameFromFirst() {
        return _setSubjectNameFromFirst;
    }

    public Boolean setSubjectNameFromLast() {
        return _setSubjectNameFromLast;
    }

    public Boolean setSubjectNameFromFull() {
        return _setSubjectNameFromFull;
    }
    public Boolean setSubjectNameFromIDandLast () {
    	return _setSubjectNameFromIDandLast;
    }

    public String setSubjectNameFromIgnoreAfterLastDelim() {
        return _setSubjectNameFromIgnoreAfterLastDelim;
    }

    public String setSubjectNameFromIndexRange() {
        return _setSubjectNameFromIndexRange;
    }

    public Boolean autoSubjectCreate() {
        return _autoSubjectCreate;
    }

    public Boolean cloneFirstSubject() {
        return _cloneFirstSubject;
    }

    public String subjectMetaService() {
        return _subjectMetaService;
    }

    public Boolean ignoreModality() {
        return _ignoreModality;
    }

    public String projectSelector() {
        return _projectSelector;
    }

    public Boolean writeDICOMPatient() {
        return _writeDICOMPatient;
    }

    public Boolean dropSR() {
        return _dropDoseReports;
    }

    public Boolean useEncryptedPatient() {
        return _useEncryptedPatient;
    }
    
    public String discardElementName () {
    	return _discardElementName;
    }
    
    public String discardElementValue () {
    	return _discardElementValue;
    }
    
    public String cidDirector () {
    	return _cidDirector;
    }
    
    public String cidDefault () {
    	return _cidDefault;
    }

    public String cidFromProjectName () {
    	return _cidIDFromProject;
    }
    
    /**
     * Configure controls by reading either directly from the command line (e.g.
     * dicom.ingest :arg -name nig.dicom.id.citable 1.2.3.4) or from the network
     * configuration
     * 
     * @param args
     * @throws Throwable
     */
    protected void configure(Map<String, String> args) throws Throwable {

        // Root namespace for storing data.
        // _ns = (String)args.get("nig.dicom.asset.namespace.root");

        // Either the Citable ID is directly specified (can be on the CLI or in a DICOM server config) or it is
        // extracted from the DICOM metadata. 
        _citableID = (String) args.get("nig.dicom.id.citable");

        // Directs data to the write CID from meta-data configuration.  nig.dicom.id.citable over-rides this
        _cidDirector = (String)args.get("nig.dicom.id.citable.director");
        
        // Fetch the rpoject name from this DICOM element and look it up
        _cidIDFromProject = (String)args.get("nig.dicom.id.citable.project.from");
 

        if (_citableID == null) {
            String idBy = (String) args.get("nig.dicom.id.by");
            if (idBy != null) {
                StringTokenizer st = new StringTokenizer(idBy, ",");
                _cidElements = new int[st.countTokens()];

                int i = 0;
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();

                    if (tok.equalsIgnoreCase("patient.id")) {
                        _cidElements[i] = ID_BY_PATIENT_ID;
                    } else if (tok.equalsIgnoreCase("patient.name")) {
                        _cidElements[i] = ID_BY_PATIENT_FULL_NAME;
                    } else if (tok.equalsIgnoreCase("patient.name.first")) {
                        _cidElements[i] = ID_BY_PATIENT_FIRST_NAME;
                    } else if (tok.equalsIgnoreCase("patient.name.last")) {
                        _cidElements[i] = ID_BY_PATIENT_LAST_NAME;
                    } else if (tok.equalsIgnoreCase("study.id")) {
                        _cidElements[i] = ID_BY_STUDY_ID;
                    } else if (tok.equalsIgnoreCase("referring.physician.name")) {
                        _cidElements[i] = ID_BY_REFERRING_PHYSICIAN_NAME;
                    } else if (tok.equalsIgnoreCase("referring.physician.phone")) {
                        _cidElements[i] = ID_BY_REFERRING_PHYSICIAN_PHONE;
                    } else if (tok.equalsIgnoreCase("performing.physician.name")) {
                        _cidElements[i] = ID_BY_PERFORMING_PHYSICIAN;
                    } else if (tok.equalsIgnoreCase("requesting.physician")) {
                        _cidElements[i] = ID_BY_REQUESTING_PHYSICIAN;
                    } else {
                        throw new ExInvalidSetting("nig.dicom.id.by",
                                "expected one of [patient.id, patient.name, patient.name.first, patient.name.last, study.id, performing.physician.name, referring.physician.name, referring.physician.phone, requesting.physician] for id.patient.by - found: "
                                        + idBy);
                    }

                    i++;
                }
            }

            String ignoreChars = (String) args.get("nig.dicom.id.ignore-non-digits");
            if (ignoreChars != null) {
                if (ignoreChars.equalsIgnoreCase("true")) {
                    _ignoreNonDigits = true;
                } else if (ignoreChars.equalsIgnoreCase("false")) {
                    _ignoreNonDigits = false;
                } else {
                    throw new ExInvalidSetting("nig.dicom.id.ignore-non-digits",
                            "expected one of [true,false] - found: " + ignoreChars);
                }
            }

            _ignoreBeforeLastDelim = (String) args.get("nig.dicom.id.ignore-before-last-delim");
            _ignoreAfterLastDelim = (String) args.get("nig.dicom.id.ignore-after-last-delim");
        }
        
        // Default CID if other CID extraction methods fail
        _cidDefault = (String)args.get("nig.dicom.id.citable.default");
        if (_cidDefault!=null) {
        	if (!CiteableIdUtil.isCiteableId(_cidDefault)) {
        		throw new Exception ("The value for DICOM server control nig.dicom.id.citable.default '" + _cidDefault + "' is not a valid CID");
        	}
        }

        // Add CID prefix 
        _cidPrefix = (String) args.get("nig.dicom.id.prefix");

        // Method for finding Subjects
        String findSubject = (String) args.get("nig.dicom.subject.find.method");
        if (findSubject != null) {
            if (findSubject.equalsIgnoreCase("id")) {
                _findSubjectMethod = "id";
            } else if (findSubject.equalsIgnoreCase("name")) {
                _findSubjectMethod = "name";
            } else if (findSubject.equalsIgnoreCase("name+")) {
                _findSubjectMethod = "name+";
            } else if (findSubject.equalsIgnoreCase("all")) {
                _findSubjectMethod = "all";
            } else {
                throw new ExInvalidSetting("nig.dicom.subject.find.method",
                        "expected one of [id,name,name+,all] - found: " + findSubject);
            }
        }

        // Auto SUbject creation
        String createSubject = (String) args.get("nig.dicom.subject.create");
        if (createSubject != null) {
            if (createSubject.equalsIgnoreCase("true")) {
                _autoSubjectCreate = true;
            } else if (createSubject.equalsIgnoreCase("false")) {
                _autoSubjectCreate = false;
            } else {
                throw new ExInvalidSetting("nig.dicom.subject.create",
                        "expected one of [true,false] - found: " + createSubject);
            }
        }

        // Name the Subject object from a DICOM element
        String setFromName = (String) args.get("nig.dicom.subject.name.from");
        if (setFromName != null) {
        	if (setFromName.equalsIgnoreCase("patient.name.first")) {
        		_setSubjectNameFromFirst = true;
        	} else if (setFromName.equalsIgnoreCase("patient.name.last")) {
        		_setSubjectNameFromLast = true;
        	}  else if (setFromName.equalsIgnoreCase("patient.name.full")) {
        		_setSubjectNameFromFull = true;
           	} else if (setFromName.equalsIgnoreCase("patient.id")) {
        		_setSubjectNameFromID = true;
           	} else if (setFromName.equalsIgnoreCase("patient.id+name.last")) {
        		_setSubjectNameFromIDandLast = true;
            } else {
                throw new ExInvalidSetting("nig.dicom.subject.name.from",
                        "expected one of [patient.name.first, patient.name.last, patient.name.full, patient.id, patient.id+name.last] - found: " + setFromName);
            }
        }
        //
        _setSubjectNameFromIgnoreAfterLastDelim = (String) args
                .get("nig.dicom.subject.name.from.ignore-after-last-delim");
        ;
        _setSubjectNameFromIndexRange = (String) args.get("nig.dicom.subject.name.from.index.range");
        ;
        //

        // Clone SUbject
        String cloneSubject = (String) args.get("nig.dicom.subject.clone_first");
        if (cloneSubject != null) {
            if (cloneSubject.equalsIgnoreCase("true")) {
                _cloneFirstSubject = true;
            } else if (cloneSubject.equalsIgnoreCase("false")) {
                _cloneFirstSubject = false;
            } else {
                throw new ExInvalidSetting("nig.dicom.subject.clone_first",
                        "expected one of [true,false] - found: " + cloneSubject);
            }
        }

        // Service to update meta-data by parsing the DICOM meta-data in a
        // domain-specific way
        _subjectMetaService = (String) args.get("nig.dicom.subject.meta.set-service");

        //
        String ignoreModality = (String) args.get("nig.dicom.modality.ignore");
        if (ignoreModality != null) {
            if (ignoreModality.equalsIgnoreCase("true")) {
                _ignoreModality = true;
            } else if (ignoreModality.equalsIgnoreCase("false")) {
                _ignoreModality = false;
            } else {
                throw new ExInvalidSetting("nig.dicom.modality.ignore",
                        "expected one of [true,false] - found: " + ignoreModality);
            }
        }

        // Place constraints on certain users so that they can only access
        // certain projects
        _projectSelector = (String) args.get("nig.dicom.project.selector");
        

        // Tell the server to write mf-dicom-patient or
        // mf-dicom-patient-encrypted on the Subject
        String writePatient = (String) args.get("nig.dicom.write.mf-dicom-patient");
        if (writePatient != null) {
            if (writePatient.equalsIgnoreCase("true")) {
                _writeDICOMPatient = true;
            } else if (writePatient.equalsIgnoreCase("false")) {
                _writeDICOMPatient = false;
            } else {
                throw new ExInvalidSetting("nig.dicom.write.mf-dicom-patient",
                        "expected one of [true,false] - found: " + writePatient);
            }
        }

        // Use mf-dicom-patient-encrypted instead of mf-dicom-patient
        String useEncrypted = (String) args.get("nig.dicom.encrypt.patient.metadata");
        if (useEncrypted != null) {
            if (useEncrypted.equalsIgnoreCase("true")) {
                _useEncryptedPatient = true;
            } else if (writePatient.equalsIgnoreCase("false")) {
                _useEncryptedPatient = false;
            } else {
                throw new ExInvalidSetting("nig.dicom.encrypt.patient.metadata",
                        "expected one of [true,false] - found: " + useEncrypted);
            }
        }

        // Autoamted processes at CT scanners may upload automatics structured
        // dose reports
        // Sometimes on their own requiring special step/modality. Allow the
        // server
        // to drop these for non-humans (not required)
        String dropDoseReports = (String) args.get("nig.dicom.dose-reports.drop");
        if (dropDoseReports != null) {
            if (dropDoseReports.equalsIgnoreCase("true")) {
                _dropDoseReports = true;
            } else if (dropDoseReports.equalsIgnoreCase("false")) {
                _dropDoseReports = false;
            } else {
                throw new ExInvalidSetting("nig.dicom.dose-reports.drop",
                        "expected one of [true,false] - found: " + dropDoseReports);
            }
        }
        
        
        // Discard the Study  when this meta-data is matched
        // Use pattern <element name>-<element value>
        String dropElement = (String) args.get("nig.dicom.study.discard");
        if (dropElement != null) {
        	String[] t = dropElement.split(":");
        	if (t.length!=2) {
        		throw new Exception ("Failed to parse DICOM control nig.dicom.study.discard = '" + dropElement + "'");
        	}
        	_discardElementName = t[0];
        	_discardElementValue = t[1];
        }

    }
}
