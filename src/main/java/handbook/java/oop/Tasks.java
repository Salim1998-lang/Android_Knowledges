package handbook.java.oop;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Тема 2 «ООП и структура классов» — 20 задач.
 * Реализуй помеченные {@code TODO} тела (методы кидают {@code UnsupportedOperationException},
 * блоки инициализации пусты). Декларации типов даны целиком — дописываешь поведение.
 * Проверка: {@code ./gradlew test --tests "handbook.java.oop.*"}.
 * Эталон — в {@link handbook.java.oop.solutions.Solutions}.
 *
 * Нумерация: Л1–Л8 (лёгкие), С9–С15 (средние), СЛ16–СЛ20 (сложные).
 */
public final class Tasks {

    private Tasks() { }

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
            throw new UnsupportedOperationException("TODO Л1 deposit");
        }
    }

    /** Программируй «на интерфейс», а не на реализацию. */
    public interface Named {
        String name();
    }

    /** Л2. Верни имя в ВЕРХНЕМ регистре, приняв абстракцию {@link Named} (не конкретный класс). */
    public static String upperName(Named n) {
        throw new UnsupportedOperationException("TODO Л2 upperName");
    }

    /** У функционального интерфейса (один абстрактный метод) может быть {@code default}-метод. */
    public interface Greeter {
        String subject();
        /** Л3. Реализуй default-метод: верни {@code "Hello, " + subject() + "!"}. */
        default String greet() {
            throw new UnsupportedOperationException("TODO Л3 greet");
        }
    }

    public interface Shape {
        double area();
    }

    /** Л4. Верни {@link Shape} площади {@code side * side} через АНОНИМНЫЙ класс (без отдельного класса). */
    public static Shape square(double side) {
        throw new UnsupportedOperationException("TODO Л4 square");
    }

    public enum Direction { NORTH, EAST, SOUTH, WEST }

    /** Л5. Верни противоположное направление (NORTH↔SOUTH, EAST↔WEST). Подсказка: {@code switch}. */
    public static Direction opposite(Direction d) {
        throw new UnsupportedOperationException("TODO Л5 opposite");
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
        throw new UnsupportedOperationException("TODO Л6 coinValue");
    }

    /** У точки есть координаты; за читаемый вывод отвечает {@code toString}. */
    public static final class Point {
        final int x, y;
        public Point(int x, int y) { this.x = x; this.y = y; }
        /** Л7. Переопредели {@code toString}: верни {@code "(x, y)"}, напр. {@code "(3, 4)"}. */
        @Override public String toString() {
            throw new UnsupportedOperationException("TODO Л7 toString");
        }
    }

    /** Л8. Посчитай, сколько элементов являются {@link String}. Подсказка: {@code o instanceof String}. */
    public static int countStrings(List<Object> items) {
        throw new UnsupportedOperationException("TODO Л8 countStrings");
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
            throw new UnsupportedOperationException("TODO С9 sound");
        }
    }

    /**
     * Порядок инициализации: static-блок (один раз при загрузке класса) → instance-блок → конструктор,
     * и так для каждого объекта. LOG фиксирует порядок.
     */
    public static final class Tracker {
        public static final List<String> LOG = new ArrayList<>();
        // С10. Заполни три места так, чтобы LOG получал строки в правильном порядке:
        // static-блок → добавь "static-init"; instance-блок → "instance-init"; конструктор → "ctor".
        static { /* TODO С10: LOG.add("static-init"); */ }
        { /* TODO С10: LOG.add("instance-init"); */ }
        public Tracker() {
            /* TODO С10: LOG.add("ctor"); */
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
            throw new UnsupportedOperationException("TODO С11 power");
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
        throw new UnsupportedOperationException("TODO С12 reportOf");
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
            throw new UnsupportedOperationException("TODO С13 move");
        }
    }

    /** Статическое поле — общее для всех объектов; instance-поле — своё у каждого. */
    public static final class Widget {
        private static int count = 0;
        private final int id;
        /** С14. В конструкторе увеличь общий счётчик и присвой {@code id} новый номер: {@code id = ++count}. */
        public Widget() {
            throw new UnsupportedOperationException("TODO С14 Widget");
        }
        public int id() { return id; }
        public static int count() { return count; }
    }

    /** Enum с поведением, специфичным для константы (каждая переопределяет абстрактный метод). */
    public enum Operation {
        PLUS  { @Override public int apply(int a, int b) { throw new UnsupportedOperationException("TODO С15 PLUS"); } },
        MINUS { @Override public int apply(int a, int b) { throw new UnsupportedOperationException("TODO С15 MINUS"); } },
        TIMES { @Override public int apply(int a, int b) { throw new UnsupportedOperationException("TODO С15 TIMES"); } };
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
                throw new UnsupportedOperationException("TODO СЛ16 bump");
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
                throw new UnsupportedOperationException("TODO СЛ17 age");
            }
            public User build() {
                throw new UnsupportedOperationException("TODO СЛ17 build");
            }
        }
    }

    public interface IntOp { int apply(int x); }

    /**
     * СЛ18. Верни функцию, прибавляющую {@code base} к аргументу (замыкание захватывает {@code base}).
     * Подсказка: лямбда {@code x -> x + base}.
     */
    public static IntOp makeAdder(int base) {
        throw new UnsupportedOperationException("TODO СЛ18 makeAdder");
    }

    /** Enum-синглтон: единственный экземпляр {@code INSTANCE}, потокобезопасно из коробки. */
    public enum Registry {
        INSTANCE;
        private final Map<String, Integer> data = new HashMap<>();
        /** СЛ19. Реализуй {@code register} (положи k→v) и {@code get} (верни значение или {@code null}). */
        public void register(String key, int value) {
            throw new UnsupportedOperationException("TODO СЛ19 register");
        }
        public Integer get(String key) {
            throw new UnsupportedOperationException("TODO СЛ19 get");
        }
    }

    /** СЛ20. Полиморфизм: просуммируй площади всех фигур (реализации могут быть любыми). */
    public static double totalArea(List<Shape> shapes) {
        throw new UnsupportedOperationException("TODO СЛ20 totalArea");
    }
}
