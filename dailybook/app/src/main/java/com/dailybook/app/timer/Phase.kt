package com.dailybook.app.timer

import com.dailybook.app.i18n.AppStrings
import com.dailybook.app.i18n.Lang

/** 专注计时的三个阶段 */
enum class Phase {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK;

    fun label(lang: Lang): String = when (this) {
        FOCUS -> AppStrings.phaseFocus(lang)
        SHORT_BREAK -> AppStrings.phaseShortBreak(lang)
        LONG_BREAK -> AppStrings.phaseLongBreak(lang)
    }
}
