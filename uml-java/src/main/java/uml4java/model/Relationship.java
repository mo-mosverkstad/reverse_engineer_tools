package uml4java.model;

public class Relationship {
    public enum Type { ASSOCIATION, DEPENDENCY, INHERITANCE, IMPLEMENTATION }

    private final String from;
    private final String to;
    private final Type type;
    private final String label;

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
