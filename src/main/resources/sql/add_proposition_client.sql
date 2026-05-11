-- Script de migration pour ajouter les colonnes de gestion des propositions de clients
-- à la table voyage

-- Ajouter les colonnes de gestion des propositions clients
ALTER TABLE voyage ADD COLUMN IF NOT EXISTS id_user_createur INT DEFAULT NULL;
ALTER TABLE voyage ADD COLUMN IF NOT EXISTS est_proposition_client BOOLEAN DEFAULT FALSE;

-- Index pour optimiser les recherches
CREATE INDEX IF NOT EXISTS idx_voyage_proposition ON voyage(est_proposition_client);
CREATE INDEX IF NOT EXISTS idx_voyage_createur ON voyage(id_user_createur);
CREATE INDEX IF NOT EXISTS idx_voyage_statut ON voyage(statut);

-- Créer une view pour voir les propositions en attente
CREATE OR REPLACE VIEW propositions_voyage_en_attente AS
SELECT 
    v.*,
    CONCAT(u.nom, ' ', u.prenom) as createur_nom
FROM voyage v
LEFT JOIN user u ON v.id_user_createur = u.idUser
WHERE v.est_proposition_client = TRUE AND v.statut = 'EN ATTENTE'
ORDER BY v.idVoyage DESC;
