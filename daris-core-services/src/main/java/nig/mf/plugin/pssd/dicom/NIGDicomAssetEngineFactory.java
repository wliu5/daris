package nig.mf.plugin.pssd.dicom;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import arc.mf.plugin.dicom.DicomAssetEngine;
import arc.mf.plugin.dicom.DicomAssetEngineFactory;

/**
 * This class is used by the Mediaflux DICOM engine framework
 * 
 * @author jason
 *
 */
public class NIGDicomAssetEngineFactory implements DicomAssetEngineFactory {

    public static final String DESCRIPTION = "Handles PSSD data - stores data in an ex-method and study for a project and subject. Calls 'pss' engine (if configured) to handle PSS and non-citeable DICOM data.";

    public Map<String, String> arguments() {
        Map<String, String> args = new TreeMap<String, String>();

        args.put("nig.dicom.id.citable.project.from", 
        		"Specifies a DICOM element from which the name of the Project is extracted. Allowed values for the DICOM element are one of " +
        		"[patient.name.first, patient.name.last, patient.id, referring.physician.name, requesting.physician.name, performing.physician.name]." +
        	    " A query is then done to find the Project by name and direct the data to it. " +
                "An exception will arise if there are multiple Projects with the same name. You should enable auto-subject creation with this control.");
        args.put("nig.dicom.id.citable", 
        		"The citeable ID (using the P.S.EM.S notation) of the DaRIS object to locate the data in. Using this will deactivate all other controls looking for this ID in other DICOM meta-data");
        args.put("nig.dicom.id.citable.director", 
        		"Direct data to a citable ID ((using the P.S.EM.S notation) based on DICOM meta-data values. The pattern is <Default CID>;<CID>:<DICOM Element name>:<DICOM Element value>.  "
        		+ " Multiple groups (<cid>:<name>:<value>)  can be separated by a semicolon.  Values for <element name> are one of [patient.name.first, patient.name.last, patient.id].  When the value of the named element matches (actually, CONTAINS [case insensitive]) the value, the CID in that group is used. There is a default CID at the start when there are no matches.");
        args.put("nig.dicom.id.citable.default", "If all other methods to extract a citeable ID (using the P.S.EM.S notation) fail, then use this value. This provides a means of sending data to a default CID (usually Project).");
        args.put("nig.dicom.id.by",
                "The method of identifying studies using P.S[.EM[.S]] (project, subject, ex-method, study) notation. If specified, one of [patient.id, patient.name, patient.name.first, patient.name.last, study.id, performing.physician.name, referring.physician.name, referring.physician.phone, requesting.physician].");
        args.put("nig.dicom.id.ignore-non-digits",
                "Specifies whether non-digits in part of an element should be ignored when constructing a P.S.EM.S identifier. One of [false,true]. Defaults to false.");
        args.put("nig.dicom.id.ignore-before-last-delim",
                "Specifies whether all characters before (and including) the last occurrence of the given delimiter should be ignored  when constructing a P.S.EM.S identifier. Invoked before ignore-non-digits if both supplied. Invoked after nig.dicom.id.ignore-after-last-delim");
        args.put("nig.dicom.id.ignore-after-last-delim",
                "Specifies whether all characters after (and including) the last occurrence of the given delimiter should be ignored  when constructing a P.S.EM.S identifier. Invoked before ignore-non-digits if both supplied. Invoked before nig.dicom.id.ignore-before-last-delim");
        args.put("nig.dicom.id.prefix",
                "If specified, the value to be prepended to any P.S.EM.S identifier.");
        args.put("nig.dicom.subject.find.method",
                "Specifies how the server tries to find Subjects if the CID is for a Project. Allowed values are 'id',name', 'name+', 'all'. 'id' means mf-dicom-patient/id, 'name' means the full name from mf-dicom-patient/name (first and last), 'name+' means match on name,sex and DOB, 'all' means match on id,name,sex and DOB.  The default if not specified is 'name'.");
        args.put("nig.dicom.subject.create",
                "If true, will auto-create Subjects if the identifier is of the form P.S and the Subject does not exist.");
        args.put("nig.dicom.subject.clone_first",
                "If auto-creating subjects, make new ones by cloning the first one if it exists. If it does not exist, just generate new subject as per normal");
        args.put("nig.dicom.subject.name.from",
        		"When auto-creating Subjects, set the Subject name to values source from the DICOM meta-data. "
       		+ "Allowed values are [patient.name.first, patient.name.last, patient.name.full, patient.id, patient.id+name.last]");
        args.put("nig.dicom.subject.name.from.index.range",
                "When auto-creating Subjects, and utilising a control nig.dicom.subject.name.from.{full,last,full,id} to set the Subject name, select characters from the given index range pair. E.g. 0,11 would select the String starting at index 0 and finishing at index 11.  This control is applied before control nig.dicom.subject.name.from.ignore-after-last-delim");
        args.put("nig.dicom.subject.name.from.ignore-after-last-delim",
                "When auto-creating Subjects, and utilising a control nig.dicom.subject.name.from.{full,last,full,id} to set the SUbject name, ignore all characters after and including the last occurrence of the specified delimiter.");
        args.put("nig.dicom.subject.meta.set-service",
                "Service to populate domain-specific meta-data on Subject objects.");
        args.put("nig.dicom.write.mf-dicom-patient",
                "Instructs the server to populate document mf-dicom-patient (or possibly mf-dicom-patient-encrypted - see nig.dicom.use.encrypted.patient)  on all Subjects (sets in private meta-data)");
        args.put("nig.dicom.encrypt.patient.metadata",
                "Instructs the server to populate document mf-dicom-patient-encrypted rather than mf-dicom-patient (if it is being written according to nig.dicom.write.mf-dicom-patient");
        args.put("nig.dicom.modality.ignore",
                "Ignore the DICOM modality in the data and Method and just create the next Study under the next available empty Method step");
        args.put("nig.dicom.dose-reports.drop",
                "Drop Structured dose reports (modality SR) if the Method specifies is not for humans (see service om.pssd.method.for.subject.create and document daris:pssd-method-subject). Repors will not be dropped for human or unspecified Methods.");
        args.put("nig.dicom.project.selector",
                "Specifies Projects that specific proxy DICOM users are allowed to write to.");
        
        /*
         * TBD: Not enabled because the DICOM engine framework does not yet support this process (null StudyProxy returns are seen as errors)
        args.put("nig.dicom.study.discard", "Specify a DICOM element name and value <name>:<value> so that any Study for which the DICOM element value matches the value specified here is discarded." 
        		+ "Allowed values for <name> are : 'patient.name.first', 'patient.name.last', 'patient.id', 'referring.physicans.name', 'requesting.physican.name', 'performing.physicans.name', 'modality' and 'protocol.name'");
        */
        return args;
    }

    public Object createConfiguration(Map<String, String> args)
            throws Throwable {
        DicomIngestControls ic = new DicomIngestControls();
        if (args == null) {
            args = new HashMap<String, String>();
        }

        ic.configure(args);
        return ic;
    }

    public String description() {
        return DESCRIPTION;
    }

    public DicomAssetEngine instantate() {
        return new NIGDicomAssetEngine();
    }

    public String type() {
        return NIGDicomAssetEngine.TYPE_NAME;
    }
}
