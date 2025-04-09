package com.br.leo.moodsnap.ui.utils

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import com.br.leo.moodsnap.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogUtils {
    fun createMaterialDialog(
        context: Context,
        view: View,
        cancelable: Boolean = false
    ): AlertDialog {
        val dialog = MaterialAlertDialogBuilder(context, R.style.CustomAlertDialog)
            .setView(view)
            .setCancelable(cancelable)
            .create()

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        
        // Apply font to dialog view
        FontUtils.applyFontToView(context, view)
        
        return dialog
    }

    fun setupDialogBackButton(dialog: AlertDialog, backButton: Button, onBack: (() -> Unit)? = null) {
        backButton.setOnClickListener {
            dialog.dismiss()
            onBack?.invoke()
        }
    }

    fun setupDialogConfirmButton(context: Context, confirmButton: Button) {
        Utils.updateBackGroundColor(context, confirmButton)
    }

    fun setupStandardButtons(
        context: Context,
        dialog: AlertDialog,
        backButton: Button,
        confirmButton: Button,
        onBack: (() -> Unit)? = null
    ) {
        setupDialogBackButton(dialog, backButton, onBack)
        setupDialogConfirmButton(context, confirmButton)
    }
} 