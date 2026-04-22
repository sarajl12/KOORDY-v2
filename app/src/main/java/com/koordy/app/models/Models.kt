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

data class JoinAssociationRequest(
    @SerializedName("id_membre") val idMembre: Int
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
    @SerializedName("nom_equipe") val nomEquipe: String = "",
    @SerializedName("photo_membre") val photoMembre: String = ""
)

data class PhotoUploadResponse(
    val success: Boolean = false,
    val photo: String = ""
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
    @SerializedName("conseil_asso") val conseilAsso: Int = 0,
    val age: Int = 0,
    @SerializedName("mail_membre") val email: String = "",
    @SerializedName("date_adhesion") val dateAdhesion: String = ""
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
    @SerializedName("date_fin_event") val dateFinEvent: String?,
    val participants: List<Int>? = null  // null = inviter tous les membres
)

data class EvenementAvecStatut(
    @SerializedName("id_evenement") val idEvenement: Int = 0,
    @SerializedName("id_association") val idAssociation: Int = 0,
    @SerializedName("titre_evenement") val titreEvenement: String = "",
    @SerializedName("type_evenement") val typeEvenement: String = "",
    @SerializedName("description_evenement") val descriptionEvenement: String = "",
    @SerializedName("lieu_event") val lieuEvent: String = "",
    @SerializedName("date_debut_event") val dateDebutEvent: String = "",
    @SerializedName("date_fin_event") val dateFinEvent: String? = null,
    val statut: String = "En attente"  // "En attente" | "Accepté" | "Refusé"
)

data class RsvpRequest(
    @SerializedName("id_membre") val idMembre: Int,
    val statut: String
)

// ── Actualité ────────────────────────────────────────────────────────────────

data class Actualite(
    @SerializedName("id_actualite") val idActualite: Int = 0,
    @SerializedName("id_association") val idAssociation: Int = 0,
    @SerializedName("type_actualite") val typeActualite: String = "",
    val titre: String = "",
    val contenu: String = "",
    @SerializedName("image_principale") val imagePrincipale: String? = null,
    @SerializedName("date_publication") val datePublication: String = "",
    val statut: String = "",
    @SerializedName("event_date") val eventDate: String = ""
)

data class IsAdminResponse(
    val isAdmin: Boolean,
    val role: String = ""
)

data class AssociationInfosRequest(
    val description: String,
    val adresse: String,
    @SerializedName("code_postal") val codePostal: String,
    val ville: String,
    val pays: String,
    val telephone: String
)

data class AssociationPhotoResponse(
    val success: Boolean = false,
    val photo: String = ""
)

data class GenericResponse(
    val message: String,
    val success: Boolean = false
)

// ── Chat ──────────────────────────────────────────────────────────────────────

data class Conversation(
    @SerializedName("id_conversation") val idConversation: Int = 0,
    @SerializedName("id_association") val idAssociation: Int = 0,
    val nom: String? = null,
    val type: String = "direct",
    @SerializedName("last_message") val lastMessage: String? = null,
    @SerializedName("last_message_at") val lastMessageAt: String? = null,
    @SerializedName("last_message_type") val lastMessageType: String? = null,
    @SerializedName("last_sender_nom") val lastSenderNom: String? = null,
    @SerializedName("last_sender_prenom") val lastSenderPrenom: String? = null,
    @SerializedName("other_id_membre") val otherIdMembre: Int? = null,
    @SerializedName("other_nom") val otherNom: String? = null,
    @SerializedName("other_prenom") val otherPrenom: String? = null
)

data class Message(
    @SerializedName("id_message") val idMessage: Int = 0,
    @SerializedName("id_conversation") val idConversation: Int = 0,
    @SerializedName("id_auteur") val idAuteur: Int = 0,
    val contenu: String = "",
    @SerializedName("type_message") val typeMessage: String = "text",
    @SerializedName("id_evenement") val idEvenement: Int? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("nom_auteur") val nomAuteur: String = "",
    @SerializedName("prenom_auteur") val prenomAuteur: String = "",
    @SerializedName("titre_evenement") val titreEvenement: String? = null,
    @SerializedName("date_debut_event") val dateDebutEvent: String? = null,
    @SerializedName("lieu_event") val lieuEvent: String? = null,
    @SerializedName("type_evenement") val typeEvenement: String? = null,
    @SerializedName("statut_rsvp") val statutRsvp: String? = null
)

data class ConversationRequest(
    @SerializedName("id_association") val idAssociation: Int,
    @SerializedName("id_initiateur") val idInitiateur: Int,
    val type: String = "direct",
    @SerializedName("id_destinataire") val idDestinataire: Int? = null,
    val nom: String? = null,
    val participants: List<Int>? = null
)

data class ConversationResponse(
    @SerializedName("id_conversation") val idConversation: Int,
    val existing: Boolean = false
)

data class MessageRequest(
    @SerializedName("id_auteur") val idAuteur: Int,
    val contenu: String,
    @SerializedName("type_message") val typeMessage: String = "text",
    @SerializedName("id_evenement") val idEvenement: Int? = null
)

data class SendMessageResponse(
    val success: Boolean = false,
    @SerializedName("id_message") val idMessage: Int = 0,
    @SerializedName("created_at") val createdAt: String = ""
)

// ── Président ─────────────────────────────────────────────────────────────────

data class CreateEquipeRequest(
    @SerializedName("id_association") val idAssociation: Int,
    @SerializedName("nom_equipe") val nomEquipe: String,
    val description: String = "",
    val membres: List<Int> = emptyList()
)

data class EquipeDetail(
    @SerializedName("id_equipe") val idEquipe: Int = 0,
    @SerializedName("nom_equipe") val nomEquipe: String = "",
    val description: String = "",
    @SerializedName("nb_membres") val nbMembres: Int = 0
)

data class UpdateRoleRequest(
    val role: String,
    @SerializedName("id_association") val idAssociation: Int
)

data class UpdateEquipeMembresRequest(
    @SerializedName("id_association") val idAssociation: Int,
    val membres: List<Int>
)
