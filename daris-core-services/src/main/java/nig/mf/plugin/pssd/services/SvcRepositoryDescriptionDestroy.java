package nig.mf.plugin.pssd.services;

import nig.mf.plugin.pssd.RepositoryDescription;
import arc.mf.plugin.PluginService;
import arc.xml.XmlDoc;
import arc.xml.XmlWriter;

public class SvcRepositoryDescriptionDestroy extends PluginService {

    public static final String SERVICE_NAME = "daris.repository.description.destroy";
    public static final String SERVICE_DESCRIPTION = "Destroy the description (asset) of daris repository.";

    private Interface _defn;

    public SvcRepositoryDescriptionDestroy() {

        _defn = new Interface();
    }

    public String name() {
        return SERVICE_NAME;
    }

    public String description() {
        return SERVICE_DESCRIPTION;
    }

    public Interface definition() {
        return _defn;
    }

    public Access access() {
        return ACCESS_ADMINISTER;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w)
            throws Throwable {

        String assetId = RepositoryDescription.getAssetId(executor());
        if (assetId != null) {
            executor().execute("asset.destroy",
                    "<args><id>" + assetId + "</id></args>", null, null);
        }
    }
}
