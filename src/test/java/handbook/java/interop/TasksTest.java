package handbook.java.interop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Тесты темы 10 «Interop Java ↔ Kotlin»: как Kotlin выглядит из Java — свойства/геттеры,
 * companion/@JvmStatic, @JvmField, top-level/@JvmName, @JvmOverloads, data class, SAM,
 * nullability, @Throws, function-типы.
 */
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_property() {
        assertEquals("Alice", Tasks.configProperty("Alice"));
    }

    @Test void Л2_jvmFieldVsProperty() {
        assertEquals(49, Tasks.holderFieldAndProp(), "42 (@JvmField) + 7 (getProp)");
    }

    @Test void Л3_objectSingleton() {
        assertEquals("pong", Tasks.registryPing());
    }

    @Test void Л4_jvmStatic() {
        assertEquals("static", Tasks.factoryStatic());
    }

    @Test void Л5_companion() {
        assertEquals("companion", Tasks.factoryCompanion());
    }

    @Test void Л6_topLevel() {
        assertEquals("Hi, Bob", Tasks.topLevel("Bob"));
    }

    @Test void Л7_const() {
        assertEquals("1.0", Tasks.constant());
    }

    @Test void Л8_extension() {
        assertEquals("wow!", Tasks.extension("wow"));
    }

    // ── Средние ──

    @Test void С9_jvmOverloads_default() {
        assertEquals("Hello, Kate", Tasks.greetDefault("Kate"));
    }

    @Test void С10_jvmOverloads_full() {
        assertEquals("Hi, Kate", Tasks.greetCustom("Kate", "Hi"));
    }

    @Test void С11_dataClassGetters() {
        assertEquals(34, Tasks.pointCoords());
    }

    @Test void С12_dataClassEquals() {
        assertTrue(Tasks.pointEquals(), "data class сгенерировал equals по содержимому");
    }

    @Test void С13_samConversion() {
        assertEquals(14, Tasks.samConversion(7));
    }

    @Test void С14_nonNullBoundary() {
        assertEquals(3, Tasks.safeLength("abc"));
        assertEquals(-1, Tasks.safeLength(null), "null на non-null параметре → NPE на границе");
    }

    @Test void С15_nullableReturn() {
        assertEquals("found", Tasks.nullableFind("known"));
        assertEquals("none", Tasks.nullableFind("unknown"));
    }

    // ── Сложные ──

    @Test void СЛ16_throwsAnnotation() {
        assertEquals("ok", Tasks.checkedThrows(false));
        assertEquals("caught", Tasks.checkedThrows(true), "@Throws позволил Java поймать IOException");
    }

    @Test void СЛ17_jvmName() {
        assertEquals("renamed", Tasks.renamed());
    }

    @Test void СЛ18_nullableParam() {
        assertEquals(4, Tasks.nullableParam("abcd"));
        assertEquals(-1, Tasks.nullableParam(null), "nullable параметр принимает null");
    }

    @Test void СЛ19_functionType() {
        assertEquals(15, Tasks.functionType(5), "Function1.invoke утроил");
    }

    @Test void СЛ20_unitCallback() {
        assertTrue(Tasks.unitCallback(), "Function0<Unit> вызвался и вернул Unit.INSTANCE");
    }
}
