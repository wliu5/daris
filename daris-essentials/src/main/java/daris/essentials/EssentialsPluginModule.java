package daris.essentials;

import java.util.Collection;
import java.util.Vector;

import arc.mf.plugin.ConfigurationResolver;
import arc.mf.plugin.PluginModule;
import arc.mf.plugin.PluginService;

public class EssentialsPluginModule implements PluginModule {

    private Collection<PluginService> _svs = null;

    public String description() {
        return "DaRIS Essentials Plugin Module.";
    }

    @Override
    public void initialize(ConfigurationResolver config) throws Throwable {

        _svs = new Vector<PluginService>();
        _svs.add(new SvcTest());

        _svs.add(new SvcActorsHaveRole());
        _svs.add(new SvcAssetCidGet());
        _svs.add(new SvcAssetClone());
        _svs.add(new SvcAssetDMFGet());
        _svs.add(new SvcAssetDMFPut());
        _svs.add(new SvcAssetCheck());
        _svs.add(new SvcAssetContentChecksumGenerate());
        _svs.add(new SvcAssetContentStringReplace());
        _svs.add(new SvcAssetContentPrune());
        _svs.add(new SvcAssetsNoContentList());
        _svs.add(new SvcAssetsNoMembers());
        _svs.add(new SvcAssetDocCopy());
        _svs.add(new SvcAssetDocRemove());
        _svs.add(new SvcAssetDocElementCopy());
        _svs.add(new SvcAssetDocElementRemove());
        _svs.add(new SvcAssetDocElementRename());
        _svs.add(new SvcAssetDocElementReplace());
        _svs.add(new SvcAssetDocTypeDictionaryReplace());
        _svs.add(new SvcAssetDocTypeNameReplace());
        _svs.add(new SvcAssetDocElementDateFix());
        _svs.add(new SvcAssetDocNamespaceReplace());
        _svs.add(new SvcAssetIdGet());
        _svs.add(new SvcAssetMetaStringReplace());
        _svs.add(new SvcAssetPidSet());
        _svs.add(new SvcAssetDocStringCheck());

        //
        _svs.add(new SvcFileSystemCheck());
        _svs.add(new SvcIPAddressResolve());
        _svs.add(new SvcLicenceUsage());
        _svs.add(new SvcLicenceUsageDescribe());
        //
        _svs.add(new SvcNameSpaceChildSum());
        _svs.add(new SvcNameSpacesChildDestroy());
        _svs.add(new SvcNameSpaceMetaDataCopy());
        //
        _svs.add(new SvcQueryIngestRate());
        _svs.add(new SvcQueryFileDistribution());
        //
        _svs.add(new SvcReplicateSync());
        _svs.add(new SvcReplicateCheck());
        _svs.add(new SvcReplicateNameSpaceCheck());

        // DICOM things could be pulled out into their own package
        _svs.add(new SvcDICOMMetadataCSVExport());
        _svs.add(new SvcDICOMMetadataPopulate());
        _svs.add(new SvcDICOMModelFix());
        // _svs.add(new SvcDICOMSendEss());
        _svs.add(new SvcDICOMStudyFind());
        _svs.add(new SvcDICOMMetaGrab());
        _svs.add(new SvcDICOMHeaderEdit());
        _svs.add(new SvcDICOMDestroy());
        // _svs.add(new SvcDarisDicomSend());
        _svs.add(new SvcDicomDownload());
        //
        _svs.add(new SvcRolePermsRemove());
        _svs.add(new SvcSecureWalletKeyEntryGenerate());
        //
        _svs.add(new SvcUserFind());
        _svs.add(new SvcUserEMailExport());
    }

    public void shutdown(ConfigurationResolver config) throws Throwable {

    }

    public String vendor() {

        return "Neuroimaging and Neuroinformatics Group, Centre for Neuroscience Research, the University of Melbourne.";
    }

    public String version() {

        return "1.0";
    }

    public Collection<PluginService> services() {

        return _svs;
    }

}