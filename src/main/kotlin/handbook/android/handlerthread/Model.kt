package handbook.android.handlerthread

import java.util.concurrent.LinkedBlockingQueue

/**
 * Учебная мини-версия [android.os.HandlerThread] — НАСТОЯЩИЙ поток со своим циклом событий,
 * который последовательно (FIFO) выполняет присланные задачи. Соответствие реальному Android:
 *
 * | Здесь                | Android                                        |
 * |----------------------|------------------------------------------------|
 * | `start()`            | `HandlerThread.start()` — создаёт `Looper`     |
 * | `post { }`           | `Handler(ht.looper).post { }`                  |
 * | `quit()`             | `Looper.quit()` — бросить ещё не выполненное   |
 * | `quitSafely()`       | `Looper.quitSafely()` — доработать очередь, стоп|
 * | `join()`             | дождаться завершения потока                     |
 * | `isOnThisThread()`   | `Looper.myLooper() == ht.looper`               |
 *
 * Упрощения относительно темы 1: без времени/`postDelayed` (тут важен сам поток, а не `when`),
 * сообщение = `Runnable`, очередь строго FIFO. Ключевые гарантии — те же, что у настоящего
 * `HandlerThread`:
 *  • **сериализация**: все задачи одного потока идут по одной, по порядку постановки;
 *  • **confinement**: они выполняются на ОДНОМ и том же потоке (не на потоке отправителя);
 *  • **happens-before**: запись в задаче N видна задаче N+1 без `volatile`/локов;
 *  • после `quit`/`quitSafely` [post] отвергает новые задачи (возвращает `false`).
 */
class LooperThread(val threadName: String) {

    private sealed interface Cmd
    private class Task(val block: Runnable) : Cmd
    private object QuitNow : Cmd     // бросить ещё не выполненные (очередь уже очищена в quit())
    private object QuitSafe : Cmd    // доработать всё, что стоит в очереди до этого маркера

    private val queue = LinkedBlockingQueue<Cmd>()

    @Volatile private var accepting = false
    @Volatile private var worker: Thread? = null

    /** Запустить поток и его цикл. Повторный запуск запрещён (как у обычного `Thread`). */
    fun start() {
        check(worker == null) { "$threadName уже запущен" }
        accepting = true
        Thread({ loop() }, threadName).also { worker = it }.start()
    }

    /**
     * Поставить задачу в очередь. Возвращает `true`, если принято; `false`, если поток ещё не
     * запущен или уже завершается (аналог `Handler.post`, вернувшего `false` после `quit`).
     */
    fun post(block: Runnable): Boolean {
        if (!accepting) return false
        queue.put(Task(block))
        return true
    }

    /** Немедленно завершить: ещё не выполненные задачи ОТБРАСЫВАЮТСЯ. */
    fun quit() {
        if (!accepting) return
        accepting = false
        queue.clear()            // выкинуть всё ожидающее
        queue.put(QuitNow)       // разбудить цикл, чтобы он вышел
    }

    /** Мягко завершить: доработать уже поставленные задачи, затем остановить цикл. */
    fun quitSafely() {
        if (!accepting) return
        accepting = false
        queue.put(QuitSafe)      // маркер в хвост — всё, что до него, отработает (FIFO)
    }

    /** Дождаться завершения потока (барьер happens-before: после join видны все записи задач). */
    fun join() {
        worker?.join()
    }

    /** Выполняется ли текущий код на потоке этого `LooperThread` (проверка confinement). */
    fun isOnThisThread(): Boolean = Thread.currentThread() === worker

    private fun loop() {
        while (true) {
            when (val cmd = queue.take()) {
                is Task -> cmd.block.run()
                QuitSafe -> return   // всё до маркера уже выполнено
                QuitNow -> return    // очередь очищена в quit()
            }
        }
    }
}
