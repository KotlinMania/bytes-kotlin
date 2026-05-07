// port-lint: source src/loom.rs
package io.github.kotlinmania.bytes

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal object Sync {
    internal object Atomic {
        internal enum class Ordering {
            Relaxed,
            Release,
            Acquire,
            AcqRel,
            SeqCst,
        }

        private fun observeOrdering(ordering: Ordering) {
            when (ordering) {
                Ordering.Relaxed,
                Ordering.Release,
                Ordering.Acquire,
                Ordering.AcqRel,
                Ordering.SeqCst,
                -> Unit
            }
        }

        @OptIn(ExperimentalAtomicApi::class)
        internal class AtomicPtr<T>(
            initial: T? = null,
        ) : AtomicMut<T> {
            private val value = AtomicReference(initial)

            internal fun load(ordering: Ordering): T? {
                observeOrdering(ordering)
                return value.load()
            }

            internal fun store(value: T?, ordering: Ordering) {
                observeOrdering(ordering)
                this.value.store(value)
            }

            override fun <R> withMut(f: (AtomicPtr<T>) -> R): R = f(this)
        }

        @OptIn(ExperimentalAtomicApi::class)
        internal class AtomicUsize(
            initial: Long = 0,
        ) {
            private val value = AtomicLong(initial)

            internal fun load(ordering: Ordering): Long {
                observeOrdering(ordering)
                return value.load()
            }

            internal fun store(value: Long, ordering: Ordering) {
                observeOrdering(ordering)
                this.value.store(value)
            }
        }

        internal interface AtomicMut<T> {
            fun <R> withMut(f: (AtomicPtr<T>) -> R): R
        }
    }
}

// The upstream test-with-loom configuration replaces these atomic names with loom's atomic
// implementations and keeps the mutable atomic marker trait empty. Kotlin keeps one shared
// implementation for all targets.
