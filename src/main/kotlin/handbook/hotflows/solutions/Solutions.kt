package handbook.hotflows.solutions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Эталонные решения темы 7. Подсмотри, если застрял с [handbook.hotflows.HotFlowsTasks]. */
object HotFlowsSolutions {

    // ── Лёгкие ──

    fun mutableIntState(initial: Int): MutableStateFlow<Int> = MutableStateFlow(initial)

    fun currentValue(state: StateFlow<Int>): Int = state.value

    fun setValue(state: MutableStateFlow<Int>, newValue: Int) {
        state.value = newValue
    }

    fun incrementState(state: MutableStateFlow<Int>) {
        state.update { it + 1 }
    }

    fun <T> asReadOnly(state: MutableStateFlow<T>): StateFlow<T> = state.asStateFlow()

    fun compareAndSetValue(state: MutableStateFlow<Int>, expect: Int, update: Int): Boolean =
        state.compareAndSet(expect, update)

    fun sharedWithReplay(replay: Int): MutableSharedFlow<Int> = MutableSharedFlow(replay = replay)

    suspend fun emitEach(sharedFlow: MutableSharedFlow<Int>, values: List<Int>) {
        for (v in values) sharedFlow.emit(v)
    }

    // ── Средние ──

    fun lastTwoReplayed(values: List<Int>): List<Int> {
        val sf = MutableSharedFlow<Int>(replay = 2)
        values.forEach { sf.tryEmit(it) }
        return sf.replayCache
    }

    fun dropOldestKeepsLatest(values: List<Int>): List<Int> {
        val sf = MutableSharedFlow<Int>(replay = 2, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        values.forEach { sf.tryEmit(it) }
        return sf.replayCache
    }

    fun subscriberCount(sharedFlow: MutableSharedFlow<Int>): Int = sharedFlow.subscriptionCount.value

    suspend fun firstValue(flow: SharedFlow<Int>): Int = flow.first()

    fun stateFromFlow(scope: CoroutineScope, source: Flow<Int>, initial: Int): StateFlow<Int> =
        source.stateIn(scope, SharingStarted.Eagerly, initial)

    fun shareFromFlow(scope: CoroutineScope, source: Flow<Int>, replay: Int): SharedFlow<Int> =
        source.shareIn(scope, SharingStarted.Eagerly, replay)

    fun sumState(scope: CoroutineScope, a: StateFlow<Int>, b: StateFlow<Int>): StateFlow<Int> =
        combine(a, b) { x, y -> x + y }.stateIn(scope, SharingStarted.Eagerly, a.value + b.value)

    // ── Сложные ──

    fun mirror(scope: CoroutineScope, source: StateFlow<Int>, sink: MutableList<Int>): Job =
        scope.launch { source.collect { sink.add(it) } }

    fun runningTotalState(scope: CoroutineScope, source: Flow<Int>): StateFlow<Int> =
        source.runningReduce { acc, x -> acc + x }.stateIn(scope, SharingStarted.Eagerly, 0)

    suspend fun takeN(source: SharedFlow<Int>, n: Int): List<Int> = source.take(n).toList()

    fun <R> mapState(scope: CoroutineScope, source: StateFlow<Int>, f: (Int) -> R): StateFlow<R> =
        source.map { f(it) }.stateIn(scope, SharingStarted.Eagerly, f(source.value))

    fun whileSubscribedShare(scope: CoroutineScope, source: Flow<Int>, replay: Int): SharedFlow<Int> =
        source.shareIn(scope, SharingStarted.WhileSubscribed(), replay)
}
