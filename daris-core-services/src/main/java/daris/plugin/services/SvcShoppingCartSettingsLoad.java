package daris.plugin.services;

import nig.mf.plugin.pssd.user.self.settings.ShoppingCartUserSelfSettings;
import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.BooleanType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlWriter;

public class SvcShoppingCartSettingsLoad extends PluginService {

    public static final String SERVICE_NAME = "daris.shoppingcart.settings.load";

    private Interface _defn;

    public SvcShoppingCartSettingsLoad() {
        _defn = new Interface();
        _defn.add(new Interface.Element("validate", BooleanType.DEFAULT,
                "Validate the settings and returns only if it is still valid. This is to make sure the returned settings, e.g. destinations/sinks in the settings, are still available. Defaults to true.",
                0, 1));
    }

    @Override
    public Access access() {
        return ACCESS_ACCESS;
    }

    @Override
    public Interface definition() {
        return _defn;
    }

    @Override
    public String description() {
        return "Gets the current user's default shopping cart settings.";
    }

    @Override
    public void execute(Element args, Inputs arg1, Outputs arg2, XmlWriter w) throws Throwable {
        Boolean validate = args.booleanValue("validate", true);
        XmlDoc.Element se = ShoppingCartUserSelfSettings.get(executor());
        if (se != null) {
            if (validate) {
                try {
                    ShoppingCartUserSelfSettings.validate(executor(), se);
                } catch (Throwable e) {
                    return;
                }
            }
            w.push("settings");
            w.add(se, false);
            w.pop();
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
