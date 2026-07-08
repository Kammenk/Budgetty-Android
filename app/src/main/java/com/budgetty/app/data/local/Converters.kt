package com.budgetty.app.data.local

import androidx.room.TypeConverter
import java.math.BigDecimal

/** Room type converters for types Room cannot store natively. */
class Converters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }
}
