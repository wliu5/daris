package daris.plugin.services;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.AssetType;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.LongType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcShoppingCartContentRemove extends PluginService {

    public static final String SERVICE_NAME = "daris.shoppingcart.content.remove";

    private Interface _defn;

    public SvcShoppingCartContentRemove() throws Throwable {

        _defn = new Interface();
        _defn.add(new Interface.Element("sid", LongType.DEFAULT, "The shopping cart id.", 1, 1));
        Interface.Element cid = new Interface.Element("cid", CiteableIdType.DEFAULT,
                "The citable id of the object to be removed.", 0, Integer.MAX_VALUE);
        cid.add(new Interface.Attribute("recursive", BooleanType.DEFAULT,
                "Should the descendants be included? Defaults to true.", 0));
        _defn.add(cid);
        _defn.add(new Interface.Element("id", AssetType.DEFAULT, "The asset id of the asset to be removed.", 0,
                Integer.MAX_VALUE));
    }

    public String name() {

        return SERVICE_NAME;
    }

    public String description() {

        return "Remove contents from the specified shopping cart.";
    }

    public Interface definition() {

        return _defn;
    }

    public Access access() {

        return ACCESS_MODIFY;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

        if (!args.elementExists("id") && !args.elementExists("cid")) {
            return;
        }

        String cartId = args.value("sid");

        /*
         * refresh the content list first
         */
        SvcShoppingCartContentList.refreshContent(executor(), cartId);

        if (args.elementExists("id")) {
            /*
             * remove by asset id
             */
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("sid", cartId);
            dm.addAll(args.elements("id"));
            executor().execute("shopping.cart.content.remove", dm.root());
        }

        if (args.elementExists("cid")) {
            /*
             * remove by citeable id
             */
            Map<String, String> items = getContentItemCids(executor(), cartId);
            if (items != null) {
                Set<String> idsToRemove = new HashSet<String>();
                List<XmlDoc.Element> cides = args.elements("cid");
                for (XmlDoc.Element cide : cides) {
                    String cid = cide.value();
                    boolean recursive = cide.booleanValue("recursive", true);
                    if (items.containsKey(cid)) {
                        idsToRemove.add(items.get(cid));
                        items.remove(cid);
                    }
                    if (recursive) {
                        for (Iterator<String> it = items.keySet().iterator(); it.hasNext();) {
                            String itemCid = it.next();
                            String itemId = items.get(itemCid);
                            if (itemCid.startsWith(cid + ".") || itemCid.equals(cid)) {
                                idsToRemove.add(itemId);
                                it.remove();
                            }
                        }
                    }
                }
                if (idsToRemove.isEmpty()) {
                    XmlDocMaker dm = new XmlDocMaker("args");
                    dm.add("sid", cartId);
                    for (String idToRemove : idsToRemove) {
                        dm.add("id", idToRemove);
                    }
                    executor().execute("shopping.cart.content.remove", dm.root());
                }
            }
        }

    }

    private static Map<String, String> getContentItemCids(ServiceExecutor executor, String cartId) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("sid", cartId);
        dm.add("size", "infinity");
        Collection<String> assetIds = executor.execute("shopping.cart.content.list", dm.root())
                .values("cart/item/asset/@id");
        Map<String, String> items = new HashMap<String, String>();
        if (assetIds != null && !assetIds.isEmpty()) {
            dm = new XmlDocMaker("args");
            for (String assetId : assetIds) {
                dm.add("id", assetId);
            }
            List<XmlDoc.Element> ides = executor.execute("asset.identifier.get", dm.root()).elements("id");
            if (ides != null && !ides.isEmpty()) {
                for (XmlDoc.Element ide : ides) {
                    String id = ide.value();
                    String cid = ide.value("@cid");
                    if (cid != null) {
                        items.put(cid, id);
                    }
                }
            }
        }
        if (!items.isEmpty()) {
            return items;
        } else {
            return null;
        }
    }

}
