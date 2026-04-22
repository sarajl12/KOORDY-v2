package com.koordy.app.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.bumptech.glide.Glide
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentEditAssociationBinding
import com.koordy.app.models.AssociationInfosRequest
import com.koordy.app.utils.Constants
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class EditAssociationFragment : Fragment() {

    private var _binding: FragmentEditAssociationBinding? = null
    private val binding get() = _binding!!

    private var idAsso = -1

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) uploadPhoto(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditAssociationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        idAsso = arguments?.getInt("id_association", -1) ?: -1

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.framePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener { saveInfos() }

        if (idAsso != -1) loadCurrentData()
    }

    private fun loadCurrentData() {
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.getAssociation(idAsso)
                if (resp.isSuccessful) {
                    val asso = resp.body() ?: return@launch
                    binding.etDescription.setText(asso.description)
                    binding.etAdresse.setText(asso.adresse)
                    binding.etCodePostal.setText(asso.codePostal)
                    binding.etVille.setText(asso.ville)
                    binding.etPays.setText(asso.pays)
                    binding.etTelephone.setText(asso.telephone)

                    if (asso.image.isNotEmpty()) {
                        Glide.with(this@EditAssociationFragment)
                            .load("${Constants.BASE_URL.trimEnd('/')}${asso.image}")
                            .circleCrop()
                            .placeholder(com.koordy.app.R.drawable.bg_avatar_round)
                            .into(binding.ivAssoPhoto)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun uploadPhoto(uri: Uri) {
        binding.progressPhoto.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val file = compressImage(uri) ?: run {
                    Toast.makeText(requireContext(), "Impossible de lire l'image.", Toast.LENGTH_SHORT).show()
                    binding.progressPhoto.visibility = View.GONE
                    return@launch
                }
                val part = MultipartBody.Part.createFormData(
                    "photo", file.name,
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                val resp = RetrofitClient.api.uploadAssociationPhoto(idAsso, part)
                file.delete()
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val photoUrl = "${Constants.BASE_URL.trimEnd('/')}${resp.body()!!.photo}"
                    Glide.with(this@EditAssociationFragment)
                        .load(photoUrl)
                        .circleCrop()
                        .into(binding.ivAssoPhoto)
                    Toast.makeText(requireContext(), "Photo mise à jour !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Erreur lors de l'upload.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressPhoto.visibility = View.GONE
            }
        }
    }

    private fun saveInfos() {
        val description = binding.etDescription.text.toString().trim()
        val adresse     = binding.etAdresse.text.toString().trim()
        val codePostal  = binding.etCodePostal.text.toString().trim()
        val ville       = binding.etVille.text.toString().trim()
        val pays        = binding.etPays.text.toString().trim()
        val telephone   = binding.etTelephone.text.toString().trim()

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Enregistrement…"

        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.api.updateAssociationInfos(
                    idAsso,
                    AssociationInfosRequest(description, adresse, codePostal, ville, pays, telephone)
                )
                if (resp.isSuccessful) {
                    Toast.makeText(requireContext(), "Informations mises à jour !", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                } else {
                    Toast.makeText(requireContext(), "Erreur lors de l'enregistrement.", Toast.LENGTH_SHORT).show()
                    resetButton()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau : ${e.message}", Toast.LENGTH_SHORT).show()
                resetButton()
            }
        }
    }

    private fun resetButton() {
        binding.btnSave.isEnabled = true
        binding.btnSave.text = "Enregistrer"
    }

    private fun compressImage(uri: Uri): File? {
        return try {
            val cr = requireContext().contentResolver
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }

            var inSampleSize = 1
            var w = boundsOpts.outWidth
            var h = boundsOpts.outHeight
            while (w > 1280 || h > 1280) { inSampleSize *= 2; w /= 2; h /= 2 }

            val bitmap = cr.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { this.inSampleSize = inSampleSize })
            } ?: return null

            val tmp = File.createTempFile("asso_img_", ".jpg", requireContext().cacheDir)
            FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 82, it) }
            bitmap.recycle()
            tmp
        } catch (_: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
