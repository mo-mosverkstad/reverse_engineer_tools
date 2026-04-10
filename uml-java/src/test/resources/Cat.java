package sample;

public class Cat extends Animal implements Runnable, Comparable<Cat> {
    public static int count;

    public Cat(String name) {
        super(name, 0);
    }

    public void run() {
    }

    public int compareTo(Cat other) {
        return 0;
    }

    public static int getCount() {
        return count;
    }
}
