package com.koordy.app.ui.membre

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.koordy.app.MainActivity
import com.koordy.app.api.RetrofitClient
import com.koordy.app.databinding.FragmentMembreBinding
import com.koordy.app.models.MembreUpdateRequest
import com.koordy.app.utils.Constants
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MembreFragment : Fragment() {

    private var _binding: FragmentMembreBinding? = null
    private val binding get() = _binding!!

    // Picker d'image depuis la galerie
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadPhoto(it) }
    }

    // Demande de permission (API ≤ 32)
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickImage.launch("image/*")
        else Toast.makeText(requireContext(), "Permission refusée.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = (activity as MainActivity).session
        val id = session.idMembre

        loadProfil(id)
        loadAssociation(id)
        loadEquipes(id)
        loadPresences(id)

        binding.avatarContainer.setOnClickListener { openGallery() }

        binding.btnEditProfile.setOnClickListener {
            binding.layoutEdit.visibility = View.VISIBLE
            binding.btnEditProfile.visibility = View.GONE
        }

        binding.btnSaveProfile.setOnClickListener { saveProfile(id) }

        binding.btnCancelEdit.setOnClickListener {
            binding.layoutEdit.visibility = View.GONE
            binding.btnEditProfile.visibility = View.VISIBLE
        }

        binding.btnLogout.setOnClickListener {
            session.clear()
            requireActivity().finish()
            requireActivity().startActivity(requireActivity().intent)
        }
    }

    // ── Galerie ───────────────────────────────────────────────────────────────

    private fun openGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED ->
                pickImage.launch("image/*")
            else ->
                requestPermission.launch(permission)
        }
    }

    private fun uploadPhoto(uri: Uri) {
        val session = (activity as MainActivity).session
        val id = session.idMembre

        lifecycleScope.launch {
            try {
                binding.ivAvatarPhoto.alpha = 0.5f
                val file = compressImage(uri) ?: run {
                    Toast.makeText(requireContext(), "Impossible de lire l'image.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val requestBody = file.asRequestBody("image/jpeg".toMediaType())
                val part = MultipartBody.Part.createFormData("photo", file.name, requestBody)

                val resp = RetrofitClient.api.uploadMemberPhoto(id, part)
                if (resp.isSuccessful && resp.body()?.success == true) {
                    val photoPath = resp.body()!!.photo
                    showPhoto("${Constants.BASE_URL.trimEnd('/')}$photoPath")
                    Toast.makeText(requireContext(), "Photo mise à jour !", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Erreur lors de l'envoi.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur réseau.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.ivAvatarPhoto.alpha = 1f
            }
        }
    }

    // Compresse l'image, corrige l'orientation EXIF, et la place dans un fichier temp
    private fun compressImage(uri: Uri): File? {
        return try {
            val cr = requireContext().contentResolver

            // Lire l'orientation EXIF avant de décoder le bitmap
            val rotation = cr.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val original = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null

            // Redimensionner
            val maxSize = 800
            val ratio = maxSize.toFloat() / maxOf(original.width, original.height)
            val scaled = if (ratio < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * ratio).toInt(),
                    (original.height * ratio).toInt(),
                    true
                )
            } else original

            // Appliquer la rotation si nécessaire
            val bitmap = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
            } else scaled

            val tmp = File(requireContext().cacheDir, "avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tmp).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
            tmp
        } catch (_: Exception) { null }
    }

    private fun showPhoto(url: String) {
        binding.tvAvatarInitial.visibility = View.GONE
        binding.ivAvatarPhoto.visibility = View.VISIBLE
        Glide.with(this)
            .load(url)
            .transform(CircleCrop())
            .into(binding.ivAvatarPhoto)
    }

    // ── Chargement des données ────────────────────────────────────────────────

    private fun loadProfil(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembre(id)
                if (res.isSuccessful) {
                    val m = res.body()!!

                    // Photo ou initiale
                    if (m.photoMembre.isNotEmpty()) {
                        showPhoto("${Constants.BASE_URL.trimEnd('/')}${m.photoMembre}")
                    } else {
                        binding.tvAvatarInitial.text =
                            m.prenomMembre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                    }

                    binding.tvFullName.text = "${m.prenomMembre} ${m.nomMembre}"
                    binding.tvRole.text = m.roleAsso.ifEmpty { "Membre" }
                    binding.tvAdhesion.text = if (m.dateAdhesion.isNotEmpty())
                        "Membre depuis le ${formatDateShort(m.dateAdhesion.take(10))}"
                    else ""

                    binding.tvEmail.text = m.mailMembre

                    var formattedDate = "—"
                    var ageText = "—"
                    if (m.dateNaissance.isNotEmpty()) {
                        try {
                            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH)
                            val d = sdfIn.parse(m.dateNaissance.take(10))
                            formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(d!!)
                            val birthCal = Calendar.getInstance().apply { time = d }
                            val today = Calendar.getInstance()
                            var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
                            if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) age--
                            ageText = "$age ans"
                        } catch (_: Exception) {}
                    }
                    binding.tvNaissance.text = formattedDate
                    binding.tvAge.text = ageText

                    binding.etEditNom.setText(m.nomMembre)
                    binding.etEditPrenom.setText(m.prenomMembre)
                    binding.etEditEmail.setText(m.mailMembre)
                    binding.etEditBirth.setText(m.dateNaissance.take(10))
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur chargement profil.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAssociation(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembreAssociation(id)
                if (res.isSuccessful) {
                    val a = res.body()
                    if (a != null && a.nom.isNotEmpty()) {
                        binding.tvAssoNom.text = a.nom
                        binding.tvAssoSport.text = a.sport.ifEmpty { "Sport" }
                        binding.tvAssoVille.text = a.ville.ifEmpty { "—" }
                    } else {
                        binding.tvAssoNom.text = "Aucune association"
                        binding.tvAssoSport.visibility = View.GONE
                        binding.tvAssoVille.text = "—"
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadEquipes(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembreEquipes(id)
                if (res.isSuccessful) {
                    val equipes = res.body() ?: emptyList()
                    binding.tvEquipe.text = if (equipes.isNotEmpty())
                        "${equipes[0].nomEquipe}  ·  ${equipes[0].role}"
                    else "Aucune équipe"
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadPresences(id: Int) {
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.getMembreEvenements(id)
                if (!res.isSuccessful) return@launch

                val tous = res.body() ?: emptyList()
                // Ne garder que les événements passés
                val now = System.currentTimeMillis()
                val sdfIn = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val passes = tous.filter { ev ->
                    val t = try { sdfIn.parse(ev.dateDebutEvent)?.time ?: 0L } catch (_: Exception) { 0L }
                    t < now
                }

                val acceptes = passes.filter { it.statut == "Accepté" }

                if (acceptes.isEmpty()) {
                    binding.tvPresences.text = "Aucun événement accepté."
                    return@launch
                }

                val typeLabels = mapOf(
                    "MATCH"        to "Match",
                    "ENTRAINEMENT" to "Entraînement",
                    "REUNION"      to "Réunion",
                    "OTHER"        to "Autre"
                )
                val sdfOut = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
                sdfIn.timeZone = java.util.TimeZone.getTimeZone("UTC")

                val sb = StringBuilder()
                acceptes.sortedByDescending { it.dateDebutEvent }.forEach { ev ->
                    val dateStr = try {
                        sdfOut.format(sdfIn.parse(ev.dateDebutEvent)!!)
                    } catch (_: Exception) { ev.dateDebutEvent.take(10) }
                    val type = typeLabels[ev.typeEvenement] ?: ev.typeEvenement
                    sb.append("●  ${ev.titreEvenement}  ·  $type  ·  $dateStr\n")
                }
                binding.tvPresences.text = sb.toString().trimEnd()

            } catch (_: Exception) {}
        }
    }

    private fun saveProfile(id: Int) {
        val nom = binding.etEditNom.text.toString().trim()
        val prenom = binding.etEditPrenom.text.toString().trim()
        val email = binding.etEditEmail.text.toString().trim()
        val birthday = binding.etEditBirth.text.toString().trim()

        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.updateMembre(
                    id, MembreUpdateRequest(nom, prenom, email, birthday)
                )
                if (res.isSuccessful) {
                    Toast.makeText(requireContext(), "Profil mis à jour !", Toast.LENGTH_SHORT).show()
                    binding.layoutEdit.visibility = View.GONE
                    binding.btnEditProfile.visibility = View.VISIBLE
                    loadProfil(id)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erreur mise à jour.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatDateShort(iso: String): String {
        return try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.FRENCH).parse(iso) ?: return iso
            SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(d)
        } catch (_: Exception) { iso }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
