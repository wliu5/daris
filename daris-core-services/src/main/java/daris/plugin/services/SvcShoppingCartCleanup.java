package daris.plugin.services;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.IntegerType;
import arc.xml.XmlDoc;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;
import nig.mf.plugin.pssd.sc.Status;

public class SvcShoppingCartCleanup extends PluginService {

    public static final String SERVICE_NAME = "daris.shoppingcart.cleanup";

    public static enum TimeUnit {
        year, month, week, day, hour, minute, second;

        public static TimeUnit fromString(String unit) {
            if (unit != null) {
                TimeUnit[] vs = values();
                for (TimeUnit v : vs) {
                    if (v.name().equalsIgnoreCase(unit)) {
                        return v;
                    }
                }
            }
            return null;
        }
    }

    private Interface _defn;

    public SvcShoppingCartCleanup() throws Throwable {

        _defn = new Interface();
        Interface.Element e = new Interface.Element("before", new IntegerType(1, Integer.MAX_VALUE),
                "The shopping cart finished before the specified time point.", 1, 1);
        e.add(new Interface.Attribute("unit", new arc.mf.plugin.dtype.EnumType(TimeUnit.values()), "The time unit", 1));
        _defn.add(e);
        _defn.add(new Interface.Element("all", BooleanType.DEFAULT,
                "Set to true to clear also the shopping carts belong to other users. For system-administrator only. Defaults to false.",
                0, 1));

    }

    public String name() {

        return SERVICE_NAME;
    }

    public String description() {

        return "Clean up the shopping carts finished the specified days ago.";
    }

    public Interface definition() {

        return _defn;
    }

    public Access access() {

        return ACCESS_MODIFY;
    }

    public void execute(XmlDoc.Element args, Inputs in, Outputs out, XmlWriter w) throws Throwable {

        boolean listAll = args.booleanValue("all", false);
        TimeUnit unit = TimeUnit.fromString(args.value("before/@unit"));
        int n = args.intValue("before");
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        if (TimeUnit.year == unit) {
            cal.add(Calendar.YEAR, -n);
        } else if (TimeUnit.month == unit) {
            cal.add(Calendar.MONTH, -n);
        } else if (TimeUnit.week == unit) {
            cal.add(Calendar.DATE, -(n * 7));
        } else if (TimeUnit.day == unit) {
            cal.add(Calendar.DATE, -n);
        } else if (TimeUnit.hour == unit) {
            cal.add(Calendar.HOUR, -n);
        } else if (TimeUnit.minute == unit) {
            cal.add(Calendar.MINUTE, -n);
        } else if (TimeUnit.second == unit) {
            cal.add(Calendar.SECOND, -n);
        }
        Date d = cal.getTime();

        XmlDocMaker dm = new XmlDocMaker("args");
        dm.add("status", Status.aborted);
        dm.add("status", Status.data_ready);
        dm.add("status", Status.rejected);
        dm.add("status", Status.error);
        dm.add("status", Status.withdrawn);
        dm.add("size", "infinity");
        dm.add("list-all", listAll);

        XmlDoc.Element r = executor().execute("shopping.cart.describe", dm.root());
        List<XmlDoc.Element> ces = r.elements("cart");
        System.out.println("### 1: " + (ces == null ? 0 : ces.size()));
        Set<String> cartIds = new LinkedHashSet<String>();
        if (ces != null) {
            for (XmlDoc.Element ce : ces) {
                String id = ce.value("@id");
                Date changed = ce.dateValue("status/@changed");
                if (changed.before(d)) {
                    cartIds.add(id);
                }
            }
        }
        if (!cartIds.isEmpty()) {
            System.out.println("### 2: " + cartIds.size());
            SvcShoppingCartDestroy.destroy(executor(), cartIds);
        }
        w.add("total-destroyed", cartIds.size());
    }
}