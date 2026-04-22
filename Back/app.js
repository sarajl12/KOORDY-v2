import cors from "cors";
import express from "express";
import multer from "multer";
import path from "path";
import fs from "fs";
import { fileURLToPath } from "url";
import { initDB } from "./connexionBDD.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const app = express();
const port = 3001;

const db = await initDB();

// Colonne photo_membre si elle n'existe pas encore
await db.query(`ALTER TABLE membre ADD COLUMN IF NOT EXISTS photo_membre TEXT DEFAULT ''`).catch(() => {});

// Dossier d'upload
const uploadDir = path.join(__dirname, "uploads", "membres");
fs.mkdirSync(uploadDir, { recursive: true });

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, uploadDir),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname) || ".jpg";
    cb(null, `membre_${Date.now()}${ext}`);
  },
});
const upload = multer({ storage, limits: { fileSize: 5 * 1024 * 1024 } });

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use("/uploads", express.static(path.join(__dirname, "uploads")));



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
      `SELECT m.id_membre, m.nom_membre AS nom, m.prenom_membre AS prenom, ma.role,
              (CASE WHEN ma.conseil_asso THEN 1 ELSE 0 END) AS conseil_asso,
              m.photo_membre
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
      `SELECT m.id_membre, m.nom_membre AS nom, m.prenom_membre AS prenom,
              ma.role,
              (CASE WHEN ma.conseil_asso THEN 1 ELSE 0 END) AS conseil_asso,
              ma.date_adhesion,
              m.age, m.mail_membre, m.photo_membre
       FROM membre_asso ma
       JOIN membre m ON ma.id_membre = m.id_membre
       WHERE ma.id_association = $1
       ORDER BY ma.date_adhesion ASC`,
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
  if (!nom || !type_structure || !adresse || !date_creation || !code_postal || !ville || !pays) {
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


// POST /api/association/:id/rejoindre
app.post("/api/association/:id/rejoindre", async (req, res) => {
  const id_association = parseInt(req.params.id);
  const { id_membre } = req.body;

  if (!id_membre) {
    return res.status(400).json({ message: "id_membre manquant." });
  }

  try {
    // Vérifier que l'association existe
    const assoCheck = await db.query(
      "SELECT id_association FROM association WHERE id_association = $1",
      [id_association]
    );
    if (!assoCheck.rows.length) {
      return res.status(404).json({ message: "Association introuvable." });
    }

    // Vérifier si le membre est déjà dans l'association
    const existing = await db.query(
      "SELECT id_membre_asso FROM membre_asso WHERE id_association = $1 AND id_membre = $2",
      [id_association, id_membre]
    );
    if (existing.rows.length) {
      return res.status(200).json({ message: "Membre déjà dans l'association.", id_association });
    }

    await db.query(
      `INSERT INTO membre_asso (id_association, id_membre, role, date_adhesion, conseil_asso)
       VALUES ($1, $2, 'Membre', CURRENT_DATE, FALSE)`,
      [id_association, id_membre]
    );

    return res.status(201).json({ message: "Membre ajouté à l'association.", id_association });
  } catch (err) {
    console.error("❌ Erreur rejoindre association :", err);
    return res.status(500).json({ message: "Erreur serveur" });
  }
});


// PUT /api/associations/:id/infos  (modifier description, adresse, cp, ville, pays, téléphone)
app.put("/api/associations/:id/infos", async (req, res) => {
  const { description, adresse, code_postal, ville, pays, telephone } = req.body;
  const id = req.params.id;
  try {
    await db.query(
      `UPDATE association
       SET description = $1, adresse = $2, code_postal = $3, ville = $4, pays = $5, telephone = $6
       WHERE id_association = $7`,
      [description || "", adresse || "", code_postal || "", ville || "", pays || "", telephone || "", id]
    );
    res.json({ success: true });
  } catch (err) {
    console.error("Erreur PUT infos association :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// PATCH /api/associations/:id/photo  (upload photo de profil de l'asso)
const assoUploadDir = path.join(__dirname, "uploads", "associations");
fs.mkdirSync(assoUploadDir, { recursive: true });

const assoStorage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, assoUploadDir),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname) || ".jpg";
    cb(null, `asso_${Date.now()}${ext}`);
  },
});
const uploadAssoPhoto = multer({ storage: assoStorage, limits: { fileSize: 8 * 1024 * 1024 } });

app.patch("/api/associations/:id/photo", uploadAssoPhoto.single("photo"), async (req, res) => {
  const id = req.params.id;
  if (!req.file) return res.status(400).json({ message: "Aucun fichier reçu." });
  const photoPath = `/uploads/associations/${req.file.filename}`;
  try {
    await db.query(
      "UPDATE association SET image = $1 WHERE id_association = $2",
      [photoPath, id]
    );
    res.json({ success: true, photo: photoPath });
  } catch (err) {
    console.error("Erreur upload photo association :", err);
    res.status(500).json({ message: "Erreur serveur" });
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

    // 4) Créer les participations en attente pour tous (y compris le créateur)
    const auteurIdNum = Number(id_auteur);
    if (!membresIds.includes(auteurIdNum)) {
      membresIds.push(auteurIdNum);
    }
    for (const idMembre of membresIds) {
      await client.query(
        `INSERT INTO participation (id_evenement, id_membre, presence)
         VALUES ($1, $2, 'en attente')
         ON CONFLICT (id_evenement, id_membre) DO NOTHING`,
        [id_evenement, idMembre]
      );
    }

    await client.query("COMMIT");

    // 5) Poster dans le chat général — fire-and-forget, ne bloque pas la réponse
    postInvitationToGeneralChat(db, id_association, id_auteur, titre_evenement, id_evenement, membresIds);

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


// Helper : trouver ou créer une conversation directe entre deux membres
async function getOrCreateDirectConv(db, id_association, idA, idB) {
  const existing = await db.query(`
    SELECT c.id_conversation FROM conversation c
    JOIN conversation_membre cm1 ON c.id_conversation = cm1.id_conversation AND cm1.id_membre = $1
    JOIN conversation_membre cm2 ON c.id_conversation = cm2.id_conversation AND cm2.id_membre = $2
    WHERE c.type = 'direct'
    LIMIT 1
  `, [idA, idB]);

  if (existing.rows.length > 0) return existing.rows[0].id_conversation;

  const newConv = await db.query(
    `INSERT INTO conversation (id_association, type) VALUES ($1, 'direct') RETURNING id_conversation`,
    [id_association]
  );
  const idConv = newConv.rows[0].id_conversation;

  // Utiliser deux INSERT séparés pour éviter les problèmes ON CONFLICT sans contrainte déclarée
  await db.query(
    `INSERT INTO conversation_membre (id_conversation, id_membre)
     SELECT $1, $2 WHERE NOT EXISTS (
       SELECT 1 FROM conversation_membre WHERE id_conversation = $1 AND id_membre = $2
     )`,
    [idConv, idA]
  );
  await db.query(
    `INSERT INTO conversation_membre (id_conversation, id_membre)
     SELECT $1, $2 WHERE NOT EXISTS (
       SELECT 1 FROM conversation_membre WHERE id_conversation = $1 AND id_membre = $2
     )`,
    [idConv, idB]
  );

  return idConv;
}

// Helper interne : envoyer l'invitation en message direct à chaque membre invité
async function postInvitationToGeneralChat(db, id_association, id_auteur, titre_evenement, id_evenement, membresIds) {
  try {
    const auteurId = Number(id_auteur);
    // Inclure tous les membres invités (y compris le créateur pour qu'il voie l'invitation)
    const destinataires = membresIds.map(Number).filter(id => id !== auteurId);

    for (const idDest of destinataires) {
      try {
        const idConv = await getOrCreateDirectConv(db, Number(id_association), auteurId, idDest);
        await db.query(
          `INSERT INTO message (id_conversation, id_auteur, contenu, type_message, id_evenement)
           VALUES ($1, $2, $3, 'invitation', $4)`,
          [idConv, auteurId, titre_evenement, id_evenement]
        );
      } catch (innerErr) {
        console.warn(`⚠️ Invitation non envoyée à membre ${idDest} :`, innerErr.message);
      }
    }
  } catch (err) {
    console.warn("⚠️ postInvitationToGeneralChat échoué :", err.message);
  }
}


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


// POST /api/news  (création d'actualité — statut passé par le client)
app.post("/api/news", async (req, res) => {
  const { id_association, id_auteur, titre, contenu, image_principale, statut, type_actualite } = req.body;

  if (!id_association || !id_auteur || !titre) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  try {
    await db.query(
      `INSERT INTO actualite
         (id_association, id_auteur, type_actualite, titre, contenu, image_principale, statut, date_creation, date_publication)
       VALUES ($1,$2,$3,$4,$5,$6,$7,NOW(),NOW())`,
      [
        id_association, id_auteur,
        type_actualite || "Article",
        titre, contenu || "",
        image_principale || null,
        statut || "Pending"
      ]
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


// PATCH /api/membre/:id/photo
app.patch("/api/membre/:id/photo", upload.single("photo"), async (req, res) => {
  const id = req.params.id;
  if (!req.file) return res.status(400).json({ message: "Aucun fichier reçu." });

  const photoPath = `/uploads/membres/${req.file.filename}`;
  try {
    await db.query(
      "UPDATE membre SET photo_membre = $1 WHERE id_membre = $2",
      [photoPath, id]
    );
    res.json({ success: true, photo: photoPath });
  } catch (err) {
    console.error("Erreur upload photo membre :", err);
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
//  ROUTES CHAT
// ===============================

// GET /api/membre/:id/conversations
app.get("/api/membre/:id/conversations", async (req, res) => {
  const idMembre = parseInt(req.params.id);
  try {
    const result = await db.query(`
      SELECT
        c.id_conversation,
        c.nom,
        c.type,
        c.id_association,
        last_msg.contenu        AS last_message,
        last_msg.created_at     AS last_message_at,
        last_msg.type_message   AS last_message_type,
        auteur.nom_membre       AS last_sender_nom,
        auteur.prenom_membre    AS last_sender_prenom,
        other_m.id_membre       AS other_id_membre,
        other_m.nom_membre      AS other_nom,
        other_m.prenom_membre   AS other_prenom,
        other_m.photo_membre    AS other_photo_membre
      FROM conversation c
      JOIN conversation_membre cm ON c.id_conversation = cm.id_conversation AND cm.id_membre = $1
      LEFT JOIN LATERAL (
        SELECT contenu, created_at, type_message, id_auteur
        FROM message
        WHERE id_conversation = c.id_conversation
        ORDER BY created_at DESC LIMIT 1
      ) last_msg ON true
      LEFT JOIN membre auteur ON last_msg.id_auteur = auteur.id_membre
      LEFT JOIN conversation_membre cm2
        ON c.id_conversation = cm2.id_conversation AND cm2.id_membre != $1 AND c.type = 'direct'
      LEFT JOIN membre other_m ON cm2.id_membre = other_m.id_membre
      ORDER BY COALESCE(last_msg.created_at, c.created_at) DESC
    `, [idMembre]);
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur GET conversations :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// POST /api/conversations  — créer ou récupérer une conversation
app.post("/api/conversations", async (req, res) => {
  const { id_association, id_initiateur, type, id_destinataire, nom, participants } = req.body;

  if (!id_association || !id_initiateur || !type) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  const client = await db.connect();
  try {
    await client.query("BEGIN");

    if (type === "direct" && id_destinataire) {
      const existing = await client.query(`
        SELECT c.id_conversation FROM conversation c
        JOIN conversation_membre cm1 ON c.id_conversation = cm1.id_conversation AND cm1.id_membre = $1
        JOIN conversation_membre cm2 ON c.id_conversation = cm2.id_conversation AND cm2.id_membre = $2
        WHERE c.type = 'direct'
        LIMIT 1
      `, [id_initiateur, id_destinataire]);

      if (existing.rows.length > 0) {
        await client.query("ROLLBACK");
        return res.json({ id_conversation: existing.rows[0].id_conversation, existing: true });
      }
    }

    if (type === "group" && nom) {
      const existing = await client.query(`
        SELECT id_conversation FROM conversation
        WHERE id_association = $1 AND nom = $2 AND type = 'group'
        LIMIT 1
      `, [id_association, nom]);

      if (existing.rows.length > 0) {
        await client.query("ROLLBACK");
        return res.json({ id_conversation: existing.rows[0].id_conversation, existing: true });
      }
    }

    const convResult = await client.query(
      `INSERT INTO conversation (id_association, nom, type) VALUES ($1, $2, $3) RETURNING id_conversation`,
      [id_association, nom || null, type]
    );
    const idConversation = convResult.rows[0].id_conversation;

    const membersToAdd = type === "direct"
      ? [Number(id_initiateur), Number(id_destinataire)]
      : [Number(id_initiateur), ...(participants || []).map(Number)];

    for (const idM of membersToAdd.filter(Boolean)) {
      await client.query(
        `INSERT INTO conversation_membre (id_conversation, id_membre) VALUES ($1, $2) ON CONFLICT DO NOTHING`,
        [idConversation, idM]
      );
    }

    await client.query("COMMIT");
    res.status(201).json({ id_conversation: idConversation });
  } catch (err) {
    await client.query("ROLLBACK");
    console.error("Erreur création conversation :", err);
    res.status(500).json({ message: "Erreur serveur" });
  } finally {
    client.release();
  }
});


// GET /api/conversations/:id/messages?id_membre=X
app.get("/api/conversations/:id/messages", async (req, res) => {
  const idConversation = req.params.id;
  const idMembre = req.query.id_membre || 0;

  try {
    const result = await db.query(`
      SELECT
        m.id_message,
        m.id_conversation,
        m.id_auteur,
        m.contenu,
        m.type_message,
        m.id_evenement,
        m.created_at,
        mb.nom_membre       AS nom_auteur,
        mb.prenom_membre    AS prenom_auteur,
        e.titre_evenement,
        e.date_debut_event,
        e.lieu_event,
        e.type_evenement,
        CASE
          WHEN m.type_message = 'invitation' AND m.id_auteur = $2 THEN 'Envoyée'
          WHEN p.presence = 'present'   THEN 'Accepté'
          WHEN p.presence = 'absent'    THEN 'Refusé'
          WHEN p.presence = 'peut etre' THEN 'Peut-être'
          ELSE                               'En attente'
        END AS statut_rsvp
      FROM message m
      JOIN membre mb ON m.id_auteur = mb.id_membre
      LEFT JOIN evenement e ON m.id_evenement = e.id_evenement
      LEFT JOIN participation p ON p.id_evenement = m.id_evenement AND p.id_membre = $2
      WHERE m.id_conversation = $1
      ORDER BY m.created_at ASC
    `, [idConversation, idMembre]);
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur GET messages :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// DELETE /api/conversations/:id
app.delete("/api/conversations/:id", async (req, res) => {
  const id = parseInt(req.params.id);
  try {
    await db.query(`DELETE FROM message WHERE id_conversation = $1`, [id]);
    await db.query(`DELETE FROM conversation_membre WHERE id_conversation = $1`, [id]);
    await db.query(`DELETE FROM conversation WHERE id_conversation = $1`, [id]);
    res.json({ success: true });
  } catch (err) {
    console.error("Erreur DELETE conversation :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// POST /api/conversations/:id/messages
app.post("/api/conversations/:id/messages", async (req, res) => {
  const idConversation = req.params.id;
  const { id_auteur, contenu, type_message, id_evenement } = req.body;

  if (!id_auteur) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  try {
    const result = await db.query(
      `INSERT INTO message (id_conversation, id_auteur, contenu, type_message, id_evenement)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id_message, created_at`,
      [idConversation, id_auteur, contenu || "", type_message || "text", id_evenement || null]
    );
    res.status(201).json({
      success: true,
      id_message: result.rows[0].id_message,
      created_at: result.rows[0].created_at,
    });
  } catch (err) {
    console.error("Erreur POST message :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// ===============================
//  ROUTES ÉQUIPES
// ===============================

// GET /api/associations/:id/equipes — liste des équipes avec nb membres
app.get("/api/associations/:id/equipes", async (req, res) => {
  const idAsso = parseInt(req.params.id);
  try {
    const result = await db.query(
      `SELECT e.id_equipe, e.nom_equipe, e.description_equipe,
              COUNT(ma2.id_membre_activite) AS nb_membres
       FROM equipe e
       JOIN membre_activite ma2 ON e.id_equipe = ma2.id_equipe
       JOIN membre_asso masso ON ma2.id_membre_asso = masso.id_membre_asso
       WHERE masso.id_association = $1
       GROUP BY e.id_equipe, e.nom_equipe, e.description_equipe
       ORDER BY e.nom_equipe ASC`,
      [idAsso]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur GET équipes asso :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// POST /api/equipes — créer une équipe et y ajouter des membres
app.post("/api/equipes", async (req, res) => {
  const { id_association, nom_equipe, description, membres } = req.body;

  if (!id_association || !nom_equipe) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  const client = await db.connect();
  try {
    await client.query("BEGIN");

    // Créer l'équipe
    const equipeResult = await client.query(
      `INSERT INTO equipe (nom_equipe, description_equipe)
       VALUES ($1, $2)
       RETURNING id_equipe`,
      [nom_equipe, description || ""]
    );
    const id_equipe = equipeResult.rows[0].id_equipe;

    // Lier les membres sélectionnés
    if (Array.isArray(membres) && membres.length > 0) {
      for (const id_membre of membres) {
        const membreAsso = await client.query(
          `SELECT id_membre_asso FROM membre_asso
           WHERE id_association = $1 AND id_membre = $2
           LIMIT 1`,
          [id_association, id_membre]
        );
        if (membreAsso.rows.length > 0) {
          await client.query(
            `INSERT INTO membre_activite (id_membre_asso, id_equipe, role_activite)
             VALUES ($1, $2, 'Joueur')
             ON CONFLICT DO NOTHING`,
            [membreAsso.rows[0].id_membre_asso, id_equipe]
          );
        }
      }
    }

    await client.query("COMMIT");
    res.status(201).json({ success: true, id_equipe });
  } catch (err) {
    await client.query("ROLLBACK");
    console.error("Erreur POST équipe :", err);
    res.status(500).json({ message: "Erreur serveur" });
  } finally {
    client.release();
  }
});


// GET /api/equipes/:id/membres — membres d'une équipe
app.get("/api/equipes/:id/membres", async (req, res) => {
  const id_equipe = parseInt(req.params.id);
  try {
    const result = await db.query(
      `SELECT m.id_membre, m.nom_membre AS nom, m.prenom_membre AS prenom, masso.role
       FROM membre_activite ma
       JOIN membre_asso masso ON ma.id_membre_asso = masso.id_membre_asso
       JOIN membre m ON masso.id_membre = m.id_membre
       WHERE ma.id_equipe = $1`,
      [id_equipe]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur GET membres équipe :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// PUT /api/equipes/:id/membres — mettre à jour les membres d'une équipe
app.put("/api/equipes/:id/membres", async (req, res) => {
  const id_equipe = parseInt(req.params.id);
  const { id_association, membres } = req.body;

  if (!id_association) {
    return res.status(400).json({ message: "id_association manquant." });
  }

  const client = await db.connect();
  try {
    await client.query("BEGIN");

    // Supprimer les anciennes liaisons membres de cette équipe pour cette association
    await client.query(
      `DELETE FROM membre_activite
       WHERE id_equipe = $1
         AND id_membre_asso IN (
           SELECT id_membre_asso FROM membre_asso WHERE id_association = $2
         )`,
      [id_equipe, id_association]
    );

    // Re-lier les membres cochés
    if (Array.isArray(membres) && membres.length > 0) {
      for (const id_membre of membres) {
        const membreAsso = await client.query(
          `SELECT id_membre_asso FROM membre_asso
           WHERE id_association = $1 AND id_membre = $2
           LIMIT 1`,
          [id_association, id_membre]
        );
        if (membreAsso.rows.length > 0) {
          await client.query(
            `INSERT INTO membre_activite (id_membre_asso, id_equipe, role_activite)
             VALUES ($1, $2, 'Joueur')
             ON CONFLICT DO NOTHING`,
            [membreAsso.rows[0].id_membre_asso, id_equipe]
          );
        }
      }
    }

    await client.query("COMMIT");
    res.json({ success: true });
  } catch (err) {
    await client.query("ROLLBACK");
    console.error("Erreur PUT membres équipe :", err);
    res.status(500).json({ message: "Erreur serveur" });
  } finally {
    client.release();
  }
});


// ===============================
//  ROUTES PRÉSIDENT
// ===============================

// PATCH /api/membre/:id/role — modifier le rôle d'un membre dans une association
app.patch("/api/membre/:id/role", async (req, res) => {
  const id_membre = parseInt(req.params.id);
  const { role, id_association } = req.body;

  if (!role || !id_association) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  try {
    const conseilRoles = ["Président", "Trésorier", "Secrétaire", "Adjoint"];
    const conseil_asso = conseilRoles.some(r =>
      r.toLowerCase() === role.toLowerCase()
    );

    await db.query(
      `UPDATE membre_asso
       SET role = $1, conseil_asso = $2
       WHERE id_membre = $3 AND id_association = $4`,
      [role, conseil_asso, id_membre, id_association]
    );
    res.json({ success: true });
  } catch (err) {
    console.error("Erreur PATCH rôle :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// ===============================
//  NEWS AVEC IMAGE (Président)
// ===============================

// Dossier upload actualités
const newsUploadDir = path.join(__dirname, "uploads", "actualites");
fs.mkdirSync(newsUploadDir, { recursive: true });

const newsStorage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, newsUploadDir),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname) || ".jpg";
    cb(null, `actu_${Date.now()}${ext}`);
  },
});
const uploadNews = multer({ storage: newsStorage, limits: { fileSize: 8 * 1024 * 1024 } });

// POST /api/news/upload — créer une actualité avec image (multipart)
app.post("/api/news/upload", uploadNews.single("image"), async (req, res) => {
  const { id_association, id_auteur, type_actualite, titre, contenu, statut } = req.body;

  if (!id_association || !id_auteur || !titre) {
    return res.status(400).json({ message: "Champs manquants." });
  }

  const image_principale = req.file
    ? `/uploads/actualites/${req.file.filename}`
    : null;

  try {
    await db.query(
      `INSERT INTO actualite
         (id_association, id_auteur, type_actualite, titre, contenu, image_principale, statut, date_creation, date_publication)
       VALUES ($1,$2,$3,$4,$5,$6,$7,NOW(),NOW())`,
      [
        id_association, id_auteur,
        type_actualite || "Article",
        titre, contenu || "",
        image_principale,
        statut || "Approuve"
      ]
    );
    res.status(201).json({ success: true });
  } catch (err) {
    console.error("Erreur POST news/upload :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// ===============================
//  ROUTES CHECKLIST "À NE PAS OUBLIER"
// ===============================

await db.query(`
  CREATE TABLE IF NOT EXISTS checklist (
    id_checklist      SERIAL PRIMARY KEY,
    id_association    INT NOT NULL,
    nom_evenement     VARCHAR(255) NOT NULL,
    date_evenement    VARCHAR(255) DEFAULT '',
    date_evenement_ts TIMESTAMP,
    lieu_evenement    VARCHAR(255) DEFAULT '',
    created_at        TIMESTAMP DEFAULT NOW()
  )
`).catch(() => {});

await db.query(`ALTER TABLE checklist ADD COLUMN IF NOT EXISTS date_evenement_ts TIMESTAMP`).catch(() => {});

await db.query(`
  CREATE TABLE IF NOT EXISTS checklist_item (
    id_item        SERIAL PRIMARY KEY,
    id_checklist   INT NOT NULL REFERENCES checklist(id_checklist) ON DELETE CASCADE,
    id_auteur      INT NOT NULL DEFAULT 0,
    nom_auteur     VARCHAR(255) NOT NULL DEFAULT '',
    nom_item       VARCHAR(255) NOT NULL,
    commentaire    TEXT DEFAULT '',
    is_checked     BOOLEAN DEFAULT FALSE,
    checked_by_nom VARCHAR(255),
    checked_at     TIMESTAMP,
    created_at     TIMESTAMP DEFAULT NOW()
  )
`).catch(() => {});

// GET /api/associations/:id/checklists — liste avec items (events passés exclus)
app.get("/api/associations/:id/checklists", async (req, res) => {
  const idAsso = parseInt(req.params.id);
  try {
    const clResult = await db.query(
      `SELECT * FROM checklist
       WHERE id_association = $1
         AND (date_evenement_ts IS NULL OR date_evenement_ts >= NOW())
       ORDER BY date_evenement_ts ASC NULLS LAST, created_at DESC`,
      [idAsso]
    );
    const checklists = clResult.rows;
    for (const cl of checklists) {
      const items = await db.query(
        `SELECT * FROM checklist_item WHERE id_checklist = $1 ORDER BY created_at ASC`,
        [cl.id_checklist]
      );
      cl.items = items.rows;
    }
    res.json(checklists);
  } catch (err) {
    console.error("Erreur GET checklists :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});

// POST /api/associations/:id/checklists — créer une checklist
app.post("/api/associations/:id/checklists", async (req, res) => {
  const idAsso = parseInt(req.params.id);
  const { nom_evenement, date_evenement, date_evenement_ts, lieu_evenement } = req.body;
  if (!nom_evenement) return res.status(400).json({ message: "Nom de l'événement requis." });
  try {
    const result = await db.query(
      `INSERT INTO checklist (id_association, nom_evenement, date_evenement, date_evenement_ts, lieu_evenement)
       VALUES ($1, $2, $3, $4, $5) RETURNING id_checklist`,
      [idAsso, nom_evenement, date_evenement || "", date_evenement_ts || null, lieu_evenement || ""]
    );
    res.status(201).json({ success: true, id_checklist: result.rows[0].id_checklist });
  } catch (err) {
    console.error("Erreur POST checklist :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});

// POST /api/checklists/:id/items — ajouter un élément
app.post("/api/checklists/:id/items", async (req, res) => {
  const idChecklist = parseInt(req.params.id);
  const { id_auteur, nom_auteur, nom_item, commentaire } = req.body;
  if (!nom_item) return res.status(400).json({ message: "nom_item requis." });
  try {
    await db.query(
      `INSERT INTO checklist_item (id_checklist, id_auteur, nom_auteur, nom_item, commentaire)
       VALUES ($1, $2, $3, $4, $5)`,
      [idChecklist, id_auteur || 0, nom_auteur || "", nom_item, commentaire || ""]
    );
    res.status(201).json({ success: true });
  } catch (err) {
    console.error("Erreur POST item :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});

// PATCH /api/checklists/:id/items/:idItem/toggle — cocher/décocher
app.patch("/api/checklists/:id/items/:idItem/toggle", async (req, res) => {
  const idChecklist = parseInt(req.params.id);
  const idItem = parseInt(req.params.idItem);
  const { nom_membre } = req.body;
  try {
    const current = await db.query(
      `SELECT is_checked FROM checklist_item WHERE id_item = $1 AND id_checklist = $2`,
      [idItem, idChecklist]
    );
    if (current.rows.length === 0) return res.status(404).json({ message: "Item non trouvé." });
    const newChecked = !current.rows[0].is_checked;
    await db.query(
      `UPDATE checklist_item
       SET is_checked = $1,
           checked_by_nom = $2,
           checked_at = CASE WHEN $1 = true THEN NOW() ELSE NULL END
       WHERE id_item = $3 AND id_checklist = $4`,
      [newChecked, newChecked ? (nom_membre || "") : null, idItem, idChecklist]
    );
    res.json({ success: true, is_checked: newChecked });
  } catch (err) {
    console.error("Erreur PATCH toggle :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});

// DELETE /api/checklists/:id/items/:idItem — supprimer un élément
app.delete("/api/checklists/:id/items/:idItem", async (req, res) => {
  const idChecklist = parseInt(req.params.id);
  const idItem = parseInt(req.params.idItem);
  try {
    await db.query(
      `DELETE FROM checklist_item WHERE id_item = $1 AND id_checklist = $2`,
      [idItem, idChecklist]
    );
    res.json({ success: true });
  } catch (err) {
    console.error("Erreur DELETE item :", err);
    res.status(500).json({ message: "Erreur serveur" });
  }
});


// ===============================
//  STATIC + LISTEN
// ===============================



app.listen(port, () => {
  console.log(`✅ Serveur Koordy démarré sur http://localhost:${port}`);
});