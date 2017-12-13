package daris.client.model.sc.messages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import arc.mf.client.util.ListUtil;
import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlWriter;
import arc.mf.object.Null;
import arc.mf.object.ObjectMessage;
import daris.client.model.object.DObjectRef;
import daris.client.model.sc.ShoppingCart;
import daris.client.model.sc.ShoppingCartRef;

public class ShoppingCartContentAdd extends ObjectMessage<Null> {

    public static final String SERVICE_NAME = "daris.shoppingcart.content.add";

    private ShoppingCartRef _cart;
    private Map<String, Boolean> _cids;
    private Collection<String> _ids;
    private String _where;

    protected ShoppingCartContentAdd(ShoppingCartRef cart, Map<String, Boolean> cids, Collection<String> ids,
            String where) {
        _cart = cart;
        _cids = cids == null ? null : new LinkedHashMap<String, Boolean>(cids);
        _ids = ids == null ? null : new ArrayList<String>(ids);
        _where = where;
    }

    public ShoppingCartContentAdd(ShoppingCartRef cart, boolean recursive, Collection<DObjectRef> os) {
        _cart = cart;
        _cids = os == null ? null : new LinkedHashMap<String, Boolean>();
        for (DObjectRef o : os) {
            _cids.put(o.id(), recursive);
        }
        _ids = null;
        _where = null;
    }

    public ShoppingCartContentAdd(ShoppingCartRef cart, Collection<String> cids, boolean recursive) {
        _cart = cart;
        _cids = cids == null ? null : new LinkedHashMap<String, Boolean>();
        for (String cid : cids) {
            _cids.put(cid, recursive);
        }
        _ids = null;
        _where = null;
    }

    public ShoppingCartContentAdd(ShoppingCartRef cart, DObjectRef o, boolean recursive) {
        this(cart, ListUtil.list(o.id()), recursive);
    }

    public ShoppingCartContentAdd(ShoppingCartRef cart, String where) {
        this(cart, null, null, where);
    }

    public ShoppingCartContentAdd(ShoppingCartRef cart, Collection<String> ids) {
        this(cart, null, ids, null);
    }

    @Override
    protected void messageServiceArgs(XmlWriter w) {

        w.add("sid", _cart.id());
        if (_cids != null) {
            Set<String> cids = _cids.keySet();
            for (String cid : cids) {
                w.add("cid", new String[] { "recursive", Boolean.toString(_cids.get(cid)) }, cid);
            }
        }
        if (_where != null) {
            w.add("where", _where);
        }
        if (_ids != null) {
            for (String id : _ids) {
                w.add("id", id);
            }
        }
    }

    @Override
    protected String messageServiceName() {

        return SERVICE_NAME;
    }

    @Override
    protected Null instantiate(XmlElement xe) throws Throwable {

        return new Null();
    }

    @Override
    protected String objectTypeName() {

        return ShoppingCart.TYPE_NAME;
    }

    @Override
    protected String idToString() {

        return Long.toString(_cart.id());
    }

}
