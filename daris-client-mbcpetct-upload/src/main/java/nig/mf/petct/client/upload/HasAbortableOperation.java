package nig.mf.petct.client.upload;

import arc.utils.CanAbort;

public interface HasAbortableOperation {

    void setAbortableOperation(CanAbort ca);

    CanAbort abortableOperation();

}
