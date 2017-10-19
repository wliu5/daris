package nig.mf.plugin.pssd.dicom.study;

import arc.mf.plugin.dicom.StudyProxy;



public class StudyProxyHolder {

	StudyProxy _study;
	Boolean _keep;            // If false, indicates the Study should be discarded (some business process made this decision)

	public StudyProxyHolder (StudyProxy study, Boolean keep) {
		_study = study;
		_keep = keep; 
	}

	public StudyProxyHolder () {
		_study = null;
		_keep = null;
	}

	public StudyProxy studyProxy () {
		return _study;
	}
	
	public Boolean keep () {
		return _keep;
	}
}

