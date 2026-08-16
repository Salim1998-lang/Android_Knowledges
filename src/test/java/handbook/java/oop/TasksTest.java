package handbook.java.oop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Тесты темы 2 «ООП и структура классов»: инкапсуляция, интерфейсы и default-методы, enum'ы,
 * наследование vs композиция, порядок инициализации, вложенные/внутренние/анонимные классы.
 */
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_encapsulation_deposit() {
        Tasks.BankAccount acc = new Tasks.BankAccount(100);
        acc.deposit(50);
        assertEquals(150, acc.balance());
    }

    @Test void Л2_programToInterface() {
        assertEquals("ANN", Tasks.upperName(() -> "Ann"));
    }

    @Test void Л3_defaultMethod() {
        Tasks.Greeter g = () -> "World";
        assertEquals("Hello, World!", g.greet());
    }

    @Test void Л4_anonymousClass() {
        assertEquals(9.0, Tasks.square(3).area(), 1e-9);
    }

    @Test void Л5_enumSwitch() {
        assertEquals(Tasks.Direction.SOUTH, Tasks.opposite(Tasks.Direction.NORTH));
        assertEquals(Tasks.Direction.WEST, Tasks.opposite(Tasks.Direction.EAST));
    }

    @Test void Л6_enumValueOf() {
        assertEquals(10, Tasks.coinValue("DIME"));
        assertEquals(25, Tasks.coinValue("QUARTER"));
    }

    @Test void Л7_toString() {
        assertEquals("(3, 4)", new Tasks.Point(3, 4).toString());
    }

    @Test void Л8_instanceof() {
        assertEquals(2, Tasks.countStrings(Arrays.asList("a", 1, "b", 2.0)));
    }

    // ── Средние ──

    @Test void С9_inheritance() {
        assertEquals("Rex says woof", new Tasks.Dog("Rex").describe());
    }

    @Test void С10_initOrder() {
        Tasks.Tracker.LOG.clear();
        new Tasks.Tracker();
        new Tasks.Tracker();
        // static-блок отработал один раз при загрузке класса (до clear), поэтому в LOG его нет;
        // на каждый объект: instance-блок → конструктор.
        assertEquals(List.of("instance-init", "ctor", "instance-init", "ctor"), Tasks.Tracker.LOG);
    }

    @Test void С11_composition() {
        assertEquals(120, new Tasks.Car(new Tasks.Engine(120)).power());
    }

    @Test void С12_templateMethod() {
        assertEquals("[hi]", Tasks.reportOf("hi").render());
    }

    @Test void С13_diamondDefault() {
        assertEquals("walk/swim", new Tasks.Amphibian().move());
    }

    @Test void С14_staticCounter() {
        int before = Tasks.Widget.count();
        Tasks.Widget w1 = new Tasks.Widget();
        Tasks.Widget w2 = new Tasks.Widget();
        assertEquals(w1.id() + 1, w2.id(), "id растёт по общему счётчику");
        assertEquals(before + 2, Tasks.Widget.count(), "счётчик общий для всех объектов");
    }

    @Test void С15_enumAbstractMethod() {
        assertEquals(7, Tasks.Operation.PLUS.apply(3, 4));
        assertEquals(-1, Tasks.Operation.MINUS.apply(3, 4));
        assertEquals(12, Tasks.Operation.TIMES.apply(3, 4));
    }

    // ── Сложные ──

    @Test void СЛ16_innerClass() {
        Tasks.Counter c = new Tasks.Counter(5);
        c.newTick().bump();
        c.newTick().bump();
        assertEquals(7, c.value(), "внутренний класс меняет поле внешнего объекта");
    }

    @Test void СЛ17_builder() {
        Tasks.User u = Tasks.User.builder().name("Ann").age(30).build();
        assertEquals("Ann", u.name());
        assertEquals(30, u.age());
    }

    @Test void СЛ18_closure() {
        Tasks.IntOp add10 = Tasks.makeAdder(10);
        assertEquals(15, add10.apply(5));
        assertEquals(10, add10.apply(0));
    }

    @Test void СЛ19_enumSingleton() {
        Tasks.Registry.INSTANCE.register("answer", 42);
        assertEquals(42, Tasks.Registry.INSTANCE.get("answer"));
        assertNull(Tasks.Registry.INSTANCE.get("missing"));
    }

    @Test void СЛ20_polymorphism() {
        List<Tasks.Shape> shapes = Arrays.asList(Tasks.square(2), Tasks.square(3));
        assertTrue(Math.abs(13.0 - Tasks.totalArea(shapes)) < 1e-9);
    }
}
