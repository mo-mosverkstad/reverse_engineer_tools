package uml4java.model;

public abstract class Member {
    private final char visibility;
    private final String type;
    private final String name;
    private final boolean isStatic;

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
    public abstract boolean isMethod();

    public abstract String toMermaid();
}
