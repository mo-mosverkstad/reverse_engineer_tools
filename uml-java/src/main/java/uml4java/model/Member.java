package uml4java.model;

/**
 * Abstract base class representing a member (field or method) of a Java class.
 * Subclasses provide specific Mermaid rendering via {@link #toMermaid()}.
 */
public abstract class Member {
    private final char visibility;
    private final String type;
    private final String name;
    private final boolean isStatic;

    /**
     * @param visibility UML visibility character ({@code +}, {@code -}, {@code #}, or {@code ~})
     * @param type       the declared type (return type for methods, field type for fields)
     * @param name       the member name
     * @param isStatic   whether the member is declared {@code static}
     */
    protected Member(char visibility, String type, String name, boolean isStatic) {
        this.visibility = visibility;
        this.type = type;
        this.name = name;
        this.isStatic = isStatic;
    }

    public char getVisibility() { return visibility; }
    public String getType() { return type; }
    public String getName() { return name; }
    public boolean isStatic() { return isStatic; }
    /** @return {@code true} if this member is a method, {@code false} if it is a field */
    public abstract boolean isMethod();

    /** @return the Mermaid class-diagram syntax for this member */
    public abstract String toMermaid();
}
