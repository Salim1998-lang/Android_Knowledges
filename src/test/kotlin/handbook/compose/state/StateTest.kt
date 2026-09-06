package handbook.compose.state

import handbook.compose.state.solutions.StateSolutions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты темы 2 «Состояние». Проверяют механику хранения состояния на детерминированной модели:
 * remember и ключи, mutableStateOf без/с remember, rememberSaveable и переживание событий,
 * derivedStateOf (гашение шума), state hoisting (LCA, single source of truth, prop drilling).
 *
 * Чтобы гонять против эталона — замени `SUT` на [StateSolutions].
 */
private typealias SUT = StateTasks

class StateTest {

    // Дерево для задач про hoisting (ребёнок → родитель):
    //   app ─ screen ─ { header, form ─ { nameField, emailField } }
    private val parents = mapOf(
        "screen" to "app",
        "header" to "screen",
        "form" to "screen",
        "nameField" to "form",
        "emailField" to "form",
    )

    // ── Лёгкие ──

    @Test fun `Л1 keysUnchanged`() {
        assertTrue(SUT.keysUnchanged(listOf(1, "a"), listOf(1, "a")))
        assertFalse(SUT.keysUnchanged(listOf(1), listOf(2)))
    }

    @Test fun `Л2 rememberValue — переиспользует при равных ключах, не зовёт compute`() {
        var calls = 0
        val first = SUT.rememberValue(prev = null, keys = listOf("k")) { calls++; "value" }
        assertEquals(Slot(listOf("k"), "value"), first)
        assertEquals(1, calls)

        val reused = SUT.rememberValue(prev = first, keys = listOf("k")) { calls++; "other" }
        assertEquals(first, reused)
        assertEquals(1, calls) // compute НЕ вызвана повторно

        val recomputed = SUT.rememberValue(prev = first, keys = listOf("k2")) { calls++; "fresh" }
        assertEquals(Slot(listOf("k2"), "fresh"), recomputed)
        assertEquals(2, calls)
    }

    @Test fun `Л3 stateValueAfterRecompose — без remember значение сбрасывается`() {
        assertEquals(42, SUT.stateValueAfterRecompose(remembered = true, initial = 0, current = 42))
        assertEquals(0, SUT.stateValueAfterRecompose(remembered = false, initial = 0, current = 42))
    }

    @Test fun `Л4 rememberSurvives — только рекомпозицию`() {
        assertTrue(SUT.rememberSurvives(SurvivalEvent.RECOMPOSE))
        assertFalse(SUT.rememberSurvives(SurvivalEvent.CONFIG_CHANGE))
        assertFalse(SUT.rememberSurvives(SurvivalEvent.PROCESS_DEATH))
    }

    @Test fun `Л5 rememberSaveableSurvives — все три события`() {
        assertTrue(SUT.rememberSaveableSurvives(SurvivalEvent.RECOMPOSE))
        assertTrue(SUT.rememberSaveableSurvives(SurvivalEvent.CONFIG_CHANGE))
        assertTrue(SUT.rememberSaveableSurvives(SurvivalEvent.PROCESS_DEATH))
    }

    @Test fun `Л6 saveableCanStore — авто-сохраняемый или свой Saver`() {
        assertTrue(SUT.saveableCanStore(autoSaveable = true, hasSaver = false))
        assertTrue(SUT.saveableCanStore(autoSaveable = false, hasSaver = true))
        assertFalse(SUT.saveableCanStore(autoSaveable = false, hasSaver = false))
    }

    @Test fun `Л7 derivedNotifies — только при смене результата`() {
        assertTrue(SUT.derivedNotifies(oldResult = false, newResult = true))
        assertFalse(SUT.derivedNotifies(oldResult = true, newResult = true))
    }

    @Test fun `Л8 recomputeTimes`() {
        assertEquals(3, SUT.recomputeTimes(listOf(listOf(1), listOf(1), listOf(2), listOf(2), listOf(1))))
        assertEquals(1, SUT.recomputeTimes(listOf(listOf("a"), listOf("a"))))
    }

    // ── Средние ──

    @Test fun `С9 rememberedValues — пересчёт при смене ключа, иначе переиспользование`() {
        var calls = 0
        val values = SUT.rememberedValues(listOf(listOf(1), listOf(1), listOf(2))) { keys ->
            calls++
            (keys[0] as Int) * 10
        }
        assertEquals(listOf(10, 10, 20), values)
        assertEquals(2, calls) // compute только на 1-й и при смене ключа
    }

    @Test fun `С10 derivedNotifications — считает смены результата`() {
        assertEquals(2, SUT.derivedNotifications(listOf(false, false, true, true, false)))
    }

    @Test fun `С11 plainVsDerived — derived гасит шум входа`() {
        val inputs = listOf(0, 5, 12, 30)
        val (plain, derived) = SUT.plainVsDerived(inputs) { (it as Int) > 0 }
        assertEquals(3, plain)   // вход менялся 3 раза
        assertEquals(1, derived) // результат (>0) сменился один раз
    }

    @Test fun `С12 lowestCommonAncestor`() {
        assertEquals("form", SUT.lowestCommonAncestor(parents, "nameField", "emailField"))
        assertEquals("screen", SUT.lowestCommonAncestor(parents, "header", "nameField"))
    }

    @Test fun `С13 hoistTarget — LCA всех пользователей`() {
        assertEquals("form", SUT.hoistTarget(parents, listOf("nameField", "emailField")))
        assertEquals("screen", SUT.hoistTarget(parents, listOf("header", "nameField", "emailField")))
    }

    @Test fun `С14 isStateless`() {
        assertTrue(SUT.isStateless(hasInternalState = false, exposesValue = true, exposesCallback = true))
        assertFalse(SUT.isStateless(hasInternalState = true, exposesValue = true, exposesCallback = true))
        assertFalse(SUT.isStateless(hasInternalState = false, exposesValue = true, exposesCallback = false))
    }

    @Test fun `С15 rememberIsUseless — ключ меняется каждый раз`() {
        assertTrue(SUT.rememberIsUseless(listOf(listOf(1), listOf(2), listOf(3))))
        assertFalse(SUT.rememberIsUseless(listOf(listOf(1), listOf(1), listOf(2))))
        assertFalse(SUT.rememberIsUseless(listOf(listOf(1))))
    }

    // ── Сложные ──

    @Test fun `СЛ16 restoreAfterProcessDeath`() {
        val saved = mapOf<String, Any?>("count" to 5)
        assertEquals(5, SUT.restoreAfterProcessDeath(saved, key = "count", default = 0))
        assertEquals(0, SUT.restoreAfterProcessDeath(saved, key = "missing", default = 0))
    }

    @Test fun `СЛ17 valueAfterEvents — remember слетает на повороте, saveable нет`() {
        val recompose = listOf(SurvivalEvent.RECOMPOSE, SurvivalEvent.RECOMPOSE)
        val rotate = listOf(SurvivalEvent.RECOMPOSE, SurvivalEvent.CONFIG_CHANGE)

        assertEquals("hi", SUT.valueAfterEvents(saveable = false, initial = "", written = "hi", events = recompose))
        assertEquals("", SUT.valueAfterEvents(saveable = false, initial = "", written = "hi", events = rotate))
        assertEquals("hi", SUT.valueAfterEvents(saveable = true, initial = "", written = "hi", events = rotate))
        assertEquals(
            "hi",
            SUT.valueAfterEvents(
                saveable = true,
                initial = "",
                written = "hi",
                events = listOf(SurvivalEvent.CONFIG_CHANGE, SurvivalEvent.PROCESS_DEATH),
            ),
        )
    }

    @Test fun `СЛ18 derivedStats — пересчёты и инвалидации отдельно`() {
        val (recomputes, notifications) = SUT.derivedStats(
            inputChanged = listOf(true, false, true, true),
            resultChanged = listOf(true, false, false, true),
        )
        assertEquals(3, recomputes)
        assertEquals(2, notifications)
    }

    @Test fun `СЛ19 overHoisted — состояние выше LCA пользователей`() {
        val users = listOf("nameField", "emailField") // LCA = form
        assertTrue(SUT.overHoisted(parents, users, host = "screen")) // выше нужного
        assertTrue(SUT.overHoisted(parents, users, host = "app"))    // ещё выше
        assertFalse(SUT.overHoisted(parents, users, host = "form"))  // оптимально
    }

    @Test fun `СЛ20 propDrillingDepth`() {
        assertEquals(3, SUT.propDrillingDepth(parents, host = "app", user = "nameField"))
        assertEquals(1, SUT.propDrillingDepth(parents, host = "form", user = "nameField"))
        assertEquals(0, SUT.propDrillingDepth(parents, host = "nameField", user = "nameField"))
    }
}
