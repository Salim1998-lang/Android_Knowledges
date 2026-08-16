package handbook.java.exceptions.solutions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/** Эталонные решения темы 5. Подсмотри, если застрял с {@link handbook.java.exceptions.Tasks}. */
public final class Solutions {

    private Solutions() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Подели {@code a/b}, а при делении на ноль ({@link ArithmeticException}) верни {@code fallback}. */
    public static int safeDivide(int a, int b, int fallback) {
        try {
            return a / b;
        } catch (ArithmeticException e) {
            return fallback;
        }
    }

    /** Л2. Распарси число, при {@link NumberFormatException} верни {@code def}. */
    public static int parseOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Л3. Покажи, что {@code finally} выполняется ВСЕГДА — даже при {@code return} из {@code try}.
     * Добавь в {@code log} строку {@code "try"}, верни {@code "ok"}, а в {@code finally} добавь {@code "finally"}.
     */
    public static String runFinally(List<String> log) {
        try {
            log.add("try");
            return "ok";
        } finally {
            log.add("finally");
        }
    }

    /**
     * Л4. Подтип ловится обработчиком супертипа: брось {@link IllegalArgumentException}, поймай его как
     * {@link RuntimeException} и верни {@code true}.
     */
    public static boolean catchesAsSupertype() {
        try {
            throw new IllegalArgumentException("boom");
        } catch (RuntimeException e) {
            return true;
        }
    }

    /**
     * Л5. Мульти-catch: для {@code code==1} брось {@link IllegalArgumentException}, для {@code 2} —
     * {@link IllegalStateException}, иначе верни {@code "ok"}. Оба исключения обработай ОДНИМ блоком
     * {@code catch (A | B e)} и верни {@code "handled: " + e.getMessage()}.
     */
    public static String handleMultiCatch(int code) {
        try {
            if (code == 1) throw new IllegalArgumentException("a");
            if (code == 2) throw new IllegalStateException("b");
            return "ok";
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "handled: " + e.getMessage();
        }
    }

    /**
     * Л6. Брось ПРОВЕРЯЕМОЕ (checked) {@link InsufficientFundsException}, если {@code amount > balance};
     * иначе верни остаток. Обрати внимание на {@code throws} в сигнатуре — checked обязателен к объявлению.
     */
    public static int withdraw(int balance, int amount) throws InsufficientFundsException {
        if (amount > balance) {
            throw new InsufficientFundsException("need " + amount + ", have " + balance);
        }
        return balance - amount;
    }

    /**
     * Л7. Оберни низкоуровневое исключение: при {@link NumberFormatException} брось
     * {@link IllegalArgumentException} с сообщением {@code "not a number: " + s} и ПРИЧИНОЙ (cause) —
     * исходным исключением. Подсказка: {@code new IllegalArgumentException(msg, e)}.
     */
    public static int parseWrapped(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a number: " + s, e);
        }
    }

    /** Л8. Найди КОРНЕВУЮ причину: иди по {@code getCause()} до самого глубокого исключения. */
    public static Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur;
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /**
     * С9. Открой один {@link Resource} через try-with-resources, вызови {@code use()}. Ресурс закроется
     * АВТОМАТИЧЕСКИ (даже при исключении). Ожидаемый {@code log}: {@code ["use-A", "close-A"]}.
     */
    public static void useResource(List<String> log) {
        try (Resource r = new Resource("A", log)) {
            r.use();
        }
    }

    /**
     * С10. Открой ДВА ресурса в одном try-with-resources и вызови {@code use()} у каждого. Ресурсы
     * закрываются в ОБРАТНОМ порядке. Ожидаемый {@code log}: {@code ["use-A","use-B","close-B","close-A"]}.
     */
    public static void useTwoResources(List<String> log) {
        try (Resource a = new Resource("A", log); Resource b = new Resource("B", log)) {
            a.use();
            b.use();
        }
    }

    /**
     * С11. Если тело try бросает исключение И закрытие ресурса тоже — «главным» становится исключение
     * ТЕЛА, а исключение close() добавляется к нему как SUPPRESSED. Открой ресурс, падающий на close,
     * брось из тела {@code new RuntimeException("body")}, поймай и верни его (с suppressed внутри).
     */
    public static Throwable suppressedException() {
        List<String> log = new ArrayList<>();
        try {
            try (Resource r = new Resource("A", log, true)) {
                throw new RuntimeException("body");
            }
        } catch (RuntimeException e) {
            return e;
        }
    }

    /**
     * С12. ЛОВУШКА: {@code return} в {@code finally} перекрывает {@code return} из {@code try}. Верни 1
     * из try и 2 из finally — наружу уйдёт 2. (Так писать НЕ надо — это демонстрация антипаттерна.)
     */
    @SuppressWarnings("finally")
    public static int returnInFinally() {
        try {
            return 1;
        } finally {
            return 2;
        }
    }

    /**
     * С13. Выполни {@code action}; если оно бросило ПРОВЕРЯЕМОЕ исключение — оберни в
     * {@link RuntimeException} (частый приём: «протащить» checked через API без {@code throws}).
     * {@code RuntimeException} пробрасывай как есть.
     */
    public static <T> T toUnchecked(Callable<T> action) {
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * С14. Валидация с данными в исключении: если {@code age < 0}, брось {@link ValidationException} с
     * полем {@code "age"} и сообщением {@code "must be >= 0"}; иначе верни {@code age}.
     */
    public static int validateAge(int age) {
        if (age < 0) {
            throw new ValidationException("age", "must be >= 0");
        }
        return age;
    }

    /**
     * С15. Распарси все строки в числа. При первой ошибке брось {@link IllegalArgumentException} с
     * сообщением {@code "bad element at index " + i} и ПРИЧИНОЙ — исходным {@link NumberFormatException}.
     */
    public static List<Integer> parseAllOrThrow(List<String> items) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            try {
                result.add(Integer.parseInt(items.get(i)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad element at index " + i, e);
            }
        }
        return result;
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /** СЛ16. Собери сообщения всей цепочки причин — от верхнего исключения до корня (по {@code getCause}). */
    public static List<String> causeChainMessages(Throwable t) {
        List<String> messages = new ArrayList<>();
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            messages.add(cur.getMessage());
        }
        return messages;
    }

    /**
     * СЛ17. Два ресурса, ОБА падают на close, и тело тоже бросает исключение. Верни число SUPPRESSED
     * исключений у главного (ожидается 2). Подсказка: {@code e.getSuppressed().length}.
     */
    public static int multipleSuppressed() {
        List<String> log = new ArrayList<>();
        try (Resource a = new Resource("A", log, true); Resource b = new Resource("B", log, true)) {
            throw new RuntimeException("body");
        } catch (RuntimeException e) {
            return e.getSuppressed().length;
        }
    }

    /**
     * СЛ18. Специфичность catch: подклассы ловят раньше базового (иначе — ошибка компиляции «уже
     * поймано»). Для {@code "missing"} брось {@link NotFoundException} → верни 404; для {@code "dup"} —
     * {@link ConflictException} → 409; иначе {@link AppException} → 500. Лови в правильном порядке.
     */
    public static int statusFor(String kind) {
        try {
            switch (kind) {
                case "missing": throw new NotFoundException("not found");
                case "dup":     throw new ConflictException("conflict");
                default:        throw new AppException("generic");
            }
        } catch (NotFoundException e) {
            return 404;
        } catch (ConflictException e) {
            return 409;
        } catch (AppException e) {
            return 500;
        }
    }

    /**
     * СЛ19. Повтори {@code action} до {@code attempts} раз: верни первый успех; если все попытки бросили
     * исключение — брось {@link RuntimeException} с сообщением {@code "all " + attempts + " attempts failed"}
     * и ПРИЧИНОЙ — последним пойманным исключением.
     */
    public static <T> T retry(Callable<T> action, int attempts) {
        Exception last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                return action.call();
            } catch (Exception e) {
                last = e;
            }
        }
        throw new RuntimeException("all " + attempts + " attempts failed", last);
    }

    /**
     * СЛ20. Идиома «тихого закрытия»: закрой ресурс, ПРОГЛОТИВ любое исключение из {@code close()}
     * (так делают {@code closeQuietly} в OkHttp/Android для потоков в путях очистки).
     */
    public static void closeQuietly(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception ignored) {
            // намеренно проглатываем — путь очистки не должен маскировать основную ошибку
        }
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
