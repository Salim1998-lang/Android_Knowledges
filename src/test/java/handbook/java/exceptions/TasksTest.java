package handbook.java.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Тесты темы 5 «Исключения и ресурсы»: try/catch/finally, checked vs unchecked, try-with-resources,
 * suppressed-исключения, cause/chaining, кастомные исключения, специфичность catch.
 */
class TasksTest {

    // ── Лёгкие ──

    @Test void Л1_safeDivide() {
        assertEquals(3, Tasks.safeDivide(6, 2, -1));
        assertEquals(-1, Tasks.safeDivide(6, 0, -1), "деление на ноль → fallback");
    }

    @Test void Л2_parseOrDefault() {
        assertEquals(42, Tasks.parseOrDefault("42", 0));
        assertEquals(0, Tasks.parseOrDefault("nope", 0));
    }

    @Test void Л3_finallyAlwaysRuns() {
        List<String> log = new ArrayList<>();
        assertEquals("ok", Tasks.runFinally(log));
        assertEquals(List.of("try", "finally"), log);
    }

    @Test void Л4_catchAsSupertype() {
        assertTrue(Tasks.catchesAsSupertype());
    }

    @Test void Л5_multiCatch() {
        assertEquals("ok", Tasks.handleMultiCatch(0));
        assertEquals("handled: a", Tasks.handleMultiCatch(1));
        assertEquals("handled: b", Tasks.handleMultiCatch(2));
    }

    @Test void Л6_customChecked() throws Exception {
        assertEquals(70, Tasks.withdraw(100, 30));
        assertThrows(Tasks.InsufficientFundsException.class, () -> Tasks.withdraw(100, 200));
    }

    @Test void Л7_wrapWithCause() {
        assertEquals(5, Tasks.parseWrapped("5"));
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> Tasks.parseWrapped("x"));
        assertInstanceOf(NumberFormatException.class, ex.getCause(), "причина сохранена");
    }

    @Test void Л8_rootCause() {
        Throwable root = new NumberFormatException("root");
        Throwable top = new RuntimeException("top", new IllegalStateException("mid", root));
        assertSame(root, Tasks.rootCause(top));
    }

    // ── Средние ──

    @Test void С9_tryWithResources() {
        List<String> log = new ArrayList<>();
        Tasks.useResource(log);
        assertEquals(List.of("use-A", "close-A"), log);
    }

    @Test void С10_reverseCloseOrder() {
        List<String> log = new ArrayList<>();
        Tasks.useTwoResources(log);
        assertEquals(List.of("use-A", "use-B", "close-B", "close-A"), log);
    }

    @Test void С11_suppressed() {
        Throwable primary = Tasks.suppressedException();
        assertEquals("body", primary.getMessage(), "главное — исключение тела");
        assertEquals(1, primary.getSuppressed().length);
        assertTrue(primary.getSuppressed()[0].getMessage().contains("close failed"));
    }

    @Test void С12_returnInFinallyPitfall() {
        assertEquals(2, Tasks.returnInFinally(), "return в finally перекрывает return из try");
    }

    @Test void С13_checkedToUnchecked() {
        assertEquals("done", Tasks.toUnchecked(() -> "done"));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> Tasks.toUnchecked(() -> { throw new Exception("checked"); }));
        assertEquals("checked", ex.getCause().getMessage());
    }

    @Test void С14_exceptionCarriesData() {
        assertEquals(5, Tasks.validateAge(5));
        Tasks.ValidationException ex =
                assertThrows(Tasks.ValidationException.class, () -> Tasks.validateAge(-1));
        assertEquals("age", ex.field());
    }

    @Test void С15_parseAllOrThrow() {
        assertEquals(List.of(1, 2, 3), Tasks.parseAllOrThrow(List.of("1", "2", "3")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> Tasks.parseAllOrThrow(List.of("1", "x", "3")));
        assertTrue(ex.getMessage().contains("index 1"));
        assertInstanceOf(NumberFormatException.class, ex.getCause());
    }

    // ── Сложные ──

    @Test void СЛ16_causeChain() {
        Throwable t = new RuntimeException("a",
                new IllegalStateException("b", new NumberFormatException("c")));
        assertEquals(List.of("a", "b", "c"), Tasks.causeChainMessages(t));
    }

    @Test void СЛ17_multipleSuppressed() {
        assertEquals(2, Tasks.multipleSuppressed());
    }

    @Test void СЛ18_catchSpecificity() {
        assertEquals(404, Tasks.statusFor("missing"));
        assertEquals(409, Tasks.statusFor("dup"));
        assertEquals(500, Tasks.statusFor("other"));
    }

    @Test void СЛ19_retry() {
        int[] calls = {0};
        String result = Tasks.retry(() -> {
            calls[0]++;
            if (calls[0] < 3) throw new IllegalStateException("fail " + calls[0]);
            return "success";
        }, 5);
        assertEquals("success", result);
        assertEquals(3, calls[0], "успех с третьей попытки");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> Tasks.retry(() -> { throw new IllegalStateException("always"); }, 2));
        assertTrue(ex.getMessage().contains("all 2 attempts failed"));
        assertEquals("always", ex.getCause().getMessage());
    }

    @Test void СЛ20_closeQuietly() {
        List<String> log = new ArrayList<>();
        Tasks.closeQuietly(new Tasks.Resource("A", log, true));   // close бросает — но проглочено
        assertEquals(List.of("close-A"), log);
    }
}
