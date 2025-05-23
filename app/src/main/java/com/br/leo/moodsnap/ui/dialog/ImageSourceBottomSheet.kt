package com.br.leo.moodsnap.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.utils.ClickUtils
import com.br.leo.moodsnap.ui.utils.FontUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ImageSourceBottomSheet : BottomSheetDialogFragment() {

    interface ImageSourceListener {
        fun onCameraSelected()
        fun onGallerySelected()
        fun onDeleteSelected()
    }

    private var listener: ImageSourceListener? = null
    private var hasExistingImage: Boolean = false

    companion object {
        fun newInstance(hasExistingImage: Boolean = false): ImageSourceBottomSheet {
            val fragment = ImageSourceBottomSheet()
            fragment.hasExistingImage = hasExistingImage
            return fragment
        }
    }

    fun setImageSourceListener(listener: ImageSourceListener) {
        this.listener = listener
    }

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_image_source, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aplicar a fonte personalizada a todas as views
        FontUtils.applyFontToView(requireContext(), view)

        // Configurar a visibilidade da opção de exclusão
        val deleteOption = view.findViewById<LinearLayout>(R.id.delete_option)
        val deleteDivider = view.findViewById<View>(R.id.delete_divider)
        
        if (hasExistingImage) {
            deleteOption.visibility = View.VISIBLE
            deleteDivider.visibility = View.VISIBLE
        } else {
            deleteOption.visibility = View.GONE
            deleteDivider.visibility = View.GONE
        }

        // Configurar os listeners de clique
        ClickUtils.setDebounceClickListener(view.findViewById<LinearLayout>(R.id.camera_option)){
            listener?.onCameraSelected()
            dismiss()
        }
        ClickUtils.setDebounceClickListener(view.findViewById<LinearLayout>(R.id.gallery_option)){
            listener?.onGallerySelected()
            dismiss()
        }

        ClickUtils.setDebounceClickListener(deleteOption){
            listener?.onDeleteSelected()
            dismiss()
        }

        // Configurar o clique fora do BottomSheet para fechá-lo
        ClickUtils.setDebounceClickListener(view.findViewById<ImageView>(R.id.btn_close)){
            dismiss()
        }
    }
}
