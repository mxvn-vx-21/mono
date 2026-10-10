package com.mono.app.logic

import com.mono.app.data.TaskEntity

enum class TaskFilter(val label: String) {
    All("All"), Today("Today"), Upcoming("Upcoming"), Completed("Completed")
}

data class TaskSections(
    val overdue: List<TaskEntity>,
    val pending: List<TaskEntity>,
    val completed: List<TaskEntity>,
) {
    val isEmpty get() = overdue.isEmpty() && pending.isEmpty() && completed.isEmpty()
}

data class DayStats(val done: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else done * 100 / total
    val progress: Float get() = if (total == 0) 0f else done.toFloat() / total
}

object TaskLogic {
    /** Earliest due first (undated last), then higher priority, then oldest. */
    val pendingOrder: Comparator<TaskEntity> =
        compareBy<TaskEntity>({ it.dueDate ?: Long.MAX_VALUE }, { it.dueTimeMinutes ?: Int.MAX_VALUE })
            .thenByDescending { it.priority }
            .thenBy { it.createdAt }

    fun sections(tasks: List<TaskEntity>, today: Long, filter: TaskFilter): TaskSections {
        val visible = when (filter) {
            TaskFilter.All -> tasks
            TaskFilter.Today -> tasks.filter { it.dueDate == today }
            TaskFilter.Upcoming -> tasks.filter { !it.isCompleted && it.dueDate != null && it.dueDate > today }
            TaskFilter.Completed -> tasks.filter { it.isCompleted }
        }
        val open = visible.filter { !it.isCompleted }
        val overdue = open.filter { it.dueDate != null && it.dueDate < today }
        return TaskSections(
            overdue = overdue.sortedWith(pendingOrder),
            pending = (open - overdue.toSet()).sortedWith(pendingOrder),
            completed = visible.filter { it.isCompleted }.sortedByDescending { it.updatedAt },
        )
    }

    /** Tasks scheduled for today: open ones by time first, completed after. */
    fun todayList(tasks: List<TaskEntity>, today: Long): List<TaskEntity> =
        tasks.filter { it.dueDate == today }.sortedWith(
            compareBy<TaskEntity> { it.isCompleted }
                .thenBy { it.dueTimeMinutes ?: Int.MAX_VALUE }
                .thenByDescending { it.priority }
        )

    fun dayStats(tasks: List<TaskEntity>, today: Long): DayStats {
        val t = tasks.filter { it.dueDate == today }
        return DayStats(t.count { it.isCompleted }, t.size)
    }
}
