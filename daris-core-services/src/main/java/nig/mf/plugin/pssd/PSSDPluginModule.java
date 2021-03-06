package nig.mf.plugin.pssd;

import java.util.Collection;
import java.util.Vector;

import arc.mf.plugin.ConfigurationResolver;
import arc.mf.plugin.PluginService;
import arc.mf.plugin.dicom.DicomAssetEngineRegistry;
import arc.mf.plugin.event.FilterRegistry;
import arc.mf.plugin.http.HttpServlet;
import arc.mf.plugin.http.HttpServletPluginModule;
import daris.plugin.services.SvcCollectionTranscodeList;
import daris.plugin.services.SvcCollectionTypeList;
import daris.plugin.services.SvcProjectDictionaryCreate;
import daris.plugin.services.SvcProjectDictionaryDestroy;
import daris.plugin.services.SvcProjectDictionaryList;
import daris.plugin.services.SvcProjectDictionaryNamespaceCreate;
import daris.plugin.services.SvcProjectDictionaryNamespaceDescribe;
import daris.plugin.services.SvcShoppingCartSettingsLoad;
import daris.plugin.services.SvcShoppingCartSettingsRemove;
import daris.plugin.services.SvcShoppingCartSettingsSave;
import daris.plugin.services.SvcUserDescribe;
import daris.plugin.services.SvcUserList;
import nig.mf.plugin.pssd.announcement.events.PSSDAnnouncementEvent;
import nig.mf.plugin.pssd.announcement.events.PSSDAnnouncementEventFilterFactory;
import nig.mf.plugin.pssd.dicom.NIGDicomAssetEngineFactory;
import nig.mf.plugin.pssd.services.*;
import nig.mf.plugin.pssd.servlets.ArchiveServlet;
import nig.mf.plugin.pssd.servlets.DicomServlet;
import nig.mf.plugin.pssd.servlets.MainServlet;
import nig.mf.plugin.pssd.servlets.NiftiServlet;
import nig.mf.plugin.pssd.servlets.ObjectServlet;
import nig.mf.plugin.pssd.servlets.ShoppingCartServlet;

public class PSSDPluginModule implements HttpServletPluginModule {

    private Collection<PluginService> _services = null;
    private Collection<HttpServlet> _servlets = null;

    public String description() {

        return "PSSD Package Plugin module.";
    }

    @Override
    public void initialize(ConfigurationResolver config) throws Throwable {

        _services = new Vector<PluginService>();

        _services.add(new daris.plugin.services.SvcAssetContentExists());
        _services.add(new daris.plugin.services.SvcAssetPathGenerate());
        _services.add(new daris.plugin.services.SvcAssetReplicateCheck());
        _services.add(new daris.plugin.services.SvcAssetReplicateDestroyedCheck());
        _services.add(new SvcExMethodOrdinalSet());
        _services.add(new SvcAnnouncementCreate());
        _services.add(new SvcAnnouncementDescribe());
        _services.add(new SvcAnnouncementDestroy());
        _services.add(new SvcAnnouncementList());

        _services.add(new SvcAssetTemplateNamespaceReplace());

        _services.add(new SvcRoleTypeDescribe());
        _services.add(new SvcRoleList());
        _services.add(new SvcRoleCleanup());

        _services.add(new SvcUserList());
        _services.add(new SvcUserDescribe());
        _services.add(new SvcUserCanAccess());
        _services.add(new SvcUserCanCreate());
        _services.add(new SvcUserCanDestroy());
        _services.add(new SvcUserCanModify());
        _services.add(new SvcUserRevoke());
        _services.add(new SvcUserRoleGrant());
        _services.add(new SvcUserRoleRevoke());
        _services.add(new SvcUserCreate());

        _services.add(new SvcModelTypesList());
        _services.add(new SvcTypeMetadataSet());
        // _services.add(new SvcTypeMetadataRemove());
        _services.add(new SvcTypeMetadataDescribe());
        _services.add(new SvcTypeMetadataList());

        _services.add(new SvcMethodCreate());
        _services.add(new SvcMethodForSubjectCreate());
        _services.add(new SvcMethodUpdate());
        _services.add(new SvcMethodForSubjectUpdate());
        _services.add(new SvcMethodList());
        _services.add(new SvcMethodDescribe());
        _services.add(new SvcMethodSubjectMetadataDescribe());
        _services.add(new SvcMethodDestroy());
        _services.add(new SvcMethodUseCount());
        _services.add(new SvcMethodUseFind());
        _services.add(new SvcMethodFind());

        _services.add(new SvcProjectRootId());

        _services.add(new SvcProjectCreate());
        _services.add(new SvcProjectDestroy());
        _services.add(new SvcProjectInternalize());
        _services.add(new SvcProjectMembersList());
        _services.add(new SvcProjectMembersReplace());
        _services.add(new SvcProjectMembersRemove());
        _services.add(new SvcProjectMembersAdd());
        //
        _services.add(new SvcStudyTypesFind());

        _services.add(new SvcProjectUpdate());
        _services.add(new SvcProjectRoles());
        _services.add(new SvcProjectDataUseRoles());
        // _services.add(new SvcProjectSetCid()); // Service has some flaws. See
        // code [nebk]
        _services.add(new SvcProjectRolesCreate());
        _services.add(new SvcProjectSetModel());
        _services.add(new SvcProjectMailSend());
        _services.add(new SvcProjectRSubjectFind());
        _services.add(new SvcProjectMetaDataHarvest());
        //
        _services.add(new SvcProjectMethodReplace()); // Method migration
                                                      // service
        // _services.add(new SvcProjectMembersMetaRemove()); // One off
        // transition service
        _services.add(new SvcProjectMetaHarvestMigrate()); // One off migration
        _services.add(new SvcProjectDiskUsageGet());

        _services.add(new SvcDocTypeRename()); // Rename Doc Type (inc
                                               // templates)
        //
        _services.add(new SvcDaRISProjectMetaDataHarvest());

        _services.add(new SvcRSubjectCreate());
        _services.add(new SvcRSubjectFind());
        _services.add(new SvcRSubjectAdminAdd());
        _services.add(new SvcRSubjectAdminRemove());
        _services.add(new SvcRSubjectGuestAdd());
        _services.add(new SvcRSubjectGuestRemove());
        _services.add(new SvcRSubjectCleanup());

        _services.add(new SvcSubjectCreate());
        _services.add(new SvcSubjectUpdate());
        // _services.add(new SvcSubjectStateCreate()); // Service not utilised
        _services.add(new SvcSubjectStateSet());
        _services.add(new SvcSubjectMethodFind());
        _services.add(new SvcSubjectClone());
        _services.add(new SvcSubjectMethodTemplateReplace());
        _services.add(new SvcSubjectMethodReplace());
        // _services.add(new SvcSubjectOrdinalSet()); // TODO Need to test

        _services.add(new SvcExMethodCreate());
        _services.add(new SvcExMethodUpdate());

        _services.add(new SvcExMethodStepDescribe());
        _services.add(new SvcExMethodStepList());
        _services.add(new SvcExMethodStepUpdate());
        _services.add(new SvcExMethodStudyStepFind());
        _services.add(new SvcExMethodStepStudyFind());
        _services.add(new SvcExMethodStepTransformFind());
        _services.add(new SvcExMethodStudyTypeList());
        _services.add(new SvcExMethodSubjectStepFind());
        _services.add(new SvcExMethodSubjectStepUpdate());
        _services.add(new SvcExMethodTransformStepFind());
        _services.add(new SvcExMethodTransformStepExecute());

        _services.add(new SvcExMethodMethodReplace());

        _services.add(new SvcStudyCreate());
        _services.add(new SvcStudyUpdate());
        _services.add(new SvcStudyMove());
        _services.add(new SvcStudyCopy());
        _services.add(new SvcStudyRename());
        _services.add(new SvcExMethodStudiesPreCreate());
        _services.add(new SvcStudyTemplateReplace());
        _services.add(new SvcStudyOrdinalSet());
        _services.add(new SvcStudyDataSetOrdinalReset());

        _services.add(new SvcDataSetPrimaryCreate());
        _services.add(new SvcDataSetPrimaryUpdate());
        _services.add(new SvcDataSetDerivationCreate());
        _services.add(new SvcDataSetDerivationUpdate());
        _services.add(new SvcDataSetMove());
        _services.add(new SvcDataSetCount());
        _services.add(new SvcDatasetNameGrab());
        _services.add(new SvcDatasetNameGrabFromFileName());
        _services.add(new SvcDatasetDescriptionGrab());
        _services.add(new SvcDataSetDerivationFind());
        _services.add(new SvcDataSetClone());

        _services.add(new SvcDataSetProcessedCount());
        _services.add(new SvcDataSetProcessedExists());
        _services.add(new SvcDataSetProcessedDestroy());
        _services.add(new SvcDataSetProcessedDestroyableCount());
        _services.add(new SvcDataSetProcessedDestroyableExists());
        _services.add(new SvcDataSetProcessedInputList());
        _services.add(new SvcDataSetUnprocessedList());

        //
        _services.add(new SvcDataObjectCreate());

        _services.add(new daris.plugin.services.SvcDownloaderGet());
        _services.add(new daris.plugin.services.SvcDownloaderManifestGenerate());
        _services.add(new daris.plugin.services.SvcDownloaderPut());

        _services.add(new SvcObjectMetaCopy());
        _services.add(new SvcCollectionMembers());
        _services.add(new SvcCollectionMemberList());
        _services.add(new SvcCollectionMemberCount());
        _services.add(new daris.plugin.services.SvcCollectionArchiveCreate());
        _services.add(new daris.plugin.services.SvcCollectionContentSizeSum());
        _services.add(new daris.plugin.services.SvcCollectionSummaryGet());
        _services.add(new SvcCollectionTranscodeList());
        _services.add(new SvcCollectionTypeList());
        _services.add(new SvcObjectCidChange());
        _services.add(new SvcObjectExists());
        _services.add(new SvcObjectType());
        _services.add(new SvcObjectDescribe());
        _services.add(new SvcObjectDestroy());
        _services.add(new SvcObjectFind());
        _services.add(new SvcObjectUpdate());
        _services.add(new SvcObjectIconGet());
        _services.add(new SvcObjectIsReplica());
        _services.add(new SvcObjectHasRemoteChildren());
        _services.add(new SvcObjectLock());
        _services.add(new SvcObjectUnlock());
        _services.add(new SvcObjectSessionLock());
        _services.add(new SvcObjectSessionLocked());
        _services.add(new SvcObjectSessionUnlock());

        _services.add(new SvcObjectsDestroyHard());

        _services.add(new daris.plugin.services.SvcObjectAttach());
        _services.add(new daris.plugin.services.SvcObjectAttachmentGet());
        _services.add(new daris.plugin.services.SvcObjectAttachmentList());
        _services.add(new daris.plugin.services.SvcObjectDetach());

        _services.add(new SvcObjectCSVExport());
        _services.add(new SvcObjectDownload());

        _services.add(new SvcObjectTagAdd());
        _services.add(new SvcObjectTagDescribe());
        _services.add(new SvcObjectTagDictionaryCreate());
        _services.add(new SvcObjectTagDictionaryEntryAdd());
        _services.add(new SvcObjectTagDictionaryEntryExists());
        _services.add(new SvcObjectTagDictionaryEntryList());
        _services.add(new SvcObjectTagDictionaryEntryRemove());
        _services.add(new SvcObjectTagDictionaryDestroy());
        _services.add(new SvcObjectTagDictionaryGet());
        _services.add(new SvcObjectTagDictionaryGlobalCreate());
        _services.add(new SvcObjectTagDictionaryGlobalEntryAdd());
        _services.add(new SvcObjectTagDictionaryGlobalEntryList());
        _services.add(new SvcObjectTagDictionaryGlobalEntryRemove());
        _services.add(new SvcObjectTagDictionaryGlobalDestroy());
        _services.add(new SvcObjectTagExists());
        _services.add(new SvcObjectTagList());
        _services.add(new SvcObjectTagRemove());
        _services.add(new SvcObjectTagRemoveAll());

        _services.add(new daris.plugin.services.SvcTmpDirCreate());
        _services.add(new daris.plugin.services.SvcTmpDirDestroy());
        _services.add(new daris.plugin.services.SvcTmpDirFileUpload());
        _services.add(new daris.plugin.services.SvcTmpDirConsume());

        _services.add(new daris.plugin.services.SvcServerAddressPublicGet());
        _services.add(new daris.plugin.services.SvcServerAddressPublicSet());

        _services.add(new SvcStudyTypeCreate());
        _services.add(new SvcStudyTypeDestroy());
        _services.add(new SvcStudyTypeDestroyAll());
        _services.add(new SvcStudyTypeDescribe());
        _services.add(new SvcStudyFind());
        _services.add(new SvcStudyRetrofit());

        _services.add(new SvcRoleMemberRegAdd());
        _services.add(new SvcRoleMemberRegRemove());
        _services.add(new SvcRoleMemberRegList());
        _services.add(new SvcRoleMemberRegDestroy());
        _services.add(new SvcRoleMemberRegID());

        _services.add(new SvcObjectDMFGet());
        _services.add(new SvcObjectDMFPut());
        _services.add(new SvcObjectDMFStatus());

        _services.add(new SvcProjectMetadataDescribe());
        _services.add(new SvcSubjectMetadataDescribe());
        _services.add(new SvcStudyMetadataDescribe());

        _services.add(new daris.plugin.services.SvcShoppingCartTemplateCreate());
        _services.add(new daris.plugin.services.SvcShoppingCartTemplateDestroy());
        _services.add(new daris.plugin.services.SvcShoppingCartCreate());
        _services.add(new daris.plugin.services.SvcShoppingCartList());
        _services.add(new daris.plugin.services.SvcShoppingCartExists());
        _services.add(new daris.plugin.services.SvcShoppingCartDestroy());
        _services.add(new daris.plugin.services.SvcShoppingCartDescribe());
        _services.add(new daris.plugin.services.SvcShoppingCartContentList());
        _services.add(new daris.plugin.services.SvcShoppingCartContentAdd());
        _services.add(new daris.plugin.services.SvcShoppingCartContentRemove());
        _services.add(new daris.plugin.services.SvcShoppingCartContentClear());
        _services.add(new daris.plugin.services.SvcShoppingCartOrder());
        _services.add(new daris.plugin.services.SvcShoppingCartDestinationList());
        _services.add(new daris.plugin.services.SvcShoppingCartCleanup());

        _services.add(new SvcShoppingCartSettingsSave());
        _services.add(new SvcShoppingCartSettingsLoad());
        _services.add(new SvcShoppingCartSettingsRemove());

        _services.add(new SvcRepositoryDescriptionSet());
        _services.add(new SvcRepositoryDescribe());
        _services.add(new SvcRepositoryDescriptionDestroy());
        _services.add(new SvcRepositoryDescriptionGet());

        _services.add(new SvcReplicate());
        _services.add(new SvcReplicateCheck());

        _services.add(new SvcObjectThumbnailSet());
        _services.add(new SvcObjectThumbnailImageGet());
        _services.add(new SvcObjectThumbnailGet());
        _services.add(new SvcObjectThumbnailUnset());

        _services.add(new SvcDicomSend());
        _services.add(new daris.plugin.services.SvcDicomScpSend());
        _services.add(new daris.plugin.services.SvcDicomSftpSend());
        _services.add(new SvcDICOMAERegID());
        _services.add(new SvcDICOMAEAdd());
        _services.add(new SvcDICOMAERemove());
        _services.add(new SvcDICOMAERegList());
        _services.add(new SvcDICOMAERegDestroy());
        _services.add(new SvcDICOMAEAccess());
        _services.add(new SvcDicomLocalAETitleList());

        _services.add(new SvcDicomSRGet());
        _services.add(new SvcDicomSRExport());

        _services.add(new daris.plugin.services.SvcDicomMetadataSet());
        _services.add(new daris.plugin.services.SvcDicomOnsendUserList());
        _services.add(new daris.plugin.services.SvcDicomPixelDataChecksumGenerate());
        _services.add(new daris.plugin.services.SvcDicomSendCalledAEAdd());
        _services.add(new daris.plugin.services.SvcDicomSendCalledAEExists());
        _services.add(new daris.plugin.services.SvcDicomSendCalledAERemove());
        _services.add(new daris.plugin.services.SvcDicomSendCalledAEList());
        _services.add(new daris.plugin.services.SvcDicomSendCallingAETitleList());

        _services.add(new daris.plugin.services.SvcSinkDescribe());

        _services.add(new daris.plugin.services.SvcUserDicomSendCalledAEAdd());
        _services.add(new daris.plugin.services.SvcUserDicomSendCalledAERemove());
        _services.add(new daris.plugin.services.SvcUserDicomSendCallingAETitleAdd());
        _services.add(new daris.plugin.services.SvcUserDicomSendCallingAETitleRemove());
        _services.add(new daris.plugin.services.SvcUserDicomSendSettingsGet());
        _services.add(new daris.plugin.services.SvcUserDicomSendSettingsSet());

        //
        _services.add(new daris.plugin.services.SvcCollectionDatasetCount());
        _services.add(new daris.plugin.services.SvcCollectionDicomDatasetCount());
        _services.add(new SvcDICOMUploadNotify());
        //
        _services.add(new SvcDICOMNormalizeHeader());
        _services.add(new SvcDICOMHeaderEdit());
        _services.add(new SvcDICOMAnonymize());
        //
        _services.add(new SvcDICOMControls());
        _services.add(new SvcDICOMUserCreate());

        _services.add(new SvcFCPList());

        _services.add(new SvcArchiveContentList());
        _services.add(new SvcArchiveContentGet());
        _services.add(new SvcArchiveContentImageGet());

        _services.add(new SvcTempNamespaceGet());
        _services.add(new SvcTempNamespaceSet());
        _services.add(new SvcTempAssetCreate());

        _services.add(new SvcTransformFind());

        _services.add(new SvcNamespaceDefaultGet());
        _services.add(new SvcNamespaceDefaultSet());
        _services.add(new SvcProjectNamespaceDefaultGet());
        _services.add(new SvcProjectCitableRootCreate());

        _services.add(new SvcProjectDictionaryNamespaceCreate());
        _services.add(new SvcProjectDictionaryNamespaceDescribe());
        _services.add(new SvcProjectDictionaryCreate());
        _services.add(new SvcProjectDictionaryDestroy());
        _services.add(new SvcProjectDictionaryList());

        // XNAT rest client
        _services.add(new daris.plugin.experimental.xnat.services.SvcXnatExperimentDicomIngest());
        _services.add(new daris.plugin.experimental.xnat.services.SvcXnatExperimentDownload());
        _services.add(new daris.plugin.experimental.xnat.services.SvcXnatScanDicomIngest());
        _services.add(new daris.plugin.experimental.xnat.services.SvcXnatScanDownload());

        //
        _services.add(new daris.plugin.services.SvcObjectChildCursorGet());
        _services.add(new daris.plugin.services.SvcObjectChildrenCount());
        _services.add(new daris.plugin.services.SvcObjectChildrenList());
        _services.add(new daris.plugin.services.SvcObjectPathFind());
        _services.add(new daris.plugin.services.SvcRoleUserAdd());
        _services.add(new daris.plugin.services.SvcRoleUserDescribe());
        _services.add(new daris.plugin.services.SvcRoleUserList());
        _services.add(new daris.plugin.services.SvcRoleUserRemove());
        _services.add(new daris.plugin.services.SvcProjectUserAdd());
        _services.add(new daris.plugin.services.SvcProjectUserList());
        _services.add(new daris.plugin.services.SvcProjectUserRemove());
        _services.add(new daris.plugin.services.SvcProjectUserSet());

        _services.add(new daris.plugin.services.SvcObjectTagAdd());
        _services.add(new daris.plugin.services.SvcObjectTagRemove());

        _services.add(new daris.plugin.services.SvcPathExpressionAdd());
        _services.add(new daris.plugin.services.SvcPathExpressionList());
        _services.add(new daris.plugin.services.SvcPathExpressionRemove());

        // MyTardis dataset import
        _services.add(new daris.plugin.experimental.mytardis.services.SvcMyTardisDatasetImport());

        // Register a DICOM handler specific to NIG.
        DicomAssetEngineRegistry.register(new NIGDicomAssetEngineFactory());

        // register system events
        registerSystemEvents();

        _servlets = new Vector<HttpServlet>();
        _servlets.add(new MainServlet());
        _servlets.add(new ObjectServlet());
        _servlets.add(new ShoppingCartServlet());
        _servlets.add(new DicomServlet());
        _servlets.add(new NiftiServlet());
        _servlets.add(new ArchiveServlet());
    }

    protected void registerSystemEvents() throws Throwable {

        FilterRegistry.remove(PSSDObjectEvent.EVENT_TYPE);
        FilterRegistry.add(PSSDObjectEvent.EVENT_TYPE, PSSDObjectEventFilterFactory.INSTANCE);
        FilterRegistry.remove(PSSDAnnouncementEvent.EVENT_TYPE);
        FilterRegistry.add(PSSDAnnouncementEvent.EVENT_TYPE, PSSDAnnouncementEventFilterFactory.INSTANCE);

    }

    public boolean isCompatible(ConfigurationResolver config) throws Throwable {

        return false;
    }

    public void shutdown(ConfigurationResolver config) throws Throwable {

        // Unregister system events
        unregisterSystemEvents();
    }

    protected void unregisterSystemEvents() throws Throwable {

        FilterRegistry.remove(PSSDObjectEvent.EVENT_TYPE);
        FilterRegistry.remove(PSSDAnnouncementEvent.EVENT_TYPE);
    }

    public String vendor() {

        return "Neuroimaging and Neuroinformatics Group, Centre for Neuroscience Research, the University of Melbourne.";
    }

    public String version() {

        return "1.0";
    }

    public Collection<PluginService> services() {

        return _services;
    }

    @Override
    public Collection<HttpServlet> servlets() {
        return _servlets;
    }

}
