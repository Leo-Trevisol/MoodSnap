package com.br.leo.moodsnap.ui.dialog

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.FileProvider
import com.br.leo.moodsnap.R
import com.br.leo.moodsnap.ui.utils.FontUtils
import com.br.leo.moodsnap.ui.utils.Utils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import java.io.File
import java.io.FileOutputStream

class ShareImageBottomSheet : BottomSheetDialogFragment() {

    private var shareImageBitmap: Bitmap? = null
    private var shareTitle: String = ""
    
    companion object {
        fun newInstance(bitmap: Bitmap, title: String): ShareImageBottomSheet {
            val fragment = ShareImageBottomSheet()
            fragment.shareImageBitmap = bitmap
            fragment.shareTitle = title
            return fragment
        }
    }

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_share_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aplicar a fonte personalizada a todas as views
        FontUtils.applyFontToView(requireContext(), view)

        // Configurar a imagem de prévia
        val imageView = view.findViewById<ShapeableImageView>(R.id.share_preview_image)
        shareImageBitmap?.let {
            imageView.setImageBitmap(it)
        }

        // Configurar o botão de compartilhar
        val btnShare = view.findViewById<MaterialButton>(R.id.btn_share)
        Utils.updateBackGroundColor(requireContext(), btnShare)
        btnShare.setOnClickListener {
            shareImage()
        }

        // Configurar o botão de compartilhar
        val btnClose = view.findViewById<ImageView>(R.id.btn_close)
        btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun shareImage() {
        shareImageBitmap?.let { bitmap ->
            try {
                // Criar um arquivo temporário para a imagem
                val cachePath = File(requireContext().cacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, "shared_image.png")
                
                // Salvar o bitmap no arquivo
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
                
                // Obter a URI do arquivo usando FileProvider
                val fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )
                
                // Criar intent para compartilhar
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // Iniciar a atividade de compartilhamento
                startActivity(Intent.createChooser(shareIntent, getString(R.string.btn_share)))
                
                // Fechar o BottomSheet após compartilhar
                dismiss()
            } catch (e: Exception) {
                Utils.showCustomToast(requireContext(), getString(R.string.error_sharing_image))
            }
        }
    }
}
