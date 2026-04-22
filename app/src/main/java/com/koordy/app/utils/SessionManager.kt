package com.koordy.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Équivalent du localStorage de ton app web.
 * Stocke id_membre, id_association, etc.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

    var idMembre: Int
        get() = prefs.getInt(Constants.KEY_ID_MEMBRE, -1)
        set(value) = prefs.edit().putInt(Constants.KEY_ID_MEMBRE, value).apply()

    var idAssociation: Int
        get() = prefs.getInt(Constants.KEY_ID_ASSOCIATION, -1)
        set(value) = prefs.edit().putInt(Constants.KEY_ID_ASSOCIATION, value).apply()

    var nomMembre: String
        get() = prefs.getString(Constants.KEY_NOM, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_NOM, value).apply()

    var prenomMembre: String
        get() = prefs.getString(Constants.KEY_PRENOM, "") ?: ""
        set(value) = prefs.edit().putString(Constants.KEY_PRENOM, value).apply()

    var lastOpenedChat: Long
        get() = prefs.getLong(Constants.KEY_LAST_OPENED_CHAT, 0L)
        set(value) = prefs.edit().putLong(Constants.KEY_LAST_OPENED_CHAT, value).apply()

    var lastShownUnreadDialog: Long
        get() = prefs.getLong(Constants.KEY_LAST_SHOWN_NOTIF, 0L)
        set(value) = prefs.edit().putLong(Constants.KEY_LAST_SHOWN_NOTIF, value).apply()

    fun getPinnedConversations(): Set<Int> =
        (prefs.getStringSet("pinned_convs", emptySet()) ?: emptySet()).map { it.toInt() }.toSet()

    fun setPinnedConversations(ids: Set<Int>) =
        prefs.edit().putStringSet("pinned_convs", ids.map { it.toString() }.toSet()).apply()

    fun getMutedConversations(): Set<Int> =
        (prefs.getStringSet("muted_convs", emptySet()) ?: emptySet()).map { it.toInt() }.toSet()

    fun setMutedConversations(ids: Set<Int>) =
        prefs.edit().putStringSet("muted_convs", ids.map { it.toString() }.toSet()).apply()

    fun isLoggedIn() = idMembre != -1

    fun clear() = prefs.edit().clear().apply()
}
