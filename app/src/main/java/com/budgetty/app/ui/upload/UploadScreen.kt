package com.budgetty.app.ui.upload

import com.budgetty.app.ui.theme.dimens
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.budgetty.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.category.Categories
import com.budgetty.app.data.ingest.ParsedTransaction
import com.budgetty.app.data.local.CategoryEntity
import com.budgetty.app.ui.components.AdaptiveSheet
import com.budgetty.app.ui.components.CategoryPickerScreen
import com.budgetty.app.ui.components.CustomCategoryActions
import com.budgetty.app.ui.util.formatDayMonth
import com.budgetty.app.ui.util.formatMoney
import com.budgetty.app.ui.util.isExpandedWidth
import com.budgetty.app.ui.util.isWideWidth
import androidx.compose.ui.tooling.preview.Preview
import com.budgetty.app.ui.theme.BudgettyTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    source: String,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier,
    receiptId: Long = -1L,
    viewModel: UploadViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isEdit = source == "edit"
    // Resolved here (stringResource is @Composable-only) so the ViewModel can surface localized
    // empty-receipt errors without holding a Context.
    val noItemsMsg = stringResource(R.string.upload_error_no_items)
    val noAmountMsg = stringResource(R.string.upload_error_no_amount)

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.onReceiptPicked(uri) else onNavigateBack() }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok -> if (ok) cameraUri?.let(viewModel::onReceiptPicked) else onNavigateBack() }

    // "Add receipt" launchers (manual-edit only): scan a photo/file to attach + fill the receipt.
    // Unlike the initial-source launchers above, cancelling here stays on the edit screen.
    var addReceiptUri by remember { mutableStateOf<Uri?>(null) }
    val addReceiptCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok -> if (ok) addReceiptUri?.let(viewModel::attachAndScan) }
    val addReceiptFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) viewModel.attachAndScan(uri) }
    var showAddReceiptSheet by remember { mutableStateOf(false) }

    // High-quality capture: the ML Kit Document Scanner (auto edge-detect, deskew, glare handling
    // and a review/retake step) replaces the raw camera intent, whose marginal images caused the
    // model to drop or merge lines. Its cleaned JPEG still flows through the same prepareUpload path.
    // If the scanner can't start (no Play Services / too old) we fall back to the camera intent above.
    val docScanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(1)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build(),
        )
    }
    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val scanned = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.firstOrNull()?.imageUri
        // Re-host under our own FileProvider so extraction gets a readable, image/jpeg-typed uri.
        val uri = scanned?.let { persistScannedImage(context, it) ?: it }
        if (result.resultCode == Activity.RESULT_OK && uri != null) viewModel.onReceiptPicked(uri)
        else onNavigateBack()
    }
    val addReceiptScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val scanned = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages?.firstOrNull()?.imageUri
        val uri = scanned?.let { persistScannedImage(context, it) ?: it }
        // Cancelling "Add receipt" stays on the edit screen (mirrors addReceiptCameraLauncher).
        if (result.resultCode == Activity.RESULT_OK && uri != null) viewModel.attachAndScan(uri)
    }

    // Launches capture for the current source. Reused by the initial effect below and by "Try
    // again" on the error state, which re-opens the scanner/file picker to capture a new image.
    val launchSource: () -> Unit = {
        when (source) {
            "edit" -> viewModel.startEdit(receiptId)
            "manual" -> viewModel.startManual()
            "camera" -> docScanner.getStartScanIntent(context.findActivity())
                .addOnSuccessListener { sender ->
                    scanLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
                .addOnFailureListener {
                    // Fallback: plain system-camera capture to a cache file.
                    val uri = createReceiptImageUri(context)
                    cameraUri = uri
                    cameraLauncher.launch(uri)
                }
            else -> fileLauncher.launch(arrayOf("application/pdf", "image/*"))
        }
    }

    // Launch capture exactly once per screen visit. The guard lives in the ViewModel (which is
    // retained across configuration changes) so folding/unfolding or rotating — which recreates the
    // Activity and re-runs this effect — doesn't re-open the camera over an already-captured receipt.
    LaunchedEffect(Unit) { if (viewModel.consumeInitialLaunch()) launchSource() }

    val customActions = CustomCategoryActions(
        categories = state.categories,
        isPremium = state.isPremium,
        onSave = viewModel::saveCustomCategory,
        onDelete = viewModel::deleteCustomCategory,
        onCountTransactions = viewModel::transactionCount,
        onOpenPaywall = onNavigateToPaywall,
    )

    UploadScreenContent(
        state = state,
        isEdit = isEdit,
        isWide = isWideWidth(),
        customActions = customActions,
        onStoreChange = viewModel::updateStore,
        onDateChange = viewModel::updateDate,
        onNameChange = viewModel::updateName,
        onCategoryChange = viewModel::updateCategory,
        onPriceChange = viewModel::updatePrice,
        onQuantityChange = viewModel::updateQuantity,
        onDiscountChange = viewModel::updateDiscount,
        onRemove = viewModel::removeRow,
        onAddRow = viewModel::addRow,
        onFinalize = { viewModel.finalizeUpload(noItemsMsg, noAmountMsg, onNavigateBack) },
        onAddReceipt = { showAddReceiptSheet = true },
        onRetry = launchSource,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )

    if (showAddReceiptSheet) {
        AddReceiptSourceSheet(
            onDismiss = { showAddReceiptSheet = false },
            onTakePhoto = {
                showAddReceiptSheet = false
                docScanner.getStartScanIntent(context.findActivity())
                    .addOnSuccessListener { sender ->
                        addReceiptScanLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                    .addOnFailureListener {
                        val uri = createReceiptImageUri(context)
                        addReceiptUri = uri
                        addReceiptCameraLauncher.launch(uri)
                    }
            },
            onUploadFile = {
                showAddReceiptSheet = false
                addReceiptFileLauncher.launch(arrayOf("application/pdf", "image/*"))
            },
        )
    }

    state.propagationPrompt?.let { prompt ->
        CategoryPropagationSheet(
            prompt = prompt,
            onConfirm = viewModel::confirmPropagation,
            onDismiss = viewModel::dismissPropagation,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadScreenContent(
    state: UploadUiState,
    isEdit: Boolean,
    isWide: Boolean,
    customActions: CustomCategoryActions = CustomCategoryActions(),
    onStoreChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onNameChange: (String, String) -> Unit,
    onCategoryChange: (String, String) -> Unit,
    onPriceChange: (String, String) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onDiscountChange: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAddRow: () -> Unit,
    onFinalize: () -> Unit,
    onAddReceipt: () -> Unit,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isEdit) R.string.upload_edit_title else R.string.upload_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                // Outer MainScaffold already applies the status-bar inset; don't add it again.
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (state.stage) {
                UploadStage.IDLE -> if (state.error != null) {
                    ErrorState(kind = state.errorKind, onBack = onNavigateBack, onRetry = onRetry)
                } else {
                    Loading(stringResource(R.string.upload_preparing))
                }

                UploadStage.EXTRACTING -> Loading(stringResource(R.string.upload_reading))
                UploadStage.SAVING -> Loading(stringResource(R.string.upload_saving))

                UploadStage.REVIEW, UploadStage.DONE -> ReviewList(
                    isWide = isWide,
                    transactions = state.transactions,
                    categories = state.categories,
                    customActions = customActions,
                    storeName = state.storeName,
                    receiptDate = state.receiptDate,
                    discount = state.discount,
                    tax = state.tax,
                    taxOnTop = state.taxOnTop,
                    extraCharges = state.extraCharges,
                    expectedItemsTotal = state.expectedItemsTotal,
                    receiptSubtotal = state.receiptSubtotal,
                    isEdit = isEdit,
                    isManual = state.isManual,
                    error = state.error,
                    onStoreChange = onStoreChange,
                    onDateChange = onDateChange,
                    onNameChange = onNameChange,
                    onCategoryChange = onCategoryChange,
                    onPriceChange = onPriceChange,
                    onQuantityChange = onQuantityChange,
                    onDiscountChange = onDiscountChange,
                    onRemove = onRemove,
                    onAddRow = onAddRow,
                    onFinalize = onFinalize,
                    onAddReceipt = onAddReceipt,
                )
            }
        }
    }
}

/**
 * Full-screen extraction-failure state. Splits a receipt we genuinely couldn't read (poor photo /
 * model abstention — [UploadErrorKind.UNREADABLE], "retake the photo") from a backend/service failure
 * ([UploadErrorKind.SERVICE] — an HTTP/network/billing error on the proxy, "try again later"), since
 * the two have different fixes. Offers "Go back" and "Try again" — the latter re-opens capture for the
 * original source. Centred with a capped width so it reads as a balanced card on tablets too.
 */
@Composable
private fun ErrorState(kind: UploadErrorKind, onBack: () -> Unit, onRetry: () -> Unit) {
    val titleRes = when (kind) {
        UploadErrorKind.UNREADABLE -> R.string.upload_error_title
        UploadErrorKind.SERVICE -> R.string.upload_service_error_title
    }
    val bodyRes = when (kind) {
        UploadErrorKind.UNREADABLE -> R.string.upload_error_body
        UploadErrorKind.SERVICE -> R.string.upload_service_error_body
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Error illustration tile: a soft error-container square with an error glyph, mirroring
            // the BudgettyIllustration tile used by the empty states.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.xxl))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.xxl))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Text(stringResource(R.string.upload_go_back), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1f)
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Text(stringResource(R.string.upload_try_again), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Creates a content:// uri (via FileProvider) in the cache for the camera to write a photo to. */
private fun createReceiptImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * Copies the document scanner's output image into our own cache and returns a content:// uri for it.
 * The scanner's own uri can report a null MIME type — which the ingest pipeline rejects as an
 * unsupported type — and carries only a temporary read grant; re-hosting the bytes under our
 * FileProvider as a .jpg guarantees a readable, image/jpeg-typed uri. Called from the result callback
 * while that read grant is still live. Returns null if the copy fails (caller falls back to the raw uri).
 */
private fun persistScannedImage(context: Context, source: Uri): Uri? {
    return try {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
        val copied = context.contentResolver.openInputStream(source)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false
        if (copied) FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file) else null
    } catch (e: Exception) {
        null
    }
}

/** Walks the ContextWrapper chain to the hosting Activity — the document scanner needs one to start. */
private fun Context.findActivity(): Activity {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found in the context chain")
}

@Composable
private fun Loading(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.dimens.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ReceiptScanAnimation()
        Spacer(Modifier.height(28.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A receipt illustration with a primary-colored scan line sweeping up and down while we work. */
@Composable
private fun ReceiptScanAnimation() {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanY",
    )
    val receiptWidth = 168.dp
    val receiptHeight = 208.dp
    Box(
        modifier = Modifier
            .size(receiptWidth, receiptHeight)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusLg))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        // Faux receipt content: a bolder store line, then thinner item lines.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.dimens.xl),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            FauxBar(0.55f, 13.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
            Spacer(Modifier.height(2.dp))
            listOf(1f, 0.85f, 0.92f, 0.7f, 0.95f, 0.8f).forEach { f ->
                FauxBar(f, MaterialTheme.dimens.sm, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f))
            }
        }
        val lineY = (receiptHeight - 3.dp) * progress
        // Soft beam behind the crisp scan line.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = lineY - 14.dp)
                .height(30.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = lineY)
                .height(3.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun FauxBar(fraction: Float, height: Dp, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(color),
    )
}

/**
 * The items-sum is flagged against the receipt's own stated figure only when it EXCEEDS that figure by
 * more than the larger of this fraction or [MISMATCH_TOLERANCE_ABS] — i.e. an over-read: a line price
 * read too high, or a printed discount missed. That inflates summed spend and, because the discount is
 * derived as `items − printed total`, silently inflates the savings too. Directional on purpose — items
 * summing to *less* is the benign "model netted a per-line coupon into the price" case the discount
 * reconciliation already absorbs, so warning there is just noise. Tightened from 2% (which let a ~€0.66
 * single-line over-read slip under the ~€0.67 threshold on a ~€33 receipt) to 1% with a €0.15 floor —
 * still loose enough to ignore cent-level rounding.
 */
private val MISMATCH_TOLERANCE_RATIO = BigDecimal("0.01")
private val MISMATCH_TOLERANCE_ABS = BigDecimal("0.15")

/**
 * A saved total inflated far beyond the item sum by an unexplained residual — money not itemized as
 * fees/tip/tax — when the receipt prints NO subtotal to anchor the [itemsShortfall] check. That's the
 * signature of line items read in the wrong currency (Bulgaria's euro/leva changeover: a leva grand
 * total on euro prices ≈ doubles the total) or a dropped line whose gap gets buried in extra charges.
 * Flagged once that residual exceeds BOTH this fraction of the item sum and [UNEXPLAINED_TOTAL_ABS], so
 * a genuine small deposit/fee (a low fraction of the bill) doesn't trip it.
 */
private val UNEXPLAINED_TOTAL_RATIO = BigDecimal("0.5")
private val UNEXPLAINED_TOTAL_ABS = BigDecimal("1.50")

/**
 * Soft, dismissible-by-ignoring notice shown on review when the line items sum to MORE than the
 * receipt's own total/subtotal — the signature of a misread (over-read) price. It only informs; the
 * user stays in control of the numbers (it never overrides the saved total). Styled as a caution.
 */
@Composable
private fun PriceMismatchNotice(itemsTotal: BigDecimal, receiptTotal: BigDecimal) {
    CautionNotice(
        stringResource(R.string.upload_total_mismatch, itemsTotal.formatMoney(), receiptTotal.formatMoney()),
    )
}

/**
 * Shared caution-styled inline notice (secondary container, warning icon) used on the review screen.
 * Non-blocking: it informs and the user stays in control — it never changes the saved total or gates
 * Finalize. Styled as a caution, deliberately not the red error color.
 */
@Composable
private fun CautionNotice(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.dimens.sm)
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(MaterialTheme.dimens.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(MaterialTheme.dimens.xl),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ReviewList(
    isWide: Boolean,
    transactions: List<ParsedTransaction>,
    categories: List<CategoryEntity>,
    customActions: CustomCategoryActions,
    storeName: String,
    receiptDate: Long,
    discount: BigDecimal,
    tax: BigDecimal,
    taxOnTop: Boolean,
    extraCharges: BigDecimal,
    expectedItemsTotal: BigDecimal?,
    receiptSubtotal: BigDecimal?,
    isEdit: Boolean,
    isManual: Boolean,
    error: String?,
    onStoreChange: (String) -> Unit,
    onDateChange: (Long) -> Unit,
    onNameChange: (String, String) -> Unit,
    onCategoryChange: (String, String) -> Unit,
    onPriceChange: (String, String) -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onDiscountChange: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAddRow: () -> Unit,
    onFinalize: () -> Unit,
    onAddReceipt: () -> Unit,
) {
    val gross = transactions.fold(BigDecimal.ZERO) { acc, txn ->
        acc + txn.price.multiply(BigDecimal(txn.quantity))
    }
    // A tax-exclusive receipt keeps its line prices net, so its tax is added on top to reach what was
    // paid; a tax-inclusive receipt already has the tax inside the prices and adds nothing here. Extra
    // charges (delivery/service fees, a courier tip) are likewise added so the total equals what was paid.
    val addedTax = if (taxOnTop) tax else BigDecimal.ZERO
    val total = (gross - discount + addedTax + extraCharges).coerceAtLeast(BigDecimal.ZERO)

    // Soft, non-blocking sanity check: when the items as read add up to MORE than what the receipt
    // itself states (its subtotal, or its total once tax/discount are accounted for), a line price was
    // likely read too high — or a printed discount was missed — so summed spend (and the derived
    // discount) is inflated; nudge the user to check. Only this over-read direction fires: items
    // summing to less is the benign coupon-netting case the discount reconciliation already handles.
    // Recomputes live, so it clears as they fix a price, and it never changes the saved total. Null
    // anchor (manual entry / edit) => no notice.
    val priceMismatch = expectedItemsTotal?.takeIf {
        it.signum() > 0 &&
            (gross - it) > maxOf(it.multiply(MISMATCH_TOLERANCE_RATIO), MISMATCH_TOLERANCE_ABS)
    }

    // Blocking check for the opposite, data-losing direction: the read items sum to noticeably LESS
    // than the receipt's own printed subtotal (the clean item-sum anchor — fees/deposits sit above it).
    // That signature means a line was dropped or under-read; because the saved total anchors on the
    // printed grand total, the shortfall would otherwise vanish silently into "extra charges". So we
    // confirm before saving, rather than warn softly. Recomputes live, so fixing/adding the line clears
    // it. Holds the subtotal to show when tripped.
    val itemsShortfall = receiptSubtotal?.takeIf { sub ->
        sub.signum() > 0 &&
            (sub - gross) > maxOf(sub.multiply(MISMATCH_TOLERANCE_RATIO), MISMATCH_TOLERANCE_ABS)
    }

    // Same data-losing direction, for receipts that print NO subtotal to anchor the check above — the
    // exact hole that let a dual-currency receipt through: the saved total is inflated far past the
    // items by an unexplained residual (money not itemized as fees/tip/tax and buried in [extraCharges]).
    // Bulgaria's euro changeover is the canonical case — a leva grand total on euro line items nearly
    // doubles the total. Surfaced as a SOFT, non-blocking notice below (see [CautionNotice]), NOT the
    // confirm dialog: the items themselves are usually read correctly and the user reviews them before
    // finalizing, so a hard stop they can't easily resolve would overdo it. Holds the inflated total.
    val inflatedTotal = total.takeIf {
        receiptSubtotal == null &&
            extraCharges > maxOf(gross.multiply(UNEXPLAINED_TOTAL_RATIO), UNEXPLAINED_TOTAL_ABS)
    }

    var showMismatchDialog by remember { mutableStateOf(false) }
    // Route Finalize through the dropped-line check: confirm on a subtotal shortfall, else save straight
    // away. (The [inflatedTotal] case warns softly inline instead, so it never blocks here.)
    val attemptFinalize: () -> Unit = {
        if (itemsShortfall != null) showMismatchDialog = true else onFinalize()
    }

    if (showMismatchDialog && itemsShortfall != null) {
        AlertDialog(
            onDismissRequest = { showMismatchDialog = false },
            icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null) },
            title = { Text(stringResource(R.string.upload_items_missing_title)) },
            text = {
                Text(stringResource(R.string.upload_total_mismatch, gross.formatMoney(), itemsShortfall.formatMoney()))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMismatchDialog = false
                        onFinalize()
                    },
                ) { Text(stringResource(R.string.upload_save_anyway)) }
            },
            dismissButton = {
                TextButton(onClick = { showMismatchDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    val saveLabel = "${if (isEdit) stringResource(R.string.action_save) else stringResource(R.string.upload_finalize)} · ${total.formatMoney()}"

    // The scrollable item list, shared by both layouts (the discount field only joins it inline on
    // the phone; in landscape it moves to the summary panel).
    val itemList: @Composable (Modifier, Boolean) -> Unit = { listMod, includeDiscount ->
        LazyColumn(
            modifier = listMod,
            contentPadding = PaddingValues(start = MaterialTheme.dimens.lg, end = MaterialTheme.dimens.lg, top = MaterialTheme.dimens.md, bottom = MaterialTheme.dimens.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            item {
                // Header: the editable store name (spreads to fill) beside a compact, tap-to-edit
                // date block — the two-block layout from the design. The screen title already lives
                // in the top app bar, so there's no separate heading here.
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md)) {
                    StoreCard(
                        storeName = storeName,
                        onStoreChange = onStoreChange,
                        modifier = Modifier.weight(1f),
                    )
                    DateCard(date = receiptDate, onDateChange = onDateChange)
                }
            }
            items(transactions, key = { it.clientId }) { txn ->
                ReviewRow(
                    transaction = txn,
                    categories = categories,
                    customActions = customActions,
                    // Keep at least one row: the last remaining item can't be removed here.
                    canRemove = transactions.size > 1,
                    onNameChange = { onNameChange(txn.clientId, it) },
                    onCategoryChange = { onCategoryChange(txn.clientId, it) },
                    onPriceChange = { onPriceChange(txn.clientId, it) },
                    onQuantityChange = { onQuantityChange(txn.clientId, it) },
                    onRemove = { onRemove(txn.clientId) },
                )
            }
            item {
                AddTransactionButton(onClick = onAddRow)
            }
            if (includeDiscount) {
                item {
                    DiscountField(discount = discount, onDiscountChange = onDiscountChange)
                }
            }
        }
    }

    // Error message + the Finalize (and optional Add-receipt) buttons, shared by both layouts.
    val finalizeArea: @Composable (Modifier) -> Unit = { areaMod ->
        Column(modifier = areaMod) {
            // The finalize total already includes the tax — contained in the line prices for a
            // tax-inclusive receipt, or added on top (see [addedTax]) for a tax-exclusive one — so it's
            // shown as an "incl. VAT" note beneath the total. Hidden when the receipt reports no tax.
            if (tax.signum() > 0) {
                Text(
                    text = stringResource(R.string.receipt_vat_included, tax.formatMoney()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = MaterialTheme.dimens.sm),
                )
            }
            if (priceMismatch != null) {
                PriceMismatchNotice(itemsTotal = gross, receiptTotal = priceMismatch)
            }
            if (inflatedTotal != null) {
                CautionNotice(
                    stringResource(R.string.upload_total_currency_notice, gross.formatMoney(), inflatedTotal.formatMoney()),
                )
            }
            if (error != null) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = MaterialTheme.dimens.sm),
                )
            }
            // For a manually-added receipt being edited, offer "Add receipt" next to Save so the user
            // can scan a real receipt and append its items.
            if (isEdit && isManual) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
                ) {
                    OutlinedButton(
                        onClick = onAddReceipt,
                        modifier = Modifier
                            .weight(1f)
                            .height(MaterialTheme.dimens.buttonHeight),
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                        Spacer(Modifier.width(MaterialTheme.dimens.sm))
                        Text(stringResource(R.string.add_receipt_title), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = attemptFinalize,
                        modifier = Modifier
                            .weight(1f)
                            .height(MaterialTheme.dimens.buttonHeight),
                    ) {
                        Text(
                            text = saveLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            } else {
                Button(
                    onClick = attemptFinalize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MaterialTheme.dimens.buttonHeight),
                ) {
                    Text(
                        text = saveLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (isWide) {
        // Landscape: scrollable items on the left, a pinned summary/finalize panel on the right —
        // matching the TabletLs Review & Edit design.
        Row(modifier = Modifier.fillMaxSize()) {
            itemList(Modifier.weight(1f).fillMaxHeight(), false)
            Column(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .padding(MaterialTheme.dimens.lg),
            ) {
                DiscountField(discount = discount, onDiscountChange = onDiscountChange)
                Spacer(Modifier.weight(1f))
                Text(
                    text = total.formatMoney(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = MaterialTheme.dimens.md),
                )
                finalizeArea(Modifier.fillMaxWidth())
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            itemList(Modifier.fillMaxWidth().weight(1f), true)
            finalizeArea(Modifier.padding(MaterialTheme.dimens.lg).fillMaxWidth())
        }
    }
}

/** Rounded corner radius shared by the filled input fields on this screen. */
private val FieldShape = RoundedCornerShape(12.dp)

/**
 * Filled-field colours used across the review form: a light inset fill (lighter than the
 * surrounding card), no indicator line, and a primary-coloured label that stays accented
 * whether or not the field is focused.
 */
@Composable
private fun reviewFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.primary,
    // Clearly-muted placeholder (e.g. the "0.00" price/discount hint) so it never reads as filled-in.
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
)

/**
 * Store header card: a tonal store glyph and the editable (rename-able) store name shown as the
 * card's headline. The trailing pencil focuses the name field for a quick rename.
 */
@Composable
private fun StoreCard(
    storeName: String,
    onStoreChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(MaterialTheme.dimens.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.upload_store_name),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                BasicTextField(
                    value = storeName,
                    onValueChange = onStoreChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { inner ->
                        if (storeName.isEmpty()) {
                            Text(
                                text = stringResource(R.string.upload_store_hint),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.avatar)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .clickable { focusRequester.requestFocus() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.cd_edit_store),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaterialTheme.dimens.xl),
                )
            }
        }
    }
}

/**
 * Compact date block shown opposite the store card: a tonal calendar glyph beside the receipt's
 * day + month (no year, e.g. "24 Jun"). Tapping anywhere on the card opens the Material date picker —
 * there's no separate edit affordance. The picker works in UTC, so we convert to/from the device's
 * local calendar day to keep the chosen date stable across time zones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateCard(date: Long, onDateChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .clickable { showPicker = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = stringResource(R.string.cd_edit_date),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(MaterialTheme.dimens.md))
            Text(
                text = date.formatDayMonth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.toUtcDayMillis())
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onDateChange(it.toLocalDayMillis()) }
                    showPicker = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** Local-day millis → the UTC-midnight millis the date picker uses to highlight that calendar day. */
private fun Long.toUtcDayMillis(): Long =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Date picker's UTC-midnight selection → local noon on that calendar day (stable for bucketing). */
private fun Long.toLocalDayMillis(): Long =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
        .atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Dashed-outline "Add transaction" button, matching the design's tonal add affordance. */
@Composable
private fun AddTransactionButton(onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(MaterialTheme.dimens.buttonHeight)
            .clip(FieldShape)
            .clickable(onClick = onClick)
            .dashedBorder(color = color, cornerRadius = MaterialTheme.dimens.md),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = color)
            Spacer(Modifier.width(MaterialTheme.dimens.sm))
            Text(stringResource(R.string.upload_add_transaction), color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Draws a dashed rounded-rectangle border just inside the element's bounds. */
private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.5.dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 4.dp,
) = this.drawBehind {
    val sw = strokeWidth.toPx()
    val stroke = Stroke(
        width = sw,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength.toPx(), gapLength.toPx())),
    )
    val inset = sw / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - sw, size.height - sw),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke,
    )
}

/** Editable order-level discount, shown below the item rows; applied to the finalize total. */
@Composable
private fun DiscountField(
    discount: BigDecimal,
    onDiscountChange: (String) -> Unit,
) {
    var discountText by remember {
        mutableStateOf(if (discount.signum() == 0) "" else discount.toEditableMoney())
    }
    Card(
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
        ) {
            Text("🏷️", style = MaterialTheme.typography.titleLarge)
            TextField(
                value = discountText,
                onValueChange = {
                    discountText = it
                    onDiscountChange(it)
                },
                label = { Text(stringResource(R.string.upload_discount), fontWeight = FontWeight.SemiBold) },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = FieldShape,
                colors = reviewFieldColors(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * A money value formatted for an editable field: always 2 decimals, so a scanned "3.20" shows as
 * "3.20" (not "3.2") and whole amounts show "10.00". Uses a dot; [UploadViewModel.updatePrice]
 * accepts either a dot or a comma when parsing the field back.
 */
private fun BigDecimal.toEditableMoney(): String =
    setScale(2, RoundingMode.HALF_UP).toPlainString()

@Composable
private fun ReviewRow(
    transaction: ParsedTransaction,
    categories: List<CategoryEntity>,
    customActions: CustomCategoryActions,
    canRemove: Boolean,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    // Local text state so the user can type freely (e.g. trailing "." while entering
    // a decimal) without the parsed BigDecimal snapping the field back.
    // Show a scanned price to 2 decimals so trailing zeros survive — "3.20" stays "3.20" (not "3.2")
    // and whole amounts read "10.00". A zero price shows as empty (the "0.00" placeholder) so a new
    // manual row starts clean and the user can type freely.
    var priceText by remember(transaction.clientId) {
        mutableStateOf(if (transaction.price.signum() == 0) "" else transaction.price.toEditableMoney())
    }
    var qtyText by remember(transaction.clientId) {
        mutableStateOf(transaction.quantity.toString())
    }

    Card(
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = transaction.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.upload_product), fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = reviewFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(MaterialTheme.dimens.sm))
                IconButton(
                    onClick = onRemove,
                    enabled = canRemove,
                    modifier = Modifier
                        .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                        .background(
                            if (canRemove) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = if (canRemove) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
            Spacer(Modifier.padding(MaterialTheme.dimens.xs))
            CategoryField(
                category = transaction.category,
                fromRule = transaction.fromRule,
                onCategoryChange = onCategoryChange,
                customActions = customActions,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(MaterialTheme.dimens.xs))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                TextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        onPriceChange(it)
                    },
                    label = { Text(stringResource(R.string.upload_price), fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = FieldShape,
                    colors = reviewFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                TextField(
                    value = qtyText,
                    onValueChange = {
                        qtyText = it.filter(Char::isDigit)
                        onQuantityChange(qtyText.toIntOrNull() ?: 1)
                    },
                    label = { Text(stringResource(R.string.upload_qty), fontWeight = FontWeight.SemiBold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = FieldShape,
                    colors = reviewFieldColors(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Category picker: a read-only field that opens the grouped [CategoryPickerScreen] (groups +
 * sub-categories, with emojis) for selection.
 */
@Composable
private fun CategoryField(
    category: String,
    fromRule: Boolean,
    onCategoryChange: (String) -> Unit,
    customActions: CustomCategoryActions = CustomCategoryActions(),
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val display = if (category.isBlank()) "" else "${Categories.emojiOf(category)} $category".trim()
    Box(modifier = modifier) {
        TextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.upload_category), fontWeight = FontWeight.SemiBold)
                    // Auto-filled from a saved rule → show the subtle "from your rules" badge, in the
                    // floating label next to "Category" (the field is always populated when this is set).
                    if (fromRule) {
                        Spacer(Modifier.width(6.dp))
                        FromRuleBadge()
                    }
                }
            },
            placeholder = { Text(stringResource(R.string.upload_select_category)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            singleLine = true,
            shape = FieldShape,
            colors = reviewFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        // A read-only field won't receive taps, so overlay a transparent click target.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showSheet = true },
        )
    }

    if (showSheet) {
        CategoryPickerScreen(
            selected = category,
            onSelect = onCategoryChange,
            custom = customActions,
            onDismiss = { showSheet = false },
        )
    }
}

/**
 * The "✦ from your rules" pill shown on the Category field when a scanned item's category was set
 * automatically from a saved [com.budgetty.app.data.local.CategoryRuleEntity]. Tinted in the primary
 * color at low alpha so it reads as a quiet, trust-building hint rather than a nag.
 */
@Composable
private fun FromRuleBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(MaterialTheme.dimens.xs))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = "✦",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = stringResource(R.string.upload_from_rules),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

/**
 * Bottom sheet shown by "Add receipt" on a manually-added receipt: pick a photo or a file, which is
 * then scanned to attach + fill in the receipt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReceiptSourceSheet(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onUploadFile: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AdaptiveSheet(onDismiss = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = MaterialTheme.dimens.xl, end = MaterialTheme.dimens.xl, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.add_receipt_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.lg))
            AddReceiptOption(
                icon = Icons.Filled.PhotoCamera,
                title = stringResource(R.string.add_take_photo),
                subtitle = stringResource(R.string.add_take_photo_sub),
                onClick = onTakePhoto,
            )
            Spacer(Modifier.height(10.dp))
            AddReceiptOption(
                icon = Icons.Filled.UploadFile,
                title = stringResource(R.string.add_upload_file),
                subtitle = stringResource(R.string.add_upload_file_sub),
                onClick = onUploadFile,
            )
        }
    }
}

@Composable
private fun AddReceiptOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(MaterialTheme.dimens.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * "Apply to matching items?" — shown after the user reassigns a category on the review/edit screen
 * when the item has other saved occurrences. A bottom sheet on phones, a centered dialog on tablets
 * (via [AdaptiveSheet]). Two modes: when [PropagationPrompt.otherCount] > 0 it offers to recategorize
 * the differing matches, with a "remember this" checkbox that saves/updates (or, unchecked, clears)
 * the forward rule; when it is 0 (the siblings already use this category, e.g. the default Groceries)
 * it shows a lean "remember for future?" prompt so a rule can still be set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPropagationSheet(
    prompt: PropagationPrompt,
    onConfirm: (applyToAll: Boolean, remember: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val wide = isExpandedWidth()
    var rememberRule by remember(prompt) { mutableStateOf(true) }

    val chipColor = Color(prompt.categoryColor)
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val chipBg = chipColor.copy(alpha = if (dark) 0.24f else 0.15f)
    val chipFg = if (dark) lerp(chipColor, Color.White, 0.35f) else lerp(chipColor, Color.Black, 0.42f)
    val catLabel = "${Categories.emojiOf(prompt.category)} ${prompt.category}".trim()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    AdaptiveSheet(
        onDismiss = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = MaterialTheme.dimens.xl).padding(bottom = MaterialTheme.dimens.md)) {
            if (prompt.otherCount > 0) {
                Text(
                    text = stringResource(R.string.rule_prompt_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = ruleSentence(
                        full = pluralStringResource(
                            R.plurals.rule_prompt_body,
                            prompt.otherCount,
                            prompt.otherCount,
                            prompt.name,
                            catLabel,
                        ),
                        name = prompt.name,
                        chip = catLabel,
                        chipBg = chipBg,
                        chipFg = chipFg,
                        onSurface = onSurface,
                        baseColor = onSurfaceVariant,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(MaterialTheme.dimens.md))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MaterialTheme.dimens.radiusMd))
                        .clickable { rememberRule = !rememberRule },
                    verticalAlignment = Alignment.Top,
                ) {
                    Checkbox(checked = rememberRule, onCheckedChange = { rememberRule = it })
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = MaterialTheme.dimens.md),
                    ) {
                        Text(
                            text = ruleSentence(
                                full = stringResource(R.string.rule_prompt_remember, catLabel, prompt.name),
                                name = prompt.name,
                                chip = catLabel,
                                chipBg = chipBg,
                                chipFg = chipFg,
                                onSurface = onSurface,
                                baseColor = if (rememberRule) onSurface else onSurfaceVariant,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (prompt.ruleExists) {
                            Spacer(Modifier.height(MaterialTheme.dimens.xs))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = onSurfaceVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(MaterialTheme.dimens.xs))
                                Text(
                                    text = stringResource(R.string.rule_prompt_rule_exists),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                PropagationButtons(
                    wide = wide,
                    primaryLabel = pluralStringResource(R.plurals.rule_prompt_apply, prompt.otherCount, prompt.otherCount),
                    secondaryLabel = stringResource(R.string.rule_prompt_just_this),
                    onPrimary = { onConfirm(true, rememberRule) },
                    onSecondary = { onConfirm(false, rememberRule) },
                )
            } else {
                Text(
                    text = stringResource(R.string.rule_prompt_remember_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = ruleSentence(
                        full = stringResource(R.string.rule_prompt_remember_body, catLabel, prompt.name),
                        name = prompt.name,
                        chip = catLabel,
                        chipBg = chipBg,
                        chipFg = chipFg,
                        onSurface = onSurface,
                        baseColor = onSurfaceVariant,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.xl))
                PropagationButtons(
                    wide = wide,
                    primaryLabel = stringResource(R.string.rule_prompt_remember_confirm),
                    secondaryLabel = stringResource(R.string.rule_prompt_not_now),
                    onPrimary = { onConfirm(false, true) },
                    onSecondary = onDismiss,
                )
            }
        }
    }
}

/** The two prompt actions: stacked (primary on top) on phones, right-aligned inline on tablets. */
@Composable
private fun PropagationButtons(
    wide: Boolean,
    primaryLabel: String,
    secondaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.End),
        ) {
            FilledTonalButton(onClick = onSecondary, modifier = Modifier.height(MaterialTheme.dimens.buttonHeight)) {
                Text(secondaryLabel, fontWeight = FontWeight.SemiBold)
            }
            Button(onClick = onPrimary, modifier = Modifier.height(MaterialTheme.dimens.buttonHeight)) {
                Text(primaryLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(primaryLabel, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(9.dp))
        FilledTonalButton(
            onClick = onSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(secondaryLabel, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Builds a prompt sentence with the quoted item name bolded and the category rendered as a tinted
 * inline chip (background + colour on its "emoji name" run). Spans are located by substring — the
 * strings are English-only and both the quoted name and the category label are distinctive.
 */
private fun ruleSentence(
    full: String,
    name: String,
    chip: String,
    chipBg: Color,
    chipFg: Color,
    onSurface: Color,
    baseColor: Color,
): AnnotatedString = buildAnnotatedString {
    append(full)
    addStyle(SpanStyle(color = baseColor), 0, full.length)
    val quoted = "“$name”"
    full.indexOf(quoted).takeIf { it >= 0 }?.let {
        addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = onSurface), it, it + quoted.length)
    }
    full.indexOf(chip).takeIf { it >= 0 }?.let {
        addStyle(
            SpanStyle(background = chipBg, color = chipFg, fontWeight = FontWeight.SemiBold),
            it,
            it + chip.length,
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun UploadScreenPreview() {
    BudgettyTheme {
        UploadScreenContent(
            state = UploadUiState(stage = UploadStage.REVIEW, storeName = "Kaufland"),
            isEdit = false,
            isWide = true,
            onStoreChange = {},
            onDateChange = {},
            onNameChange = { _, _ -> },
            onCategoryChange = { _, _ -> },
            onPriceChange = { _, _ -> },
            onQuantityChange = { _, _ -> },
            onDiscountChange = {},
            onRemove = {},
            onAddRow = {},
            onFinalize = {},
            onAddReceipt = {},
            onRetry = {},
            onNavigateBack = {},
        )
    }
}

@Preview(name = "Upload error · phone", showBackground = true, heightDp = 740)
@Preview(name = "Upload error · tablet", showBackground = true, widthDp = 840, heightDp = 740)
@Composable
private fun UploadErrorPreview() {
    BudgettyTheme {
        UploadScreenContent(
            state = UploadUiState(stage = UploadStage.IDLE, error = "boom"),
            isEdit = false,
            isWide = false,
            onStoreChange = {},
            onDateChange = {},
            onNameChange = { _, _ -> },
            onCategoryChange = { _, _ -> },
            onPriceChange = { _, _ -> },
            onQuantityChange = { _, _ -> },
            onDiscountChange = {},
            onRemove = {},
            onAddRow = {},
            onFinalize = {},
            onAddReceipt = {},
            onRetry = {},
            onNavigateBack = {},
        )
    }
}
