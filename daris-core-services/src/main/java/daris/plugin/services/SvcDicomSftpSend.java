package daris.plugin.services;

public class SvcDicomSftpSend extends SvcDicomSshSend {

    public final String SERVICE_NAME = "daris.dicom.sftp.send";

    @Override
    public String name() {
        return SERVICE_NAME;
    }

    @Override
    protected String sshTransferService() {
        return SvcDicomSshSend.SERVICE_SFTP_PUT;
    }

}
