package com.focusflow.timer.timer

/** 三个阶段 */
enum class Phase(val label: String) {
    FOCUS("专注"),
    SHORT_BREAK("短休息"),
    LONG_BREAK("长休息");

    val isBreak: Boolean get() = this != FOCUS
}
