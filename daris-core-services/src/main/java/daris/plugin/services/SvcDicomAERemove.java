package daris.plugin.services;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.AssetType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcDicomAERemove extends PluginService {

    public static final String SERVICE_NAME = "daris.dicom.ae.remove";

    private Interface _defn;

    public SvcDicomAERemove() {
        _defn = new Interface();
        _defn.add(new Interface.Element("id", AssetType.DEFAULT, "The id of the DICOM AE asset.", 0, 1));
    }

    @Override
    public Access access() {
        return ACCESS_ADMINISTER;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Remove a public DICOM application entity.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        String assetId = args.value("id");
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("id", assetId);
        XmlDoc.Element ae = executor().execute("asset.get", dm.root()).element("asset");
        if (!ae.elementExists("meta/" + SvcDicomAEAdd.DOC_TYPE)) {
            throw new IllegalArgumentException("Asset " + assetId + " has no '" + "meta/" + SvcDicomAEAdd.DOC_TYPE
                    + "'. Not a valid DICOM AE asset.");
        }
        dm = new XmlDocMaker("args");
        dm.add("id", assetId);
        executor().execute("asset.destroy", dm.root());
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
