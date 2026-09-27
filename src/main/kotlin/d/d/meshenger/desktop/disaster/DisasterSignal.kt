/*
 * Copyright (C) 2026 Meshenger Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package d.d.meshenger.desktop.disaster

import kotlinx.serialization.Serializable

@Serializable
data class DisasterSignal(
    val senderName: String,
    val status: StatusType,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val medicalNotes: String = "",
    var ipAddress: String = "",
    val macAddress: String = "",
    var hopCount: Int = 0
) {
    @Serializable
    enum class StatusType {
        SAFE,
        HELP,
        MEDICAL
    }
}
