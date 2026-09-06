package handbook.compose.snapshot

import handbook.compose.snapshot.solutions.SnapshotSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 3 «Snapshot-система». Проверяют MVCC-чтение по версиям, изоляцию снапшотов, атомарность
 * и конфликты/слияние apply на детерминированной модели [Record]/[ApplyResult].
 *
 * Чтобы гонять против эталона — замени `SUT` на [SnapshotSolutions].
 */
private typealias SUT = SnapshotTasks

class SnapshotTest {

    private val history = listOf(Record(1, "a"), Record(3, "c"), Record(5, "e"))

    // ── Лёгкие ──

    @Test fun `Л1 visibleRecord — наибольшая версия не выше запрошенной`() {
        assertEquals(Record(3, "c"), SUT.visibleRecord(history, atVersion = 4))
        assertEquals(Record(5, "e"), SUT.visibleRecord(history, atVersion = 5))
        assertNull(SUT.visibleRecord(history, atVersion = 0))
    }

    @Test fun `Л2 readAt — значение видимой записи или default`() {
        assertEquals("c", SUT.readAt(history, atVersion = 4, default = "?"))
        assertEquals("?", SUT.readAt(history, atVersion = 0, default = "?"))
    }

    @Test fun `Л3 latestVersion`() {
        assertEquals(5, SUT.latestVersion(history))
        assertEquals(0, SUT.latestVersion(emptyList()))
    }

    @Test fun `Л4 writeRecord — добавляет запись, не мутируя`() {
        val next = SUT.writeRecord(history, version = 6, value = "f")
        assertEquals(4, next.size)
        assertEquals(Record(6, "f"), next.last())
        assertEquals(3, history.size) // исходная история не изменилась
    }

    @Test fun `Л5 isVisibleTo — read isolation`() {
        assertTrue(SUT.isVisibleTo(writeVersion = 3, snapshotBase = 5))
        assertFalse(SUT.isVisibleTo(writeVersion = 6, snapshotBase = 5))
    }

    @Test fun `Л6 hiddenWrites — сделанные позже базы`() {
        assertEquals(listOf(Record(5, "e")), SUT.hiddenWrites(history, base = 3))
    }

    @Test fun `Л7 readInSnapshot — своя запись перекрывает глобальную`() {
        assertEquals("mine", SUT.readInSnapshot(hasOwnWrite = true, ownWrite = "mine", committed = "global"))
        assertEquals("global", SUT.readInSnapshot(hasOwnWrite = false, ownWrite = "mine", committed = "global"))
    }

    @Test fun `Л8 nextVersion`() {
        assertEquals(6, SUT.nextVersion(5))
    }

    // ── Средние ──

    @Test fun `С9 readForSnapshot — своя запись или MVCC-чтение`() {
        assertEquals("Z", SUT.readForSnapshot(history, base = 4, hasOwnWrite = true, ownWrite = "Z", default = "?"))
        assertEquals("c", SUT.readForSnapshot(history, base = 4, hasOwnWrite = false, ownWrite = "Z", default = "?"))
    }

    @Test fun `С10 hasConflict`() {
        assertTrue(SUT.hasConflict(myWrites = setOf("x", "y"), modifiedSinceBase = setOf("y", "z")))
        assertFalse(SUT.hasConflict(myWrites = setOf("x"), modifiedSinceBase = setOf("z")))
    }

    @Test fun `С11 conflictingStates`() {
        assertEquals(setOf("y"), SUT.conflictingStates(setOf("x", "y"), setOf("y", "z")))
    }

    @Test fun `С12 merge — слияние по политике`() {
        assertEquals(2, SUT.merge(previous = 1, current = 1, applied = 2)) // менялись только мы
        assertEquals(2, SUT.merge(previous = 1, current = 2, applied = 1)) // менялись только они
        assertEquals(2, SUT.merge(previous = 1, current = 2, applied = 2)) // к одному значению
        assertNull(SUT.merge(previous = 1, current = 2, applied = 3))      // настоящий конфликт
    }

    @Test fun `С13 applyResult`() {
        assertEquals(ApplyResult.SUCCESS, SUT.applyResult(conflict = false, mergeable = false))
        assertEquals(ApplyResult.MERGED, SUT.applyResult(conflict = true, mergeable = true))
        assertEquals(ApplyResult.FAILURE, SUT.applyResult(conflict = true, mergeable = false))
    }

    @Test fun `С14 applyAtomic — всё или ничего`() {
        val global = mapOf<String, Any?>("a" to 1)
        val writes = mapOf<String, Any?>("a" to 2, "b" to 3)
        assertEquals(mapOf("a" to 2, "b" to 3), SUT.applyAtomic(global, writes, success = true))
        assertEquals(mapOf("a" to 1), SUT.applyAtomic(global, writes, success = false))
    }

    @Test fun `С15 snapshotFlowEmits — первое плюс изменения`() {
        assertEquals(3, SUT.snapshotFlowEmits(listOf(1, 1, 2, 2, 3)))
        assertEquals(1, SUT.snapshotFlowEmits(listOf(5)))
        assertEquals(0, SUT.snapshotFlowEmits(emptyList()))
    }

    // ── Сложные ──

    @Test fun `СЛ16 withMutableSnapshot — атомарно применяет записи`() {
        assertEquals(
            mapOf("a" to 2, "b" to 9),
            SUT.withMutableSnapshot(global = mapOf("a" to 1, "b" to 9), writes = mapOf("a" to 2)),
        )
    }

    @Test fun `СЛ17 applyTwo — без конфликта оба успешны`() {
        val (global, results) = SUT.applyTwo(
            global = mapOf("x" to 0, "y" to 0),
            aWrites = mapOf("x" to 1),
            bWrites = mapOf("y" to 2),
            mergeableStates = emptySet(),
        )
        assertEquals(mapOf("x" to 1, "y" to 2), global)
        assertEquals(listOf(ApplyResult.SUCCESS, ApplyResult.SUCCESS), results)
    }

    @Test fun `СЛ17 applyTwo — конфликт сливается политикой`() {
        // B прочитал count=0 и «записал» 0 (по сути не менял) → сливается к значению A.
        val (global, results) = SUT.applyTwo(
            global = mapOf("count" to 0),
            aWrites = mapOf("count" to 5),
            bWrites = mapOf("count" to 0),
            mergeableStates = setOf("count"),
        )
        assertEquals(mapOf("count" to 5), global)
        assertEquals(listOf(ApplyResult.SUCCESS, ApplyResult.MERGED), results)
    }

    @Test fun `СЛ17 applyTwo — неразрешимый конфликт роняет B целиком`() {
        val (global, results) = SUT.applyTwo(
            global = mapOf("x" to 0, "other" to 0),
            aWrites = mapOf("x" to 1),
            bWrites = mapOf("x" to 2, "other" to 7), // x конфликтует; other отбрасывается вместе с B
            mergeableStates = setOf("x"),
        )
        assertEquals(mapOf("x" to 1, "other" to 0), global)
        assertEquals(listOf(ApplyResult.SUCCESS, ApplyResult.FAILURE), results)
    }

    @Test fun `СЛ18 stableReadsWithinSnapshot — мир застыл на базе`() {
        val h = listOf(Record(1, "a"), Record(2, "b"), Record(5, "z"))
        assertEquals(listOf("b", "b", "b"), SUT.stableReadsWithinSnapshot(h, base = 2, reads = 3, default = "?"))
    }

    @Test fun `СЛ19 visibleRecordExcluding — игнорирует отменённые снапшоты`() {
        val h = listOf(Record(1, "a"), Record(2, "b"), Record(3, "c"))
        assertEquals(Record(3, "c"), SUT.visibleRecordExcluding(h, atVersion = 3, aborted = setOf(2)))
        assertEquals(Record(2, "b"), SUT.visibleRecordExcluding(h, atVersion = 3, aborted = setOf(3)))
    }

    @Test fun `СЛ20 autoMergeable — разбор сливаемости`() {
        val triples = mapOf(
            "a" to Triple<Any?, Any?, Any?>(0, 0, 1), // менялись только мы → сливаемо
            "b" to Triple<Any?, Any?, Any?>(0, 1, 2), // расхождение → конфликт
        )
        assertEquals(setOf("a") to setOf("b"), SUT.autoMergeable(triples))
    }
}
