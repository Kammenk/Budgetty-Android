package com.budgetty.app.ui.util

/** Best-effort friendly name from the email's local part, e.g. "alex.rivera" -> "Alex Rivera". */
fun displayNameFromEmail(email: String?): String {
    if (email.isNullOrBlank()) return "Your account"
    val words = email.substringBefore("@")
        .split('.', '_', '-', '+')
        .mapNotNull { token -> token.filter(Char::isLetter).takeIf(String::isNotEmpty) }
    if (words.isEmpty()) return email.substringBefore("@")
    return words.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
}

/** Up to two uppercase initials derived from the email, for the avatar. */
fun initialsFromEmail(email: String?): String {
    if (email.isNullOrBlank()) return "?"
    val local = email.substringBefore("@")
    val letters = local.split('.', '_', '-', '+')
        .mapNotNull { it.firstOrNull(Char::isLetter)?.uppercaseChar() }
    return when {
        letters.size >= 2 -> "${letters[0]}${letters[1]}"
        letters.size == 1 -> letters[0].toString()
        else -> local.take(1).uppercase().ifBlank { "?" }
    }
}

/** The display name to show: the user's chosen [displayName], else derived from [email]. */
fun resolveDisplayName(displayName: String?, email: String?): String =
    displayName?.takeIf { it.isNotBlank() } ?: displayNameFromEmail(email)

/** Up to two initials from the chosen [displayName] if set, else from the [email]. */
fun resolveInitials(displayName: String?, email: String?): String {
    val name = displayName?.trim().orEmpty()
    if (name.isNotBlank()) {
        val parts = name.split(' ').filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            else -> parts[0].take(2).uppercase()
        }
    }
    return initialsFromEmail(email)
}
