package nig.mf.mr.client.upload;

import arc.utils.CanAbort;

public interface HasAbortableOperation {

    void setAbortableOperation(CanAbort ca);

    CanAbort abortableOperation();

}
