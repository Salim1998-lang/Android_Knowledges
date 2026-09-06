package handbook.compose.modifiers.solutions

import handbook.compose.modifiers.Element

/** Эталонные решения темы 7. Подсмотри, если застрял с [handbook.compose.modifiers.ModifiersTasks]. */
object ModifiersSolutions {

    // ── Лёгкие ──

    fun then(a: List<Element>, b: List<Element>): List<Element> =
        a + b

    fun isIdentity(chain: List<Element>): Boolean =
        chain.isEmpty()

    fun elementCount(chain: List<Element>): Int =
        chain.size

    fun foldNames(chain: List<Element>): List<String> =
        chain.map { it.name }

    fun sameChain(a: List<Element>, b: List<Element>): Boolean =
        a == b

    fun totalPadding(chain: List<Element>): Int =
        chain.filterIsInstance<Element.Padding>().sumOf { it.amount }

    fun hasClickable(chain: List<Element>): Boolean =
        chain.any { it is Element.Clickable }

    fun semanticsOf(chain: List<Element>): Map<String, String> =
        chain.filterIsInstance<Element.Semantics>().associate { it.key to it.value }

    // ── Средние ──

    fun outerSize(content: Int, chain: List<Element>): Int {
        var size = content
        for (el in chain.reversed()) {
            size = when (el) {
                is Element.Padding -> size + 2 * el.amount
                is Element.Size -> el.value
                else -> size
            }
        }
        return size
    }

    fun contentSpace(available: Int, chain: List<Element>): Int {
        var avail = available
        for (el in chain) {
            avail = when (el) {
                is Element.Padding -> (avail - 2 * el.amount).coerceAtLeast(0)
                is Element.Size -> minOf(el.value, avail)
                else -> avail
            }
        }
        return avail
    }

    fun backgroundCoversPadding(chain: List<Element>): Boolean {
        val bg = chain.indexOfFirst { it is Element.Background }
        val pad = chain.indexOfFirst { it is Element.Padding }
        return bg != -1 && pad != -1 && bg < pad
    }

    fun clickIncludesPadding(chain: List<Element>): Boolean {
        val click = chain.indexOfFirst { it is Element.Clickable }
        val pad = chain.indexOfFirst { it is Element.Padding }
        return click != -1 && pad != -1 && click < pad
    }

    fun foldOutNames(chain: List<Element>): List<String> =
        chain.reversed().map { it.name }

    fun nodeReused(old: Element, new: Element): Boolean =
        old == new

    fun changedNodePositions(old: List<Element>, new: List<Element>): List<Int> {
        val common = minOf(old.size, new.size)
        return (0 until common).filter { old[it] != new[it] }
    }

    // ── Сложные ──

    fun measureChain(available: Int, contentDesired: Int, chain: List<Element>): Pair<Int, Int> {
        val space = contentSpace(available, chain)
        val contentActual = minOf(contentDesired, space)
        return contentActual to outerSize(contentActual, chain)
    }

    fun mergedSemantics(own: Map<String, String>, descendants: List<Map<String, String>>, mergeDescendants: Boolean): Map<String, String> {
        if (!mergeDescendants) return own
        val result = linkedMapOf<String, String>()
        for (d in descendants) result.putAll(d)
        result.putAll(own)
        return result
    }

    fun nodeLifecycle(old: List<Element>, new: List<Element>): List<Pair<Int, String>> {
        val out = mutableListOf<Pair<Int, String>>()
        for (i in 0 until maxOf(old.size, new.size)) {
            val o = old.getOrNull(i)
            val n = new.getOrNull(i)
            when {
                o == null -> out += i to "attach"
                n == null -> out += i to "detach"
                o == n -> Unit // переиспользование
                o.name == n.name -> out += i to "update"
                else -> {
                    out += i to "detach"
                    out += i to "attach"
                }
            }
        }
        return out
    }

    fun modifierStateCreations(usesNode: Boolean, recompositions: Int): Int =
        if (usesNode) 1 else recompositions

    fun flattenChains(chains: List<List<Element>>): List<Element> =
        chains.fold(emptyList()) { acc, chain -> acc + chain }
}
