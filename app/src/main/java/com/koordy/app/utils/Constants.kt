package com.koordy.app.utils

/**
 * Modifie BASE_URL selon ton environnement :
 *  - Émulateur Android → http://10.0.2.2:8080/
 *  - Appareil physique sur le même réseau → http://192.168.x.x:8080/
 *  - Production → https://ton-domaine.com/
 */
object Constants {
    const val BASE_URL = "http://10.4.249.69:3001/"
    const val PREF_NAME = "koordy_prefs"
    const val KEY_ID_MEMBRE = "id_membre"
    const val KEY_ID_ASSOCIATION = "id_association"
    const val KEY_NOM = "nom_membre"
    const val KEY_PRENOM = "prenom_membre"

    // ── Dev only ───────────────────────────────────────────────────────────────
    // Met ici l'id_association et l'id_membre de ton compte de test en BDD.
    // Ces valeurs sont utilisées quand on bypasse le login (DEV_MODE = true).
    const val DEV_MODE = true
    const val DEV_ID_ASSO = 1       // ← change avec ton id_association en BDD
    const val DEV_ID_MEMBRE = 1     // ← change avec ton id_membre en BDD
}
