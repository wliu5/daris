package daris.plugin.services;

import java.util.Collection;
import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.AssetType;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.CiteableIdType;
import arc.mf.plugin.dtype.LongType;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcShoppingCartContentAdd extends PluginService {

    public static final String SERVICE_NAME = "daris.shoppingcart.content.add";

    private Interface _defn;

    public SvcShoppingCartContentAdd() throws Throwable {

        _defn = new Interface();
        _defn.add(new Interface.Element("sid", LongType.DEFAULT, "The shopping cart id.", 1, 1));
        Interface.Element cid = new Interface.Element("cid", CiteableIdType.DEFAULT,
                "The citable id of the asset to be added.", 0, Integer.MAX_VALUE);
        cid.add(new Interface.Attribute("recursive", BooleanType.DEFAULT,
                "Should the descendants be included? Defaults to true.", 0));
        _defn.add(cid);
        _defn.add(new Interface.Element("id", AssetType.DEFAULT, "The asset id of the asset to be added.", 0,
                Integer.MAX_VALUE));
        _defn.add(new Interface.Element("where", StringType.DEFAULT, "A query to select assets to add to the cart.", 0,
                1));
    }

    public String name() {

        return SERVICE_NAME;
    }

    public String description() {

        return "Add objects into the shopping cart.";
    }

    public Interface definition() {

        return _defn;
    }

    public Access access() {

        return ACCESS_MODIFY;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

        String cartId = args.value("sid");

        if (args.elementExists("id")) {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("sid", cartId);
            dm.addAll(args.elements("id"));
            executor().execute("shopping.cart.content.add", dm.root());
        }

        if (!args.elementExists("cid") && !args.elementExists("where")) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (args.elementExists("cid")) {
            List<XmlDoc.Element> cides = args.elements("cid");
            if (cides != null && !cides.isEmpty()) {
                sb.append("(");
                for (int i = 0; i < cides.size(); i++) {
                    XmlDoc.Element cide = cides.get(i);
                    String cid = cide.value();
                    boolean recursive = cide.booleanValue("@recursive", true);
                    if (i > 0) {
                        sb.append(" or ");
                    }
                    sb.append("cid='").append(cid).append("'");
                    if (recursive) {
                        sb.append(" or cid starts with '").append(cid).append("'");
                    }
                }
                sb.append(")");
            }
        }

        Collection<String> ids = args.values("id");
        if (ids != null && !ids.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" or ");
            }
            sb.append("(");
            boolean first = true;
            for (String id : ids) {
                if (first) {
                    first = false;
                } else {
                    sb.append(" or ");
                }
                sb.append("id=").append(id);
            }
            sb.append(")");
        }

        String where = args.value("where");
        if (where != null) {
            if (sb.length() > 0) {
                sb.append(" or (").append(where).append(")");
            } else {
                sb.append(where);
            }
        }

        where = sb.toString();
        if (!where.isEmpty()) {
            where = "(" + where + ") and asset has content";
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("sid", cartId);
            dm.add("where", where);
            executor().execute("shopping.cart.content.add", dm.root());
        }

    }

}
