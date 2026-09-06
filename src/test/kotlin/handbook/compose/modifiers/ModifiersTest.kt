package handbook.compose.modifiers

import handbook.compose.modifiers.Element.Background
import handbook.compose.modifiers.Element.Clickable
import handbook.compose.modifiers.Element.Padding
import handbook.compose.modifiers.Element.Semantics
import handbook.compose.modifiers.Element.Size
import handbook.compose.modifiers.solutions.ModifiersSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 7 «Модификаторы». Проверяют цепочку, свёртку и — главное — влияние ПОРЯДКА на размер,
 * фон и область клика, а также переиспользование узлов [Modifier.Node] и семантику, на
 * детерминированной модели [Element].
 *
 * Чтобы гонять против эталона — замени `SUT` на [ModifiersSolutions].
 */
private typealias SUT = ModifiersTasks

class ModifiersTest {

    // ── Лёгкие ──

    @Test fun `Л1 then — склейка цепочек`() {
        assertEquals(listOf(Padding(10), Size(100)), SUT.then(listOf(Padding(10)), listOf(Size(100))))
    }

    @Test fun `Л2 isIdentity — пустой Modifier`() {
        assertTrue(SUT.isIdentity(emptyList()))
        assertFalse(SUT.isIdentity(listOf(Padding(10))))
    }

    @Test fun `Л3 elementCount`() {
        assertEquals(2, SUT.elementCount(listOf(Padding(10), Size(100))))
    }

    @Test fun `Л4 foldNames — слева направо`() {
        assertEquals(
            listOf("padding", "background", "clickable"),
            SUT.foldNames(listOf(Padding(10), Background("red"), Clickable)),
        )
    }

    @Test fun `Л5 sameChain — порядок и содержимое`() {
        assertTrue(SUT.sameChain(listOf(Padding(10)), listOf(Padding(10))))
        assertFalse(SUT.sameChain(listOf(Padding(10)), listOf(Padding(20))))
    }

    @Test fun `Л6 totalPadding`() {
        assertEquals(14, SUT.totalPadding(listOf(Padding(10), Size(5), Padding(4))))
    }

    @Test fun `Л7 hasClickable`() {
        assertTrue(SUT.hasClickable(listOf(Padding(10), Clickable)))
        assertFalse(SUT.hasClickable(listOf(Padding(10))))
    }

    @Test fun `Л8 semanticsOf`() {
        assertEquals(
            mapOf("role" to "button", "desc" to "ok"),
            SUT.semanticsOf(listOf(Semantics("role", "button"), Semantics("desc", "ok"))),
        )
    }

    // ── Средние ──

    @Test fun `С9 outerSize — порядок padding и size`() {
        assertEquals(100, SUT.outerSize(content = 80, chain = listOf(Size(100), Padding(10))))
        assertEquals(120, SUT.outerSize(content = 80, chain = listOf(Padding(10), Size(100))))
        assertEquals(70, SUT.outerSize(content = 50, chain = listOf(Padding(10))))
    }

    @Test fun `С10 contentSpace — место контенту`() {
        assertEquals(100, SUT.contentSpace(available = 200, chain = listOf(Padding(10), Size(100))))
        assertEquals(80, SUT.contentSpace(available = 200, chain = listOf(Size(100), Padding(10))))
    }

    @Test fun `С11 backgroundCoversPadding`() {
        assertTrue(SUT.backgroundCoversPadding(listOf(Background("red"), Padding(10))))
        assertFalse(SUT.backgroundCoversPadding(listOf(Padding(10), Background("red"))))
        assertFalse(SUT.backgroundCoversPadding(listOf(Padding(10))))
    }

    @Test fun `С12 clickIncludesPadding`() {
        assertTrue(SUT.clickIncludesPadding(listOf(Clickable, Padding(10))))
        assertFalse(SUT.clickIncludesPadding(listOf(Padding(10), Clickable)))
    }

    @Test fun `С13 foldOutNames — справа налево`() {
        assertEquals(
            listOf("clickable", "background", "padding"),
            SUT.foldOutNames(listOf(Padding(10), Background("red"), Clickable)),
        )
    }

    @Test fun `С14 nodeReused — по равенству элемента`() {
        assertTrue(SUT.nodeReused(Padding(10), Padding(10)))
        assertFalse(SUT.nodeReused(Padding(10), Padding(20)))
    }

    @Test fun `С15 changedNodePositions`() {
        assertEquals(
            listOf(1),
            SUT.changedNodePositions(listOf(Padding(10), Background("red")), listOf(Padding(10), Background("blue"))),
        )
    }

    // ── Сложные ──

    @Test fun `СЛ16 measureChain — вниз constraints, вверх размер`() {
        assertEquals(100 to 120, SUT.measureChain(200, 1000, listOf(Padding(10), Size(100))))
        assertEquals(80 to 100, SUT.measureChain(200, 1000, listOf(Size(100), Padding(10))))
        assertEquals(50 to 70, SUT.measureChain(200, 50, listOf(Padding(10))))
    }

    @Test fun `СЛ17 mergedSemantics — склейка потомков`() {
        val own = mapOf("role" to "button")
        val descendants = listOf(mapOf("text" to "A"), mapOf("desc" to "B"))
        assertEquals(
            mapOf("role" to "button", "text" to "A", "desc" to "B"),
            SUT.mergedSemantics(own, descendants, mergeDescendants = true),
        )
        assertEquals(own, SUT.mergedSemantics(own, descendants, mergeDescendants = false))
    }

    @Test fun `СЛ18 nodeLifecycle — update против detach+attach`() {
        assertEquals(
            listOf(0 to "update", 1 to "detach", 1 to "attach"),
            SUT.nodeLifecycle(listOf(Padding(10), Background("red")), listOf(Padding(20), Clickable)),
        )
        assertEquals(
            listOf(1 to "attach"),
            SUT.nodeLifecycle(listOf(Padding(10)), listOf(Padding(10), Clickable)),
        )
    }

    @Test fun `СЛ19 modifierStateCreations — Node против composed`() {
        assertEquals(1, SUT.modifierStateCreations(usesNode = true, recompositions = 5))
        assertEquals(5, SUT.modifierStateCreations(usesNode = false, recompositions = 5))
    }

    @Test fun `СЛ20 flattenChains — моноид с пустой единицей`() {
        assertEquals(
            listOf(Padding(10), Background("red"), Clickable),
            SUT.flattenChains(listOf(listOf(Padding(10)), emptyList(), listOf(Background("red"), Clickable))),
        )
    }
}
