package handbook.java.interop;

/**
 * Тема 10 «Interop Java ↔ Kotlin» — 20 задач.
 * Реализуй помеченные {@code TODO} тела: вызови Kotlin-API из {@code KotlinApi} и соседних типов
 * ПРАВИЛЬНО из Java. Kotlin-код дан целиком в {@code KotlinApi.kt} (пакет {@code handbook.java.interop}).
 * Проверка: {@code ./gradlew test --tests "handbook.java.interop.*"}.
 * Эталон — в {@link handbook.java.interop.solutions.Solutions}.
 *
 * Нумерация: Л1–Л8 (лёгкие), С9–С15 (средние), СЛ16–СЛ20 (сложные).
 */
public final class Tasks {

    private Tasks() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Свойство Kotlin → пара геттер/сеттер: установи имя через {@code setName} и верни его через {@code getName}. */
    public static String configProperty(String value) {
        throw new UnsupportedOperationException("TODO Л1 configProperty");
    }

    /**
     * Л2. {@code @JvmField} даёт ПРЯМОЕ поле, обычное свойство — геттер. Верни сумму {@code holder.value}
     * (поле) и {@code holder.getProp()} (геттер). Ожидается 49.
     */
    public static int holderFieldAndProp() {
        throw new UnsupportedOperationException("TODO Л2 holderFieldAndProp");
    }

    /** Л3. {@code object} Kotlin — синглтон: вызови {@code ping()} через {@code Registry.INSTANCE}. */
    public static String registryPing() {
        throw new UnsupportedOperationException("TODO Л3 registryPing");
    }

    /** Л4. {@code @JvmStatic} метод companion зовётся как статический: {@code Factory.create()}. */
    public static String factoryStatic() {
        throw new UnsupportedOperationException("TODO Л4 factoryStatic");
    }

    /** Л5. Обычный метод companion — через {@code Factory.Companion}: {@code Factory.Companion.createDefault()}. */
    public static String factoryCompanion() {
        throw new UnsupportedOperationException("TODO Л5 factoryCompanion");
    }

    /** Л6. Top-level функция Kotlin → статический метод класса файла: {@code KotlinApi.topLevelGreet(name)}. */
    public static String topLevel(String name) {
        throw new UnsupportedOperationException("TODO Л6 topLevel");
    }

    /** Л7. Top-level {@code const} → статическая константа: верни {@code KotlinApi.VERSION}. */
    public static String constant() {
        throw new UnsupportedOperationException("TODO Л7 constant");
    }

    /** Л8. Extension-функция → статический метод с получателем-аргументом: {@code KotlinApi.exclaim(s)}. */
    public static String extension(String s) {
        throw new UnsupportedOperationException("TODO Л8 extension");
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /** С9. Благодаря {@code @JvmOverloads} доступна перегрузка без второго аргумента: {@code greet(name)} → "Hello, name". */
    public static String greetDefault(String name) {
        throw new UnsupportedOperationException("TODO С9 greetDefault");
    }

    /** С10. Полная перегрузка с обоими аргументами: {@code greet(name, greeting)}. */
    public static String greetCustom(String name, String greeting) {
        throw new UnsupportedOperationException("TODO С10 greetCustom");
    }

    /** С11. data class → геттеры {@code getX()}/{@code getY()}. Верни {@code x*10 + y} для {@code Point(3,4)} (=34). */
    public static int pointCoords() {
        throw new UnsupportedOperationException("TODO С11 pointCoords");
    }

    /** С12. data class генерирует {@code equals}: два {@code Point(1,2)} равны по содержимому. Верни результат сравнения. */
    public static boolean pointEquals() {
        throw new UnsupportedOperationException("TODO С12 pointEquals");
    }

    /**
     * С13. {@code fun interface} (SAM): передай Kotlin-функции {@code applyTransform} обычную Java-ЛЯМБДУ,
     * удваивающую число. Подсказка: {@code KotlinApi.applyTransform(x, v -> v * 2)}.
     */
    public static int samConversion(int x) {
        throw new UnsupportedOperationException("TODO С13 samConversion");
    }

    /**
     * С14. Non-null параметр Kotlin: {@code requireLength} на {@code null} бросит {@link NullPointerException}
     * на границе. Верни длину строки, а при {@code null} перехвати и верни -1.
     */
    public static int safeLength(String s) {
        throw new UnsupportedOperationException("TODO С14 safeLength");
    }

    /** С15. Nullable-возврат Kotlin ({@code findOrNull}): верни найденное или {@code "none"} при {@code null}. */
    public static String nullableFind(String key) {
        throw new UnsupportedOperationException("TODO С15 nullableFind");
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. {@code @Throws} делает checked-исключение видимым Java: {@code risky(fail)} объявляет
     * {@code throws IOException}. Верни {@code "ok"} при успехе или {@code "caught"}, поймав {@code IOException}.
     */
    public static String checkedThrows(boolean fail) {
        throw new UnsupportedOperationException("TODO СЛ16 checkedThrows");
    }

    /** СЛ17. {@code @JvmName} переименовал функцию для Java: зови {@code KotlinApi.renamedForJava()}. */
    public static String renamed() {
        throw new UnsupportedOperationException("TODO СЛ17 renamed");
    }

    /** СЛ18. Nullable параметр Kotlin принимает {@code null} без исключения: верни {@code nullableLength(s)}. */
    public static int nullableParam(String s) {
        throw new UnsupportedOperationException("TODO СЛ18 nullableParam");
    }

    /**
     * СЛ19. Kotlin function type {@code (Int)->Int} виден из Java как {@code kotlin.jvm.functions.Function1}:
     * передай реализацию, утраивающую число (её {@code invoke} возвращает результат). Подсказка:
     * {@code KotlinApi.mapWith(x, f)}.
     */
    public static int functionType(int x) {
        throw new UnsupportedOperationException("TODO СЛ19 functionType");
    }

    /**
     * СЛ20. {@code () -> Unit} виден из Java как {@code Function0<Unit>}: реализуй колбэк, который ставит флаг,
     * и верни его. {@code invoke} обязан вернуть {@code kotlin.Unit.INSTANCE}.
     */
    public static boolean unitCallback() {
        throw new UnsupportedOperationException("TODO СЛ20 unitCallback");
    }
}
