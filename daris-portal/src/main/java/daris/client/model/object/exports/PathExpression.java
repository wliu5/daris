package daris.client.model.object.exports;

public class PathExpression {

    public final String name;
    public final String project;
    public final String expression;

    public PathExpression(String name, String project, String expression) {
        this.name = name;
        this.project = project;
        this.expression = expression;
    }

    public String displayName() {
        return this.project == null ? this.name : (this.project + ":" + this.name);
    }
    
    @Override
    public String toString() {
        return displayName();
    }

}
