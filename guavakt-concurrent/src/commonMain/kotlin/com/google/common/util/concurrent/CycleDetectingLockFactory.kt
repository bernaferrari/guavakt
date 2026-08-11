package dev.guavakt.util.concurrent

/**
 * Guava CycleDetectingLockFactory — creates locks that detect ordering cycles.
 * Tracks lock acquisition order per thread and delegates exclusion to a real JVM lock.
 */
class CycleDetectingLockFactory private constructor(
    private val policy: Policy,
) {
    enum class Policies : Policy {
        THROW {
            override fun handlePotentialDeadlock(exception: PotentialDeadlockException) {
                throw exception
            }
        },
        WARN {
            override fun handlePotentialDeadlock(exception: PotentialDeadlockException) {}
        },
        DISABLED {
            override fun handlePotentialDeadlock(exception: PotentialDeadlockException) {}
        }
    }

    interface Policy {
        fun handlePotentialDeadlock(exception: PotentialDeadlockException)
    }

    class PotentialDeadlockException(message: String) : IllegalStateException(message)

    private class LockGraphNode(val lockName: String) {
        val allowedPriorLocks = LinkedHashMap<LockGraphNode, PotentialDeadlockException>()
        fun checkAcquiredLock(policy: Policy, acquiredLock: LockGraphNode) {
            if (this === acquiredLock) {
                policy.handlePotentialDeadlock(
                    PotentialDeadlockException("Attempting to acquire the same lock: $lockName")
                )
                return
            }
            if (acquiredLock.allowedPriorLocks.containsKey(this)) {
                policy.handlePotentialDeadlock(
                    PotentialDeadlockException("Cycle: ${acquiredLock.lockName} -> $lockName")
                )
            }
            if (!allowedPriorLocks.containsKey(acquiredLock)) {
                allowedPriorLocks[acquiredLock] =
                    PotentialDeadlockException("${acquiredLock.lockName} before $lockName")
            }
        }
    }

    private val lockGraphNodes = LinkedHashMap<String, LockGraphNode>()
    private val graphLock = Any()
    private val acquiredByThread = LinkedHashMap<Long, ArrayDeque<LockGraphNode>>()

    fun newReentrantLock(name: String): CycleDetectingLock {
        val node = lockGraphNodes.getOrPut(name) { LockGraphNode(name) }
        val factory = this
        return object : CycleDetectingLock {
            private val actualLock = PlatformLock(false)
            private val holdCounts = LinkedHashMap<Long, Int>()
            override fun lock() {
                val threadId = platformThreadId()
                val reentrant = monitorSync(graphLock) { (holdCounts[threadId] ?: 0) > 0 }
                if (!reentrant) {
                    monitorSync(graphLock) {
                        for (prior in acquiredByThread[threadId].orEmpty()) {
                            node.checkAcquiredLock(policy, prior)
                        }
                    }
                }
                actualLock.lock()
                monitorSync(graphLock) {
                    val count = holdCounts[threadId] ?: 0
                    holdCounts[threadId] = count + 1
                    if (count == 0) acquiredByThread.getOrPut(threadId) { ArrayDeque() }.addLast(node)
                }
            }
            override fun unlock() {
                val threadId = platformThreadId()
                monitorSync(graphLock) {
                    val count = holdCounts[threadId] ?: throw IllegalStateException("Lock not held by current thread")
                    if (count == 1) {
                        holdCounts.remove(threadId)
                        acquiredByThread[threadId]?.remove(node)
                        if (acquiredByThread[threadId]?.isEmpty() == true) acquiredByThread.remove(threadId)
                    } else {
                        holdCounts[threadId] = count - 1
                    }
                }
                actualLock.unlock()
            }
            override fun isHeld(): Boolean = monitorSync(graphLock) {
                (holdCounts[platformThreadId()] ?: 0) > 0
            }
            override fun toString(): String = name
        }
    }

    interface CycleDetectingLock {
        fun lock()
        fun unlock()
        fun isHeld(): Boolean
    }

    companion object {
        fun newInstance(policy: Policy): CycleDetectingLockFactory = CycleDetectingLockFactory(policy)
    }
}
