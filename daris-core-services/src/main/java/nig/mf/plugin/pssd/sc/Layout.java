package nig.mf.plugin.pssd.sc;

public class Layout {

    public static final Type DEFAULT_TYPE = Type.custom;

    public static final String DEFAULT_PATTERN = "cid(-7,-5)/cid(-7,-4)/cid(-7,-3)/cid(-7,-2)/replace(if-null(variable(tx-to-type),xpath(asset/type)),'/','_')/if-null(xpath(daris:pssd-filename/original), cid(-1), xpath(daris:pssd-filename/original))if-null(xpath(daris:pssd-filename/original), if-null(xpath(daris:pssd-object/name),'','_'),'')if-null(xpath(daris:pssd-filename/original),xpath(daris:pssd-object/name),'')";

    public static enum Type {

        custom, flat, preserved;

        public static Type fromString(String type) {
            Type[] ts = values();
            for (int i = 0; i < ts.length; i++) {
                if (ts[i].toString().equalsIgnoreCase(type)) {
                    return ts[i];
                }
            }
            return null;
        }

    }

}
