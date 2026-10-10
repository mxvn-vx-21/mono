package com.mono.app

import com.mono.app.data.NoteEntity
import com.mono.app.data.TaskEntity
import com.mono.app.logic.Block
import com.mono.app.logic.BlockType
import com.mono.app.logic.NoteLogic
import com.mono.app.logic.RichContent
import com.mono.app.logic.TaskFilter
import com.mono.app.logic.TaskLogic
import com.mono.app.util.Dates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    private fun task(id: Long, due: Long? = null, done: Boolean = false, prio: Int = 1, time: Int? = null, updated: Long = id) =
        TaskEntity(id, "t$id", dueDate = due, dueTimeMinutes = time, priority = prio, isCompleted = done, createdAt = id, updatedAt = updated)

    private fun note(id: Long, title: String, content: String, pinned: Boolean = false, folder: Long? = null, updated: Long = id) =
        NoteEntity(id, title, content, pinned, folder, 0, updated)

    // ---- Progress / empty state
    @Test fun emptyDayHasNoDivisionByZero() {
        val s = TaskLogic.dayStats(emptyList(), 100)
        assertEquals(0, s.total); assertEquals(0, s.percent); assertEquals(0f, s.progress, 0f)
    }

    @Test fun dayStatsCountOnlyTodayAndCompleted() {
        val tasks = listOf(task(1, 100, true), task(2, 100), task(3, 100, true), task(4, 101, true), task(5, null))
        val s = TaskLogic.dayStats(tasks, 100)
        assertEquals(2, s.done); assertEquals(3, s.total); assertEquals(66, s.percent)
    }

    // ---- Sorting & sections
    @Test fun sectionsSplitOverduePendingCompleted() {
        val tasks = listOf(task(1, 90), task(2, 100), task(3, 110), task(4, null), task(5, 100, done = true))
        val s = TaskLogic.sections(tasks, 100, TaskFilter.All)
        assertEquals(listOf(1L), s.overdue.map { it.id })
        assertEquals(listOf(2L, 3L, 4L), s.pending.map { it.id })   // undated last
        assertEquals(listOf(5L), s.completed.map { it.id })
    }

    @Test fun priorityBreaksTiesAndFiltersWork() {
        val tasks = listOf(task(1, 100, prio = 0), task(2, 100, prio = 2), task(3, 105), task(4, 100, done = true))
        assertEquals(listOf(2L, 1L), TaskLogic.sections(tasks, 100, TaskFilter.Today).pending.map { it.id })
        assertEquals(listOf(3L), TaskLogic.sections(tasks, 100, TaskFilter.Upcoming).pending.map { it.id })
        assertEquals(listOf(4L), TaskLogic.sections(tasks, 100, TaskFilter.Completed).completed.map { it.id })
    }

    @Test fun todayListOrdersByTimeThenCompleted() {
        val tasks = listOf(task(1, 100, time = 600), task(2, 100, done = true, time = 60), task(3, 100, time = 540))
        assertEquals(listOf(3L, 1L, 2L), TaskLogic.todayList(tasks, 100).map { it.id })
    }

    // ---- Rich content
    @Test fun richContentRoundTripKeepsFormattingAndChecklistState() {
        val blocks = listOf(
            Block(BlockType.Paragraph, "Hello", bold = true, italic = true),
            Block(BlockType.Bullet, "a"), Block(BlockType.Numbered, "b"),
            Block(BlockType.Check, "milk", checked = true), Block(BlockType.Check, "eggs", checked = false),
        )
        assertEquals(blocks, RichContent.decode(RichContent.encode(blocks)))
    }

    @Test fun malformedContentRecoversAsPlainText() {
        val b = RichContent.decode("not json\nsecond line")
        assertEquals(listOf("not json", "second line"), b.map { it.text })
        assertEquals(1, RichContent.decode(null).size)
        assertEquals(1, RichContent.decode("{\"v\":1,\"blocks\":[]}").size)
    }

    // ---- Notes: pin, search, folders
    @Test fun pinnedFirstThenNewest() {
        val c = RichContent.encode(listOf(Block(text = "x")))
        val r = NoteLogic.filter(listOf(note(1, "a", c, updated = 1), note(2, "b", c, updated = 5), note(3, "c", c, pinned = true, updated = 2)), null, "")
        assertEquals(listOf(3L, 2L, 1L), r.map { it.id })
    }

    @Test fun searchMatchesTitleAndBodyCaseInsensitive() {
        val n1 = note(1, "Groceries", RichContent.encode(listOf(Block(text = "buy Arduino"))))
        val n2 = note(2, "Arduino plans", RichContent.encode(listOf(Block(text = "nothing"))))
        val n3 = note(3, "Other", RichContent.encode(listOf(Block(text = "zzz"))))
        assertEquals(setOf(1L, 2L), NoteLogic.filter(listOf(n1, n2, n3), null, "arduino").map { it.id }.toSet())
        assertTrue(NoteLogic.filter(listOf(n1, n2, n3), null, "nope").isEmpty())
    }

    @Test fun folderFilterWorks() {
        val c = RichContent.encode(listOf(Block(text = "x")))
        val r = NoteLogic.filter(listOf(note(1, "a", c, folder = 9), note(2, "b", c, folder = null)), 9, "")
        assertEquals(listOf(1L), r.map { it.id })
    }

    // ---- Dates
    @Test fun pickerMillisRoundTripHasNoTimezoneShift() {
        val day = java.time.LocalDate.of(2026, 10, 16).toEpochDay()
        assertEquals(day, Dates.pickerMillisToEpochDay(Dates.epochDayToPickerMillis(day)))
    }

    @Test fun countdownDays() {
        assertEquals(12L, Dates.daysUntil(112, 100)); assertEquals(-3L, Dates.daysUntil(97, 100))
    }

    @Test fun dueLabels() {
        assertTrue(Dates.dueLabel(100, 600, 100).startsWith("Today, "))
        assertEquals("Tomorrow", Dates.dueLabel(101, null, 100)); assertEquals("No date", Dates.dueLabel(null, null, 100))
    }
}
