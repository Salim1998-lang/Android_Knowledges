package handbook.java.interop.solutions;

import handbook.java.interop.Config;
import handbook.java.interop.Factory;
import handbook.java.interop.Holder;
import handbook.java.interop.IntTransformer;
import handbook.java.interop.KotlinApi;
import handbook.java.interop.Point;
import handbook.java.interop.Registry;
import java.io.IOException;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

/** Эталонные решения темы 10. Подсмотри, если застрял с {@link handbook.java.interop.Tasks}. */
public final class Solutions {

    private Solutions() { }

    // ═══════════════════════════ Лёгкие (1–8) ═══════════════════════════

    /** Л1. Свойство Kotlin → пара геттер/сеттер: установи имя через {@code setName} и верни его через {@code getName}. */
    public static String configProperty(String value) {
        Config config = new Config();
        config.setName(value);
        return config.getName();
    }

    /**
     * Л2. {@code @JvmField} даёт ПРЯМОЕ поле, обычное свойство — геттер. Верни сумму {@code holder.value}
     * (поле) и {@code holder.getProp()} (геттер). Ожидается 49.
     */
    public static int holderFieldAndProp() {
        Holder holder = new Holder();
        return holder.value + holder.getProp();
    }

    /** Л3. {@code object} Kotlin — синглтон: вызови {@code ping()} через {@code Registry.INSTANCE}. */
    public static String registryPing() {
        return Registry.INSTANCE.ping();
    }

    /** Л4. {@code @JvmStatic} метод companion зовётся как статический: {@code Factory.create()}. */
    public static String factoryStatic() {
        return Factory.create();
    }

    /** Л5. Обычный метод companion — через {@code Factory.Companion}: {@code Factory.Companion.createDefault()}. */
    public static String factoryCompanion() {
        return Factory.Companion.createDefault();
    }

    /** Л6. Top-level функция Kotlin → статический метод класса файла: {@code KotlinApi.topLevelGreet(name)}. */
    public static String topLevel(String name) {
        return KotlinApi.topLevelGreet(name);
    }

    /** Л7. Top-level {@code const} → статическая константа: верни {@code KotlinApi.VERSION}. */
    public static String constant() {
        return KotlinApi.VERSION;
    }

    /** Л8. Extension-функция → статический метод с получателем-аргументом: {@code KotlinApi.exclaim(s)}. */
    public static String extension(String s) {
        return KotlinApi.exclaim(s);
    }

    // ═══════════════════════════ Средние (9–15) ═══════════════════════════

    /** С9. Благодаря {@code @JvmOverloads} доступна перегрузка без второго аргумента: {@code greet(name)} → "Hello, name". */
    public static String greetDefault(String name) {
        return KotlinApi.greet(name);
    }

    /** С10. Полная перегрузка с обоими аргументами: {@code greet(name, greeting)}. */
    public static String greetCustom(String name, String greeting) {
        return KotlinApi.greet(name, greeting);
    }

    /** С11. data class → геттеры {@code getX()}/{@code getY()}. Верни {@code x*10 + y} для {@code Point(3,4)} (=34). */
    public static int pointCoords() {
        Point point = new Point(3, 4);
        return point.getX() * 10 + point.getY();
    }

    /** С12. data class генерирует {@code equals}: два {@code Point(1,2)} равны по содержимому. Верни результат сравнения. */
    public static boolean pointEquals() {
        return new Point(1, 2).equals(new Point(1, 2));
    }

    /**
     * С13. {@code fun interface} (SAM): передай Kotlin-функции {@code applyTransform} обычную Java-ЛЯМБДУ,
     * удваивающую число. Подсказка: {@code KotlinApi.applyTransform(x, v -> v * 2)}.
     */
    public static int samConversion(int x) {
        return KotlinApi.applyTransform(x, v -> v * 2);
    }

    /**
     * С14. Non-null параметр Kotlin: {@code requireLength} на {@code null} бросит {@link NullPointerException}
     * на границе. Верни длину строки, а при {@code null} перехвати и верни -1.
     */
    public static int safeLength(String s) {
        try {
            return KotlinApi.requireLength(s);
        } catch (NullPointerException e) {
            return -1;
        }
    }

    /** С15. Nullable-возврат Kotlin ({@code findOrNull}): верни найденное или {@code "none"} при {@code null}. */
    public static String nullableFind(String key) {
        String result = KotlinApi.findOrNull(key);
        return result == null ? "none" : result;
    }

    // ═══════════════════════════ Сложные (16–20) ═══════════════════════════

    /**
     * СЛ16. {@code @Throws} делает checked-исключение видимым Java: {@code risky(fail)} объявляет
     * {@code throws IOException}. Верни {@code "ok"} при успехе или {@code "caught"}, поймав {@link IOException}.
     */
    public static String checkedThrows(boolean fail) {
        try {
            KotlinApi.risky(fail);
            return "ok";
        } catch (IOException e) {
            return "caught";
        }
    }

    /** СЛ17. {@code @JvmName} переименовал функцию для Java: зови {@code KotlinApi.renamedForJava()}. */
    public static String renamed() {
        return KotlinApi.renamedForJava();
    }

    /** СЛ18. Nullable параметр Kotlin принимает {@code null} без исключения: верни {@code nullableLength(s)}. */
    public static int nullableParam(String s) {
        return KotlinApi.nullableLength(s);
    }

    /**
     * СЛ19. Kotlin function type {@code (Int)->Int} виден из Java как {@link Function1}: передай реализацию,
     * утраивающую число (её {@code invoke} возвращает результат). Подсказка: {@code KotlinApi.mapWith(x, f)}.
     */
    public static int functionType(int x) {
        return KotlinApi.mapWith(x, new Function1<Integer, Integer>() {
            @Override public Integer invoke(Integer v) {
                return v * 3;
            }
        });
    }

    /**
     * СЛ20. {@code () -> Unit} виден из Java как {@link Function0}{@code <Unit>}: реализуй колбэк, который
     * ставит флаг, и верни его. {@code invoke} обязан вернуть {@link Unit#INSTANCE}.
     */
    public static boolean unitCallback() {
        boolean[] flag = {false};
        KotlinApi.runCallback(new Function0<Unit>() {
            @Override public Unit invoke() {
                flag[0] = true;
                return Unit.INSTANCE;
            }
        });
        return flag[0];
    }
}
