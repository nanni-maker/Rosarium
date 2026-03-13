package com.cambria.rosarium.core

import java.time.DayOfWeek
import java.time.LocalDate

object RosaryCalendar {

    fun crownForDate(date: LocalDate): CrownType {
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> CrownType.JOYFUL
            DayOfWeek.TUESDAY -> CrownType.SORROWFUL
            DayOfWeek.WEDNESDAY -> CrownType.GLORIOUS
            DayOfWeek.THURSDAY -> CrownType.LUMINOUS
            DayOfWeek.FRIDAY -> CrownType.SORROWFUL
            DayOfWeek.SATURDAY -> CrownType.JOYFUL
            DayOfWeek.SUNDAY -> CrownType.GLORIOUS
        }
    }

    fun next(crown: CrownType): CrownType {
        return when (crown) {
            CrownType.JOYFUL -> CrownType.SORROWFUL
            CrownType.SORROWFUL -> CrownType.GLORIOUS
            CrownType.GLORIOUS -> CrownType.LUMINOUS
            CrownType.LUMINOUS -> CrownType.JOYFUL
        }
    }

    fun previous(crown: CrownType): CrownType {
        return when (crown) {
            CrownType.JOYFUL -> CrownType.LUMINOUS
            CrownType.SORROWFUL -> CrownType.JOYFUL
            CrownType.GLORIOUS -> CrownType.SORROWFUL
            CrownType.LUMINOUS -> CrownType.GLORIOUS
        }
    }
}