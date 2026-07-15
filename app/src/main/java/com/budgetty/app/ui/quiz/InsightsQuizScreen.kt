package com.budgetty.app.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.budgetty.app.R
import com.budgetty.app.data.settings.Currency
import com.budgetty.app.ui.theme.BudgettyTheme
import com.budgetty.app.ui.theme.dimens
import com.budgetty.app.ui.util.formatMoney
import com.budgetty.app.ui.util.isExpandedWidth
import java.util.Locale
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/** How long a freshly-picked option shows its selected state before the quiz auto-advances. */
private const val AUTO_ADVANCE_MS = 300L

/** Width cap of the centred quiz column on expanded (tablet) layouts. */
private val TabletColumnWidth = 520.dp

/**
 * The one-time post-signup Insights setup questionnaire (see [InsightsQuiz] for the steps and the
 * answer → customization mapping). Shown as a full-screen gate between registration and the main
 * app; finishing applies the derived section visibility/order plus the optional currency, income
 * and budget seeds, and both finishing and skipping clear the pending flag that keeps the gate up.
 */
@Composable
fun InsightsQuizScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsQuizViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val incomeLabel = stringResource(R.string.recurring_income)
    InsightsQuizContent(
        isExpanded = isExpandedWidth(),
        currency = settings.currency,
        onCurrencySelected = viewModel::selectCurrency,
        onFinish = { answers, amountTexts ->
            viewModel.finish(answers, amountTexts, incomeLabel)
        },
        onSkip = viewModel::skip,
        modifier = modifier,
    )
}

/** Round-trips the answer map through rememberSaveable as "id=value,…" (same shape as [InsightsQuiz.encode]). */
private val AnswersSaver = Saver<Map<String, String>, String>(
    save = { answers -> answers.entries.joinToString(",") { (id, value) -> "$id=$value" } },
    restore = { encoded ->
        encoded.split(",")
            .filter { it.contains('=') }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
    },
)

@Composable
private fun InsightsQuizContent(
    isExpanded: Boolean,
    currency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    onFinish: (answers: Map<String, String>, amountTexts: Map<String, String>) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stepCount = InsightsQuiz.stepCount
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    var answers by rememberSaveable(stateSaver = AnswersSaver) { mutableStateOf(emptyMap<String, String>()) }
    // Amount-field texts keyed by question id. Sanitized on input (digits + one point), so the
    // encoded "id=value," saver format stays unambiguous.
    var amountTexts by rememberSaveable(stateSaver = AnswersSaver) { mutableStateOf(emptyMap<String, String>()) }
    // Armed by tapping an auto-advancing option: shows the selected state briefly, then moves on.
    // Deliberately not saved — restoring mid-advance would just re-land on the same question.
    var advanceArmed by remember { mutableStateOf(false) }
    val isDoneStep = stepIndex == stepCount

    LaunchedEffect(stepIndex, advanceArmed) {
        if (advanceArmed) {
            delay(AUTO_ADVANCE_MS)
            advanceArmed = false
            stepIndex += 1
        }
    }

    // Hardware back mirrors the chevron. On the first question the quiz is a gate with nowhere to
    // go back to, so the default (leaving the app) applies; next launch resumes the quiz.
    BackHandler(enabled = stepIndex > 0) {
        advanceArmed = false
        stepIndex -= 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .then(if (isExpanded) Modifier.widthIn(max = TabletColumnWidth) else Modifier)
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.screenPadding)
                .padding(
                    top = if (isExpanded) 32.dp else MaterialTheme.dimens.md,
                    bottom = if (isExpanded) 48.dp else MaterialTheme.dimens.xl,
                ),
        ) {
            // Top bar: back chevron (from the second step), progress, Skip (question steps only).
            // Fixed height so the bar doesn't jump as the chevron and Skip come and go.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.md),
            ) {
                if (stepIndex in 1 until stepCount) {
                    IconButton(
                        onClick = {
                            advanceArmed = false
                            stepIndex -= 1
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                }
                QuizProgress(
                    filled = (stepIndex + 1).coerceAtMost(stepCount),
                    stepCount = stepCount,
                    isExpanded = isExpanded,
                    modifier = Modifier.weight(1f),
                )
                if (!isDoneStep) {
                    TextButton(
                        onClick = onSkip,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.onb_skip),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = stepIndex,
                transitionSpec = {
                    // Slide with the direction of travel: forward from the end, back from the start.
                    val forward = targetState > initialState
                    val enter = slideInHorizontally { full -> if (forward) full / 3 else -full / 3 } + fadeIn()
                    val exit = slideOutHorizontally { full -> if (forward) -full / 3 else full / 3 } + fadeOut()
                    enter togetherWith exit
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "quizStep",
            ) { step ->
                val question = if (step < stepCount) InsightsQuiz.questionAt(step) else null
                when {
                    step == InsightsQuiz.CURRENCY_STEP -> QuizCurrencyStep(
                        stepNumber = step + 1,
                        stepCount = stepCount,
                        pinned = currency,
                        selectedCode = answers[InsightsQuiz.CURRENCY],
                        isExpanded = isExpanded,
                        onSelect = { picked ->
                            answers = answers + (InsightsQuiz.CURRENCY to picked.code)
                            onCurrencySelected(picked)
                            advanceArmed = true
                        },
                    )
                    question != null -> QuizQuestionStep(
                        question = question,
                        stepNumber = step + 1,
                        stepCount = stepCount,
                        selectedOptionId = answers[question.id],
                        amountText = amountTexts[question.id].orEmpty(),
                        currencySymbol = currency.symbol,
                        isExpanded = isExpanded,
                        onSelect = { optionId ->
                            answers = answers + (question.id to optionId)
                            // The field-revealing option waits for Continue; everything else
                            // auto-advances (and picking it cancels a pending advance).
                            advanceArmed = question.amountField?.optionId != optionId
                        },
                        onAmountChange = { text ->
                            amountTexts = amountTexts + (question.id to sanitizeAmountInput(text))
                        },
                        onContinue = { stepIndex += 1 },
                    )
                    else -> QuizDoneStep(
                        answers = answers,
                        incomeAmount = InsightsQuiz.incomeSeed(answers, amountTexts)?.formatMoney(),
                        budgetAmount = InsightsQuiz.budgetSeed(answers, amountTexts)?.formatMoney(),
                        onGetStarted = { onFinish(answers, amountTexts) },
                    )
                }
            }
        }
    }
}

/** The segmented step progress bar; [filled] segments light up in primary. */
@Composable
private fun QuizProgress(filled: Int, stepCount: Int, isExpanded: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 5.dp else MaterialTheme.dimens.xs),
    ) {
        repeat(stepCount) { index ->
            val color by animateColorAsState(
                if (index < filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                label = "segmentColor",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

/** The shared step header: "QUESTION X OF Y" overline, title, muted subtitle. */
@Composable
private fun QuizStepHeader(
    @StringRes titleRes: Int,
    @StringRes subtitleRes: Int,
    stepNumber: Int,
    stepCount: Int,
    isExpanded: Boolean,
) {
    Text(
        text = stringResource(R.string.quiz_progress, stepNumber, stepCount)
            .uppercase(Locale.getDefault()),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = if (isExpanded) 12.sp else 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.7.sp,
    )
    Spacer(Modifier.height(if (isExpanded) 10.dp else MaterialTheme.dimens.sm))
    Text(
        text = stringResource(titleRes),
        fontSize = if (isExpanded) 30.sp else 23.sp,
        lineHeight = if (isExpanded) 36.sp else 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.4).sp,
    )
    Spacer(Modifier.height(7.dp))
    Text(
        text = stringResource(subtitleRes),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = if (isExpanded) 15.sp else 13.sp,
        lineHeight = if (isExpanded) 22.sp else 19.sp,
    )
}

/** One question step: the shared header above the stacked option cards, plus the optional revealed amount field. */
@Composable
private fun QuizQuestionStep(
    question: QuizQuestion,
    stepNumber: Int,
    stepCount: Int,
    selectedOptionId: String?,
    amountText: String,
    currencySymbol: String,
    isExpanded: Boolean,
    onSelect: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        // The tablet mock centres the question block in the column; the phone keeps it up top.
        verticalArrangement = if (isExpanded) Arrangement.Center else Arrangement.Top,
    ) {
        if (!isExpanded) Spacer(Modifier.height(26.dp))
        QuizStepHeader(
            titleRes = question.titleRes,
            subtitleRes = question.subtitleRes,
            stepNumber = stepNumber,
            stepCount = stepCount,
            isExpanded = isExpanded,
        )
        Spacer(Modifier.height(if (isExpanded) 28.dp else 22.dp))
        question.options.forEachIndexed { index, option ->
            if (index > 0) Spacer(Modifier.height(if (isExpanded) 10.dp else 9.dp))
            QuizOptionCard(
                option = option,
                selected = option.id == selectedOptionId,
                isExpanded = isExpanded,
                onClick = { onSelect(option.id) },
            )
        }

        val amountField = question.amountField
        if (amountField != null) {
            val revealed = selectedOptionId == amountField.optionId
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(revealed) {
                if (revealed) {
                    // Let the reveal animation land before pulling up the keyboard.
                    delay(AUTO_ADVANCE_MS)
                    runCatching { focusRequester.requestFocus() }
                }
            }
            AnimatedVisibility(
                visible = revealed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(amountField.labelRes),
                        fontSize = if (isExpanded) 14.sp else 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(MaterialTheme.dimens.sm))
                    QuizAmountInput(
                        value = amountText,
                        symbol = currencySymbol,
                        isExpanded = isExpanded,
                        focusRequester = focusRequester,
                        onValueChange = onAmountChange,
                        onDone = onContinue,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = stringResource(amountField.helperRes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (isExpanded) 12.sp else 11.sp,
                        lineHeight = if (isExpanded) 17.sp else 16.sp,
                    )
                    Spacer(Modifier.height(MaterialTheme.dimens.lg))
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaterialTheme.dimens.buttonHeight),
                    ) {
                        Text(stringResource(R.string.quiz_continue))
                    }
                }
            }
        }
    }
}

/** An emoji-chip option card; selecting swaps it to the secondary-container + primary-border state. */
@Composable
private fun QuizOptionCard(
    option: QuizOption,
    selected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (isExpanded) 18.dp else MaterialTheme.dimens.radiusLg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
            )
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = if (isExpanded) 64.dp else 56.dp)
            .padding(
                horizontal = if (isExpanded) MaterialTheme.dimens.lg else 13.dp,
                vertical = if (isExpanded) MaterialTheme.dimens.md else 11.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isExpanded) 14.dp else MaterialTheme.dimens.md),
    ) {
        Box(
            modifier = Modifier
                .size(if (isExpanded) 44.dp else 38.dp)
                .clip(RoundedCornerShape(if (isExpanded) 13.dp else MaterialTheme.dimens.radiusMd))
                .background(
                    // The chip lightens to the screen background inside a selected card.
                    if (selected) MaterialTheme.colorScheme.background
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = option.emoji, fontSize = if (isExpanded) 22.sp else 19.sp)
        }
        Text(
            text = stringResource(option.labelRes),
            modifier = Modifier.weight(1f),
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurface,
            fontSize = if (isExpanded) 16.sp else 14.sp,
            lineHeight = if (isExpanded) 21.sp else 18.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
        // The check badge keeps its slot when unselected (alpha 0) so the label never reflows.
        Box(
            modifier = Modifier
                .size(if (isExpanded) 20.dp else 18.dp)
                .alpha(if (selected) 1f else 0f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(if (isExpanded) 12.dp else 11.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * The revealed amount field: bold amount with the currency symbol as a suffix right after the text
 * (Budgetty renders "2,400 €", never "€2,400"). The amount is optional, so there is no error state.
 */
@Composable
private fun QuizAmountInput(
    value: String,
    symbol: String,
    isExpanded: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (isExpanded) 19.sp else 17.sp,
            fontWeight = FontWeight.Bold,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Min width keeps the cursor visible while the field is empty.
                Box(Modifier.widthIn(min = 4.dp)) { innerTextField() }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = symbol,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (isExpanded) 16.sp else 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
            }
        },
    )
}

/**
 * The currency step: the device-region suggestion pinned on top (the current setting, so a picked
 * currency stays pinned when the user comes Back), then every other supported currency. Tapping a
 * row applies it and auto-advances like any other step.
 */
@Composable
private fun QuizCurrencyStep(
    stepNumber: Int,
    stepCount: Int,
    pinned: Currency,
    selectedCode: String?,
    isExpanded: Boolean,
    onSelect: (Currency) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = if (isExpanded) Arrangement.Center else Arrangement.Top,
    ) {
        if (!isExpanded) Spacer(Modifier.height(26.dp))
        QuizStepHeader(
            titleRes = R.string.quiz_q_currency,
            subtitleRes = R.string.quiz_q_currency_sub,
            stepNumber = stepNumber,
            stepCount = stepCount,
            isExpanded = isExpanded,
        )
        Spacer(Modifier.height(if (isExpanded) 22.dp else 18.dp))
        QuizSectionLabel(R.string.quiz_currency_suggested, isExpanded)
        Spacer(Modifier.height(7.dp))
        CurrencyRow(
            currency = pinned,
            selected = pinned.code == selectedCode,
            pinnedStyle = true,
            isExpanded = isExpanded,
            onClick = { onSelect(pinned) },
        )
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        QuizSectionLabel(R.string.quiz_currency_all, isExpanded)
        Spacer(Modifier.height(7.dp))
        Currency.entries.filterNot { it == pinned }.forEachIndexed { index, entry ->
            if (index > 0) Spacer(Modifier.height(7.dp))
            CurrencyRow(
                currency = entry,
                selected = entry.code == selectedCode,
                pinnedStyle = false,
                isExpanded = isExpanded,
                onClick = { onSelect(entry) },
            )
        }
    }
}

/** The tiny uppercase section label above the currency lists. */
@Composable
private fun QuizSectionLabel(@StringRes textRes: Int, isExpanded: Boolean) {
    Text(
        text = stringResource(textRes).uppercase(Locale.getDefault()),
        modifier = Modifier.padding(horizontal = 2.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = if (isExpanded) 11.sp else 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
    )
}

/** A compact currency row: symbol badge, code, localized name. [pinnedStyle] elevates the suggested row. */
@Composable
private fun CurrencyRow(
    currency: Currency,
    selected: Boolean,
    pinnedStyle: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (isExpanded) 14.dp else 13.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                when {
                    selected -> MaterialTheme.colorScheme.secondaryContainer
                    pinnedStyle -> MaterialTheme.colorScheme.surfaceContainerHigh
                    else -> MaterialTheme.colorScheme.surfaceContainer
                },
            )
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = if (isExpanded) 52.dp else 46.dp)
            .padding(horizontal = MaterialTheme.dimens.md, vertical = MaterialTheme.dimens.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (isExpanded) 36.dp else 32.dp)
                .clip(RoundedCornerShape(if (isExpanded) 11.dp else 10.dp))
                .background(
                    // Mirrors the option cards: the badge lightens to the screen background on the
                    // emphasized (pinned or selected) rows.
                    if (selected || pinnedStyle) MaterialTheme.colorScheme.background
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = currency.symbol,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = currencySymbolSize(currency.symbol, isExpanded),
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = currency.code,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurface,
            fontSize = if (isExpanded) 15.sp else 13.5.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = currencyDisplayName(currency.code),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (isExpanded) 14.sp else 12.5.sp,
        )
    }
}

/** The badge scales its glyph down for two- and three-letter symbols so "CHF" fits where "€" sits. */
private fun currencySymbolSize(symbol: String, isExpanded: Boolean) = when {
    symbol.length >= 3 -> if (isExpanded) 11.sp else 9.5.sp
    symbol.length == 2 -> if (isExpanded) 13.5.sp else 12.sp
    else -> if (isExpanded) 17.sp else 15.sp
}

/** Localized currency display name from ICU ("Euro", "Британска лира"), falling back to the code. */
private fun currencyDisplayName(code: String): String = runCatching {
    android.icu.util.Currency.getInstance(code).getDisplayName(Locale.getDefault())
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}.getOrDefault(code)

/**
 * Normalizes typed amount text so it stays parseable and saver-safe: digits plus at most one
 * decimal point (a typed comma becomes the point — EU decimal keyboards only offer ','), capped
 * so the field never outgrows its row.
 */
private fun sanitizeAmountInput(raw: String): String {
    val cleaned = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    val single = if (dot >= 0) {
        cleaned.take(dot + 1) + cleaned.drop(dot + 1).replace(".", "")
    } else {
        cleaned
    }
    return single.take(10)
}

/** The closing step: celebration glyph, the tailoring summary, the CTA, and the bills hand-off hint. */
@Composable
private fun QuizDoneStep(
    answers: Map<String, String>,
    incomeAmount: String?,
    budgetAmount: String?,
    onGetStarted: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl + MaterialTheme.dimens.xs))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🎉", fontSize = 38.sp)
            }
            Spacer(Modifier.height(MaterialTheme.dimens.xl))
            Text(
                text = stringResource(R.string.quiz_done_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(MaterialTheme.dimens.sm))
            Text(
                text = stringResource(R.string.quiz_done_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))
            QuizSummaryCard(lines = InsightsQuiz.summary(answers, incomeAmount, budgetAmount))
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.quiz_done_footnote),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(MaterialTheme.dimens.lg))
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimens.buttonHeight),
        ) {
            Text(stringResource(R.string.onb_get_started))
        }
        if (InsightsQuiz.showsBillsHint(answers)) {
            Spacer(Modifier.height(MaterialTheme.dimens.md))
            Text(
                text = stringResource(R.string.quiz_done_bills_hint),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The "what got tailored" rows on the closing step, one per meaningful answer. */
@Composable
private fun QuizSummaryCard(lines: List<QuizSummaryLine>, modifier: Modifier = Modifier) {
    if (lines.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaterialTheme.dimens.radiusXl))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = MaterialTheme.dimens.lg, vertical = MaterialTheme.dimens.xs),
    ) {
        lines.forEachIndexed { index, line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                Text(text = line.emoji, fontSize = 16.sp)
                Text(
                    text = line.arg?.let { stringResource(line.labelRes, it) }
                        ?: stringResource(line.labelRes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (index < lines.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 740)
@Composable
private fun InsightsQuizPreview() {
    BudgettyTheme {
        InsightsQuizContent(
            isExpanded = false,
            currency = Currency.EUR,
            onCurrencySelected = {},
            onFinish = { _, _ -> },
            onSkip = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun InsightsQuizTabletPreview() {
    BudgettyTheme {
        InsightsQuizContent(
            isExpanded = true,
            currency = Currency.EUR,
            onCurrencySelected = {},
            onFinish = { _, _ -> },
            onSkip = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 740)
@Composable
private fun InsightsQuizCurrencyPreview() {
    BudgettyTheme {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(MaterialTheme.dimens.screenPadding),
        ) {
            QuizCurrencyStep(
                stepNumber = 2,
                stepCount = InsightsQuiz.stepCount,
                pinned = Currency.EUR,
                selectedCode = null,
                isExpanded = false,
                onSelect = {},
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 740)
@Composable
private fun InsightsQuizAmountFieldPreview() {
    BudgettyTheme {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(MaterialTheme.dimens.screenPadding),
        ) {
            QuizQuestionStep(
                question = InsightsQuiz.questionAt(2)!!,
                stepNumber = 3,
                stepCount = InsightsQuiz.stepCount,
                selectedOptionId = "yes",
                amountText = "2400",
                currencySymbol = "€",
                isExpanded = false,
                onSelect = {},
                onAmountChange = {},
                onContinue = {},
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 740)
@Composable
private fun InsightsQuizDonePreview() {
    BudgettyTheme {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(MaterialTheme.dimens.screenPadding),
        ) {
            QuizDoneStep(
                answers = mapOf(
                    "goal" to "budget",
                    "currency" to "EUR",
                    "income" to "yes",
                    "bills" to "yes",
                    "budget" to "yes",
                    "detail" to "big",
                ),
                incomeAmount = "2,400.00 €",
                budgetAmount = "1,500.00 €",
                onGetStarted = {},
            )
        }
    }
}
