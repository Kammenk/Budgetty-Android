package com.budgetty.app.category

/**
 * Which bucket of the 50/30/20 rule a category's spend counts toward, for the Insights
 * Needs / Wants / Savings split:
 *
 * - [NEED]   — the things you'd keep paying in a lean month (housing, utilities, groceries,
 *              transport, insurance, health). Target 50% of income.
 * - [WANT]   — discretionary spending (dining, shopping, entertainment, travel). Target 30%.
 * - [SAVINGS] — money set aside (savings-goal transfers, investments). Target 20%.
 *
 * Every category resolves to a bucket: a built-in from [Categories.defaultBucketOf] (its group's
 * default, with a few per-category exceptions), a sub-category inherits its parent's, and a user can
 * override any of them in Manage categories. Kept Android-free (a plain enum) so it can live on the
 * Room entity, flow through the [com.budgetty.app.data.local.Converters] as its `name`, and be reused
 * by the pure taxonomy logic in [Categories].
 */
enum class CategoryBucket { NEED, WANT, SAVINGS }
