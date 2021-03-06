package nig.mf.plugin.pssd.services;

import nig.mf.plugin.pssd.DataObject;
import nig.mf.plugin.pssd.DataSet;
import nig.mf.plugin.pssd.PSSDObject;
import nig.mf.plugin.pssd.PSSDObjectEvent;
import nig.mf.plugin.pssd.PSSDObjectEvent.Action;
import nig.mf.plugin.pssd.util.PSSDUtils;
import nig.mf.pssd.plugin.util.DistributedAsset;
import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginService.Interface.Element;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.StringType;
import arc.mf.plugin.dtype.XmlDocType;
import arc.mf.plugin.event.SystemEventChannel;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcDataObjectCreate extends PluginService {
    private Interface _defn;

    public SvcDataObjectCreate() throws Throwable {
        _defn = new Interface();

        Interface.Element me = new Interface.Element("pid", CiteableIdType.DEFAULT,
                "The identity of the parent DataSet.", 1, 1);
        me.add(new Interface.Attribute(
                "proute",
                CiteableIdType.DEFAULT,
                "In a federation, specifies the route to the peer that manages this citable ID.  If not supplied, then the object will be assumed to be local.",
                0));
        _defn.add(me);
        //
        _defn.add(new Interface.Element("name", StringType.DEFAULT, "The name of this data object.", 0, 1));
        _defn.add(new Interface.Element("description", StringType.DEFAULT,
                "An arbitrary description for the data object.", 0, 1));

        _defn.add(new Interface.Element("type", StringType.DEFAULT,
                "MIME type of the data set if different from the content.", 0, 1));

        me = new Element("meta", XmlDocType.DEFAULT, "Optional metadata - a list of asset documents.", 0, 1);
        me.setIgnoreDescendants(true);

        _defn.add(me);
    }

    public String name() {
        return "om.pssd.data-object.create";
    }

    public String description() {
        return "Creates a PSSD DataObject on the local server.";
    }

    public Interface definition() {
        return _defn;
    }

    public Access access() {
        return ACCESS_MODIFY;
    }

    public int minNumberOfInputs() {
        return 0;
    }

    public int maxNumberOfInputs() {
        return 1;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

        // Distributed ID for parent DataSet. It must be a primary or we are not
        // allowed
        // to create children under it.
        DistributedAsset dDSID = new DistributedAsset(args.element("pid"));

        // Validate
        PSSDObject.Type type = PSSDObject.Type.parse(nig.mf.pssd.plugin.util.PSSDUtil.typeOf(executor(), dDSID));
        if (type == null) {
            throw new Exception("The asset associated with " + dDSID.toString() + " does not exist");
        }
        if (type != DataSet.TYPE) {
            throw new Exception("Object " + dDSID.getCiteableID() + " [type=" + type + "] is not a " + DataSet.TYPE);
        }
        if (dDSID.isReplica()) {
            throw new Exception("The supplied parent DataSet is a replica and this service cannot create its child");
        }

        // Generate next CID.
        // TBD:Set distation to ...
        String pdist = null;
        String cid = nig.mf.pssd.plugin.util.CiteableIdUtil.createCid(executor(), dDSID.getCiteableID(), pdist);

        // Create DataObject
        createDataObjectAsset(args, dDSID, cid, in);

        w.add("id", cid);

        // Generate system event
        SystemEventChannel.generate(new PSSDObjectEvent(Action.CREATE, cid, 0));

    }

    private void createDataObjectAsset(XmlDoc.Element args, DistributedAsset dDSID, String cid, Inputs in)
            throws Throwable {

        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("cid", cid);
        dm.add("namespace", PSSDUtils.namespace(executor(), dDSID));
        dm.add("model", DataObject.MODEL);

        String type = args.value("type");
        if (type != null) {
            dm.add("type", type);
        }

        dm.push("meta");
        PSSDUtils.setObjectMeta(dm, DataObject.TYPE, args.value("name"), args.value("description"), true);
        PSSDUtils.setObjectOptionalMeta(dm, args.element("meta"), "om.pssd.data-object");
        dm.pop();

        // Get the parent Project CID; this is required for ACLs on the DataSet
        PSSDUtils.addDataObjectACLs(dm, dDSID.getParentProjectCID());

        // Create on local server
        executor().execute("asset.create", dm.root(), in, null);
    }

}
