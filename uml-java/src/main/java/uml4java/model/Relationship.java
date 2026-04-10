package uml4java.model;

/**
 * Represents a UML relationship between two classes.
 * Supports inheritance, implementation, association, and dependency types.
 */
public class Relationship {
    /** The four supported UML relationship types. */
    public enum Type { ASSOCIATION, DEPENDENCY, INHERITANCE, IMPLEMENTATION }

    private final String from;
    private final String to;
    private final Type type;
    private final String label;

    /**
     * @param from  the source class name
     * @param to    the target class name
     * @param type  the relationship type
     * @param label optional label (used for association names); may be {@code null}
     */
    public Relationship(String from, String to, Type type, String label) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.label = label;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public Type getType() { return type; }
    public String getLabel() { return label; }

    /**
     * Renders this relationship as a single Mermaid class-diagram line.
     *
     * @return the Mermaid syntax string (e.g. {@code "    A --|> B"})
     */
    public String toMermaid() {
        switch (type) {
            case INHERITANCE:
                return String.format("    %s --|> %s", from, to);
            case IMPLEMENTATION:
                return String.format("    %s ..|> %s", from, to);
            case ASSOCIATION:
                if (label != null && !label.isEmpty())
                    return String.format("    %s --> %s : %s", from, to, label);
                return String.format("    %s --> %s", from, to);
            case DEPENDENCY:
                return String.format("    %s ..> %s", from, to);
            default:
                return "";
        }
    }
}
