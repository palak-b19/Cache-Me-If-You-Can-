package com.example.ussdemoproject.ui

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.ussdemoproject.R

object FontProvider {

    private var anton: Typeface? = null
    private var montserratRegular: Typeface? = null
    private var montserratSemi: Typeface? = null
    private var poppinsRegular: Typeface? = null
    private var poppinsSemi: Typeface? = null

    fun anton(context: Context): Typeface =
        anton ?: loadSafeFont(context, R.font.anton_regular).also { anton = it }

    fun montserratRegular(context: Context): Typeface =
        montserratRegular ?: loadSafeFont(context, R.font.montserrat_regular).also { montserratRegular = it }

    fun montserratSemi(context: Context): Typeface =
        montserratSemi ?: loadSafeFont(context, R.font.montserrat_semibold).also { montserratSemi = it }

    fun poppinsRegular(context: Context): Typeface =
        poppinsRegular ?: loadSafeFont(context, R.font.poppins_regular).also { poppinsRegular = it }

    fun poppinsSemi(context: Context): Typeface =
        poppinsSemi ?: loadSafeFont(context, R.font.poppins_semibold).also { poppinsSemi = it }

    private fun loadSafeFont(context: Context, id: Int): Typeface {
        return try {
            ResourcesCompat.getFont(context, id) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }
}
