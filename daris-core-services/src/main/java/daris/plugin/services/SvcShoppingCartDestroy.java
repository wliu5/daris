package daris.plugin.services;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.mf.plugin.dtype.EnumType;
import arc.mf.plugin.dtype.LongType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import nig.mf.plugin.pssd.sc.Status;

public class SvcShoppingCartDestroy extends PluginService {

    public static final String SERVICE_NAME = "daris.shoppingcart.destroy";

    private Interface _defn;

    public SvcShoppingCartDestroy() throws Throwable {

        _defn = new Interface();
        _defn.add(new Interface.Element("sid", LongType.DEFAULT, "The id of the shopping cart.", 0, Integer.MAX_VALUE));
        _defn.add(new Interface.Element("status",
                new EnumType(new String[] { Status.data_ready.status(), Status.aborted.status(),
                        Status.rejected.status(), Status.withdrawn.status(), Status.error.status() }),
                "All the shopping carts in the give status will be destroyed.", 0, 5));
    }

    public String name() {

        return SERVICE_NAME;
    }

    public String description() {

        return "Destroy the specified shopping cart.";
    }

    public Interface definition() {

        return _defn;
    }

    public Access access() {

        return ACCESS_MODIFY;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {
        Set<String> cartIds = new LinkedHashSet<String>();
        if (args.elementExists("sid")) {
            Collection<String> sids = args.values("sid");
            if (sids != null && !sids.isEmpty()) {
                cartIds.addAll(sids);
            }
        }
        if (args.elementExists("status")) {
            XmlDocMaker dm = new XmlDocMaker("args");
            dm.add("size", "infinity");
            dm.addAll(args.elements("status"));
            Collection<String> ids = executor().execute("shopping.cart.list", dm.root()).values("cart/@id");
            if (ids != null && !ids.isEmpty()) {
                cartIds.addAll(ids);
            }
        }
        if (cartIds.isEmpty()) {
            return;
        }
        destroy(executor(), cartIds);
    }

    public static void destroy(ServiceExecutor executor, Collection<String> cartIds) throws Throwable {
        if (cartIds != null) {
            XmlDocMaker dm = new XmlDocMaker("args");
            for (String cartId : cartIds) {
                dm.push("service", new String[] { "name", "shopping.cart.destroy" });
                dm.add("sid", cartId);
                dm.pop();
            }
            System.out.println(dm.root());
            executor.execute("service.execute", dm.root());
        }
    }

}
