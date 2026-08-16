package handbook.java.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Тесты темы 9 «Память, GC и ссылки». Детерминированные части — семантика ссылок и рефлексия
 * {@code this$0}; сборка проверяется через надёжный GC-хелпер. @Timeout страхует GC-циклы.
 */
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_makeWeak() {
        String s = "hello";
        assertSame(s, Tasks.makeWeak(s).get());
    }

    @Test void Л2_deref() {
        assertEquals("x", Tasks.deref(new WeakReference<>("x")));
    }

    @Test void Л3_clearedWeakIsNull() {
        assertNull(Tasks.clearedWeakIsNull());
    }

    @Test void Л4_makeSoft() {
        Object o = new Object();
        assertSame(o, Tasks.makeSoft(o).get());
    }

    @Test void Л5_capturesOuter() {
        assertTrue(Tasks.capturesOuter(Tasks.Outer.Inner.class), "нестатический inner держит this$0");
        assertFalse(Tasks.capturesOuter(Tasks.Outer.StaticNested.class), "static nested — нет");
    }

    @Test void Л6_outerReferenceOf() throws Exception {
        Tasks.Outer outer = new Tasks.Outer();
        Tasks.Outer.Inner inner = outer.new Inner();
        assertSame(outer, Tasks.outerReferenceOf(inner), "inner ссылается именно на своего хозяина");
    }

    @Test void Л7_threadLocalRemove() {
        assertNull(Tasks.threadLocalRemove());
    }

    @Test void Л8_reachableSurvivesGc() {
        assertTrue(Tasks.reachableSurvivesGc(), "достижимый объект не собирается");
    }

    // ── Средние ──

    @Test void С9_gcCollectsUnreachable() {
        assertTrue(Tasks.gcCollectsUnreachable(), "недостижимый объект собирается");
    }

    @Test void С10_staticFieldKeepsAlive() {
        assertTrue(Tasks.staticFieldKeepsAlive(), "статик держит объект → утечка (жив после GC)");
    }

    @Test void С11_clearStaticFrees() {
        assertTrue(Tasks.clearStaticFrees(), "обнулили статик → объект собран");
    }

    @Test void С12_weakHashMapEvicts() {
        assertEquals(0, Tasks.weakHashMapEvicts(), "WeakHashMap теряет запись после сборки ключа");
    }

    @Test void С13_referenceQueueEnqueued() {
        assertTrue(Tasks.referenceQueueEnqueued(), "после сборки слабая ссылка попадает в очередь");
    }

    @Test void С14_weakValueCache() {
        Tasks.WeakValueCache<String, String> cache = new Tasks.WeakValueCache<>();
        String value = new String("v");
        cache.put("k", value);
        assertSame(value, cache.get("k"), "пока значение живо — возвращается");
        assertNull(cache.get("missing"));
    }

    @Test void С15_autoCloseableCleanup() {
        assertTrue(Tasks.autoCloseableCleanup(), "close() выполняется детерминированно");
    }

    // ── Сложные ──

    @Test void СЛ16_softCache() {
        Tasks.SoftCache<String, String> cache = new Tasks.SoftCache<>();
        String value = new String("v");
        cache.put("k", value);
        assertSame(value, cache.get("k"), "пока памяти хватает — значение живо");
        assertNull(cache.get("missing"));
    }

    @Test void СЛ17_scheduleWeakly_allowsCollection() {
        Tasks.PseudoActivity activity = new Tasks.PseudoActivity("Main");
        WeakReference<Tasks.PseudoActivity> ref = Tasks.scheduleWeakly(activity);
        assertSame(activity, ref.get());
        activity = null;
        assertTrue(Tasks.awaitCleared(ref), "слабая ссылка не мешает собрать закрытую activity");
    }

    @Test void СЛ18_transitiveCollection() {
        assertTrue(Tasks.transitiveCollection(), "обнулили A → собраны и A, и B");
    }

    @Test void СЛ19_phantomCleanup() {
        assertTrue(Tasks.phantomCleanup(), "фантом попадает в очередь после сборки");
    }

    @Test void СЛ20_isCollectable() {
        assertTrue(Tasks.isCollectable(Object::new), "обычный объект собираем");

        Object[] keep = new Object[1];
        assertFalse(Tasks.isCollectable(() -> {
            keep[0] = new Object();   // фабрика прячет сильную ссылку — утечка
            return keep[0];
        }), "удержанный объект не собирается (утечка обнаружена)");
    }
}
