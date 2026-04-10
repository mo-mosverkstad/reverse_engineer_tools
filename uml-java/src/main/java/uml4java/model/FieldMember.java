package uml4java.model;

public class FieldMember extends Member {

    public FieldMember(char visibility, String type, String name, boolean isStatic) {
        super(visibility, type, name, isStatic);
    }

    @Override
    public boolean isMethod() { return false; }

    @Override
    public String toMermaid() {
        return String.format("%c%s %s", getVisibility(), getType(), getName());
    }
}
