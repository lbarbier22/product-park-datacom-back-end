-- Rôle contraint à un type énuméré : plus de valeur invalide possible en base
CREATE TYPE user_role AS ENUM ('ADMIN', 'VALIDATOR');

-- Renommage : le champ contient un hash, pas un mot de passe en clair
ALTER TABLE users
    RENAME COLUMN password TO password_hash;

-- Conversion du rôle vers l'enum (les valeurs 'ADMIN'/'VALIDATOR' existantes matchent)
ALTER TABLE users
ALTER COLUMN role TYPE user_role USING role::user_role;

ALTER TABLE users
ALTER COLUMN firstname TYPE VARCHAR(100),
    ALTER COLUMN lastname TYPE VARCHAR(100);

-- Traçabilité et gestion de comptes désactivés
ALTER TABLE users
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT now();