package com.koordy.app.ui.president

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.koordy.app.MainActivity
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentCreateActualiteBinding
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class CreateActualiteFragment : Fragment() {

    private var _binding: FragmentCreateActualiteBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                binding.ivImagePreview.setImageURI(uri)
                binding.ivImagePreview.visibility = View.VISIBLE
                binding.layoutPlaceholder.visibility = View.GONE
                binding.btnSupprimerImage.visibility = View.VISIBLE
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateActualiteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.frameImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSupprimerImage.setOnClickListener {
            imageUri = null
            binding.ivImagePreview.visibility = View.GONE
            binding.layoutPlaceholder.visibility = View.VISIBLE
            binding.btnSupprimerImage.visibility = View.GONE
        }

        binding.btnPublier.setOnClickListener { publierActualite() }
    }

    private fun publierActualite() {
        val titre = binding.etTitre.text.toString().trim()
        if (titre.isEmpty()) {
            binding.etTitre.error = "Titre requis"
            return
        }
        val contenu = binding.etContenu.text.toString().trim()

        val session = (activity as MainActivity).session
        val idAssociation = session.idAssociation
        val idAuteur = session.idMembre

        binding.btnPublier.isEnabled = false
        binding.btnPublier.text = "Publication…"

        lifecycleScope.launch {
            try {
                val uri = imageUri
                if (uri != null) {
                    // Avec image : endpoint multipart POST /api/news/upload
                    val imageFile = copyUriToTempFile(uri) ?: run {
                        publishWithoutImage(idAssociation, idAuteur, titre, contenu)
                        return@launch
                    }
                    val imagePart = MultipartBody.Part.createFormData(
                        "image",
                        imageFile.name,
                        imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    )
                    val res = RetrofitClient.api.createNewsWithImage(
                        idAssociation = idAssociation.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                        idAuteur = idAuteur.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                        typeActualite = "Article".toRequestBody("text/plain".toMediaTypeOrNull()),
                        titre = titre.toRequestBody("text/plain".toMediaTypeOrNull()),
                        contenu = contenu.toRequestBody("text/plain".toMediaTypeOrNull()),
                        statut = "Approuve".toRequestBody("text/plain".toMediaTypeOrNull()),
                        image = imagePart
                    )
                    imageFile.delete()
                    handleResult(res.isSuccessful)
                } else {
                    publishWithoutImage(idAssociation, idAuteur, titre, contenu)
                }
            } catch (_: Exception) {
                resetButton()
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun publishWithoutImage(idAssociation: Int, idAuteur: Int, titre: String, contenu: String) {
        val body = mapOf<String, Any>(
            "id_association" to idAssociation,
            "id_auteur" to idAuteur,
            "type_actualite" to "Article",
            "titre" to titre,
            "contenu" to contenu,
            "statut" to "Approuve"
        )
        val res = RetrofitClient.api.createNews(body)
        handleResult(res.isSuccessful)
    }

    private fun handleResult(success: Boolean) {
        if (success) {
            Toast.makeText(requireContext(), "Actualité publiée !", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        } else {
            resetButton()
            Toast.makeText(requireContext(), "Erreur lors de la publication.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetButton() {
        binding.btnPublier.isEnabled = true
        binding.btnPublier.text = "Publier l'actualité"
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val context = requireContext()
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("actu_img_", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            tempFile
        } catch (_: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
