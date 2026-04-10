package uml4java.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipTest {

    @Test
    void inheritance() {
        Relationship r = new Relationship("Dog", "Animal", Relationship.Type.INHERITANCE, null);
        assertEquals("    Dog --|> Animal", r.toMermaid());
    }

    @Test
    void implementation() {
        Relationship r = new Relationship("Cat", "Runnable", Relationship.Type.IMPLEMENTATION, null);
        assertEquals("    Cat ..|> Runnable", r.toMermaid());
    }

    @Test
    void associationWithLabel() {
        Relationship r = new Relationship("Owner", "Animal", Relationship.Type.ASSOCIATION, "pet");
        assertEquals("    Owner --> Animal : pet", r.toMermaid());
    }

    @Test
    void associationWithoutLabel() {
        Relationship r = new Relationship("A", "B", Relationship.Type.ASSOCIATION, null);
        assertEquals("    A --> B", r.toMermaid());
    }

    @Test
    void associationEmptyLabel() {
        Relationship r = new Relationship("A", "B", Relationship.Type.ASSOCIATION, "");
        assertEquals("    A --> B", r.toMermaid());
    }

    @Test
    void dependency() {
        Relationship r = new Relationship("Service", "Dao", Relationship.Type.DEPENDENCY, null);
        assertEquals("    Service ..> Dao", r.toMermaid());
    }

    @Test
    void getters() {
        Relationship r = new Relationship("A", "B", Relationship.Type.DEPENDENCY, "lbl");
        assertEquals("A", r.getFrom());
        assertEquals("B", r.getTo());
        assertEquals(Relationship.Type.DEPENDENCY, r.getType());
        assertEquals("lbl", r.getLabel());
    }
}
