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
 * Тесты темы 8. Функциональные тесты проверяют содержимое каналов. Группа «корутинные свойства»
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
        with(ChannelsTasks) {
            assertEquals(listOf(3, 4, 5), drain(produceRange(3, 5)))
            assertEquals(listOf(7), drain(produceRange(7, 7)))                 // единичный диапазон
            assertEquals(emptyList<Int>(), drain(produceRange(5, 3)))          // from > to → пусто
        }
    }

    @Test fun `Л4 produceFrom`() = runTest {
        with(ChannelsTasks) {
            assertEquals(listOf("a", "b"), drain(produceFrom(listOf("a", "b"))))
            assertEquals(emptyList<String>(), drain(produceFrom(emptyList<String>())))
        }
    }

    @Test fun `Л5 firstTwo`() = runTest {
        with(ChannelsTasks) {
            val ch = produceNumbers(5)
            assertEquals(1 to 2, firstTwo(ch))
            ch.cancel() // прочитали только два — отменяем канал (иначе продюсер повиснет)
        }
    }

    @Test fun `Л6 sumChannel`() = runTest {
        with(ChannelsTasks) {
            assertEquals(15, sumChannel(produceNumbers(5)))
            assertEquals(0, sumChannel(produceRange(1, 0)))   // пустой канал → 0
        }
    }

    @Test fun `Л7 countChannel`() = runTest {
        with(ChannelsTasks) {
            assertEquals(5, countChannel(produceNumbers(5)))
            assertEquals(0, countChannel(produceRange(1, 0))) // пустой канал → 0
        }
    }

    @Test fun `Л8 produceEvens`() = runTest {
        with(ChannelsTasks) {
            assertEquals(listOf(2, 4, 6, 8), drain(produceEvens(4)))
            assertEquals(listOf(2), drain(produceEvens(1)))   // граница снизу
        }
    }

    // ── Средние ──

    @Test fun `С9 squares пайплайн`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(1, 4, 9, 16), drain(squares(produceNumbers(4)))) }
    }

    @Test fun `С10 mapChannel`() = runTest {
        with(ChannelsTasks) {
            assertEquals(listOf(10, 20, 30), drain(mapChannel(produceNumbers(3)) { it * 10 }))
            assertEquals(listOf("1!", "2!"), drain(mapChannel(produceNumbers(2)) { "$it!" })) // смена типа, не хардкод
        }
    }

    @Test fun `С11 filterChannel`() = runTest {
        with(ChannelsTasks) {
            assertEquals(listOf(2, 4, 6), drain(filterChannel(produceNumbers(6)) { it % 2 == 0 }))
            assertEquals(emptyList<Int>(), drain(filterChannel(produceNumbers(5)) { it > 100 })) // ни один не прошёл
        }
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
            // длина = min: короткий второй канал обрывает zip
            val longA = produceNumbers(5)
            val shorter = drain(zipChannels(longA, produceFrom(listOf("x", "y"))) { n, s -> "$n$s" })
            assertEquals(listOf("1x", "2y"), shorter)
            longA.cancel() // zip не отменяет входы — прибираем недоеденный источник, иначе продюсер повиснет
        }
    }

    // ── Сложные ──

    @Test fun `СЛ16 fanOutSum`() = runTest {
        with(ChannelsTasks) {
            assertEquals((1..100).sum(), fanOutSum(produceNumbers(100), workers = 4))
            assertEquals((1..100).sum(), fanOutSum(produceNumbers(100), workers = 1)) // один воркер
            assertEquals((1..50).sum(), fanOutSum(produceNumbers(50), workers = 8))   // воркеров больше, чем удобно
        }
    }

    @Test fun `СЛ17 pipelineSquaredPlusOne`() = runTest {
        with(ChannelsTasks) { assertEquals(listOf(2, 5, 10, 17), drain(pipelineSquaredPlusOne(4))) }
    }

    @Test fun `СЛ18 fanInMerge`() = runTest {
        with(ChannelsTasks) {
            val merged = drain(fanInMerge(listOf(produceRange(1, 3), produceRange(4, 6), produceRange(7, 9))))
            assertEquals((1..9).toSet(), merged.toSet())
            assertEquals(9, merged.size, "ничего не потеряно и не продублировано")
            // единственный источник
            assertEquals(listOf(1, 2), drain(fanInMerge(listOf(produceRange(1, 2)))).sorted())
            // пустой список источников → пусто (и не виснет)
            assertEquals(emptyList<Int>(), drain(fanInMerge<Int>(emptyList())))
        }
    }

    @Test fun `СЛ19 distributeAndSum`() = runTest {
        with(ChannelsTasks) {
            val partials = distributeAndSum(produceNumbers(100), workers = 4)
            assertEquals(4, partials.size)
            assertEquals((1..100).sum(), partials.sum(), "каждый элемент учтён ровно раз")
            // один воркер получает всё
            val single = distributeAndSum(produceNumbers(10), workers = 1)
            assertEquals(listOf((1..10).sum()), single)
        }
    }

    @Test fun `СЛ20 selectFirst`() = runTest {
        with(ChannelsTasks) {
            val a1 = Channel<String>(1); val b1 = Channel<String>(1)
            a1.send("from-a")
            assertEquals("from-a", selectFirst(a1, b1)) // готов только a
            a1.close(); b1.close()

            val a2 = Channel<String>(1); val b2 = Channel<String>(1)
            b2.send("from-b")
            assertEquals("from-b", selectFirst(a2, b2)) // готов только b
            a2.close(); b2.close()
        }
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
