package handbook.java.exceptions;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Тема 5 «Исключения и ресурсы» — 20 задач.
 * Реализуй помеченные {@code TODO} тела. Инфраструктурные типы ({@link Resource},
 * {@link InsufficientFundsException}, {@link ValidationException}, {@link AppException} и подклассы)
 * даны целиком.
 * Проверка: {@code ./gradlew test --tests "handbook.java.exceptions.*"}.
 * Эталон — в {@link handbook.java.exceptions.solutions.Solutions}.
 *
 * Нумерация: Л1–Л8 (лёгкие), С9–С15 (средние), СЛ16–СЛ20 (сложные).
 */
public final class Tasks {

    private Tasks() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Подели {@code a/b}, а при делении на ноль ({@link ArithmeticException}) верни {@code fallback}. */
    public static int safeDivide(int a, int b, int fallback) {
        throw new UnsupportedOperationException("TODO Л1 safeDivide");
    }

    /** Л2. Распарси число, при {@link NumberFormatException} верни {@code def}. */
    public static int parseOrDefault(String s, int def) {
        throw new UnsupportedOperationException("TODO Л2 parseOrDefault");
    }

    /**
     * Л3. Покажи, что {@code finally} выполняется ВСЕГДА — даже при {@code return} из {@code try}.
     * Добавь в {@code log} строку {@code "try"}, верни {@code "ok"}, а в {@code finally} добавь {@code "finally"}.
     */
    public static String runFinally(List<String> log) {
        throw new UnsupportedOperationException("TODO Л3 runFinally");
    }

    /**
     * Л4. Подтип ловится обработчиком супертипа: брось {@link IllegalArgumentException}, поймай его как
     * {@link RuntimeException} и верни {@code true}.
     */
    public static boolean catchesAsSupertype() {
        throw new UnsupportedOperationException("TODO Л4 catchesAsSupertype");
    }

    /**
     * Л5. Мульти-catch: для {@code code==1} брось {@link IllegalArgumentException}, для {@code 2} —
     * {@link IllegalStateException}, иначе верни {@code "ok"}. Оба исключения обработай ОДНИМ блоком
     * {@code catch (A | B e)} и верни {@code "handled: " + e.getMessage()}.
     */
    public static String handleMultiCatch(int code) {
        throw new UnsupportedOperationException("TODO Л5 handleMultiCatch");
    }

    /**
     * Л6. Брось ПРОВЕРЯЕМОЕ (checked) {@link InsufficientFundsException}, если {@code amount > balance};
     * иначе верни остаток. Обрати внимание на {@code throws} в сигнатуре — checked обязателен к объявлению.
     */
    public static int withdraw(int balance, int amount) throws InsufficientFundsException {
        throw new UnsupportedOperationException("TODO Л6 withdraw");
    }

    /**
     * Л7. Оберни низкоуровневое исключение: при {@link NumberFormatException} брось
     * {@link IllegalArgumentException} с сообщением {@code "not a number: " + s} и ПРИЧИНОЙ (cause) —
     * исходным исключением. Подсказка: {@code new IllegalArgumentException(msg, e)}.
     */
    public static int parseWrapped(String s) {
        throw new UnsupportedOperationException("TODO Л7 parseWrapped");
    }

    /** Л8. Найди КОРНЕВУЮ причину: иди по {@code getCause()} до самого глубокого исключения. */
    public static Throwable rootCause(Throwable t) {
        throw new UnsupportedOperationException("TODO Л8 rootCause");
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Открой один {@link Resource} через try-with-resources, вызови {@code use()}. Ресурс закроется
     * АВТОМАТИЧЕСКИ (даже при исключении). Ожидаемый {@code log}: {@code ["use-A", "close-A"]}.
     */
    public static void useResource(List<String> log) {
        throw new UnsupportedOperationException("TODO С9 useResource");
    }

    /**
     * С10. Открой ДВА ресурса в одном try-with-resources и вызови {@code use()} у каждого. Ресурсы
     * закрываются в ОБРАТНОМ порядке. Ожидаемый {@code log}: {@code ["use-A","use-B","close-B","close-A"]}.
     */
    public static void useTwoResources(List<String> log) {
        throw new UnsupportedOperationException("TODO С10 useTwoResources");
    }

    /**
     * С11. Если тело try бросает исключение И закрытие ресурса тоже — «главным» становится исключение
     * ТЕЛА, а исключение close() добавляется к нему как SUPPRESSED. Открой ресурс, падающий на close,
     * брось из тела {@code new RuntimeException("body")}, поймай и верни его (с suppressed внутри).
     */
    public static Throwable suppressedException() {
        throw new UnsupportedOperationException("TODO С11 suppressedException");
    }

    /**
     * С12. ЛОВУШКА: {@code return} в {@code finally} перекрывает {@code return} из {@code try}. Верни 1
     * из try и 2 из finally — наружу уйдёт 2. (Так писать НЕ надо — это демонстрация антипаттерна.)
     */
    public static int returnInFinally() {
        throw new UnsupportedOperationException("TODO С12 returnInFinally");
    }

    /**
     * С13. Выполни {@code action}; если оно бросило ПРОВЕРЯЕМОЕ исключение — оберни в
     * {@link RuntimeException} (частый приём: «протащить» checked через API без {@code throws}).
     * {@code RuntimeException} пробрасывай как есть.
     */
    public static <T> T toUnchecked(Callable<T> action) {
        throw new UnsupportedOperationException("TODO С13 toUnchecked");
    }

    /**
     * С14. Валидация с данными в исключении: если {@code age < 0}, брось {@link ValidationException} с
     * полем {@code "age"} и сообщением {@code "must be >= 0"}; иначе верни {@code age}.
     */
    public static int validateAge(int age) {
        throw new UnsupportedOperationException("TODO С14 validateAge");
    }

    /**
     * С15. Распарси все строки в числа. При первой ошибке брось {@link IllegalArgumentException} с
     * сообщением {@code "bad element at index " + i} и ПРИЧИНОЙ — исходным {@link NumberFormatException}.
     */
    public static List<Integer> parseAllOrThrow(List<String> items) {
        throw new UnsupportedOperationException("TODO С15 parseAllOrThrow");
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /** СЛ16. Собери сообщения всей цепочки причин — от верхнего исключения до корня (по {@code getCause}). */
    public static List<String> causeChainMessages(Throwable t) {
        throw new UnsupportedOperationException("TODO СЛ16 causeChainMessages");
    }

    /**
     * СЛ17. Два ресурса, ОБА падают на close, и тело тоже бросает исключение. Верни число SUPPRESSED
     * исключений у главного (ожидается 2). Подсказка: {@code e.getSuppressed().length}.
     */
    public static int multipleSuppressed() {
        throw new UnsupportedOperationException("TODO СЛ17 multipleSuppressed");
    }

    /**
     * СЛ18. Специфичность catch: подклассы ловят раньше базового (иначе — ошибка компиляции «уже
     * поймано»). Для {@code "missing"} брось {@link NotFoundException} → верни 404; для {@code "dup"} —
     * {@link ConflictException} → 409; иначе {@link AppException} → 500. Лови в правильном порядке.
     */
    public static int statusFor(String kind) {
        throw new UnsupportedOperationException("TODO СЛ18 statusFor");
    }

    /**
     * СЛ19. Повтори {@code action} до {@code attempts} раз: верни первый успех; если все попытки бросили
     * исключение — брось {@link RuntimeException} с сообщением {@code "all " + attempts + " attempts failed"}
     * и ПРИЧИНОЙ — последним пойманным исключением.
     */
    public static <T> T retry(Callable<T> action, int attempts) {
        throw new UnsupportedOperationException("TODO СЛ19 retry");
    }

    /**
     * СЛ20. Идиома «тихого закрытия»: закрой ресурс, ПРОГЛОТИВ любое исключение из {@code close()}
     * (так делают {@code closeQuietly} в OkHttp/Android для потоков в путях очистки).
     */
    public static void closeQuietly(AutoCloseable resource) {
        throw new UnsupportedOperationException("TODO СЛ20 closeQuietly");
    }

    // ═══════════════════════════ Инфраструктура (дана целиком) ═══════════════════════════

    /** Ресурс с автозакрытием: пишет в лог факт использования и закрытия; опционально падает на close. */
    public static final class Resource implements AutoCloseable {
        private final String name;
        private final List<String> log;
        private final boolean failOnClose;
        public Resource(String name, List<String> log) { this(name, log, false); }
        public Resource(String name, List<String> log, boolean failOnClose) {
            this.name = name;
            this.log = log;
            this.failOnClose = failOnClose;
        }
        public void use() { log.add("use-" + name); }
        @Override public void close() {
            log.add("close-" + name);
            if (failOnClose) throw new IllegalStateException("close failed: " + name);
        }
    }

    /** Проверяемое (checked) исключение — наследник {@link Exception}. */
    public static final class InsufficientFundsException extends Exception {
        public InsufficientFundsException(String message) { super(message); }
    }

    /** Непроверяемое исключение, несущее данные (имя невалидного поля). */
    public static final class ValidationException extends RuntimeException {
        private final String field;
        public ValidationException(String field, String message) {
            super(message);
            this.field = field;
        }
        public String field() { return field; }
    }

    /** Базовое доменное исключение + два подтипа (для демонстрации специфичности catch). */
    public static class AppException extends RuntimeException {
        public AppException(String message) { super(message); }
    }

    public static final class NotFoundException extends AppException {
        public NotFoundException(String message) { super(message); }
    }

    public static final class ConflictException extends AppException {
        public ConflictException(String message) { super(message); }
    }
}
