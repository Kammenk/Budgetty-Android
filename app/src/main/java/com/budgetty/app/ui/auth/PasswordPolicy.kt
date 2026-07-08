package com.budgetty.app.ui.auth

/**
 * Minimum strength required for a new account password. This is a pragmatic "medium" policy aligned
 * with NIST SP 800-63B's emphasis on length (≥ 8 characters) plus a light composition requirement
 * (mixed case and a digit) so passwords aren't trivially weak. Symbols are allowed but not required.
 *
 * Only enforced on sign-up — existing accounts may have been created under an older/weaker rule, so
 * sign-in is never blocked by this.
 */
object PasswordPolicy {
    const val MIN_LENGTH = 8

    fun hasMinLength(password: String): Boolean = password.length >= MIN_LENGTH
    fun hasMixedCase(password: String): Boolean =
        password.any(Char::isLowerCase) && password.any(Char::isUpperCase)
    fun hasDigit(password: String): Boolean = password.any(Char::isDigit)

    /** True when [password] satisfies every requirement above. */
    fun isValid(password: String): Boolean =
        hasMinLength(password) && hasMixedCase(password) && hasDigit(password)
}
