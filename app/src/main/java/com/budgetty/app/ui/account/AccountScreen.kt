package com.budgetty.app.ui.account

import com.budgetty.app.ui.theme.dimens
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.BuildConfig
import com.budgetty.app.data.settings.AccentTheme
import com.budgetty.app.data.settings.Currency
import com.budgetty.app.data.settings.DateFormatOption
import com.budgetty.app.data.settings.Language
import com.budgetty.app.data.settings.ThemeMode
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.data.settings.AppSettings
import com.budgetty.app.ui.auth.AuthState
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.auth.AuthViewModel
import com.budgetty.app.ui.components.Avatar
import com.budgetty.app.ui.util.SinglePaneMaxWidth
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.util.isWideWidth
import com.budgetty.app.ui.util.resolveDisplayName
import com.budgetty.app.ui.util.resolveInitials
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.budgetty.app.R
import com.budgetty.app.ui.util.displayNameFromEmail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun AccountScreen(
    onOpenPaywall: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenCategoryRules: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = koinViewModel(),
    accountViewModel: AccountViewModel = koinViewModel(),
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val email = (authState as? AuthState.SignedIn)?.email
    val settings by accountViewModel.settings.collectAsStateWithLifecycle()
    val isPremium by accountViewModel.isPremium.collectAsStateWithLifecycle()
    AccountScreenContent(
        email = email,
        settings = settings,
        isPremium = isPremium,
        isExpanded = isExpandedWidth(),
        isWide = isWideWidth(),
        onOpenPaywall = onOpenPaywall,
        onOpenBudget = onOpenBudget,
        onOpenWidgets = onOpenWidgets,
        onOpenCategoryRules = onOpenCategoryRules,
        onSetDisplayName = accountViewModel::setDisplayName,
        onSetThemeMode = { accountViewModel.setThemeMode(it) },
        onSetAccent = { accountViewModel.setAccent(it) },
        onSetCurrency = { accountViewModel.setCurrency(it) },
        onSetDateFormat = { accountViewModel.setDateFormat(it) },
        onSetLanguage = { accountViewModel.setLanguage(it) },
        onSetNotificationsEnabled = { accountViewModel.setNotificationsEnabled(it) },
        onSetBiometricEnabled = { accountViewModel.setBiometricEnabled(it) },
        onSetAnalyticsEnabled = { accountViewModel.setAnalyticsEnabled(it) },
        onBuildBackupJson = { accountViewModel.buildBackupJson() },
        onImportBackup = { json, replace, onResult -> accountViewModel.importBackup(json, replace, onResult) },
        onDeleteAccount = { onResult -> accountViewModel.deleteAccount(onResult) },
        onSignOut = { authViewModel.signOut() },
        onUnlockTesterPremium = accountViewModel::unlockTesterPremium,
        modifier = modifier,
    )
}

@Composable
private fun AccountScreenContent(
    email: String?,
    settings: AppSettings,
    isPremium: Boolean,
    isExpanded: Boolean,
    isWide: Boolean,
    onOpenPaywall: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenCategoryRules: () -> Unit,
    onSetDisplayName: (String) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAccent: (AccentTheme) -> Unit,
    onSetCurrency: (Currency) -> Unit,
    onSetDateFormat: (DateFormatOption) -> Unit,
    onSetLanguage: (Language) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetAnalyticsEnabled: (Boolean) -> Unit,
    onBuildBackupJson: suspend () -> String,
    onImportBackup: (String, Boolean, (Boolean) -> Unit) -> Unit,
    onDeleteAccount: ((DeleteAccountResult) -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onUnlockTesterPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var openPicker by remember { mutableStateOf<Picker?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteNotice by remember { mutableStateOf<String?>(null) }
    // Hidden tester unlock: tapping the version label VERSION_TAPS_TO_UNLOCK times in a row flips
    // this install to Premium. The count lives in screen state, so leaving the screen resets it.
    var versionTapCount by remember { mutableStateOf(0) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = try {
                    val json = onBuildBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    }
                    true
                } catch (e: Exception) {
                    false
                }
                Toast.makeText(
                    context,
                    context.getString(if (ok) R.string.toast_backup_exported else R.string.toast_export_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                } catch (e: Exception) {
                    null
                }
                if (json.isNullOrBlank()) {
                    Toast.makeText(context, context.getString(R.string.toast_read_file_failed), Toast.LENGTH_SHORT).show()
                } else {
                    pendingImportJson = json
                }
            }
        }
    }

    val accountSection: @Composable () -> Unit = {
        AccountCard {
            AccountSectionRows(
                isPremium = isPremium,
                notificationsEnabled = settings.notificationsEnabled,
                currencyLabel = "${settings.currency.code} (${settings.currency.symbol})",
                onOpenPaywall = onOpenPaywall,
                onOpenBudget = onOpenBudget,
                onSetNotificationsEnabled = onSetNotificationsEnabled,
                onOpenCurrency = { openPicker = Picker.CURRENCY },
                onExport = { exportLauncher.launch("budgetty-backup.json") },
                onImport = {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                },
                onOpenWidgets = onOpenWidgets,
                onOpenCategoryRules = onOpenCategoryRules,
            )
        }
    }
    val preferencesSection: @Composable () -> Unit = {
        AccountCard {
            PreferencesSectionRows(
                settings = settings,
                isPremium = isPremium,
                onOpenPicker = { openPicker = it },
                onOpenPaywall = onOpenPaywall,
            )
        }
    }
    val privacySection: @Composable () -> Unit = {
        AccountCard {
            PrivacySectionRows(
                settings = settings,
                onSetBiometricEnabled = onSetBiometricEnabled,
                onSetAnalyticsEnabled = onSetAnalyticsEnabled,
            )
        }
    }
    val supportSection: @Composable () -> Unit = {
        AccountCard {
            SupportSectionRows(context = context)
        }
    }
    // Sign out + delete + version, grouped so the three-column landscape can tuck them under the
    // Privacy column instead of spanning the full width.
    val footerSection: @Composable () -> Unit = {
        SignOutCard { onSignOut() }
        Spacer(Modifier.height(MaterialTheme.dimens.sm))
        DeleteAccountButton(enabled = !deleting) { showDeleteDialog = true }
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Text(
            text = "Budgetty · v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    // Invisible (no ripple): the gesture is meant to be discovered, not advertised.
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (!isPremium) {
                        versionTapCount++
                        val remaining = VERSION_TAPS_TO_UNLOCK - versionTapCount
                        when {
                            remaining <= 0 -> {
                                versionTapCount = 0
                                onUnlockTesterPremium()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_tester_premium_unlocked),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            remaining <= 3 -> Toast.makeText(
                                context,
                                context.getString(R.string.toast_tester_premium_countdown, remaining),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
                .padding(top = MaterialTheme.dimens.sm),
        )
    }

    // The landscape list-detail's selected section (portrait stacks every section instead).
    var selectedSection by remember { mutableStateOf(AccountSection.ACCOUNT) }
    val profile: @Composable () -> Unit = {
        ProfileHeader(
            resolvedName = resolveDisplayName(settings.displayName, email),
            email = email,
            initials = resolveInitials(settings.displayName, email),
            placeholderName = displayNameFromEmail(email),
            onSaveName = onSetDisplayName,
        )
    }
    val title: @Composable () -> Unit = {
        Text(
            text = stringResource(R.string.nav_account),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = MaterialTheme.dimens.xs, top = MaterialTheme.dimens.xxl, bottom = MaterialTheme.dimens.lg),
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (isWide) {
            // Landscape tablet: an iPad-Settings list-detail — profile + a selectable section nav on
            // the left, the chosen section's settings on the right.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.dimens.screenPadding)
                    .padding(bottom = MaterialTheme.dimens.lg),
            ) {
                title()
                // No per-section header — the highlighted nav row already shows the current section.
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.xxl),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.34f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        profile()
                        Spacer(Modifier.height(MaterialTheme.dimens.lg))
                        SectionNavCard(selected = selectedSection, onSelect = { selectedSection = it })
                        Spacer(Modifier.height(MaterialTheme.dimens.lg))
                        footerSection()
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.66f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        when (selectedSection) {
                            AccountSection.ACCOUNT -> accountSection()
                            AccountSection.PREFERENCES -> preferencesSection()
                            AccountSection.PRIVACY -> privacySection()
                            AccountSection.SUPPORT -> supportSection()
                        }
                    }
                }
            }
        } else {
            // Phone & portrait tablet: one column with every section stacked (portrait tablet capped).
            Column(
                modifier = (if (isExpanded) Modifier.widthIn(max = SinglePaneMaxWidth) else Modifier.fillMaxWidth())
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MaterialTheme.dimens.screenPadding)
                    .padding(bottom = MaterialTheme.dimens.xxl),
            ) {
                title()
                profile()
                Spacer(Modifier.height(MaterialTheme.dimens.lg))
                accountSection()
                Spacer(Modifier.height(MaterialTheme.dimens.xxl))
                SectionHeader(stringResource(R.string.section_preferences))
                preferencesSection()
                Spacer(Modifier.height(MaterialTheme.dimens.xxl))
                SectionHeader(stringResource(R.string.section_privacy))
                privacySection()
                Spacer(Modifier.height(MaterialTheme.dimens.xxl))
                SectionHeader(stringResource(R.string.section_support))
                supportSection()
                Spacer(Modifier.height(MaterialTheme.dimens.xxl))
                footerSection()
            }
        }
    }

    pendingImportJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImportJson = null },
            title = { Text(stringResource(R.string.import_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.import_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportJson = null
                    onImportBackup(json, false) { ok ->
                        Toast.makeText(
                            context,
                            context.getString(if (ok) R.string.toast_backup_merged else R.string.toast_import_failed),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }) { Text(stringResource(R.string.import_merge)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingImportJson = null }) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = {
                        pendingImportJson = null
                        onImportBackup(json, true) { ok ->
                            Toast.makeText(
                                context,
                                context.getString(if (ok) R.string.toast_data_replaced else R.string.toast_import_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }) { Text(stringResource(R.string.import_replace)) }
                }
            },
        )
    }

    if (showDeleteDialog) {
        val reauthMsg = stringResource(R.string.delete_reauth_message)
        val errorMsg = stringResource(R.string.delete_error_message)
        AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_message)) },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        deleting = true
                        onDeleteAccount { result ->
                            deleting = false
                            showDeleteDialog = false
                            when (result) {
                                DeleteAccountResult.SUCCESS ->
                                    Toast.makeText(context, context.getString(R.string.toast_account_deleted), Toast.LENGTH_SHORT).show()
                                DeleteAccountResult.REQUIRES_REAUTH -> deleteNotice = reauthMsg
                                DeleteAccountResult.ERROR -> deleteNotice = errorMsg
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    deleteNotice?.let { msg ->
        AlertDialog(
            onDismissRequest = { deleteNotice = null },
            title = { Text(stringResource(R.string.delete_failed_title), fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { deleteNotice = null }) { Text(stringResource(R.string.action_ok)) } },
        )
    }

    when (openPicker) {
        Picker.THEME -> SelectionDialog(
            title = stringResource(R.string.account_theme),
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label },
            onSelect = { onSetThemeMode(it); openPicker = null },
            onDismiss = { openPicker = null },
        )
        Picker.ACCENT -> SelectionDialog(
            title = stringResource(R.string.account_accent),
            options = AccentTheme.entries,
            selected = settings.accent,
            label = { it.label },
            onSelect = { onSetAccent(it); openPicker = null },
            onDismiss = { openPicker = null },
        )
        Picker.CURRENCY -> SelectionDialog(
            title = stringResource(R.string.account_currency),
            options = Currency.entries,
            selected = settings.currency,
            label = { "${it.code} (${it.symbol})" },
            onSelect = { onSetCurrency(it); openPicker = null },
            onDismiss = { openPicker = null },
        )
        Picker.DATE -> SelectionDialog(
            title = stringResource(R.string.account_date_format),
            options = DateFormatOption.entries,
            selected = settings.dateFormat,
            label = { it.sample },
            onSelect = { onSetDateFormat(it); openPicker = null },
            onDismiss = { openPicker = null },
        )
        Picker.LANGUAGE -> SelectionDialog(
            title = stringResource(R.string.account_language),
            options = Language.entries,
            selected = settings.language,
            label = { it.label },
            onSelect = { onSetLanguage(it); openPicker = null },
            onDismiss = { openPicker = null },
        )
        null -> Unit
    }
}

private enum class Picker { THEME, ACCENT, CURRENCY, DATE, LANGUAGE }

/** Rows of the "Account" settings group, shared by the phone and tablet layouts. */
@Composable
private fun AccountSectionRows(
    isPremium: Boolean,
    notificationsEnabled: Boolean,
    currencyLabel: String,
    onOpenPaywall: () -> Unit,
    onOpenBudget: () -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onOpenCurrency: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenCategoryRules: () -> Unit,
) {
    SettingRow(
        icon = Icons.Filled.Star,
        title = stringResource(R.string.account_subscription),
        trailing = { StatusBadge(stringResource(if (isPremium) R.string.tier_premium else R.string.tier_free)) },
        onClick = onOpenPaywall,
    )
    RowDivider()
    SettingRow(Icons.Filled.AccountBalanceWallet, stringResource(R.string.account_budget)) { onOpenBudget() }
    RowDivider()
    SettingRow(Icons.Filled.AutoAwesome, stringResource(R.string.account_category_rules)) { onOpenCategoryRules() }
    RowDivider()
    SwitchRow(
        icon = Icons.Filled.Notifications,
        title = stringResource(R.string.account_notifications),
        checked = notificationsEnabled,
        onCheckedChange = onSetNotificationsEnabled,
    )
    RowDivider()
    SettingRow(
        icon = Icons.Filled.AttachMoney,
        title = stringResource(R.string.account_currency),
        value = currencyLabel,
    ) { onOpenCurrency() }
    RowDivider()
    SettingRow(Icons.Filled.Upload, stringResource(R.string.account_export)) { onExport() }
    RowDivider()
    SettingRow(Icons.Filled.Download, stringResource(R.string.account_import)) { onImport() }
    RowDivider()
    SettingRow(Icons.Filled.Widgets, stringResource(R.string.account_widgets)) { onOpenWidgets() }
}

/** Rows of the "Preferences" settings group. */
@Composable
private fun PreferencesSectionRows(
    settings: AppSettings,
    isPremium: Boolean,
    onOpenPicker: (Picker) -> Unit,
    onOpenPaywall: () -> Unit,
) {
    SettingRow(Icons.Filled.DarkMode, stringResource(R.string.account_theme), value = settings.themeMode.label) {
        onOpenPicker(Picker.THEME)
    }
    RowDivider()
    SettingRow(
        icon = Icons.Filled.Palette,
        title = stringResource(R.string.account_accent),
        value = if (isPremium) settings.accent.label else stringResource(R.string.tier_premium),
    ) {
        if (isPremium) onOpenPicker(Picker.ACCENT) else onOpenPaywall()
    }
    RowDivider()
    SettingRow(Icons.Filled.CalendarMonth, stringResource(R.string.account_date_format), value = settings.dateFormat.sample) {
        onOpenPicker(Picker.DATE)
    }
    RowDivider()
    SettingRow(Icons.Filled.Language, stringResource(R.string.account_language), value = settings.language.label) {
        onOpenPicker(Picker.LANGUAGE)
    }
}

/** Rows of the "Privacy & security" settings group. */
@Composable
private fun PrivacySectionRows(
    settings: AppSettings,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetAnalyticsEnabled: (Boolean) -> Unit,
) {
    SwitchRow(
        icon = Icons.Filled.Fingerprint,
        title = stringResource(R.string.account_biometric),
        checked = settings.biometricEnabled,
        onCheckedChange = onSetBiometricEnabled,
    )
    RowDivider()
    SwitchRow(
        icon = Icons.Filled.Analytics,
        title = stringResource(R.string.account_analytics),
        checked = settings.analyticsEnabled,
        onCheckedChange = onSetAnalyticsEnabled,
    )
}

/** Rows of the "Support" settings group. */
@Composable
private fun SupportSectionRows(context: Context) {
    SettingRow(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.account_help)) { openUrl(context, URL_HELP) }
    RowDivider()
    SettingRow(Icons.Filled.MailOutline, stringResource(R.string.account_contact)) { sendSupportEmail(context) }
    RowDivider()
    SettingRow(Icons.Filled.StarRate, stringResource(R.string.account_rate)) { openPlayStore(context, context.packageName) }
    RowDivider()
    SettingRow(Icons.Filled.PrivacyTip, stringResource(R.string.account_privacy_policy)) { openUrl(context, URL_PRIVACY) }
}

/**
 * Profile card: avatar, display name, signed-in email. The pencil turns the name into an inline
 * editable field (no separate screen) with X (discard) / ✓ (save) actions.
 */
@Composable
private fun ProfileHeader(
    resolvedName: String,
    email: String?,
    initials: String,
    placeholderName: String,
    onSaveName: (String) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    // Focus the field as soon as edit mode begins so the keyboard opens immediately.
    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.dimens.xl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(initials = initials, size = 56.dp)
            Spacer(Modifier.width(MaterialTheme.dimens.lg))

            if (editing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(placeholderName) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                )
                Spacer(Modifier.width(MaterialTheme.dimens.xs))
                IconButton(onClick = { editing = false }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.account_discard_name),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                IconButton(onClick = { onSaveName(draft.text); editing = false }) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.account_save_name),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resolvedName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!email.isNullOrBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = {
                    // Seed with the name actually shown and drop the cursor at its end so the
                    // user can extend/correct it rather than retype from a blank field.
                    draft = TextFieldValue(resolvedName, TextRange(resolvedName.length))
                    editing = true
                }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.account_edit_name),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Uppercase, muted section label that sits above a grouped card. */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = MaterialTheme.dimens.sm, top = MaterialTheme.dimens.sm, bottom = MaterialTheme.dimens.sm),
    )
}

/** Rounded surface that groups a set of [SettingRow]s, matching the app's card style. */
@Composable
private fun AccountCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

/** Hairline divider between rows, inset to start under the row's text. */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}

/** The settings sections shown in the landscape list-detail's left-hand nav. */
private enum class AccountSection(val titleRes: Int, val icon: ImageVector) {
    ACCOUNT(R.string.nav_account, Icons.Filled.AccountBalanceWallet),
    PREFERENCES(R.string.section_preferences, Icons.Filled.Palette),
    PRIVACY(R.string.section_privacy, Icons.Filled.Fingerprint),
    SUPPORT(R.string.section_support, Icons.AutoMirrored.Filled.HelpOutline),
}

/** Left-pane section picker for the landscape Account list-detail; the active row is highlighted. */
@Composable
private fun SectionNavCard(selected: AccountSection, onSelect: (AccountSection) -> Unit) {
    AccountCard {
        AccountSection.entries.forEach { section ->
            AccountNavRow(
                icon = section.icon,
                label = stringResource(section.titleRes),
                selected = section == selected,
                onClick = { onSelect(section) },
            )
        }
    }
}

/** One selectable row of [SectionNavCard]. */
@Composable
private fun AccountNavRow(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MaterialTheme.dimens.icon),
        )
        Spacer(Modifier.width(MaterialTheme.dimens.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** Small pill used for the Subscription tier (Free / Premium). */
@Composable
private fun StatusBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.xs),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(MaterialTheme.dimens.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        when {
            trailing != null -> {
                trailing()
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
            }
            value != null -> {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

/** Row with a trailing [Switch] for boolean settings; tapping anywhere toggles it. */
@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = MaterialTheme.dimens.xl, vertical = MaterialTheme.dimens.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(MaterialTheme.dimens.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Full-width card that signs the user out, with the error-colored label from the mockup. */
@Composable
private fun SignOutCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.account_sign_out),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Subtle, destructive text action that opens the delete-account confirmation. */
@Composable
private fun DeleteAccountButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = MaterialTheme.dimens.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.account_delete),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

// Support & About destinations. Point HELP at a dedicated FAQ page once one is published;
// for now it lands on the public site (which also hosts the privacy policy).
// Taps on the version label that unlock tester Premium (hidden gesture; see footerSection).
private const val VERSION_TAPS_TO_UNLOCK = 11

private const val URL_HELP = "https://budgetty-96a3d.web.app/#faq"
private const val URL_PRIVACY = "https://budgetty-96a3d.web.app/"
private const val SUPPORT_EMAIL = "kamskstudio@gmail.com"

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.toast_no_link_app), Toast.LENGTH_SHORT).show()
    }
}

private fun sendSupportEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")).apply {
        putExtra(Intent.EXTRA_SUBJECT, "Budgetty support")
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.toast_no_email_app), Toast.LENGTH_SHORT).show()
    }
}

private fun openPlayStore(context: Context, packageName: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: ActivityNotFoundException) {
        openUrl(context, "https://play.google.com/store/apps/details?id=$packageName")
    }
}

/**
 * Single-choice dialog styled to the mockups: radio rows where the selected one sits on a rounded
 * tonal highlight with a bolder label, and a "Done" action. The option list scrolls when long
 * (e.g. the 20-currency / 20-language pickers).
 */
@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent,
                            )
                            .clickable { onSelect(option) }
                            .padding(horizontal = MaterialTheme.dimens.sm, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = isSelected, onClick = { onSelect(option) })
                        Spacer(Modifier.width(MaterialTheme.dimens.sm))
                        Text(
                            text = label(option),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        },
    )
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun AccountScreenPreview() {
    BudgettyTheme {
        AccountScreenContent(
            email = "alex@example.com",
            settings = AppSettings(),
            isPremium = false,
            isExpanded = false,
            isWide = false,
            onOpenPaywall = {},
            onOpenBudget = {},
            onOpenWidgets = {},
            onOpenCategoryRules = {},
            onSetDisplayName = {},
            onSetThemeMode = {},
            onSetAccent = {},
            onSetCurrency = {},
            onSetDateFormat = {},
            onSetLanguage = {},
            onSetNotificationsEnabled = {},
            onSetBiometricEnabled = {},
            onSetAnalyticsEnabled = {},
            onBuildBackupJson = { "" },
            onImportBackup = { _, _, _ -> },
            onDeleteAccount = {},
            onSignOut = {},
            onUnlockTesterPremium = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
internal fun AccountScreenTabletPreview() {
    BudgettyTheme {
        AccountScreenContent(
            email = "alex@example.com",
            settings = AppSettings(),
            isPremium = false,
            isExpanded = isExpandedWidth(),
            isWide = isWideWidth(),
            onOpenPaywall = {},
            onOpenBudget = {},
            onOpenWidgets = {},
            onOpenCategoryRules = {},
            onSetDisplayName = {},
            onSetThemeMode = {},
            onSetAccent = {},
            onSetCurrency = {},
            onSetDateFormat = {},
            onSetLanguage = {},
            onSetNotificationsEnabled = {},
            onSetBiometricEnabled = {},
            onSetAnalyticsEnabled = {},
            onBuildBackupJson = { "" },
            onImportBackup = { _, _, _ -> },
            onDeleteAccount = {},
            onSignOut = {},
            onUnlockTesterPremium = {},
        )
    }
}
