package daris.client.model.object.exports;

import java.util.ArrayList;
import java.util.List;

import arc.mf.client.util.ObjectUtil;
import arc.mf.client.xml.XmlElement;
import arc.mf.client.xml.XmlStringWriter;
import arc.mf.dtype.DynamicEnumerationExistsHandler;
import arc.mf.object.ObjectMessageResponse;
import arc.mf.object.ObjectRef;
import arc.mf.object.ObjectResolveHandler;

public class PathExpressionSetRef extends ObjectRef<List<PathExpression>> {

    public static final String SERVICE_NAME = "daris.path.expression.list";

    private String _project;

    public PathExpressionSetRef(String project) {
        _project = project;
    }

    @Override
    protected void resolveServiceArgs(XmlStringWriter w) {
        if (_project != null) {
            w.add("project", "_project");
        }
    }

    @Override
    protected String resolveServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected List<PathExpression> instantiate(XmlElement xe) throws Throwable {
        List<XmlElement> ees = xe.elements("expression");
        return instantiate(ees);
    }

    @Override
    public String referentTypeName() {
        return "path expression list";
    }

    @Override
    public String idToString() {
        return _project;
    }

    public void exists(final String name, final String project, final ObjectMessageResponse<Boolean> rh) {
        resolve(new ObjectResolveHandler<List<PathExpression>>() {

            @Override
            public void resolved(List<PathExpression> pes) {
                boolean exists = false;
                if (pes != null) {
                    for (PathExpression pe : pes) {
                        if (ObjectUtil.equals(name, pe.name) && ObjectUtil.equals(project, pe.project)) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (rh != null) {
                    rh.responded(exists);
                }
            }
        });
    }

    public void exists(final String displayName, final DynamicEnumerationExistsHandler handler) {
        if (displayName == null) {
            handler.exists(displayName, false);
            return;
        }
        String project = null;
        String name = null;
        int idx = displayName.indexOf(':');
        if (idx > 0) {
            project = displayName.substring(0, idx);
            name = displayName.substring(idx + 1);
        } else {
            name = displayName;
        }
        exists(name, project, new ObjectMessageResponse<Boolean>() {
            @Override
            public void responded(Boolean exists) {
                handler.exists(displayName, exists);
            }
        });
    }

    public static List<PathExpression> instantiate(List<XmlElement> ees) {
        if (ees != null && !ees.isEmpty()) {
            List<PathExpression> pes = new ArrayList<PathExpression>(ees.size());
            for (XmlElement ee : ees) {
                PathExpression pe = new PathExpression(ee.value("@name"), ee.value("@project"), ee.value());
                pes.add(pe);
            }
            return pes;
        }
        return null;
    }

    public static void resolve(String expression, ObjectResolveHandler<PathExpression> rh) {
        resolve(expression, null, rh);
    }

    public static void resolve(final String expression, String project, final ObjectResolveHandler<PathExpression> rh) {
        if (expression == null) {
            rh.resolved(null);
            return;
        }
        new PathExpressionSetRef(project).resolve(new ObjectResolveHandler<List<PathExpression>>() {

            @Override
            public void resolved(List<PathExpression> pes) {
                if (pes != null) {
                    for (PathExpression pe : pes) {
                        if (expression.equals(pe.expression)) {
                            rh.resolved(pe);
                            return;
                        }
                    }
                }
                rh.resolved(null);
            }
        });
    }

}
