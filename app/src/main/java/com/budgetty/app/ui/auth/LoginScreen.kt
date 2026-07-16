package com.budgetty.app.ui.auth

import com.budgetty.app.ui.theme.dimens
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.ui.util.isCompactHeight
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.util.isWideWidth
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.isDarkTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val FieldShape = RoundedCornerShape(16.dp)
// Buttons intentionally have no custom shape: they use the Material 3 default (fully rounded). See Shape.kt.

/** Rounded outer shape of the tablet split-login card. */
private val CardShape = RoundedCornerShape(28.dp)

/**
 * Fixed brand-purple gradient for the tablet branding panel. Deliberately *not* derived from
 * [MaterialTheme.colorScheme] because the scheme's primary flips to a light lavender in dark mode,
 * whereas this panel must stay a rich purple under white text in every theme — like the mockup.
 */
private val BrandGradientTop = Color(0xFF6E55B0)
private val BrandGradientBottom = Color(0xFF534195)

/**
 * Stable test tags on the three login controls. The root `testTagsAsResourceId` (see MainActivity)
 * exposes these to UI-automation tools as view resource-ids, so Firebase Test Lab's Robo login
 * directives can target them by name (kept in sync with scripts/testlab-robo.sh) to sign in during
 * an automated crawl. Invisible at runtime.
 */
const val LoginTagEmail = "login_email"
const val LoginTagPassword = "login_password"
const val LoginTagSignIn = "login_sign_in"

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LoginScreenContent(
        loading = loading,
        error = error,
        onSignIn = { e, p -> viewModel.signIn(e, p) },
        onSignUp = { e, p -> viewModel.signUp(e, p) },
        onResetPassword = { e, onSent -> viewModel.resetPassword(e, onSent) },
        onSetError = { msg -> viewModel.setError(msg) },
        onGoogleSignIn = { scope.launch { googleSignIn(context, viewModel) } },
        modifier = modifier,
    )
}

@Composable
private fun LoginScreenContent(
    loading: Boolean,
    error: String?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onResetPassword: (String, () -> Unit) -> Unit,
    onSetError: (String?) -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isWideWidth()) {
        // Landscape tablet: brand panel beside the form.
        TabletLogin(
            loading = loading,
            error = error,
            onSignIn = onSignIn,
            onSignUp = onSignUp,
            onResetPassword = onResetPassword,
            onSetError = onSetError,
            onGoogleSignIn = onGoogleSignIn,
            modifier = modifier,
        )
    } else {
        // Phone & portrait tablet: the single, centred column (capped on the tablet).
        LoginForm(
            loading = loading,
            error = error,
            onSignIn = onSignIn,
            onSignUp = onSignUp,
            onResetPassword = onResetPassword,
            onSetError = onSetError,
            onGoogleSignIn = onGoogleSignIn,
            tablet = isExpandedWidth(),
            modifier = modifier,
        )
    }
}

/**
 * Tablet/expanded login: a full-height rounded card split into a brand panel (left) and the form
 * (right), filling the window top-to-bottom in landscape (and portrait). A small inset margin plus
 * a soft shadow lift the card off the page, matching the design handoff.
 */
@Composable
private fun TabletLogin(
    loading: Boolean,
    error: String?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onResetPassword: (String, () -> Unit) -> Unit,
    onSetError: (String?) -> Unit,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            // A short landscape window can't spare the full inset margin: it comes straight off the
            // form pane's usable height, where it would push "Sign in" below the fold.
            .padding(
                horizontal = MaterialTheme.dimens.xl,
                vertical = if (isCompactHeight()) MaterialTheme.dimens.sm else MaterialTheme.dimens.xl,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = MaterialTheme.dimens.xl, shape = CardShape)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            BrandingPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            LoginForm(
                loading = loading,
                error = error,
                onSignIn = onSignIn,
                onSignUp = onSignUp,
                onResetPassword = onResetPassword,
                onSetError = onSetError,
                onGoogleSignIn = onGoogleSignIn,
                tablet = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/**
 * The purple brand panel shown on the left of the tablet layout: app mark, name, tagline and the
 * three selling points, over a brand-purple gradient with a few soft, clipped highlight circles.
 */
@Composable
private fun BrandingPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clipToBounds()
            .background(Brush.linearGradient(listOf(BrandGradientTop, BrandGradientBottom))),
    ) {
        // Decorative, lightly translucent highlight circles. clipToBounds (above) keeps them inside
        // the panel so they never bleed into the form pane.
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 90.dp, y = (-110).dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
        )
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 90.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape),
        )
        Box(
            Modifier
                .size(150.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-50).dp, y = 30.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Receipt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(MaterialTheme.dimens.xxxl),
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.xxl))
            Text(
                text = "Budgetty",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(40.dp))
            FeatureRow(stringResource(R.string.login_feature_1))
            Spacer(Modifier.height(MaterialTheme.dimens.xl))
            FeatureRow(stringResource(R.string.login_feature_2))
            Spacer(Modifier.height(MaterialTheme.dimens.xl))
            FeatureRow(stringResource(R.string.login_feature_3))
        }
    }
}

/** One brand selling-point: a soft check badge followed by white text. */
@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(MaterialTheme.dimens.icon)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.92f),
        )
    }
}

/**
 * The sign-in / sign-up form. Used as the whole screen on phones and as the right pane on tablets;
 * [tablet] only swaps the header (centred app mark on phones vs. a left-aligned "Welcome back"
 * heading beside the brand panel). The column scrolls and is vertically centred so it sits well
 * whether it fills a phone or a tall tablet pane, and its content is width-capped for readability.
 */
@Composable
private fun LoginForm(
    loading: Boolean,
    error: String?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onResetPassword: (String, () -> Unit) -> Unit,
    onSetError: (String?) -> Unit,
    onGoogleSignIn: () -> Unit,
    tablet: Boolean,
    modifier: Modifier = Modifier,
) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    // On a short window (landscape phone/tablet) the form is taller than the pane, so it scrolls and
    // the full-height rhythm below would leave "Sign in" off-screen on arrival. Economise: drop the
    // decorative header (the brand panel already names the app on tablets) and tighten every gap, so
    // email + password + "Sign in" all land above the fold. See isCompactHeight.
    val compactHeight = isCompactHeight()
    val blockGap = if (compactHeight) MaterialTheme.dimens.sm else MaterialTheme.dimens.xl
    val fieldGap = if (compactHeight) MaterialTheme.dimens.sm else MaterialTheme.dimens.lg
    val context = LocalContext.current
    // Resolved here (not inside callbacks/coroutines) because stringResource is @Composable-only.
    val resetSentMsg = stringResource(R.string.login_reset_sent)
    val enterBothMsg = stringResource(R.string.login_enter_both)
    val weakPasswordMsg = stringResource(R.string.login_password_weak)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = MaterialTheme.dimens.xxl,
                vertical = if (compactHeight) MaterialTheme.dimens.sm else MaterialTheme.dimens.xxxl,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (tablet) 420.dp else 480.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!compactHeight) {
                if (tablet) TabletFormHeader(isSignUp) else PhoneHeader(isSignUp)
                Spacer(Modifier.height(MaterialTheme.dimens.xxxl))
            }

            // Email
            FieldLabel(stringResource(R.string.login_email))
            TextField(
                value = email,
                onValueChange = { email = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = FieldShape,
                colors = loginFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag(LoginTagEmail),
            )
            Spacer(Modifier.height(fieldGap))

            // Password
            FieldLabel(stringResource(R.string.login_password))
            TextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) stringResource(R.string.login_hide_password) else stringResource(R.string.login_show_password),
                        )
                    }
                },
                shape = FieldShape,
                colors = loginFieldColors(),
                modifier = Modifier.fillMaxWidth().testTag(LoginTagPassword),
            )

            // Only surface the requirements checklist while signing up *and* the typed password is
            // still too weak — it stays hidden until then, and disappears again once every rule is met.
            if (isSignUp && password.isNotEmpty() && !PasswordPolicy.isValid(password)) {
                PasswordRequirements(password = password)
            }

            if (!isSignUp) {
                TextButton(
                    onClick = {
                        onResetPassword(email) {
                            Toast.makeText(context, resetSentMsg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.login_forgot), fontWeight = FontWeight.SemiBold)
                }
            }

            if (error != null) {
                Spacer(Modifier.height(MaterialTheme.dimens.xs))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(blockGap))
            Button(
                onClick = {
                    when {
                        email.isBlank() || password.isBlank() ->
                            onSetError(enterBothMsg)
                        isSignUp && !PasswordPolicy.isValid(password) ->
                            onSetError(weakPasswordMsg)
                        isSignUp -> onSignUp(email, password)
                        else -> onSignIn(email, password)
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.dimens.buttonHeight)
                    .testTag(LoginTagSignIn),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(MaterialTheme.dimens.xl),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = if (isSignUp) stringResource(R.string.login_create_account) else stringResource(R.string.login_sign_in),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(Modifier.height(blockGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Text(
                    text = stringResource(R.string.login_or),
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimens.lg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
            Spacer(Modifier.height(blockGap))

            OutlinedButton(
                onClick = onGoogleSignIn,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MaterialTheme.dimens.buttonHeight),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(MaterialTheme.dimens.xl),
                )
                Spacer(Modifier.width(MaterialTheme.dimens.md))
                Text(stringResource(R.string.login_google), style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(blockGap))
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        isSignUp = !isSignUp
                        onSetError(null)
                    },
            ) {
                Text(
                    text = (if (isSignUp) stringResource(R.string.login_have_account) else stringResource(R.string.login_new_here)) + " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
                Text(
                    text = if (isSignUp) stringResource(R.string.login_sign_in) else stringResource(R.string.login_create_account_link),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** Phone header: the centred app mark, name and welcome/sign-up subtitle (original design). */
@Composable
private fun PhoneHeader(isSignUp: Boolean) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl), spotColor = MaterialTheme.colorScheme.primary)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Receipt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(36.dp),
        )
    }
    Spacer(Modifier.height(MaterialTheme.dimens.xl))
    Text(
        text = "Budgetty",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = if (isSignUp) stringResource(R.string.login_create_subtitle) else stringResource(R.string.login_welcome),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
    )
}

/** Tablet header: a left-aligned "Welcome back" heading + subtitle, beside the brand panel. */
@Composable
private fun TabletFormHeader(isSignUp: Boolean) {
    Text(
        text = if (isSignUp) stringResource(R.string.login_create_subtitle) else stringResource(R.string.login_welcome),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(6.dp))
    Text(
        text = if (isSignUp) stringResource(R.string.login_subtitle_signup) else stringResource(R.string.login_subtitle_signin),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Left-aligned field label shown above each input, matching the design. */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
    )
}

/**
 * Live checklist of the [PasswordPolicy] rules shown beneath the password field while signing up.
 * Each rule turns accented with a filled check once met, so the required strength is clear up front.
 */
@Composable
private fun PasswordRequirements(password: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.login_password_requirements),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PasswordRequirementRow(stringResource(R.string.login_password_req_length), PasswordPolicy.hasMinLength(password))
        PasswordRequirementRow(stringResource(R.string.login_password_req_case), PasswordPolicy.hasMixedCase(password))
        PasswordRequirementRow(stringResource(R.string.login_password_req_number), PasswordPolicy.hasDigit(password))
    }
}

@Composable
private fun PasswordRequirementRow(text: String, met: Boolean) {
    val color = if (met) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.sm),
    ) {
        Icon(
            imageVector = if (met) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(MaterialTheme.dimens.lg),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

/**
 * Filled-field colours for the login inputs: a soft tonal fill (no outline/indicator line),
 * matching the borderless filled fields on the receipt review/edit screen. The fill is
 * theme-aware so the field reads as a clearly-filled input on the plain login background:
 * a light tonal grey in light mode ([surfaceContainerHigh][androidx.compose.material3.ColorScheme.surfaceContainerHigh]),
 * and a deeper, subtler grey in dark mode ([surfaceContainer][androidx.compose.material3.ColorScheme.surfaceContainer])
 * so it sits quietly above the near-black background, matching the mockups.
 */
@Composable
private fun loginFieldColors(): TextFieldColors {
    val fill = if (isDarkTheme()) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    return TextFieldDefaults.colors(
        focusedContainerColor = fill,
        unfocusedContainerColor = fill,
        disabledContainerColor = fill,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
    )
}

private suspend fun googleSignIn(context: Context, viewModel: AuthViewModel) {
    try {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val response = credentialManager.getCredential(context, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            viewModel.signInWithGoogle(token)
        } else {
            viewModel.setError(context.getString(R.string.login_google_failed))
        }
    } catch (e: GetCredentialException) {
        viewModel.setError(e.message ?: context.getString(R.string.login_google_failed))
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun LoginScreenPreview() {
    BudgettyTheme {
        LoginScreenContent(
            loading = false,
            error = null,
            onSignIn = { _, _ -> },
            onSignUp = { _, _ -> },
            onResetPassword = { _, _ -> },
            onSetError = {},
            onGoogleSignIn = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun LoginScreenTabletPreview() {
    BudgettyTheme {
        LoginScreenContent(
            loading = false,
            error = null,
            onSignIn = { _, _ -> },
            onSignUp = { _, _ -> },
            onResetPassword = { _, _ -> },
            onSetError = {},
            onGoogleSignIn = {},
        )
    }
}

/**
 * Short landscape window (a phone/small tablet on its side): wide enough for the split layout but
 * only ~411dp tall. "Sign in" must still be visible without scrolling — see [isCompactHeight].
 */
@Preview(name = "Landscape (short window)", showBackground = true, widthDp = 914, heightDp = 411)
@Composable
private fun LoginScreenShortLandscapePreview() {
    BudgettyTheme {
        LoginScreenContent(
            loading = false,
            error = null,
            onSignIn = { _, _ -> },
            onSignUp = { _, _ -> },
            onResetPassword = { _, _ -> },
            onSetError = {},
            onGoogleSignIn = {},
        )
    }
}
