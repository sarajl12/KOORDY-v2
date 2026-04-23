package com.koordy.app.utils

/**
 * Modifie BASE_URL selon ton environnement :
 *  - Émulateur Android → http://10.0.2.2:8080/
 *  - Appareil physique sur le même réseau → http://192.168.x.x:8080/
 *  - Production → https://ton-domaine.com/
 */
object Constants {
    const val BASE_URL = "http://10.4.255.70:3001/"
    const val PREF_NAME = "koordy_prefs"
    const val KEY_ID_MEMBRE = "id_membre"
    const val KEY_ID_ASSOCIATION = "id_association"
    const val KEY_NOM = "nom_membre"
    const val KEY_PRENOM = "prenom_membre"
    const val KEY_LAST_OPENED_CHAT = "last_opened_chat"
    const val KEY_LAST_SHOWN_NOTIF = "last_shown_notif"
}
