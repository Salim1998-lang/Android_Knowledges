package handbook.channels

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Тесты темы 6. Функциональные тесты проверяют содержимое каналов. Группа «корутинные свойства»
 * проверяет то, что делает каналы корутинным примитивом: BACKPRESSURE (rendezvous-канал двигает
 * элементы по одному, продюсер ждёт консьюмера — через виртуальное время) и РАННЮЮ ОТМЕНУ upstream
 * (`takeChannel` отменяет источник, не дожидаясь остальных элементов).
 */
class ChannelsTest {

    // ── Лёгкие ──

    @Test fun `Л1+Л2 produceNumbers + drain`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(1, 2, 3, 4, 5), drain(produceNumbers(5))) }
    }

    @Test fun `Л3 produceRange`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(3, 4, 5), drain(produceRange(3, 5))) }
    }

    @Test fun `Л4 produceFrom`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf("a", "b"), drain(produceFrom(listOf("a", "b")))) }
    }

    @Test fun `Л5 firstTwo`() = runTest {
        with(ChannelsTasks) {
            val ch = produceNumbers(5)
            assertEquals(1 to 2, firstTwo(ch))
            ch.cancel() // прочитали только два — отменяем канал (иначе продюсер повиснет)
        }
    }

    @Test fun `Л6 sumChannel`() = runTest {
        with(ChannelsTasks) { assertEquals(15, sumChannel(produceNumbers(5))) }
    }

    @Test fun `Л7 countChannel`() = runTest {
        with(ChannelsTasks) { assertEquals(5, countChannel(produceNumbers(5))) }
    }

    @Test fun `Л8 produceEvens`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(2, 4, 6, 8), drain(produceEvens(4))) }
    }

    // ── Средние ──

    @Test fun `С9 squares пайплайн`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(1, 4, 9, 16), drain(squares(produceNumbers(4)))) }
    }

    @Test fun `С10 mapChannel`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(10, 20, 30), drain(mapChannel(produceNumbers(3)) { it * 10 })) }
    }

    @Test fun `С11 filterChannel`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(2, 4, 6), drain(filterChannel(produceNumbers(6)) { it % 2 == 0 })) }
    }

    @Test fun `С12 merge fan-in`() = runTest {
        with(ChannelsTasks) {
            val merged = drain(merge(produceRange(1, 3), produceRange(4, 6)))
            assertEquals(setOf(1, 2, 3, 4, 5, 6), merged.toSet())
        }
    }

    @Test fun `С13 takeChannel`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(1, 2, 3), drain(takeChannel(produceNumbers(10), 3))) }
    }

    @Test fun `С14 receiveCatchingOrNull`() = runTest {
        with(ChannelsTasks) {
            val ch = produceNumbers(1)
            assertEquals(1, receiveCatchingOrNull(ch))
            assertNull(receiveCatchingOrNull(ch)) // канал закрыт
        }
    }

    @Test fun `С15 zipChannels`() = runTest {
        with(ChannelsTasks) {
            val zipped = drain(zipChannels(produceNumbers(3), produceFrom(listOf("a", "b", "c"))) { n, s -> "$n$s" })
            assertEquals(listOf("1a", "2b", "3c"), zipped)
        }
    }

    // ── Сложные ──

    @Test fun `СЛ16 fanOutSum`() = runTest {
        with(ChannelsTasks) { assertEquals((1..100).sum(), fanOutSum(produceNumbers(100), workers = 4)) }
    }

    @Test fun `СЛ17 pipelineSquaredPlusOne`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(2, 5, 10, 17), drain(pipelineSquaredPlusOne(4))) }
    }

    @Test fun `СЛ18 fanInMerge`() = runTest {
        with(ChannelsTasks) {
            val merged = drain(fanInMerge(listOf(produceRange(1, 3), produceRange(4, 6), produceRange(7, 9))))
            assertEquals((1..9).toSet(), merged.toSet())
        }
    }

    @Test fun `СЛ19 distributeAndSum`() = runTest {
        with(ChannelsTasks) {
            val partials = distributeAndSum(produceNumbers(100), workers = 4)
            assertEquals(4, partials.size)
            assertEquals((1..100).sum(), partials.sum())
        }
    }

    @Test fun `СЛ20 selectFirst`() = runTest {
        val a = Channel<String>(1)
        val b = Channel<String>(1)
        a.send("from-a")
        assertEquals("from-a", ChannelsTasks.selectFirst(a, b))
        a.close(); b.close()
    }

    // ── Корутинные свойства каналов ──

    @Test fun `КС1 squares - потоковый пайплайн с backpressure`() = runTest {
        // Источник шлёт по одному с задержкой; rendezvous → стадия squares тянет элементы по мере готовности.
        val upstream = produce { for (i in 1..3) { delay(10); send(i) } }
        val t0 = currentTime
        with(ChannelsTasks) {
            assertEquals(listOf(1, 4, 9), drain(squares(upstream)))
        }
        assertEquals(30, currentTime - t0, "3 элемента по delay(10) проходят пайплайн последовательно")
    }

    @Test fun `КС2 takeChannel отменяет продюсер до его оставшихся задержек`() = runTest {
        val upstream = produce { for (i in 1..5) { delay(10); send(i) } }
        val t0 = currentTime
        with(ChannelsTasks) {
            assertEquals(listOf(1, 2), drain(takeChannel(upstream, 2)))
        }
        assertEquals(20, currentTime - t0, "взяли 2 (2×delay 10) и отменили источник — не ждём остальные 3")
    }
}
