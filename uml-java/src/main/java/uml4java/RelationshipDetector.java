package uml4java;

import java.util.ArrayList;
import java.util.List;

public class RelationshipDetector {
    private final List<Relationship> rels = new ArrayList<>();
    private final TypeResolver resolver;

    public RelationshipDetector(TypeResolver resolver) {
        this.resolver = resolver;
    }

    public List<Relationship> getRels() { return rels; }

    public void detect(List<ClassInfo> classes) {
        for (ClassInfo c : classes) {
            detectInheritance(c);
            detectImplementation(c);
            detectMemberRelationships(c);
            detectBodyRefs(c);
        }
    }

    private void detectInheritance(ClassInfo c) {
        if (!c.getExtendsName().isEmpty() && resolver.isKnownClass(c.getExtendsName()))
            addRel(c.getName(), c.getExtendsName(), Relationship.Type.INHERITANCE, null);
    }

    private void detectImplementation(ClassInfo c) {
        for (String iface : c.getImplementsNames())
            if (resolver.isKnownClass(iface))
                addRel(c.getName(), iface, Relationship.Type.IMPLEMENTATION, null);
    }

    private void detectMemberRelationships(ClassInfo c) {
        for (Member m : c.getMembers()) {
            if (!m.isMethod()) {
                String t = resolver.strip(m.getType());
                if (!resolver.isPrimitive(t) && resolver.isKnownClass(t))
                    addRel(c.getName(), t, Relationship.Type.ASSOCIATION, m.getName());
            } else {
                detectMethodDependencies(c.getName(), (MethodMember) m);
            }
        }
    }

    private void detectMethodDependencies(String className, MethodMember m) {
        if (!m.getParams().isEmpty()) {
            for (String param : m.getParams().split(",")) {
                param = param.trim();
                int sp = param.indexOf(' ');
                if (sp > 0) {
                    String pt = resolver.strip(param.substring(0, sp));
                    if (!resolver.isPrimitive(pt) && resolver.isKnownClass(pt))
                        addRel(className, pt, Relationship.Type.DEPENDENCY, null);
                }
            }
        }
        if (!m.getType().isEmpty()) {
            String rt = resolver.strip(m.getType());
            if (!resolver.isPrimitive(rt) && resolver.isKnownClass(rt))
                addRel(className, rt, Relationship.Type.DEPENDENCY, null);
        }
    }

    private void detectBodyRefs(ClassInfo c) {
        for (String ref : c.getBodyRefs()) {
            String t = resolver.strip(ref);
            if (!resolver.isPrimitive(t) && resolver.isKnownClass(t))
                addRel(c.getName(), t, Relationship.Type.DEPENDENCY, null);
        }
    }

    private void addRel(String from, String to, Relationship.Type type, String label) {
        if (from.equals(to)) return;
        for (Relationship r : rels)
            if (r.getFrom().equals(from) && r.getTo().equals(to) && r.getType() == type) return;
        rels.add(new Relationship(from, to, type, label));
    }
}
