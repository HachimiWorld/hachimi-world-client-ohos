package world.hachimi.app.logging

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.PerformanceAnalysisKit.HiLog.LOG_APP
import platform.PerformanceAnalysisKit.HiLog.LOG_DEBUG
import platform.PerformanceAnalysisKit.HiLog.LOG_ERROR
import platform.PerformanceAnalysisKit.HiLog.LOG_INFO
import platform.PerformanceAnalysisKit.HiLog.LOG_WARN
import platform.PerformanceAnalysisKit.HiLog.OH_LOG_Print

/**
 * HiLog service domain, which identifies the subsystem/module emitting the log.
 * The value must be within the range [0x0, 0xFFFF]; it is kept identical to the
 * domain used by the ArkTS host (`EntryAbility.ets`).
 */
@PublishedApi
internal const val HILOG_DOMAIN = 0x0000u

/** Writes a log entry to HiLog, the HarmonyOS logging system. */
@OptIn(ExperimentalForeignApi::class)
@PublishedApi
internal fun hilogPrint(level: UInt, tag: String, message: String) {
    memScoped {
        // `%{public}s` keeps the message readable in `hdc shell hilog` output,
        // otherwise the argument would be printed as `<private>`.
        OH_LOG_Print(LOG_APP, level, HILOG_DOMAIN, tag, "%{public}s", message.cstr.ptr)
    }
}

@PublishedApi
internal fun String.withThrowable(throwable: Throwable?): String =
    if (throwable == null) this else "$this\n${throwable.stackTraceToString()}"

actual object Logger {
    actual inline fun e(tag: String, message: String, throwable: Throwable?) {
        hilogPrint(LOG_ERROR, tag, message.withThrowable(throwable))
    }

    actual inline fun w(tag: String, message: String, throwable: Throwable?) {
        hilogPrint(LOG_WARN, tag, message.withThrowable(throwable))
    }

    actual inline fun d(tag: String, message: String) {
        hilogPrint(LOG_DEBUG, tag, message)
    }

    actual inline fun i(tag: String, message: String) {
        hilogPrint(LOG_INFO, tag, message)
    }
}