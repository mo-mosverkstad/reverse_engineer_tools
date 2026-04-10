package sample;

/**
 * A class that represents a pet owner.
 * This Javadoc contains the word class multiple times.
 * It should not confuse the parser class detection.
 */
public class Owner {
    /* This is a block comment with class keyword */
    private String name;
    private Animal pet;

    // This is a line comment with class keyword
    public Owner(String name, Animal pet) {
        this.name = name;
        this.pet = pet;
    }

    public void adopt(Animal a) {
        this.pet = a;
        Dog d = new Dog("Rex", 3, "Labrador");
    }
}
