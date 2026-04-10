package uml4java;

public class MethodMember extends Member {
    private final String params;

    public MethodMember(char visibility, String type, String name, String params, boolean isStatic) {
        super(visibility, type, name, isStatic);
        this.params = params;
    }

    public String getParams() { return params; }

    @Override
    public boolean isMethod() { return true; }

    @Override
    public String toMermaid() {
        if (isStatic())
            return String.format("%c%s(%s)$ %s", getVisibility(), getName(), params, getType());
        if (!getType().isEmpty())
            return String.format("%c%s(%s) %s", getVisibility(), getName(), params, getType());
        return String.format("%c%s(%s)", getVisibility(), getName(), params);
    }
}
