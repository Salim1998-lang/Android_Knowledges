# Тема 4. Коллекции и `equals`/`hashCode`

> Читаешь теорию → решаешь 20 задач в `Tasks.java` → гоняешь тесты:
> `./gradlew test --tests "handbook.java.collections.*"`. Застрял — сверься с
> [`solutions/Solutions.java`](solutions/Solutions.java).

Коллекции — рабочая лошадка любого кода. Здесь: три семейства (`List`/`Set`/`Map`) и когда что брать,
как устроен `HashMap`, **контракт `equals`/`hashCode`** (без него ключи ломаются), `Comparator` и
`Comparable`, `TreeMap`, LRU на `LinkedHashMap` и fail-fast итераторы.

---

## 1. Три семейства

| | Что | Ключевые реализации |
|---|-----|---------------------|
| **List** | упорядоченный, допускает дубликаты, доступ по индексу | `ArrayList`, `LinkedList` |
| **Set** | без дубликатов | `HashSet`, `LinkedHashSet`, `TreeSet` |
| **Map** | пары ключ→значение, ключи уникальны | `HashMap`, `LinkedHashMap`, `TreeMap` |

`Map` — не `Collection` (стоит особняком), но идейно рядом.

## 2. `ArrayList` против `LinkedList`

| Операция | `ArrayList` | `LinkedList` |
|----------|-------------|--------------|
| `get(i)` по индексу | **O(1)** | O(n) |
| добавление в конец | амортизированно O(1) | O(1) |
| вставка/удаление в середине | O(n) (сдвиг) | O(1) *если уже стоишь итератором* |
| память | компактный массив | +2 ссылки на узел |

На практике в Android **почти всегда `ArrayList`**: доступ по индексу, кэш-локальность, меньше
мусора. `LinkedList` оправдан редко (частые вставки/удаления с головы через `Deque`). «LinkedList
быстрее вставляет в середину» — миф без учёта стоимости поиска позиции.

## 3. Как устроен `HashMap`

Массив **бакетов** (bucket) + хеш-функция. Кладём ключ:

1. считаем `hashCode()` ключа, «перемешиваем» биты, берём остаток по числу бакетов → номер бакета;
2. в бакете лежит список (при коллизиях — несколько записей); ищем/сравниваем ключи через `equals`.

- **Коллизия** — разные ключи попали в один бакет. Тогда бакет — связанный список; при **8+**
  элементах в одном бакете (Java 8+) он превращается в **красно-чёрное дерево** (treeify) — поиск в
  бакете становится O(log n) вместо O(n).
- **Load factor** (по умолчанию 0.75) — порог заполнения: когда занято > 75%, массив **resize** —
  удваивается и все записи перераскладываются. Знаешь примерный размер — задай ёмкость в
  конструкторе, чтобы избежать resize.
- Средний доступ — **O(1)**, при плохом `hashCode` (все в один бакет) деградирует к O(log n)/O(n).

## 4. Контракт `equals` / `hashCode` — почему это критично

`HashMap`/`HashSet` **находят ключ по `hashCode`, а сверяют по `equals`**. Поэтому:

> Если `a.equals(b)`, то **обязательно** `a.hashCode() == b.hashCode()`.

Нарушишь — два «равных» объекта уйдут в разные бакеты, и `map.get(key)` **не найдёт** значение,
`set.contains(x)` вернёт `false` на «том же» объекте. Классический баг: переопределили `equals`,
забыли `hashCode`.

Контракт `equals` (рефлексивность, симметричность, транзитивность, согласованность, `x.equals(null)==false`)
+ контракт `hashCode` (согласован с equals; стабилен). Практика:

```java
@Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Point)) return false;
    Point p = (Point) o;
    return x == p.x && y == p.y;
}
@Override public int hashCode() { return Objects.hash(x, y); }   // тот же набор полей, что в equals
```

Правила: используй **те же поля** в обоих; поля-ключи желательно **иммутабельные** (если поле в ключе
поменяется после вставки — объект «потеряется» в мапе). В Kotlin `data class` генерирует
`equals`/`hashCode` автоматически — но знать контракт всё равно нужно.

## 5. `Comparable` и `Comparator`

- **`Comparable<T>`** — «естественный порядок» самого класса: метод `compareTo`. Так сортируются
  `String`, числа, и твои классы, если реализуют интерфейс.

  ```java
  class Version implements Comparable<Version> {
      public int compareTo(Version o) {
          int c = Integer.compare(major, o.major);
          return c != 0 ? c : Integer.compare(minor, o.minor);
      }
  }
  ```

- **`Comparator<T>`** — внешний порядок «сбоку», когда естественного нет или нужен другой. Собирается
  декларативно:

  ```java
  list.sort(Comparator.comparingInt(String::length)      // сначала по длине
                     .thenComparing(Comparator.naturalOrder()));  // потом по алфавиту
  // ещё: .reversed(), Comparator.comparing(f).reversed(), nullsFirst(...)
  ```

`compareTo`/`compare` возвращают знак: **<0**, **0**, **>0**. Не вычитай `a - b` для `int` — можно
переполниться; используй `Integer.compare`.

## 6. `TreeMap` / `TreeSet` — отсортированные

Хранят ключи **упорядоченно** (по `Comparable`/`Comparator`), операции — **O(log n)**. Дают навигацию,
которой нет у `HashMap`:

```java
map.firstKey(); map.lastKey();
map.ceilingKey(k);   // наименьший ключ >= k
map.floorKey(k);     // наибольший ключ <= k
map.higherKey(k); map.lowerKey(k); map.headMap(k); map.tailMap(k);
```

Берёшь, когда нужен порядок или диапазонные запросы; иначе `HashMap` быстрее.

## 7. `LinkedHashMap` и LRU-кэш

`LinkedHashMap` — `HashMap` + связанный список, хранящий **порядок**: вставки (по умолчанию) или
**доступа** (`accessOrder = true`). Второе + переопределённый `removeEldestEntry` = готовый **LRU-кэш**:

```java
class LruCache<K,V> extends LinkedHashMap<K,V> {
    private final int capacity;
    LruCache(int capacity) { super(16, 0.75f, true); this.capacity = capacity; }  // access-order
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) { return size() > capacity; }
}
```

Каждый `get` двигает элемент в «свежие», при переполнении вытесняется давно не используемый. В Android
для этого есть готовый `androidx`/`android.util.LruCache` (для битмапов и т.п.) — но идея та же.

## 8. Fail-fast итераторы

Итераторы `ArrayList`/`HashMap` — **fail-fast**: если во время обхода структуру изменить в обход
итератора, при следующем `next()` прилетит `ConcurrentModificationException` (сверяется счётчик
модификаций `modCount`). Это защита от скрытых багов, а не потокобезопасность.

```java
for (String s : list) { if (cond(s)) list.remove(s); }   // ❌ ConcurrentModificationException
```

Правильно удалять во время обхода — через сам итератор или `removeIf`:

```java
Iterator<String> it = list.iterator();
while (it.hasNext()) { if (cond(it.next())) it.remove(); }   // ✅
list.removeIf(s -> cond(s));                                  // ✅ короче
```

## 9. Утилиты и неизменяемость

- `Collections.sort/reverse/shuffle/max/min/frequency`, `Collections.unmodifiableList` (обёртка только
  на чтение — запись бросит `UnsupportedOperationException`).
- `List.of(...)`, `Map.of(...)`, `Set.of(...)` — **неизменяемые** фабрики (Java 9+); не терпят `null` и
  не позволяют менять. Нужен изменяемый — оборачивай: `new ArrayList<>(List.of(...))`.

> **Android-специфика.** Для `Map<Integer, V>` и `Map<Long, V>` есть `SparseArray`/`LongSparseArray`, а
> для небольших мап — `ArrayMap`: они экономят память, избегая автоупаковки ключей и лишних объектов
> `Map.Entry`. На больших объёмах `HashMap` быстрее — это осознанный размен «память vs скорость».

---

## Что дальше

Реши 20 задач в [`Tasks.java`](Tasks.java): Л1–Л8, С9–С15, СЛ16–СЛ20. Особое внимание — контракту
`equals`/`hashCode` (С9, СЛ17) и LRU (СЛ16). Прогон: `./gradlew test --tests "handbook.java.collections.*"`.
