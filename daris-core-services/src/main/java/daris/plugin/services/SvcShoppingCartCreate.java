package daris.plugin.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.StringType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import nig.mf.plugin.pssd.sc.Archive;
import nig.mf.plugin.pssd.sc.DeliveryMethod;
import nig.mf.plugin.pssd.sc.Layout;
import nig.mf.plugin.pssd.sc.ShoppingCartTemplate;
import nig.mf.plugin.pssd.user.self.settings.ShoppingCartUserSelfSettings;

public class SvcShoppingCartCreate extends PluginService {

    public static final String SERVICE_NAME = "daris.shoppingcart.create";

    private Interface _defn;

    public SvcShoppingCartCreate() throws Throwable {

        _defn = new Interface();

        /*
         * name
         */
        _defn.add(new Interface.Element("name", StringType.DEFAULT,
                "A human readable name (could be non-unique) for the shopping-cart.", 0, 1));

        /*
         * description
         */
        _defn.add(
                new Interface.Element("description", StringType.DEFAULT, "A description for thr shopping cart.", 0, 1));

    }

    public String name() {

        return SERVICE_NAME;
    }

    public String description() {

        return "Creates a shopping-cart based on default shopping-cart template.";
    }

    public Interface definition() {

        return _defn;
    }

    public Access access() {

        return ACCESS_MODIFY;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

        /*
         * name
         */
        String name = args.value("name");

        /*
         * description
         */
        String description = args.value("description");

        /*
         * create the cart
         */
        String cartId = create(executor(), name, description, true);
        w.add("sid", cartId);
    }

    public static String create(ServiceExecutor executor, String name, String description,
            boolean loadFromUserSelfSettings) throws Throwable {

        XmlDocMaker dm = new XmlDocMaker("args");
        if (name != null) {
            dm.add("name", name);
        }
        if (description != null) {
            dm.add("description", description);
        }
        dm.add("template", ShoppingCartTemplate.NAME);

        String cartId = executor.execute("shopping.cart.create", dm.root()).value("id");

        XmlDoc.Element ce = executor
                .execute("shopping.cart.describe", "<args><sid>" + cartId + "</sid></args>", null, null)
                .element("cart");

        Settings settings = new Settings();
        settings.deliveryMethod = DeliveryMethod.fromString(ce.value("delivery-method"));
        settings.archiveType = Archive.Type.fromString(ce.value("packaging"));

        boolean needToUpdate = false;

        /*
         * load from user.self.settings?
         */
        if (loadFromUserSelfSettings) {
            loadFromUserSelfSettings(executor, settings);
            needToUpdate = true;
        }

        /*
         * if no delivery method is set, defaults to download (zip)
         */
        if (settings.deliveryMethod == null) {
            settings.deliveryMethod = DeliveryMethod.download;
            settings.archiveType = Archive.Type.zip;
            needToUpdate = true;
        }

        /*
         * correct packaging/archive settings.
         */
        if (settings.deliveryMethod == DeliveryMethod.deposit) {
            if (settings.archiveType != Archive.Type.none) {
                settings.archiveType = Archive.Type.none;
                needToUpdate = true;
            }
        } else {
            if (settings.archiveType == Archive.Type.none) {
                settings.archiveType = Archive.Type.zip;
                needToUpdate = true;
            }
        }

        if (needToUpdate) {
            dm = new XmlDocMaker("args");
            dm.add("sid", cartId);

            /*
             * delivery method, destination and args
             */
            if (settings.deliveryMethod != null) {
                dm.add("delivery", settings.deliveryMethod);
                if (settings.deliveryMethod == DeliveryMethod.deposit && settings.deliveryDestination != null) {
                    dm.add("delivery-destination", settings.deliveryDestination);
                    if (settings.deliveryArgs != null && !settings.deliveryArgs.isEmpty()) {
                        Set<String> argNames = settings.deliveryArgs.keySet();
                        for (String argName : argNames) {
                            dm.add("delivery-arg", new String[] { "name", argName },
                                    settings.deliveryArgs.get(argName));
                        }
                    }
                    if (settings.secureWalletDeliveryArgs != null && !settings.secureWalletDeliveryArgs.isEmpty()) {
                        Set<String> argNames = settings.secureWalletDeliveryArgs.keySet();
                        for (String argName : argNames) {
                            dm.add("secure-wallet-delivery-arg", new String[] { "name", argName },
                                    settings.secureWalletDeliveryArgs.get(argName));
                        }
                    }
                }
            }

            /*
             * archive
             */
            if (settings.archiveType != null) {
                dm.push("packaging");
                dm.add("package-method", settings.archiveType.toString());
                if (settings.archiveParams != null && !settings.archiveParams.isEmpty()) {
                    Set<String> paramNames = settings.archiveParams.keySet();
                    for (String paramName : paramNames) {
                        dm.add("parameter", new String[] { "name", paramName }, settings.archiveParams.get(paramName));
                    }
                }
                dm.pop();
            }

            /*
             * layout
             */
            if (settings.layoutType != null) {
                dm.add("layout", settings.layoutType.name());
                if (settings.layoutType == Layout.Type.custom && settings.layoutPattern != null) {
                    dm.add("layout-pattern", settings.layoutPattern);
                }
            }
            executor.execute("shopping.cart.modify", dm.root());
        }

        // returns the cart id
        return cartId;
    }

    private static void loadFromUserSelfSettings(ServiceExecutor executor, Settings applyTo) throws Throwable {
        Settings settings = loadFromUserSelfSettings(executor);
        if (settings != null) {
            applyTo.apply(settings);
        }
    }

    private static Settings loadFromUserSelfSettings(ServiceExecutor executor) throws Throwable {
        XmlDoc.Element usse = ShoppingCartUserSelfSettings.get(executor);
        if (usse == null || !ShoppingCartUserSelfSettings.isValid(usse, executor)) {
            return null;
        }

        Settings settings = new Settings();

        /*
         * delivery method, destination and args
         */
        settings.deliveryMethod = DeliveryMethod.fromString(usse.value("delivery/method"));
        if (settings.deliveryMethod != null) {
            if (settings.deliveryMethod == DeliveryMethod.deposit) {
                settings.deliveryDestination = usse.value("delivery/destination");
                List<XmlDoc.Element> aes = usse.elements("delivery/arg");
                if (aes != null) {
                    for (XmlDoc.Element ae : aes) {
                        boolean inSecureWallet = ae.booleanValue("@in-secure-wallet", false);
                        if (inSecureWallet) {
                            settings.secureWalletDeliveryArgs.put(ae.value("@name"), ae.value());
                        } else {
                            settings.deliveryArgs.put(ae.value("@name"), ae.value());
                        }
                    }
                }
            }
        }

        /*
         * archive
         */
        settings.archiveType = Archive.Type.fromString(usse.value("archive/type"));
        if (settings.archiveType != null) {
            if (usse.elementExists("archive/parameter")) {
                List<XmlDoc.Element> pes = usse.elements("archive/parameter");
                for (XmlDoc.Element pe : pes) {
                    settings.archiveParams.put(pe.value("@name"), pe.value());
                }
            }
        }

        /*
         * layout
         */
        settings.layoutType = Layout.Type.fromString(usse.value("layout/type"));
        if (settings.layoutType != null) {
            if (settings.layoutType == Layout.Type.custom) {
                settings.layoutPattern = usse.value("layout/pattern");
            }
        }

        if (settings.deliveryMethod != null || settings.archiveType != null || settings.layoutType != null) {
            return settings;
        }
        return null;
    }

    private static class Settings {
        DeliveryMethod deliveryMethod;
        String deliveryDestination;
        Map<String, String> deliveryArgs = new LinkedHashMap<String, String>();
        Map<String, String> secureWalletDeliveryArgs = new LinkedHashMap<String, String>();
        Archive.Type archiveType;
        Map<String, String> archiveParams;
        Layout.Type layoutType;
        String layoutPattern;

        void apply(Settings settings) {
            this.deliveryMethod = settings.deliveryMethod;
            this.deliveryDestination = settings.deliveryDestination;
            this.deliveryArgs = settings.deliveryArgs;
            this.secureWalletDeliveryArgs = settings.secureWalletDeliveryArgs;
            this.archiveType = settings.archiveType;
            this.archiveParams = settings.archiveParams;
            this.layoutType = settings.layoutType;
            this.layoutPattern = settings.layoutPattern;
        }
    }

}
