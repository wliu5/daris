package nig.mf.plugin.pssd.dicom.study;

import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dicom.StudyProxy;


public class NullStudyProxy extends StudyProxy {

	public NullStudyProxy(String studyId) {
		super(studyId);
	}

	@Override
	public boolean assetExists(ServiceExecutor executor) throws Throwable {
		return false;
	}

	@Override
	public long createOrUpdateAsset(ServiceExecutor executor) throws Throwable {
		// Do nothing
		return 0L;
	}

	@Override
	public void destroyAsset(ServiceExecutor executor) throws Throwable {
		// Do nothing
	}
}
