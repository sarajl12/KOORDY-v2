import cors from "cors";
import express from "express";
import { initDB } from "./connexionBDD.js";

const app = express();
const port = 3001;

const db = await initDB();


app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));



// ===============================
//  AUTH
// ===============================

// POST /api/inscription
app.post("/api/inscription", async (req, res) => {
  const { nom, prenom, email, password, birthday } = req.body;

  if (!nom || !prenom || !email || !password || !birthday) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  try {
    // Vérification email existant
    const existing = await db.query(
      "SELECT id_membre FROM membre WHERE mail_membre = $1",
      [email]
    );
    if (existing.rows.length > 0) {
      return res.status(409).json({ message: "Utilisateur déjà existant" });
    }

    const age = new Date().getFullYear() - new Date(birthday).getFullYear();

    const result = await db.query(
      `INSERT INTO membre (nom_membre, prenom_membre, mail_membre, password_membre, date_naissance, age)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id_membre`,
      [nom, prenom, email, password, birthday, age]
    );

    return res.status(201).json({
      message: "Utilisateur créé",
      id: result.rows[0].id_membre,
    });

  } catch (err) {
    console.error("Erreur inscription :", err);
    return res.status(500).json({ message: "Erreur serveur" });
  }
});


// POST /api/login
app.post("/api/login", async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  try {
    const result = await db.query(
      "SELECT * FROM membre WHERE mail_membre = $1",
      [email]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ message: "Aucun compte trouvé avec cet email." });
    }

    const user = result.rows[0];

    if (user.password_membre !== password) {
      return res.status(401).json({ message: "Mot de passe incorrect." });
    }

    // Récupérer l'association du membre si elle existe
    const assoResult = await db.query(
      "SELECT id_association FROM membre_asso WHERE id_membre = $1 LIMIT 1",
      [user.id_membre]
    );

    const id_association = assoResult.rows.length
      ? assoResult.rows[0].id_association
      : null;

    return res.status(200).json({
      message: "Connexion réussie",
      id_membre: user.id_membre,
      nom: user.nom_membre,
      prenom: user.prenom_membre,
      id_association,
    });

  } catch (err) {
    console.error("Erreur login :", err);
    return res.status(500).json({ message: "Erreur serveur" });
  }
});


// ===============================
//  ROUTES ASSOCIATION
// ===============================

// GET /api/associations/:id
app.get("/api/associations/:id", async (req, res) => {
  const id = req.params.id;
  try {
    const result = await db.query(
      "SELECT * FROM association WHERE id_association = $1",
      [id]
    );
    res.json(result.rows[0] || {});
  } catch (err) {
    console.error(err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// GET /api/associations/:id/conseil
app.get("/api/associations/:id/conseil", async (req, res) => {
  const id = req.params.id;
  try {
    const result = await db.query(
      `SELECT m.id_membre, m.nom_membre AS nom, m.prenom_membre AS prenom, ma.role, ma.conseil_asso
       FROM membre_asso ma
       JOIN membre m ON ma.id_membre = m.id_membre
       WHERE ma.id_association = $1 AND ma.conseil_asso = TRUE`,
      [id]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur conseil :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// GET /api/associations/:id/membres
app.get("/api/associations/:id/membres", async (req, res) => {
  const id = req.params.id;
  try {
    const result = await db.query(
      `SELECT m.id_membre, m.nom_membre AS nom, m.prenom_membre AS prenom, ma.role
       FROM membre_asso ma
       JOIN membre m ON ma.id_membre = m.id_membre
       WHERE ma.id_association = $1`,
      [id]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur membres :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// GET /api/associations/:id/is-admin/:id_membre
app.get("/api/associations/:id/is-admin/:id_membre", async (req, res) => {
  const { id, id_membre } = req.params;
  try {
    const result = await db.query(
      `SELECT role FROM membre_asso
       WHERE id_association = $1 AND id_membre = $2
       LIMIT 1`,
      [id, id_membre]
    );

    if (!result.rows.length) return res.json({ isAdmin: false });

    const rawRole = result.rows[0].role || "";
    const role = rawRole.toLowerCase();
    const isAdmin =
      role.includes("président") || role.includes("president") ||
      role.includes("secr") ||
      role.includes("trésorier") || role.includes("tresorier") ||
      role.includes("admin");

    return res.json({ isAdmin, role: rawRole });
  } catch (err) {
    console.error("Erreur is-admin :", err);
    return res.status(500).json({ message: "Erreur serveur" });
  }
});


// GET /api/association/search?nom=...
app.get("/api/association/search", async (req, res) => {
  const nom = req.query.nom?.trim();
  if (!nom) return res.json([]);

  try {
    const result = await db.query(
      `SELECT id_association, nom, sport, ville, type_structure
       FROM association
       WHERE LOWER(nom) LIKE LOWER($1)`,
      [`%${nom}%`]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur recherche association :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// POST /api/association  (création)
app.post("/api/association", async (req, res) => {
  const {
    id_membre,
    nom,
    type_structure,
    sport,
    adresse,
    adresse_2,
    description,
    date_creation,
    code_postal,
    ville,
    pays,
    image,
  } = req.body;

  console.log("📩 /api/association body =", req.body);

  if (!id_membre) {
    return res.status(400).json({ message: "id_membre manquant (créateur)." });
  }
  if (!nom || !type_structure || !sport || !adresse || !date_creation || !code_postal || !ville || !pays) {
    return res.status(400).json({ message: "Champs requis manquants." });
  }

  const couleur_1 = "#6CCFFF";
  const couleur_2 = "#FFFFFF";

  const client = await db.connect();
  try {
    await client.query("BEGIN");

    // Créer l'association
    const assoResult = await client.query(
      `INSERT INTO association
         (nom, type_structure, sport, adresse, adresse_2, description, date_creation, image, code_postal, ville, pays, couleur_1, couleur_2)
       VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
       RETURNING id_association`,
      [
        nom, type_structure, sport, adresse,
        adresse_2 || "", description || "", date_creation,
        image || "", code_postal, ville, pays,
        couleur_1, couleur_2,
      ]
    );

    const id_association = assoResult.rows[0].id_association;

    // Lier le créateur comme Président du conseil
    await client.query(
      `INSERT INTO membre_asso (id_association, id_membre, role, date_adhesion, conseil_asso)
       VALUES ($1, $2, $3, CURRENT_DATE, TRUE)`,
      [id_association, id_membre, "Président"]
    );

    await client.query("COMMIT");

    return res.status(201).json({
      message: "Informations enregistrées",
      id_association,
    });

  } catch (err) {
    await client.query("ROLLBACK");
    console.error("❌ Erreur création association :", err);
    return res.status(500).json({ message: "Erreur serveur" });
  } finally {
    client.release();
  }
});


// PUT /api/association/design/:id
app.put("/api/association/design/:id", async (req, res) => {
  const { couleur_1, couleur_2, image } = req.body;
  const id = req.params.id;

  if (!couleur_1 || !couleur_2) {
    return res.status(400).json({ message: "Couleurs manquantes." });
  }

  try {
    await db.query(
      `UPDATE association
       SET couleur_1 = $1, couleur_2 = $2, image = $3
       WHERE id_association = $4`,
      [couleur_1, couleur_2, image || "", id]
    );
    return res.status(200).json({ message: "Design mis à jour." });
  } catch (err) {
    console.error("Erreur design :", err);
    return res.status(500).json({ message: "Erreur serveur." });
  }
});


// ===============================
//  ROUTES ÉVÉNEMENTS
// ===============================

// GET /api/associations/:id/events
app.get("/api/associations/:id/events", async (req, res) => {
  const id = req.params.id;
  try {
    const result = await db.query(
      `SELECT * FROM evenement
       WHERE id_association = $1
       ORDER BY date_debut_event ASC`,
      [id]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur events :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// POST /api/evenements  (création + actualité liée + invitations participants)
app.post("/api/evenements", async (req, res) => {
  const {
    id_association,
    id_auteur,
    titre_evenement,
    type_evenement,
    description_evenement,
    lieu_event,
    date_debut_event,
    date_fin_event,
    participants, // tableau d'id_membre, ou null pour inviter tous
  } = req.body;

  console.log("📩 /api/evenements body =", req.body);

  if (!id_association || !id_auteur || !titre_evenement || !type_evenement || !date_debut_event) {
    return res.status(400).json({ message: "Champs obligatoires manquants." });
  }

  const client = await db.connect();
  try {
    await client.query("BEGIN");

    // 1) Créer l'événement
    const evResult = await client.query(
      `INSERT INTO evenement
         (id_association, titre_evenement, type_evenement, description_evenement, lieu_event, date_debut_event, date_fin_event)
       VALUES ($1,$2,$3,$4,$5,$6,$7)
       RETURNING id_evenement`,
      [
        Number(id_association),
        titre_evenement,
        type_evenement,
        description_evenement || "",
        lieu_event || "",
        date_debut_event,
        date_fin_event || null,
      ]
    );

    const id_evenement = evResult.rows[0].id_evenement;

    // 2) Créer l'actualité liée automatiquement
    await client.query(
      `INSERT INTO actualite
         (id_association, id_auteur, type_actualite, titre, contenu, date_creation, date_publication, statut, id_evenement, event_date)
       VALUES ($1,$2,'Evenement',$3,$4,NOW(),NOW(),'Approuve',$5,$6)`,
      [
        Number(id_association),
        Number(id_auteur),
        titre_evenement,
        description_evenement || "",
        id_evenement,
        date_debut_event,
      ]
    );

    // 3) Déterminer la liste des invités
    let membresIds = [];

    if (Array.isArray(participants) && participants.length > 0) {
      // Inviter uniquement les membres sélectionnés
      membresIds = participants.map(Number);
    } else {
      // Inviter tous les membres de l'association
      const allMembres = await client.query(
        "SELECT id_membre FROM membre_asso WHERE id_association = $1",
        [Number(id_association)]
      );
      membresIds = allMembres.rows.map(r => r.id_membre);
    }

    // 4) Créer les participations en attente (table participation existante)
    for (const idMembre of membresIds) {
      await client.query(
        `INSERT INTO participation (id_evenement, id_membre, presence)
         VALUES ($1, $2, 'en attente')
         ON CONFLICT (id_evenement, id_membre) DO NOTHING`,
        [id_evenement, idMembre]
      );
    }

    await client.query("COMMIT");

    return res.status(201).json({
      success: true,
      id_evenement,
      invites: membresIds.length,
    });

  } catch (err) {
    await client.query("ROLLBACK");
    console.error("❌ Erreur création événement :", err);
    return res.status(500).json({ message: "Erreur serveur" });
  } finally {
    client.release();
  }
});


// ===============================
//  ROUTES ACTUALITÉS
// ===============================

// GET /api/associations/:id/news
app.get("/api/associations/:id/news", async (req, res) => {
  const id = req.params.id;
  try {
    const result = await db.query(
      `SELECT *
       FROM actualite
       WHERE id_association = $1
         AND statut = 'Approuve'
       ORDER BY COALESCE(date_publication, date_creation) DESC`,
      [id]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur news :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// POST /api/news  (membre propose une actualité)
app.post("/api/news", async (req, res) => {
  const { id_association, id_auteur, titre, contenu, image_principale } = req.body;

  if (!id_association || !id_auteur || !titre || !contenu) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  try {
    await db.query(
      `INSERT INTO actualite
         (id_association, id_auteur, type_actualite, titre, contenu, image_principale, statut, date_creation, date_publication)
       VALUES ($1,$2,'Article',$3,$4,$5,'Pending',NOW(),NOW())`,
      [id_association, id_auteur, titre, contenu, image_principale || null]
    );
    res.json({ success: true });
  } catch (err) {
    console.error("Erreur ajout news :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// PATCH /api/news/:id/approve
app.patch("/api/news/:id/approve", async (req, res) => {
  const id = req.params.id;
  try {
    await db.query(
      `UPDATE actualite SET statut = 'Approuve', date_publication = NOW()
       WHERE id_actualite = $1`,
      [id]
    );
    res.json({ success: true });
  } catch (err) {
    console.error("Erreur approve news :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// PATCH /api/news/:id/refuse
app.patch("/api/news/:id/refuse", async (req, res) => {
  const id = req.params.id;
  try {
    await db.query(
      `UPDATE actualite SET statut = 'Refuse' WHERE id_actualite = $1`,
      [id]
    );
    res.json({ success: true });
  } catch (err) {
    console.error("Erreur refuse news :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// ===============================
//  ROUTES MEMBRE
// ===============================

// GET /api/membre/:id
app.get("/api/membre/:id", async (req, res) => {
  const id = req.params.id;
  try {
    const result = await db.query(
      `SELECT m.*, ma.role AS role_asso, ma.date_adhesion, e.nom_equipe
       FROM membre m
       LEFT JOIN membre_asso ma ON m.id_membre = ma.id_membre
       LEFT JOIN membre_activite a ON ma.id_membre_asso = a.id_membre_asso
       LEFT JOIN equipe e ON a.id_equipe = e.id_equipe
       WHERE m.id_membre = $1
       LIMIT 1`,
      [id]
    );
    res.json(result.rows[0] || {});
  } catch (err) {
    console.error("Erreur GET membre :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// PUT /api/membre/:id
app.put("/api/membre/:id", async (req, res) => {
  const id = req.params.id;
  const { nom, prenom, email, birthday } = req.body;

  try {
    let age = null;
    if (birthday) {
      age = new Date().getFullYear() - new Date(birthday).getFullYear();
    }

    await db.query(
      `UPDATE membre SET
         nom_membre = $1,
         prenom_membre = $2,
         mail_membre = $3,
         date_naissance = $4,
         age = $5
       WHERE id_membre = $6`,
      [nom, prenom, email, birthday, age, id]
    );

    res.json({ message: "Profil mis à jour" });
  } catch (err) {
    console.error("Erreur PUT membre :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// GET /api/membre/:id/association
app.get("/api/membre/:id/association", async (req, res) => {
  const id = req.params.id;
  try {
    const result = await db.query(
      `SELECT a.*
       FROM membre_asso ma
       JOIN association a ON ma.id_association = a.id_association
       WHERE ma.id_membre = $1
       LIMIT 1`,
      [id]
    );
    res.json(result.rows[0] || {});
  } catch (err) {
    console.error("Erreur association du membre :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// GET /api/membre/:id/equipes
app.get("/api/membre/:id/equipes", async (req, res) => {
  const id = req.params.id;
  try {
    const result = await db.query(
      `SELECT e.nom_equipe, a.role_activite AS role
       FROM membre_activite a
       JOIN equipe e ON a.id_equipe = e.id_equipe
       JOIN membre_asso ma ON ma.id_membre_asso = a.id_membre_asso
       WHERE ma.id_membre = $1`,
      [id]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur GET équipes :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// GET /api/membre/:id/presences
app.get("/api/membre/:id/presences", async (req, res) => {
  const id = req.params.id;
  try {
    const result = await db.query(
      `SELECT nom_activite, statut, date_presence
       FROM presence
       WHERE id_membre = $1
       ORDER BY date_presence DESC`,
      [id]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur GET presences :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// ===============================
//  ROUTES CALENDRIER / RSVP
// ===============================

// GET /api/membre/:id/evenements  — tous les événements de l'asso avec statut RSVP du membre
app.get("/api/membre/:id/evenements", async (req, res) => {
  const id = req.params.id;
  try {
    // Association du membre
    const assoResult = await db.query(
      "SELECT id_association FROM membre_asso WHERE id_membre = $1 LIMIT 1",
      [id]
    );
    if (!assoResult.rows.length) return res.json([]);

    const idAsso = assoResult.rows[0].id_association;

    // Événements auxquels le membre est invité, avec statut RSVP mappé en français
    const result = await db.query(
      `SELECT e.*,
              CASE p.presence
                  WHEN 'present'   THEN 'Accepté'
                  WHEN 'absent'    THEN 'Refusé'
                  WHEN 'peut etre' THEN 'Peut-être'
                  ELSE                  'En attente'
              END AS statut,
              p.id_participation
       FROM evenement e
       INNER JOIN participation p
              ON p.id_evenement = e.id_evenement AND p.id_membre = $1
       WHERE e.id_association = $2
       ORDER BY e.date_debut_event ASC`,
      [id, idAsso]
    );

    res.json(result.rows);
  } catch (err) {
    console.error("Erreur événements membre :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// PATCH /api/evenements/:id/rsvp  — le membre accepte ou refuse l'invitation
app.patch("/api/evenements/:id/rsvp", async (req, res) => {
  const idEvenement = req.params.id;
  const { id_membre, statut } = req.body;

  if (!id_membre || !statut) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  // Mapping français → valeur enum presence_enum de la BDD
  const presenceMap = {
    "Accepté":    "present",
    "Refusé":     "absent",
    "Peut-être":  "peut etre",
    "En attente": "en attente"
  };

  const presenceVal = presenceMap[statut];
  if (!presenceVal) {
    return res.status(400).json({ message: "Statut invalide." });
  }

  try {
    await db.query(
      `INSERT INTO participation (id_evenement, id_membre, presence)
       VALUES ($1, $2, $3)
       ON CONFLICT (id_evenement, id_membre)
       DO UPDATE SET presence = $3`,
      [idEvenement, id_membre, presenceVal]
    );
    res.json({ success: true });
  } catch (err) {
    console.error("Erreur RSVP :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// ===============================
//  STATIC + LISTEN
// ===============================



app.listen(port, () => {
  console.log(`✅ Serveur Koordy démarré sur http://localhost:${port}`);
});