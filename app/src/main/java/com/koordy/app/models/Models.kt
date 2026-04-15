package com.koordy.app.models

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    @SerializedName("id_membre") val idMembre: Int?,
    val nom: String?,
    val prenom: String?,
    @SerializedName("id_association") val idAssociation: Int?
)

data class InscriptionRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val password: String,
    val birthday: String
)

data class InscriptionResponse(
    val message: String,
    val id: Int?
)

// ── Association ───────────────────────────────────────────────────────────────

data class Association(
    @SerializedName("id_association") val idAssociation: Int = 0,
    val nom: String = "",
    @SerializedName("type_structure") val typeStructure: String = "",
    val sport: String = "",
    val adresse: String = "",
    @SerializedName("adresse_2") val adresse2: String = "",
    val description: String = "",
    @SerializedName("date_creation") val dateCreation: String = "",
    val image: String = "",
    @SerializedName("code_postal") val codePostal: String = "",
    val ville: String = "",
    val pays: String = "",
    val email: String = "",
    val telephone: String = "",
    @SerializedName("couleur_1") val couleur1: String = "#6CCFFF",
    @SerializedName("couleur_2") val couleur2: String = "#FFFFFF"
)

data class AssociationRequest(
    @SerializedName("id_membre") val idMembre: Int,
    val nom: String,
    @SerializedName("type_structure") val typeStructure: String,
    val sport: String,
    val adresse: String,
    @SerializedName("adresse_2") val adresse2: String,
    val description: String,
    @SerializedName("date_creation") val dateCreation: String,
    @SerializedName("code_postal") val codePostal: String,
    val ville: String,
    val pays: String,
    val image: String = ""
)

data class AssociationResponse(
    val message: String,
    @SerializedName("id_association") val idAssociation: Int?
)

data class DesignRequest(
    @SerializedName("couleur_1") val couleur1: String,
    @SerializedName("couleur_2") val couleur2: String,
    val image: String
)

// ── Membre ────────────────────────────────────────────────────────────────────

data class Membre(
    @SerializedName("id_membre") val idMembre: Int = 0,
    @SerializedName("nom_membre") val nomMembre: String = "",
    @SerializedName("prenom_membre") val prenomMembre: String = "",
    @SerializedName("mail_membre") val mailMembre: String = "",
    @SerializedName("date_naissance") val dateNaissance: String = "",
    val age: Int = 0,
    @SerializedName("role_asso") val roleAsso: String = "",
    @SerializedName("date_adhesion") val dateAdhesion: String = "",
    @SerializedName("nom_equipe") val nomEquipe: String = ""
)

data class MembreUpdateRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val birthday: String
)

data class ConseilMembre(
    @SerializedName("id_membre") val idMembre: Int = 0,
    val nom: String = "",
    val prenom: String = "",
    val role: String = "",
    @SerializedName("conseil_asso") val conseilAsso: Int = 0
)

data class Equipe(
    @SerializedName("nom_equipe") val nomEquipe: String = "",
    val role: String = ""
)

data class Presence(
    @SerializedName("nom_activite") val nomActivite: String = "",
    val statut: String = "",
    @SerializedName("date_presence") val datePresence: String = ""
)

// ── Événement ─────────────────────────────────────────────────────────────────

data class Evenement(
    @SerializedName("id_evenement") val idEvenement: Int = 0,
    @SerializedName("id_association") val idAssociation: Int = 0,
    @SerializedName("titre_evenement") val titreEvenement: String = "",
    @SerializedName("type_evenement") val typeEvenement: String = "",
    @SerializedName("description_evenement") val descriptionEvenement: String = "",
    @SerializedName("lieu_event") val lieuEvent: String = "",
    @SerializedName("date_debut_event") val dateDebutEvent: String = "",
    @SerializedName("date_fin_event") val dateFinEvent: String = ""
)

data class EvenementRequest(
    @SerializedName("id_association") val idAssociation: Int,
    @SerializedName("id_auteur") val idAuteur: Int,
    @SerializedName("titre_evenement") val titreEvenement: String,
    @SerializedName("type_evenement") val typeEvenement: String,
    @SerializedName("lieu_event") val lieuEvent: String,
    @SerializedName("description_evenement") val descriptionEvenement: String,
    @SerializedName("date_debut_event") val dateDebutEvent: String,
    @SerializedName("date_fin_event") val dateFinEvent: String?
)

// ── Actualité ────────────────────────────────────────────────────────────────

data class Actualite(
    @SerializedName("id_actualite") val idActualite: Int = 0,
    @SerializedName("id_association") val idAssociation: Int = 0,
    @SerializedName("type_actualite") val typeActualite: String = "",
    val titre: String = "",
    val contenu: String = "",
    @SerializedName("image_principale") val imagePrincipale: String = "",
    @SerializedName("date_publication") val datePublication: String = "",
    val statut: String = "",
    @SerializedName("event_date") val eventDate: String = ""
)

data class IsAdminResponse(
    val isAdmin: Boolean
)

data class GenericResponse(
    val message: String,
    val success: Boolean = false
)
