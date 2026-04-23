# KoordyApp

Aurélie Nass, Kelly Abendjock et Sara Ji-LO G1 A2MSI


Application mobile Android pour la plateforme Koordy — gestion d'associations sportives et étudiantes. C'est le portage natif Kotlin d'une application web Express.js + PostgreSQL.

---

## Stack

- **Android** : Kotlin, min SDK 26, Navigation Component (single activity), Retrofit 2 + OkHttp, Glide, Coroutines, ViewBinding
- **Backend** : Node.js (ES Modules), Express 5, PostgreSQL, Multer pour les uploads

---

## Lancer le projet

### Backend

```bash
cd Back
npm install
node app.js
```

Le serveur écoute sur le port `3001`. PostgreSQL doit tourner en local sur le port `5432` avec une base `koordybdd`. Les identifiants de connexion sont dans `Back/connexionBDD.js`.

### App Android

Dans `app/src/main/java/com/koordy/app/utils/Constants.kt`, adapter l'URL selon le contexte :

```kotlin
// Émulateur
const val BASE_URL = "http://10.0.2.2:3001/"

// Appareil physique (même réseau Wi-Fi que le PC)
const val BASE_URL = "http://192.168.0.21:3001/"
```

Ensuite Run → Run 'app' dans Android Studio (émulateur API 26+ ou appareil branché).

---

## Ce que fait l'app

**Tout le monde :**
- Inscription, connexion, création ou rejoindre une association
- Personnalisation de l'association (couleurs, logo)
- Page d'accueil avec les checklists d'événement, le bureau et les dernières actus
- Calendrier des événements avec participation (RSVP)
- Fil d'actualités
- Messagerie (conversations privées et de groupe)
- Profil membre : modification des infos, photo, équipes, historique de présences

**Réservé au président :**
- Tableau de bord admin : gérer les membres, les rôles et les équipes
- Modération des actualités (valider / refuser)
- Création d'événements

---

## Structure

```
KoordyApp/
├── app/src/main/java/com/koordy/app/
│   ├── api/            → RetrofitClient + KoordyApiService (toutes les routes)
│   ├── model/          → data classes (Membre, Association, Evenement…)
│   ├── ui/             → fragments par feature (auth, home, chat, membre…)
│   ├── adapter/        → RecyclerView adapters
│   └── session/        → SessionManager (SharedPreferences)
├── Back/
│   ├── app.js          → serveur Express, toutes les routes API
│   ├── connexionBDD.js → pool PostgreSQL
│   └── uploads/        → fichiers uploadés (photos, logos…)
```

---
