# KoordyApp — Android Studio

Application Android pour la plateforme Koordy.
Reproduit fidèlement le projet web (Express.js + PostgreSQL) en app native Kotlin.

---

## 🏗️ Structure du projet

```
KoordyApp/
├── app/src/main/
│   ├── java/com/koordy/app/
│   │   ├── api/
│   │   │   ├── KoordyApiService.kt   ← toutes les routes Express mappées
│   │   │   └── RetrofitClient.kt     ← singleton Retrofit
│   │   ├── models/
│   │   │   └── Models.kt             ← data classes (Association, Membre, etc.)
│   │   ├── ui/
│   │   │   ├── auth/                 ← Login, Inscription, FormAssociation, Design…
│   │   │   ├── home/                 ← HomeAssociation, Events, News
│   │   │   └── membre/               ← MembreFragment (profil)
│   │   ├── utils/
│   │   │   ├── Constants.kt          ← BASE_URL
│   │   │   └── SessionManager.kt     ← équivalent localStorage
│   │   └── MainActivity.kt
│   └── res/
│       ├── layout/                   ← tous les XML de layout
│       ├── navigation/nav_graph.xml  ← Navigation Component
│       ├── menu/bottom_nav_menu.xml
│       ├── drawable/                 ← backgrounds, boutons, steps
│       └── values/                   ← colors, strings, themes, arrays
```

---

## ⚙️ Configuration initiale

### 1. Ouvrir dans Android Studio
- File → Open → sélectionne le dossier `KoordyApp`
- Attends la synchronisation Gradle (2–3 min la première fois)

### 2. Configurer l'URL du backend

Édite `app/src/main/java/com/koordy/app/utils/Constants.kt` :

```kotlin
// Émulateur Android (localhost du PC)
const val BASE_URL = "http://10.0.2.2:8080/"

// Appareil physique sur le même réseau Wi-Fi
const val BASE_URL = "http://192.168.X.X:8080/"   // IP de ton PC

// Production
const val BASE_URL = "https://ton-domaine.com/"
```

### 3. Migrer ton backend de MySQL → PostgreSQL

Ton `connexionBDD.js` utilise `mysql2` mais ta BDD est PostgreSQL.
Remplace-le :

```bash
npm uninstall mysql2
npm install pg
```

**connexionBDD.js** (nouvelle version) :
```js
import pkg from 'pg';
const { Pool } = pkg;

let connection;
export async function initDB() {
  connection = new Pool({
    host: 'localhost',
    user: 'postgres',
    password: 'TON_MOT_DE_PASSE',
    database: 'koordybdd',
    port: 5432,
  });
  console.log('Connexion PostgreSQL établie');
  return connection;
}
export { connection };
```

Dans `app.js`, remplace tous les `connection.execute(...)` par `connection.query(...)` :
- MySQL : `const [rows] = await connection.execute(sql, params)`
- PostgreSQL : `const { rows } = await connection.query(sql, params)`  
  Et les placeholders `?` → `$1, $2, $3...`

Exemple :
```js
// Avant (MySQL)
const [rows] = await connection.execute(
  "SELECT * FROM membre WHERE mail_membre = ?", [email]
);

// Après (PostgreSQL)
const { rows } = await connection.query(
  "SELECT * FROM membre WHERE mail_membre = $1", [email]
);
```

### 4. Lancer le backend

```bash
cd Back
node app.js   # ou : npm start
```

Vérifie que le serveur tourne sur `http://localhost:8080`

### 5. Lancer l'app

- Dans Android Studio : Run → Run 'app' (Shift+F10)
- Choisis un émulateur (API 26+) ou branche un appareil physique

---

## 🗺️ Correspondance écrans Web → Android

| Page Web                          | Fragment Android                      |
|-----------------------------------|---------------------------------------|
| `login.html`                      | `LoginFragment`                       |
| `inscription_personnelle.html`    | `InscriptionFragment`                 |
| `inscription_association.html`    | `InscriptionAssociationFragment`      |
| `form-association.html`           | `FormAssociationFragment`             |
| `design_association.html`         | `DesignAssociationFragment`           |
| `success_association.html`        | `SuccessAssociationFragment`          |
| `recherche_association.html`      | `RechercheAssociationFragment`        |
| `home_association.html`           | `HomeAssociationFragment`             |
| `membre.html`                     | `MembreFragment`                      |

---

## 🔑 Points importants

- **SessionManager** remplace le `localStorage` web — stocke `id_membre`, `id_association`, etc.
- **Retrofit** remplace les `fetch()` JavaScript — même routes, même JSON
- **Navigation Component** remplace le routing HTML (window.location.href)
- `android:usesCleartextTraffic="true"` dans le Manifest = HTTP local autorisé (dev only)

---

## 📱 Fonctionnalités implémentées

- [x] Connexion / Déconnexion
- [x] Inscription membre
- [x] Création association (formulaire + design)
- [x] Recherche association existante
- [x] Page d'accueil association (hero, infos, conseil, events, news)
- [x] Page événements
- [x] Page actualités
- [x] Profil membre (affichage + modification)
- [x] Bottom navigation (Accueil / Évènements / Actualités / Profil)

---

## 🚀 Prochaines étapes

- Ajouter la gestion des images (Glide + upload)
- Implémenter la création d'événements (modal → dialog Android)
- Ajouter les notifications push (Firebase FCM)
- Gérer les rôles admin dans l'UI
