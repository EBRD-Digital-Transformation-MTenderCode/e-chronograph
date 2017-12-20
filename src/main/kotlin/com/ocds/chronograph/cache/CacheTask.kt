package com.ocds.chronograph.cache

import com.ocds.chronograph.model.domain.task.Task
import java.time.LocalDateTime
import java.util.*

class CacheTask {
    private val cache = TreeMap<LocalDateTime, HashSet<Task>>()

    fun push(task: Task) {
        val tasksOnTime: HashSet<Task> = cache.computeIfAbsent(task.launchTime) { HashSet() }
        tasksOnTime.add(task)
    }

    fun poll(time: LocalDateTime): Collection<Set<Task>> {
        val tasks = LinkedList<Set<Task>>()
        val keys = LinkedList<LocalDateTime>()

        cache.headMap(time, true)
            .forEach { (key, value) ->
                tasks.add(value)
                keys.add(key)
            }
        keys.forEach { cache.remove(it) }

        return tasks
    }
}
