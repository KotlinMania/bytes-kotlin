package io.github.kotlinmania.bytes

internal fun abort(): Nothing {
    class Abort {
        fun drop(): Nothing = throw IllegalStateException("abort")
    }

    val aborter = Abort()
    try {
        throw IllegalStateException("abort")
    } finally {
        aborter.drop()
    }
}

internal fun saturatingSubUsizeU64(a: Int, b: ULong): Int {
    if (b > Int.MAX_VALUE.toULong()) {
        return 0
    }
    return (a - b.toInt()).coerceAtLeast(0)
}

internal fun minU64Usize(a: ULong, b: Int): Int =
    if (a > Int.MAX_VALUE.toULong()) {
        b
    } else {
        minOf(a.toInt(), b)
    }
