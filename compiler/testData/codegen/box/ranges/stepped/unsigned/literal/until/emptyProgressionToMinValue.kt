// Auto-generated by GenerateSteppedRangesCodegenTestData. Do not edit!
// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val uintList = mutableListOf<UInt>()
    for (i in 2u until UInt.MIN_VALUE step 2) {
        uintList += i
    }
    assertTrue(uintList.isEmpty())

    val ulongList = mutableListOf<ULong>()
    for (i in 2uL until ULong.MIN_VALUE step 2L) {
        ulongList += i
    }
    assertTrue(ulongList.isEmpty())

    return "OK"
}