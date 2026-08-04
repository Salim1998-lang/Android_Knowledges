package handbook.channels.solutions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/** Эталонные решения темы 6. Подсмотри, если застрял с [handbook.channels.ChannelsTasks]. */
object ChannelsSolutions {

    fun CoroutineScope.produceNumbers(n: Int): ReceiveChannel<Int> = produce { for (i in 1..n) send(i) }

    suspend fun <T> drain(channel: ReceiveChannel<T>): List<T> {
        val out = mutableListOf<T>()
        channel.consumeEach { out.add(it) }
        return out
    }

    fun CoroutineScope.produceRange(from: Int, to: Int): ReceiveChannel<Int> =
        produce { for (i in from..to) send(i) }

    fun <T> CoroutineScope.produceFrom(list: List<T>): ReceiveChannel<T> =
        produce { for (item in list) send(item) }

    suspend fun <T> firstTwo(channel: ReceiveChannel<T>): Pair<T, T> {
        val x = channel.receive()
        val y = channel.receive()
        return x to y
    }

    suspend fun sumChannel(channel: ReceiveChannel<Int>): Int {
        var sum = 0
        channel.consumeEach { sum += it }
        return sum
    }

    suspend fun <T> countChannel(channel: ReceiveChannel<T>): Int {
        var count = 0
        channel.consumeEach { count++ }
        return count
    }

    fun CoroutineScope.produceEvens(n: Int): ReceiveChannel<Int> = produce { for (i in 1..n) send(2 * i) }

    fun CoroutineScope.squares(input: ReceiveChannel<Int>): ReceiveChannel<Int> =
        produce { for (x in input) send(x * x) }

    fun <T, R> CoroutineScope.mapChannel(input: ReceiveChannel<T>, transform: (T) -> R): ReceiveChannel<R> =
        produce { for (x in input) send(transform(x)) }

    fun <T> CoroutineScope.filterChannel(input: ReceiveChannel<T>, predicate: (T) -> Boolean): ReceiveChannel<T> =
        produce { for (x in input) if (predicate(x)) send(x) }

    fun <T> CoroutineScope.merge(a: ReceiveChannel<T>, b: ReceiveChannel<T>): ReceiveChannel<T> = produce {
        launch { for (x in a) send(x) }
        launch { for (x in b) send(x) }
    }

    fun <T> CoroutineScope.takeChannel(input: ReceiveChannel<T>, n: Int): ReceiveChannel<T> = produce {
        if (n <= 0) {
            input.cancel()
            return@produce
        }
        var c = 0
        for (x in input) {
            send(x)
            if (++c >= n) break
        }
        input.cancel()
    }

    suspend fun <T> receiveCatchingOrNull(channel: ReceiveChannel<T>): T? =
        channel.receiveCatching().getOrNull()

    fun <A, B, R> CoroutineScope.zipChannels(
        a: ReceiveChannel<A>, b: ReceiveChannel<B>, combine: (A, B) -> R,
    ): ReceiveChannel<R> = produce {
        while (true) {
            val x = a.receiveCatching().getOrNull() ?: break
            val y = b.receiveCatching().getOrNull() ?: break
            send(combine(x, y))
        }
    }

    suspend fun fanOutSum(numbers: ReceiveChannel<Int>, workers: Int): Int = coroutineScope {
        require(workers >= 1)
        (1..workers).map {
            async {
                var partial = 0
                for (x in numbers) partial += x
                partial
            }
        }.awaitAll().sum()
    }

    fun CoroutineScope.pipelineSquaredPlusOne(n: Int): ReceiveChannel<Int> {
        val nums = produceNumbers(n)
        val sq = squares(nums)
        return mapChannel(sq) { it + 1 }
    }

    fun <T> CoroutineScope.fanInMerge(sources: List<ReceiveChannel<T>>): ReceiveChannel<T> = produce {
        for (src in sources) launch { for (x in src) send(x) }
    }

    suspend fun distributeAndSum(numbers: ReceiveChannel<Int>, workers: Int): List<Int> = coroutineScope {
        require(workers >= 1)
        (1..workers).map {
            async {
                var partial = 0
                for (x in numbers) partial += x
                partial
            }
        }.awaitAll()
    }

    suspend fun <T> selectFirst(a: ReceiveChannel<T>, b: ReceiveChannel<T>): T = select {
        a.onReceive { it }
        b.onReceive { it }
    }
}
