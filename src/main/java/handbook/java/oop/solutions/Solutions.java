package handbook.java.oop.solutions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Эталонные решения темы 2. Подсмотри, если застрял с {@link handbook.java.oop.Tasks}. */
public final class Solutions {

    private Solutions() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /**
     * Л1. Инкапсуляция: у {@link BankAccount} баланс приватный. Реализуй {@code deposit}: увеличивает
     * баланс на {@code amount} (гарантируем {@code amount >= 0}). Обрати внимание — снаружи поле не видно.
     */
    public static final class BankAccount {
        private int balance;
        public BankAccount(int initial) { this.balance = initial; }
        public int balance() { return balance; }
        public void deposit(int amount) {
            balance += amount;
        }
    }

    /** Программируй «на интерфейс», а не на реализацию. */
    public interface Named {
        String name();
    }

    /** Л2. Верни имя в ВЕРХНЕМ регистре, приняв абстракцию {@link Named} (не конкретный класс). */
    public static String upperName(Named n) {
        return n.name().toUpperCase();
    }

    /** У функционального интерфейса (один абстрактный метод) может быть {@code default}-метод. */
    public interface Greeter {
        String subject();
        /** Л3. Реализуй default-метод: верни {@code "Hello, " + subject() + "!"}. */
        default String greet() {
            return "Hello, " + subject() + "!";
        }
    }

    public interface Shape {
        double area();
    }

    /** Л4. Верни {@link Shape} площади {@code side * side} через АНОНИМНЫЙ класс (без отдельного класса). */
    public static Shape square(double side) {
        return new Shape() {
            @Override public double area() { return side * side; }
        };
    }

    public enum Direction { NORTH, EAST, SOUTH, WEST }

    /** Л5. Верни противоположное направление (NORTH↔SOUTH, EAST↔WEST). Подсказка: {@code switch}. */
    public static Direction opposite(Direction d) {
        switch (d) {
            case NORTH: return Direction.SOUTH;
            case SOUTH: return Direction.NORTH;
            case EAST:  return Direction.WEST;
            default:    return Direction.EAST;
        }
    }

    /** Enum с полем и методом (даётся как пример — используй в Л6). */
    public enum Coin {
        PENNY(1), NICKEL(5), DIME(10), QUARTER(25);
        private final int cents;
        Coin(int cents) { this.cents = cents; }
        public int cents() { return cents; }
    }

    /** Л6. По ИМЕНИ монеты верни её номинал в центах. Подсказка: {@code Coin.valueOf(name).cents()}. */
    public static int coinValue(String name) {
        return Coin.valueOf(name).cents();
    }

    /** У точки есть координаты; за читаемый вывод отвечает {@code toString}. */
    public static final class Point {
        final int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
        /** Л7. Переопредели {@code toString}: верни {@code "(x, y)"}, напр. {@code "(3, 4)"}. */
        @Override public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /** Л8. Посчитай, сколько элементов являются {@link String}. Подсказка: {@code o instanceof String}. */
    public static int countStrings(List<Object> items) {
        int count = 0;
        for (Object o : items) {
            if (o instanceof String) count++;
        }
        return count;
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /** Абстрактный базовый класс: общий шаблон + абстрактный «шаг». */
    public abstract static class Animal {
        final String name;
        protected Animal(String name) { this.name = name; }
        protected abstract String sound();
        public String describe() { return name + " says " + sound(); }
    }

    /** С9. Доопредели {@link Dog}: {@code sound()} возвращает {@code "woof"} (конструктор уже зовёт super). */
    public static final class Dog extends Animal {
        public Dog(String name) { super(name); }
        @Override protected String sound() {
            return "woof";
        }
    }

    /**
     * Порядок инициализации: static-блок (один раз при загрузке класса) → instance-блок → конструктор,
     * и так для каждого объекта. LOG фиксирует порядок.
     */
    public static final class Tracker {
        public static final List<String> LOG = new ArrayList<>();
        // С10. Заполни три места так, чтобы LOG получал строки в правильном порядке:
        static { LOG.add("static-init"); }   // static-блок
        { LOG.add("instance-init"); }         // instance-блок
        public Tracker() {
            LOG.add("ctor");                  // конструктор
        }
    }

    /** Композиция «HAS-A»: у машины ЕСТЬ двигатель (а не «машина — это двигатель»). */
    public static final class Engine {
        private final int power;
        public Engine(int power) { this.power = power; }
        public int power() { return power; }
    }

    public static final class Car {
        private final Engine engine;
        public Car(Engine engine) { this.engine = engine; }
        /** С11. Реализуй {@code power()}: делегируй встроенному двигателю. */
        public int power() {
            return engine.power();
        }
    }

    /**
     * Шаблонный метод: {@code render()} — {@code final} каркас, {@code body()} — переменная часть.
     * С12. Верни {@link Report}, чей {@code body()} возвращает {@code text} (анонимный подкласс).
     */
    public abstract static class Report {
        protected abstract String body();
        public final String render() { return "[" + body() + "]"; }
    }

    public static Report reportOf(String text) {
        return new Report() {
            @Override protected String body() { return text; }
        };
    }

    /** Ромбовидное наследование default-методов: оба интерфейса дают {@code move()}. */
    public interface Walker { default String move() { return "walk"; } }
    public interface Swimmer { default String move() { return "swim"; } }

    /**
     * С13. {@link Amphibian} реализует оба интерфейса — компилятор ТРЕБУЕТ разрешить конфликт.
     * Переопредели {@code move()} и верни {@code "walk/swim"}, вызвав оба через {@code Walker.super.move()}
     * и {@code Swimmer.super.move()}.
     */
    public static final class Amphibian implements Walker, Swimmer {
        @Override public String move() {
            return Walker.super.move() + "/" + Swimmer.super.move();
        }
    }

    /** Статическое поле — общее для всех объектов; instance-поле — своё у каждого. */
    public static final class Widget {
        private static int count = 0;
        private final int id;
        /** С14. В конструкторе увеличь общий счётчик и присвой {@code id} новый номер: {@code id = ++count}. */
        public Widget() {
            this.id = ++count;
        }
        public int id() { return id; }
        public static int count() { return count; }
    }

    /** Enum с поведением, специфичным для константы (каждая переопределяет абстрактный метод). */
    public enum Operation {
        PLUS  { @Override public int apply(int a, int b) { return a + b; } },
        MINUS { @Override public int apply(int a, int b) { return a - b; } },
        TIMES { @Override public int apply(int a, int b) { return a * b; } };
        /** С15. Реализуй {@code apply} для каждой константы (PLUS/MINUS/TIMES). */
        public abstract int apply(int a, int b);
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /** Внутренний (нестатический) класс держит НЕЯВНУЮ ссылку на внешний объект и видит его поля. */
    public static final class Counter {
        private int n;
        public Counter(int start) { this.n = start; }
        public int value() { return n; }
        public Tick newTick() { return new Tick(); }
        public final class Tick {
            /** СЛ16. Увеличь поле {@code n} ВНЕШНЕГО {@link Counter} на 1 (внутренний класс имеет к нему доступ). */
            public void bump() {
                n++;
            }
        }
    }

    /** Неизменяемый объект + вложенный статический Builder (типичный паттерн). */
    public static final class User {
        private final String name;
        private final int age;
        private User(Builder b) { this.name = b.name; this.age = b.age; }
        public String name() { return name; }
        public int age() { return age; }
        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String name;
            private int age;
            public Builder name(String name) { this.name = name; return this; }
            /** СЛ17. Реализуй fluent-сеттер {@code age} (верни {@code this}) и {@code build()} (верни {@code new User(this)}). */
            public Builder age(int age) {
                this.age = age;
                return this;
            }
            public User build() {
                return new User(this);
            }
        }
    }

    public interface IntOp { int apply(int x); }

    /**
     * СЛ18. Верни функцию, прибавляющую {@code base} к аргументу (замыкание захватывает {@code base}).
     * Подсказка: лямбда {@code x -> x + base}.
     */
    public static IntOp makeAdder(int base) {
        return x -> x + base;
    }

    /** Enum-синглтон: единственный экземпляр {@code INSTANCE}, потокобезопасно из коробки. */
    public enum Registry {
        INSTANCE;
        private final Map<String, Integer> data = new HashMap<>();
        /** СЛ19. Реализуй {@code register} (положи k→v) и {@code get} (верни значение или {@code null}). */
        public void register(String key, int value) {
            data.put(key, value);
        }
        public Integer get(String key) {
            return data.get(key);
        }
    }

    /** СЛ20. Полиморфизм: просуммируй площади всех фигур (реализации могут быть любыми). */
    public static double totalArea(List<Shape> shapes) {
        double sum = 0;
        for (Shape s : shapes) sum += s.area();
        return sum;
    }
}
