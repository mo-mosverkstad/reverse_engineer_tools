package uml4java.model;

/**
 * Represents a method member of a Java class.
 * Renders with parameter list and optional static/return-type markers in Mermaid syntax.
 */
public class MethodMember extends Member {
    private final String params;

    /**
     * @param visibility UML visibility character
     * @param type       the method's return type (empty string for constructors)
     * @param name       the method name
     * @param params     the raw parameter list string (e.g. {@code "String name, int age"})
     * @param isStatic   whether the method is declared {@code static}
     */
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
