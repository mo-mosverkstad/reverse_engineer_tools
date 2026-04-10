package uml4java;

public class Relationship {
    public enum Type { ASSOCIATION, DEPENDENCY, INHERITANCE, IMPLEMENTATION }

    public String from;
    public String to;
    public Type type;
    public String label;

    public Relationship(String from, String to, Type type, String label) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.label = label;
    }
}
