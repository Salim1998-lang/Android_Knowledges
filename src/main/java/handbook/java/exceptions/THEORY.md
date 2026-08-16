# Тема 5. Исключения и ресурсы

> Читаешь теорию → решаешь 20 задач в `Tasks.java` → гоняешь тесты:
> `./gradlew test --tests "handbook.java.exceptions.*"`. Застрял — сверься с
> [`solutions/Solutions.java`](solutions/Solutions.java).

Про то, как в Java сигнализируют об ошибках и гарантированно освобождают ресурсы: иерархия
`Throwable`, checked vs unchecked, `try/catch/finally` и его ловушки, try-with-resources и
suppressed-исключения, cause/chaining, собственные исключения.

---

## 1. Иерархия `Throwable`

```
Throwable
├── Error               ← фатальное для JVM (OutOfMemoryError, StackOverflowError) — НЕ ловим
└── Exception
    ├── RuntimeException ← UNCHECKED (непроверяемые): NPE, IllegalArgument, IllegalState, ...
    └── (прочие)         ← CHECKED (проверяемые): IOException, ...
```

- **`Error`** — «всё плохо на уровне платформы». Ловить не нужно (обычно нельзя ничего сделать).
- **`RuntimeException` и потомки** — **unchecked**. Как правило, это баги программиста (разыменование
  `null`, кривой аргумент). Компилятор их не требует объявлять/ловить.
- **Остальные `Exception`** — **checked**. Ожидаемые сбои среды (нет файла, оборвалась сеть).
  Компилятор ЗАСТАВЛЯЕТ либо поймать, либо объявить в `throws`.

## 2. Checked vs unchecked

```java
// checked: обязателен throws или try/catch
static int withdraw(int balance, int amount) throws InsufficientFundsException {
    if (amount > balance) throw new InsufficientFundsException("...");
    return balance - amount;
}

// unchecked: объявлять не обязательно
static int parse(String s) { return Integer.parseInt(s); }  // бросит NumberFormatException — unchecked
```

> **Контекст Android/Kotlin.** В Kotlin **нет** checked-исключений — там любое исключение как
> unchecked, `throws` не требуется. Поэтому Java-код с checked-исключениями из Kotlin вызывается без
> обязательного `catch`. Знать разницу нужно: старые Java-API (`IOException` и т.п.) остаются checked.

## 3. `try` / `catch` / `finally`

- **`catch` подтипа ловится обработчиком супертипа**: `catch (RuntimeException e)` поймает и
  `IllegalArgumentException`. Поэтому **специфичные — раньше** общих (иначе ошибка компиляции «уже
  поймано»).

  ```java
  try { ... }
  catch (NotFoundException e) { return 404; }   // подкласс — первым
  catch (AppException e)      { return 500; }   // базовый — последним
  ```

- **Мульти-catch** — один блок на несколько несвязанных типов:

  ```java
  catch (IllegalArgumentException | IllegalStateException e) { ... }
  ```

- **`finally` выполняется ВСЕГДА** — после нормального выхода, после `return`, после исключения.
  Место для освобождения ресурсов.

### Ловушки `finally`

- **`return`/`throw` в `finally` перекрывает** результат/исключение из `try` — и **проглатывает** его:

  ```java
  try { return 1; } finally { return 2; }   // вернётся 2; исходный результat потерян
  ```

  Никогда не делай `return`/`throw` в `finally` — теряются и значения, и реальные ошибки.

## 4. try-with-resources и `AutoCloseable`

Ручное закрытие в `finally` многословно и легко ошибиться. Для всего, что реализует `AutoCloseable`,
есть **try-with-resources** — ресурс закрывается автоматически, даже при исключении:

```java
try (Resource r = new Resource("A")) {
    r.use();
}   // r.close() вызовется здесь — гарантированно
```

- Несколько ресурсов — через `;`; **закрываются в ОБРАТНОМ порядке** объявления:

  ```java
  try (Resource a = ...; Resource b = ...) { ... }   // close: сначала b, потом a
  ```

- Свой ресурс: реализуй `AutoCloseable` (`void close()`); можно сузить сигнатуру, не бросая checked.

### Suppressed-исключения

Если **тело `try` бросило** исключение И **`close()` тоже** — «главным» остаётся исключение **тела**, а
исключение из `close()` цепляется к нему как **suppressed** (подавленное), чтобы не потерять ни одно:

```java
try (Resource r = new Resource("A", failOnClose)) {
    throw new RuntimeException("body");     // главное
}   // close() бросил своё → оно уходит в e.getSuppressed()
// e.getMessage() == "body"; e.getSuppressed()[0] — ошибка close
```

Два падающих ресурса + ошибка тела → два suppressed. (До try-with-resources ошибка `close()` в
`finally` затирала настоящую причину — классический источник «потерянных» багов.)

## 5. Причина (cause) и оборачивание (chaining)

Ловя низкоуровневое исключение, часто бросают своё — доменное — **сохраняя причину**:

```java
try { return Integer.parseInt(s); }
catch (NumberFormatException e) {
    throw new IllegalArgumentException("not a number: " + s, e);   // e — cause
}
```

`getCause()` возвращает вложенное исключение; так строится **цепочка причин**. Обходя её до конца,
получаешь **корневую причину**:

```java
Throwable cur = t;
while (cur.getCause() != null) cur = cur.getCause();   // корень
```

**Оборачивание checked → unchecked** — частый приём, чтобы «протащить» проверяемое исключение через
API без `throws`:

```java
try { return action.call(); }
catch (Exception e) { throw new RuntimeException(e); }   // причина сохранена
```

## 6. Собственные исключения

Наследуй `Exception` (checked) или `RuntimeException` (unchecked). Хорошее исключение **несёт данные**
для обработки, а не только текст:

```java
class ValidationException extends RuntimeException {
    private final String field;
    ValidationException(String field, String message) { super(message); this.field = field; }
    String field() { return field; }
}
```

Заводи иерархию доменных исключений (`AppException` → `NotFoundException`, `ConflictException`), чтобы
вызывающий ловил нужный уровень специфичности.

## 7. Практика

- **Лови конкретное**, а не `catch (Exception e)` подряд — иначе прячешь баги.
- **Не глотай молча**: пустой `catch {}` — почти всегда ошибка. Логируй или пробрасывай.
- **Сохраняй причину** при оборачивании (`new X(msg, cause)`).
- Исключения — для **исключительных** ситуаций, не для потока управления (они дорогие: заполнение
  stacktrace).
- **`closeQuietly`** — легитимное «проглатывание» ошибки `close()` в путях очистки, где основная
  ошибка уже обрабатывается (так делают OkHttp/Android для потоков).

---

## Что дальше

Реши 20 задач в [`Tasks.java`](Tasks.java): Л1–Л8, С9–С15, СЛ16–СЛ20. Особое внимание —
try-with-resources и suppressed (С9–С11, СЛ17) и cause/chaining (Л7–Л8, СЛ16). Прогон:
`./gradlew test --tests "handbook.java.exceptions.*"`.
