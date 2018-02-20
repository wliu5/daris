package daris.plugin.services;

public class SvcDicomScpSend extends SvcDicomSshSend {

    public final String SERVICE_NAME = "daris.dicom.scp.send";

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    @Override
    protected String sshTransferService() {
        return SvcDicomSshSend.SERVICE_SCP_PUT;
    }

}
