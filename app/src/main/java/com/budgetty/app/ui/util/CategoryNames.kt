package com.budgetty.app.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.budgetty.app.R

/**
 * Maps a category's **canonical** name (the English value stored in the DB and used by the AI
 * `record_receipt` enum) to a localized display-name resource. The canonical names never change;
 * only what the user sees does. Custom user categories (not in this map) display as-is.
 */
private val categoryNameRes: Map<String, Int> = mapOf(
    "Groceries" to R.string.cat_groceries,
    "Bakery" to R.string.cat_bakery,
    "Dairy" to R.string.cat_dairy,
    "Meat & Poultry" to R.string.cat_meat_poultry,
    "Fish & Seafood" to R.string.cat_fish_seafood,
    "Fruits & Vegetables" to R.string.cat_fruits_vegetables,
    "Snacks & Sweets" to R.string.cat_snacks_sweets,
    "Frozen Foods" to R.string.cat_frozen_foods,
    "Nuts & Snacks" to R.string.cat_nuts_snacks,
    "Canned & Preserved" to R.string.cat_canned_preserved,
    "Grains & Pasta" to R.string.cat_grains_pasta,
    "Condiments & Sauces" to R.string.cat_condiments_sauces,
    "Beverages" to R.string.cat_beverages,
    "Household & Personal" to R.string.cat_household_personal,
    "Household Cleaning" to R.string.cat_household_cleaning,
    "Personal Care" to R.string.cat_personal_care,
    "Beauty" to R.string.cat_beauty,
    "Baby Products" to R.string.cat_baby_products,
    "Pet Supplies" to R.string.cat_pet_supplies,
    "Paper Products" to R.string.cat_paper_products,
    "Kitchen Supplies" to R.string.cat_kitchen_supplies,
    "Health & Wellness" to R.string.cat_health_wellness,
    "Health & Pharmacy" to R.string.cat_health_pharmacy,
    "Medical" to R.string.cat_medical,
    "Sports & Fitness" to R.string.cat_sports_fitness,
    "Dining & Entertainment" to R.string.cat_dining_entertainment,
    "Restaurant & Dining" to R.string.cat_restaurant_dining,
    "Entertainment" to R.string.cat_entertainment,
    "Video Games" to R.string.cat_video_games,
    "Tips" to R.string.cat_tips,
    "Shopping & Lifestyle" to R.string.cat_shopping_lifestyle,
    "Clothing & Accessories" to R.string.cat_clothing_accessories,
    "Electronics" to R.string.cat_electronics,
    "Garden & Plants" to R.string.cat_garden_plants,
    "Home Improvement" to R.string.cat_home_improvement,
    "Tobacco & Alcohol" to R.string.cat_tobacco_alcohol,
    "Transportation" to R.string.cat_transportation,
    "Fuel" to R.string.cat_fuel,
    "Car Maintenance" to R.string.cat_car_maintenance,
    "Services & Subscriptions" to R.string.cat_services_subscriptions,
    "Subscriptions & Services" to R.string.cat_subscriptions_services,
    "Investments" to R.string.cat_investments,
    "Education" to R.string.cat_education,
    "Travel & Accommodation" to R.string.cat_travel_accommodation,
    "Insurance & Utilities" to R.string.cat_insurance_utilities,
    "Rent" to R.string.cat_rent,
    "Office & Work Supplies" to R.string.cat_office_work_supplies,
    "Gifts & Charitable Donations" to R.string.cat_gifts_donations,
    "Delivery" to R.string.cat_delivery,
    "Other" to R.string.cat_other,
)

/** Localized display name for a category, falling back to the raw [name] for custom categories. */
@Composable
fun categoryDisplayName(name: String): String =
    categoryNameRes[name]?.let { stringResource(it) } ?: name

/** Non-composable variant for use in plain lambdas (e.g. list filtering). */
fun categoryDisplayName(context: Context, name: String): String =
    categoryNameRes[name]?.let { context.getString(it) } ?: name
