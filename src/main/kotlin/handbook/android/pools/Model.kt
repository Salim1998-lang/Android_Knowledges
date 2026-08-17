package handbook.android.pools

import java.util.concurrent.CountDownLatch

/**
 * Тема 3 работает с НАСТОЯЩИМ `java.util.concurrent.ThreadPoolExecutor` — именно его настраивают под
 * пул в Android (и он же внутри `AsyncTask.THREAD_POOL_EXECUTOR`, `Dispatchers.IO` и т.п.). Никакой
 * учебной подмены: ты конфигурируешь реальный пул и наблюдаешь его поведение.
 *
 * Единственный помощник — [Gate]: «блокирующая задача», которая сообщает о своём СТАРТЕ и ждёт
 * разрешения продолжить. Он нужен, чтобы тесты были ДЕТЕРМИНИРОВАННЫМИ: мы можем занять ровно N
 * потоков пула, дождаться, что они реально стартовали, прочитать состояние (`poolSize`, отказы),
 * а затем отпустить их. В боевом коде такого гейта нет — это только «стенд» для наблюдения за пулом.
 */
class Gate(startsExpected: Int) {

    private val started = CountDownLatch(startsExpected)
    private val released = CountDownLatch(1)

    /**
     * Задача, которая: 1) отмечает свой старт, 2) блокируется до [release], 3) выполняет [after].
     * Если поток прерван (`shutdownNow`) во время ожидания — корректно выходит.
     */
    fun task(after: () -> Unit = {}): Runnable = Runnable {
        started.countDown()
        try {
            released.await()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return@Runnable
        }
        after()
    }

    /** Дождаться, что ожидаемое число задач реально начали выполняться (заняли потоки пула). */
    fun awaitStarted() {
        started.await()
    }

    /** Отпустить все заблокированные задачи. */
    fun release() {
        released.countDown()
    }
}
