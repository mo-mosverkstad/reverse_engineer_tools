package uml4java.model;

/**
 * Represents a field member of a Java class.
 * Renders as {@code <visibility><type> <name>} in Mermaid syntax.
 */
public class FieldMember extends Member {

    /**
     * @param visibility UML visibility character
     * @param type       the field's declared type
     * @param name       the field name
     * @param isStatic   whether the field is declared {@code static}
     */
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
