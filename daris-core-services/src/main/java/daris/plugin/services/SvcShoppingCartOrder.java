package daris.plugin.services;

import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.PluginThread;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.LongType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import nig.mf.plugin.pssd.user.self.settings.ShoppingCartUserSelfSettings;

public class SvcShoppingCartOrder extends PluginService {

    public static final String SERVICE_NAME = "daris.shoppingcart.order";

    private Interface _defn;

    public SvcShoppingCartOrder() throws Throwable {

        _defn = new Interface();
        _defn.add(new Interface.Element("sid", LongType.DEFAULT, "The id of the shopping cart.", 1, 1));
    }

    public String name() {

        return SERVICE_NAME;
    }

    public String description() {

        return "Start processing the specified shopping cart.";
    }

    public Interface definition() {

        return _defn;
    }

    public Access access() {

        return ACCESS_MODIFY;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {
        String cartId = args.value("sid");
        /*
         * resolve cart information
         */
        final XmlDoc.Element ce = executor()
                .execute("shopping.cart.describe", "<args><sid>" + cartId + "</sid></args>", null, null)
                .element("cart");
        /*
         * check if cart is editable
         */
        if (ce.booleanValue("access/can-modify") == false) {
            throw new Exception(
                    "Shopping cart " + cartId + " cannot be modified because of its status: " + ce.value("status"));
        }

        /*
         * order
         */
        executor().execute("shopping.cart.order", "<args><sid>" + cartId + "</sid></args>", null, null);

        /*
         * save cart settings to user's profile (user.self.settings)
         */
        PluginThread.executeAsync("save shopping cart settings to user's profile", new Runnable() {

            @Override
            public void run() {
                try {
                    saveToUserSelfSettings(PluginThread.serviceExecutor(), ce);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void order(ServiceExecutor executor, String cartId) throws Throwable {

        /*
         * save cart settings to user.self.settings
         */
        XmlDoc.Element ce = executor
                .execute("shopping.cart.describe", "<args><sid>" + cartId + "</sid></args>", null, null)
                .element("cart");
        if (ce == null) {
            throw new IllegalArgumentException("Shopping cart " + cartId + " does not exist.");
        }
        if (ce.booleanValue("access/can-modify") == false) {
            throw new Exception(
                    "Shopping cart " + cartId + " cannot be modified because of its status: " + ce.value("status"));
        }

    }

    private static void saveToUserSelfSettings(ServiceExecutor executor, XmlDoc.Element ce) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker(ShoppingCartUserSelfSettings.ROOT_ELEMENT_NAME);

        /*
         * delivery
         */
        dm.push("delivery");
        dm.add("method", ce.value("delivery-method"));
        if (ce.elementExists("delivery-destination")) {
            dm.add("destination", ce.value("delivery-destination"));
        }
        if (ce.elementExists("delivery-arg")) {
            List<XmlDoc.Element> aes = ce.elements("delivery-arg");
            for (XmlDoc.Element ae : aes) {
                dm.add("arg", new String[] { "name", ae.value("@name") }, ae.value());
            }
        }
        if (ce.elementExists("secure-delivery-arg")) {
            List<XmlDoc.Element> saes = ce.elements("secure-delivery-arg");
            for (XmlDoc.Element sae : saes) {
                String source = sae.value("@source");
                if (source != null) {
                    dm.add("arg", new String[] { "in-secure-wallet", Boolean.toString(true) }, source);
                }
            }
        }
        dm.pop();

        /*
         * archive (packaging)
         */
        if (ce.elementExists("packaging")) {
            dm.push("archive");
            dm.add("type", ce.value("packaging"));
            if (ce.elementExists("packaging/parameter")) {
                List<XmlDoc.Element> pes = ce.elements("packaging/parameter");
                for (XmlDoc.Element pe : pes) {
                    dm.add("parameter", new String[] { "name", pe.value("@name") }, pe.value());
                }
            }
            dm.pop();
        }

        /*
         * layout
         */
        if (ce.elementExists("layout")) {
            dm.push("layout");
            dm.add("type", ce.value("layout"));
            if (ce.elementExists("layout/layout-pattern")) {
                dm.add("pattern", ce.value("layout/layout-pattern"));
            }
            dm.pop();
        }

        ShoppingCartUserSelfSettings.set(executor, dm.root());
    }

}
