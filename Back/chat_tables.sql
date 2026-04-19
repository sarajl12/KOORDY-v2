-- ============================================================
--  TABLES CHAT  —  à exécuter dans pgAdmin sur koordybdd
--  Outils → Query Tool → colle ce fichier → F5
-- ============================================================

CREATE TABLE public.conversation (
    id_conversation SERIAL PRIMARY KEY,
    id_association  INTEGER REFERENCES public.association(id_association) ON DELETE CASCADE,
    nom             VARCHAR(100),
    type            VARCHAR(20) NOT NULL DEFAULT 'direct',  -- 'direct' | 'group'
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE public.conversation_membre (
    id_conversation INTEGER REFERENCES public.conversation(id_conversation) ON DELETE CASCADE,
    id_membre       INTEGER REFERENCES public.membre(id_membre) ON DELETE CASCADE,
    PRIMARY KEY (id_conversation, id_membre)
);

CREATE TABLE public.message (
    id_message      SERIAL PRIMARY KEY,
    id_conversation INTEGER REFERENCES public.conversation(id_conversation) ON DELETE CASCADE,
    id_auteur       INTEGER REFERENCES public.membre(id_membre) ON DELETE CASCADE,
    contenu         TEXT NOT NULL DEFAULT '',
    type_message    VARCHAR(20) NOT NULL DEFAULT 'text',  -- 'text' | 'invitation'
    id_evenement    INTEGER REFERENCES public.evenement(id_evenement) ON DELETE SET NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_message_conv ON public.message(id_conversation);
CREATE INDEX idx_message_date ON public.message(created_at);
CREATE INDEX idx_conv_membre  ON public.conversation_membre(id_membre);
