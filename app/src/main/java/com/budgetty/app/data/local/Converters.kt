package com.budgetty.app.data.local

import androidx.room.TypeConverter
import java.math.BigDecimal

/** Room type converters for types Room cannot store natively. */
class Converters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter
    fun fromTimeframe(value: BuyingLimitTimeframe?): String? = value?.name

    // An unknown/legacy value falls back to MONTHLY rather than crashing the read.
    @TypeConverter
    fun toTimeframe(value: String?): BuyingLimitTimeframe? = value?.let {
        runCatching { BuyingLimitTimeframe.valueOf(it) }.getOrDefault(BuyingLimitTimeframe.MONTHLY)
    }
}
