package com.br.leo.moodsnap.ui.dialog

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.annotation.DrawableRes
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.databinding.DialogCustomPositiveNegativeAltBinding
import com.br.leo.moodsnap.ui.utils.Utils

class CustomAlertDialog private constructor(
    context: Context,
    private val typeSystemAlert: Boolean
) {
    private var builder: AlertDialog.Builder? = null
    private val binding: DialogCustomPositiveNegativeAltBinding
    var presentDialog: AlertDialog? = null
        private set

    init {
        builder = AlertDialog.Builder(context, R.style.RoundedDialog)
        binding = DialogCustomPositiveNegativeAltBinding.inflate(LayoutInflater.from(context))
        builder?.setView(binding.root)

        binding.imageViewIconDialog.setBackgroundResource(android.R.drawable.ic_dialog_alert)
        binding.textViewCustomDialogMessage.visibility = View.GONE

        val mainColor: Int = context.getColor(R.color.primary_green)
        binding.btnNegative.setTextColor(mainColor)
        Utils.updateBackGroundColor(context, binding.btnPositive)
    }

    fun setTitle(title: String?): CustomAlertDialog {
        binding.textViewTitleDialog.text = title
        return this
    }

    fun setPositiveListener(listener: (() -> Unit)?): CustomAlertDialog {
        binding.btnPositive.setOnClickListener {
            listener?.invoke()
            presentDialog?.dismiss()
        }
        return this
    }

    fun setNegativeListener(listener: (() -> Unit)?): CustomAlertDialog {
        binding.btnNegative.setOnClickListener {
            listener?.invoke()
            presentDialog?.dismiss()
        }
        return this
    }

    fun setCancelable(
        cancelable: Boolean,
        onCancelListener: DialogInterface.OnCancelListener? = null
    ): CustomAlertDialog {
        builder?.setCancelable(cancelable)
        onCancelListener?.let { builder?.setOnCancelListener(it) }
        return this
    }

    fun setMessage(message: String?): CustomAlertDialog {
        binding.textViewCustomDialogMessage.visibility = View.VISIBLE
        binding.textViewCustomDialogMessage.text = message
        return this
    }

    fun setMessageHtml(message: String?): CustomAlertDialog {
        binding.textViewCustomDialogMessage.visibility = View.VISIBLE
        binding.textViewCustomDialogMessage.text =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(message)
            }
        return this
    }

    fun isSingleButton(title: String?, listener: (() -> Unit)?): CustomAlertDialog {
        binding.btnPositive.text = title
        binding.btnNegative.visibility = View.GONE
        setPositiveListener(listener)
        return this
    }

    fun show() {
        presentDialog = builder?.create()
        if (typeSystemAlert) {
            presentDialog?.window?.setType(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            )
        }
        presentDialog?.window?.attributes?.windowAnimations = R.style.DialogAnimation;
        presentDialog?.show()
    }

    fun setDescricaoBtnPositive(descricao: String?): CustomAlertDialog {
        binding.btnPositive.text = descricao
        return this
    }

    fun setDescricaoBtnNegative(descricao: String?): CustomAlertDialog {
        binding.btnNegative.text = descricao
        return this
    }

    fun setIcon(@DrawableRes resid: Int): CustomAlertDialog {
        binding.imageViewIconDialog.setBackgroundResource(resid)
        return this
    }

    fun dismiss() {
        presentDialog?.dismiss()
    }

    companion object {
        fun create(context: Context, typeSystemAlert: Boolean = false): CustomAlertDialog {
            return CustomAlertDialog(context, typeSystemAlert)
        }
    }
}