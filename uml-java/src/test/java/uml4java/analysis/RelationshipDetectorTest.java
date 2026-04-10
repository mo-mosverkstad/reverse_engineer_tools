package uml4java.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uml4java.model.*;
import uml4java.parser.TypeResolver;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipDetectorTest {
    private List<ClassInfo> classes;
    private TypeResolver resolver;
    private RelationshipDetector detector;

    @BeforeEach
    void setUp() {
        classes = new ArrayList<>();
        resolver = new TypeResolver(classes);
        detector = new RelationshipDetector(resolver);
    }

    private ClassInfo makeClass(String name) {
        ClassInfo cls = new ClassInfo();
        cls.setName(name);
        classes.add(cls);
        return cls;
    }

    @Test
    void detectInheritance() {
        ClassInfo animal = makeClass("Animal");
        ClassInfo dog = makeClass("Dog");
        dog.setExtendsName("Animal");

        detector.detect(classes);

        assertEquals(1, detector.getRels().size());
        Relationship r = detector.getRels().get(0);
        assertEquals("Dog", r.getFrom());
        assertEquals("Animal", r.getTo());
        assertEquals(Relationship.Type.INHERITANCE, r.getType());
    }

    @Test
    void detectImplementation() {
        makeClass("Runnable");
        ClassInfo cat = makeClass("Cat");
        cat.addImplements("Runnable");

        detector.detect(classes);

        assertEquals(1, detector.getRels().size());
        assertEquals(Relationship.Type.IMPLEMENTATION, detector.getRels().get(0).getType());
    }

    @Test
    void detectAssociationFromField() {
        ClassInfo animal = makeClass("Animal");
        ClassInfo owner = makeClass("Owner");
        owner.addMember(new FieldMember('-', "Animal", "pet", false));

        detector.detect(classes);

        Relationship r = detector.getRels().stream()
                .filter(rel -> rel.getType() == Relationship.Type.ASSOCIATION).findFirst().orElse(null);
        assertNotNull(r);
        assertEquals("Owner", r.getFrom());
        assertEquals("Animal", r.getTo());
        assertEquals("pet", r.getLabel());
    }

    @Test
    void detectDependencyFromMethodParam() {
        ClassInfo animal = makeClass("Animal");
        ClassInfo owner = makeClass("Owner");
        owner.addMember(new MethodMember('+', "void", "adopt", "Animal a", false));

        detector.detect(classes);

        Relationship r = detector.getRels().stream()
                .filter(rel -> rel.getType() == Relationship.Type.DEPENDENCY).findFirst().orElse(null);
        assertNotNull(r);
        assertEquals("Owner", r.getFrom());
        assertEquals("Animal", r.getTo());
    }

    @Test
    void detectDependencyFromReturnType() {
        ClassInfo animal = makeClass("Animal");
        ClassInfo factory = makeClass("Factory");
        factory.addMember(new MethodMember('+', "Animal", "create", "", false));

        detector.detect(classes);

        Relationship r = detector.getRels().stream()
                .filter(rel -> rel.getType() == Relationship.Type.DEPENDENCY).findFirst().orElse(null);
        assertNotNull(r);
        assertEquals("Factory", r.getFrom());
        assertEquals("Animal", r.getTo());
    }

    @Test
    void detectDependencyFromBodyRef() {
        ClassInfo dog = makeClass("Dog");
        ClassInfo owner = makeClass("Owner");
        owner.addBodyRef("Dog");

        detector.detect(classes);

        Relationship r = detector.getRels().stream()
                .filter(rel -> rel.getType() == Relationship.Type.DEPENDENCY).findFirst().orElse(null);
        assertNotNull(r);
        assertEquals("Owner", r.getFrom());
        assertEquals("Dog", r.getTo());
    }

    @Test
    void noDuplicateRelationships() {
        ClassInfo animal = makeClass("Animal");
        ClassInfo owner = makeClass("Owner");
        // Two fields of the same type should not create duplicate associations
        owner.addMember(new FieldMember('-', "Animal", "pet1", false));
        owner.addMember(new FieldMember('-', "Animal", "pet2", false));

        detector.detect(classes);

        long assocCount = detector.getRels().stream()
                .filter(r -> r.getType() == Relationship.Type.ASSOCIATION).count();
        // First one creates the rel, second is deduplicated (same from/to/type)
        assertEquals(1, assocCount);
    }

    @Test
    void selfReferenceSkipped() {
        ClassInfo node = makeClass("Node");
        node.addMember(new FieldMember('-', "Node", "next", false));

        detector.detect(classes);

        assertTrue(detector.getRels().isEmpty());
    }

    @Test
    void unknownExtendsIgnored() {
        ClassInfo child = makeClass("Child");
        child.setExtendsName("UnknownParent");

        detector.detect(classes);

        assertTrue(detector.getRels().isEmpty());
    }

    @Test
    void primitiveFieldIgnored() {
        makeClass("Owner");
        ClassInfo cls = classes.get(0);
        cls.addMember(new FieldMember('-', "String", "name", false));

        detector.detect(classes);

        assertTrue(detector.getRels().isEmpty());
    }
}
