package daris.plugin.services;

import java.util.Collection;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.ServiceExecutor;
import arc.xml.XmlDoc;
import arc.xml.XmlWriter;
import nig.mf.plugin.pssd.sc.DeliveryDestination;
import nig.mf.plugin.pssd.sc.DeliveryMethod;
import nig.mf.plugin.pssd.sink.Sink;

public class SvcShoppingCartDestinationList extends PluginService {

    public static final String SERVICE_NAME = "daris.shoppingcart.destination.list";

    private Interface _defn;

    public SvcShoppingCartDestinationList() throws Throwable {

        _defn = new Interface();

    }

    public String name() {

        return SERVICE_NAME;
    }

    public String description() {

        return "List the deposit destinations on the server side for shopping carts.";
    }

    public Interface definition() {

        return _defn;
    }

    public Access access() {

        return ACCESS_ACCESS;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {
        list(executor(), w);
    }

    public static void list(ServiceExecutor executor, XmlWriter w) throws Throwable {
        w.add("destination", new String[] { "method", DeliveryMethod.download.name() }, DeliveryDestination.BROWSER);
        Collection<String> sinkUrls = Sink.listUrls(executor);
        if (sinkUrls != null) {
            for (String sinkUrl : sinkUrls) {
                w.add("destination", new String[] { "method", DeliveryMethod.deposit.name() }, sinkUrl);
            }
        }
    }

}
