package daris.client.model.sc;

import arc.mf.client.util.ObjectUtil;
import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlWriter;

public class Layout {

    public static enum Type {
        custom, flat, preserved;
        public static Type fromString(String type) {
            Type[] ts = values();
            for (Type t : ts) {
                if (t.name().equalsIgnoreCase(type)) {
                    return t;
                }
            }
            return null;
        }

    }

    private Type _type;
    private String _pattern;

    public Layout(Type type, String pattern) {
        _type = type;
        _pattern = pattern;
    }

    public Layout(XmlElement le) {
        _type = Layout.Type.fromString(le.value());
        _pattern = le.value("layout-pattern");
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof Layout)) {
            return false;
        }
        Layout lo = (Layout) o;
        return _type == lo.type() && ObjectUtil.equals(_pattern, lo.pattern());
    }

    public Type type() {
        return _type;
    }

    public String pattern() {
        return _pattern;
    }

    public static Layout instantiate(XmlElement le) throws Throwable {
        if (le != null) {
            return new Layout(le);
        }
        return null;
    }

    public void saveUpdateArgs(XmlWriter w) {
        w.add("layout", _type.name());
        if (_type == Type.custom) {
            w.add("layout-pattern", _pattern);
        }
    }

}
