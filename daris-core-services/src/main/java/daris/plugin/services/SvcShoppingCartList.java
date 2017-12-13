package daris.plugin.services;

import java.util.List;

import arc.mf.plugin.PluginService;
import arc.mf.plugin.dtype.BooleanType;
import arc.mf.plugin.dtype.EnumType;
import arc.mf.plugin.dtype.IntegerType;
import arc.xml.XmlDoc;
import arc.xml.XmlDoc.Element;
import arc.xml.XmlDocMaker;
import arc.xml.XmlWriter;

public class SvcShoppingCartList extends PluginService {

    public static final String SERVICE_NAME = "daris.shoppingcart.list";

    private Interface _defn;

    public SvcShoppingCartList() {
        _defn = new Interface();
        _defn.add(new Interface.Element("idx", IntegerType.POSITIVE_ONE,
                "The starting position of the result set. Defaults to 1.", 0, 1));
        _defn.add(new Interface.Element("size", IntegerType.POSITIVE_ONE,
                "The number of entries to retrieve. Defaults to 100.", 0, 1));
        _defn.add(new Interface.Element("status",
                new EnumType(new String[] { "editable", "await processing", "assigned", "processing", "data ready",
                        "fulfilled", "rejected", "error", "withdrawn", "aborted" }),
                "Only cart matching the specified status will be returned.", 0, 1));
        _defn.add(new Interface.Element("all", BooleanType.DEFAULT,
                "Whether or not to list all carts in the system. This operation is limited to system-administrator. Defaults to false.",
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
        return "list shopping carts.";
    }

    @Override
    public void execute(Element args, Inputs inputs, Outputs outputs, XmlWriter w) throws Throwable {
        XmlDocMaker dm = new XmlDocMaker("args");
        int idx = args.intValue("idx", 1);
        dm.add("idx", idx);
        int size = args.intValue("size", 100);
        dm.add("size", size);
        dm.add("count", true);
        if (args.elementExists("status")) {
            dm.add(args.element("status"));
        }
        if (args.elementExists("all")) {
            dm.add(args.element("list-all"));
        }
        XmlDoc.Element re = executor().execute("shopping.cart.describe", dm.root());
        List<XmlDoc.Element> ces = re.elements("cart");
        if (ces != null) {
            for (XmlDoc.Element ce : ces) {
                String id = ce.value("@id");
                String status = ce.value("status");
                String template = ce.value("template");
                String name = ce.value("name");
                String description = ce.value("description");
                String itemCount = ce.value("content-statistics/item-count");
                String itemSize = ce.value("content-statistics/item-size");
                w.add("cart", new String[] { "id", id, "status", status, "template", template, "name", name,
                        "description", description, "count", itemCount, "size", itemSize });
            }
        }
        long total = re.longValue("size", 0);
        long from = re.longValue("cursor/from", 0);
        Long prev = from > 1 ? (from - 1) : null;
        long to = re.longValue("cursor/to", 0);
        Long next = to < total ? (to + 1) : null;
        int pageCount = ces == null ? 0 : ces.size();
        long remaining = total - to;
        boolean complete = total == 0 || to >= total;

        w.add("size", total);

        if (re.elementExists("cursor")) {
            w.push("cursor");
            w.add("count", pageCount);
            w.add("from", from);
            w.add("to", to);
            if (prev != null) {
                w.add("prev", prev);
            }
            if (next != null) {
                w.add("next", next);
            }
            w.add("total", new String[] { "complete", Boolean.toString(complete) }, total);
            w.add("remaining", remaining);
            w.pop();
        }
    }

    @Override
    public String name() {
        return SERVICE_NAME;
    }

}
