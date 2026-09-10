package com.budgetty.app.ui.util

import com.budgetty.app.category.Categories
import com.budgetty.app.category.CategoryBucket
import com.budgetty.app.data.local.CategoryEntity

/**
 * The effective Needs/Wants/Savings bucket of the category named [name], given the stored category
 * rows ([byName], keyed by lower-cased name): an explicit tag on the category wins, else a non-null
 * tag on its (effective) parent — so a sub-category inherits its group's bucket — else the code
 * default from [Categories.defaultBucketOf].
 *
 * Shared by the Insights 50/30/20 derivation and the Manage categories tagging UI so both resolve a
 * category's bucket the same way; because it reads live from the rows, re-tagging a category
 * reclassifies its past months too.
 */
fun effectiveBucketOf(name: String, byName: Map<String, CategoryEntity>): CategoryBucket {
    val row = byName[name.lowercase()]
    row?.bucket?.let { return it }
    val parent = row?.parent ?: Categories.defaultParentOf(name)
    if (parent != null) byName[parent.lowercase()]?.bucket?.let { return it }
    return Categories.defaultBucketOf(name)
}
