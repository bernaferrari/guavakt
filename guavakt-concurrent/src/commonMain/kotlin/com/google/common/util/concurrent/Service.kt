package dev.guavakt.util.concurrent

interface Service {
    enum class State {
        NEW, STARTING, RUNNING, STOPPING, TERMINATED, FAILED
    }
    fun startAsync(): Service
    fun stopAsync(): Service
    fun awaitRunning()
    fun awaitTerminated()
    fun state(): State
    fun isRunning(): Boolean = state() == State.RUNNING
    fun failureCause(): Throwable
    fun addListener(listener: Listener)
    abstract class Listener {
        open fun starting() {}
        open fun running() {}
        open fun stopping(from: State) {}
        open fun terminated(from: State) {}
        open fun failed(from: State, failure: Throwable) {}
    }
}
