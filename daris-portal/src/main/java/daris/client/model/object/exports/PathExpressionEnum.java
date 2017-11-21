package daris.client.model.object.exports;

import java.util.ArrayList;
import java.util.List;

import arc.mf.dtype.DynamicEnumerationDataHandler;
import arc.mf.dtype.DynamicEnumerationDataSource;
import arc.mf.dtype.DynamicEnumerationExistsHandler;
import arc.mf.dtype.EnumerationType.Value;
import arc.mf.object.ObjectResolveHandler;

public class PathExpressionEnum implements DynamicEnumerationDataSource<PathExpression> {

    private PathExpressionSetRef _pes;

    public PathExpressionEnum(String project) {
        _pes = new PathExpressionSetRef(project);
    }

    @Override
    public boolean supportPrefix() {
        return false;
    }

    @Override
    public void exists(final String displayName, final DynamicEnumerationExistsHandler handler) {
        _pes.exists(displayName, handler);
    }

    @Override
    public void retrieve(String prefix, final long start, final long end,
            final DynamicEnumerationDataHandler<PathExpression> handler) {
        _pes.resolve(new ObjectResolveHandler<List<PathExpression>>() {

            @Override
            public void resolved(List<PathExpression> pes) {
                if (pes != null && !pes.isEmpty()) {
                    List<Value<PathExpression>> evs = new ArrayList<Value<PathExpression>>(pes.size());
                    for (PathExpression pe : pes) {
                        Value<PathExpression> ev = new Value<PathExpression>(pe.displayName(), pe.expression, pe);
                        evs.add(ev);
                    }
                    handler.process(0, evs.size(), evs.size(), evs);
                    return;
                }
                handler.process(0, 0, 0, null);
            }
        });
    }
}
