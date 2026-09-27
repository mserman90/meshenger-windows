/*
 * Copyright (C) 2026 Meshenger Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package d.d.meshenger.desktop.contact

import kotlinx.serialization.Serializable

@Serializable
data class Contact(
    val id: String,
    val name: String,
    val ipAddress: String,
    val publicKey: String = "",
    val isOnline: Boolean = true,
    val isDisasterActive: Boolean = false,
    val statusText: String = "Online"
)
