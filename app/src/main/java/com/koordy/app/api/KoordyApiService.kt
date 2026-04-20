package com.koordy.app.api

import com.koordy.app.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Toutes les routes de ton backend Express.js (app.js / app2.js)
 * mappées en interface Retrofit.
 */
interface KoordyApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/inscription")
    suspend fun inscription(@Body request: InscriptionRequest): Response<InscriptionResponse>

    // ── Association ───────────────────────────────────────────────────────────

    @GET("api/associations/{id}")
    suspend fun getAssociation(@Path("id") id: Int): Response<Association>

    @GET("api/associations/{id}/conseil")
    suspend fun getConseil(@Path("id") id: Int): Response<List<ConseilMembre>>

    @GET("api/associations/{id}/membres")
    suspend fun getMembres(@Path("id") id: Int): Response<List<ConseilMembre>>

    @GET("api/associations/{id}/events")
    suspend fun getEvents(@Path("id") id: Int): Response<List<Evenement>>

    @GET("api/associations/{id}/news")
    suspend fun getNews(@Path("id") id: Int): Response<List<Actualite>>

    @GET("api/associations/{id}/is-admin/{idMembre}")
    suspend fun isAdmin(
        @Path("id") idAssociation: Int,
        @Path("idMembre") idMembre: Int
    ): Response<IsAdminResponse>

    @POST("api/association")
    suspend fun createAssociation(@Body request: AssociationRequest): Response<AssociationResponse>

    @PUT("api/association/design/{id}")
    suspend fun updateDesign(
        @Path("id") id: Int,
        @Body request: DesignRequest
    ): Response<GenericResponse>

    @GET("api/association/search")
    suspend fun searchAssociation(@Query("nom") nom: String): Response<List<Association>>

    @POST("api/association/{id}/rejoindre")
    suspend fun rejoindreAssociation(
        @Path("id") idAssociation: Int,
        @Body request: JoinAssociationRequest
    ): Response<AssociationResponse>

    // ── Membre ────────────────────────────────────────────────────────────────

    @GET("api/membre/{id}")
    suspend fun getMembre(@Path("id") id: Int): Response<Membre>

    @PUT("api/membre/{id}")
    suspend fun updateMembre(
        @Path("id") id: Int,
        @Body request: MembreUpdateRequest
    ): Response<GenericResponse>

    @Multipart
    @PATCH("api/membre/{id}/photo")
    suspend fun uploadMemberPhoto(
        @Path("id") id: Int,
        @Part photo: MultipartBody.Part
    ): Response<PhotoUploadResponse>

    @GET("api/membre/{id}/association")
    suspend fun getMembreAssociation(@Path("id") id: Int): Response<Association>

    @GET("api/membre/{id}/equipes")
    suspend fun getMembreEquipes(@Path("id") id: Int): Response<List<Equipe>>

    @GET("api/membre/{id}/presences")
    suspend fun getMembrePresences(@Path("id") id: Int): Response<List<Presence>>

    // ── Événements ────────────────────────────────────────────────────────────

    @POST("api/evenements")
    suspend fun createEvenement(@Body request: EvenementRequest): Response<GenericResponse>

    @GET("api/membre/{id}/evenements")
    suspend fun getMembreEvenements(@Path("id") idMembre: Int): Response<List<EvenementAvecStatut>>

    @PATCH("api/evenements/{id}/rsvp")
    suspend fun respondRsvp(
        @Path("id") idEvenement: Int,
        @Body request: RsvpRequest
    ): Response<GenericResponse>

    // ── Actualités ────────────────────────────────────────────────────────────

    @POST("api/news")
    suspend fun createNews(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<GenericResponse>

    @PATCH("api/news/{id}/approve")
    suspend fun approveNews(@Path("id") id: Int): Response<GenericResponse>

    @PATCH("api/news/{id}/refuse")
    suspend fun refuseNews(@Path("id") id: Int): Response<GenericResponse>

    // ── Chat ──────────────────────────────────────────────────────────────────

    @GET("api/membre/{id}/conversations")
    suspend fun getConversations(@Path("id") idMembre: Int): Response<List<Conversation>>

    @POST("api/conversations")
    suspend fun createConversation(@Body request: ConversationRequest): Response<ConversationResponse>

    @GET("api/conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") idConversation: Int,
        @Query("id_membre") idMembre: Int
    ): Response<List<Message>>

    @POST("api/conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") idConversation: Int,
        @Body request: MessageRequest
    ): Response<SendMessageResponse>
}
