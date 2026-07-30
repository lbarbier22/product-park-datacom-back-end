-- Statut contraint à un type énuméré.
-- PLACEHOLDER : ces valeurs sont des hypothèses de ma part, à ajuster avec
-- vos vraies valeurs métier avant toute mise en prod (aucune donnée produit
-- n'existe encore à ce stade, donc aucun risque à corriger maintenant).
CREATE TYPE product_status AS ENUM (
    'DRAFT', 'PENDING', 'VALIDATED', 'REJECTED'
);

-- Champs obligatoires + unicité de la référence produit
ALTER TABLE products
ALTER COLUMN name TYPE VARCHAR(150),
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN reference TYPE VARCHAR(50),
    ALTER COLUMN reference SET NOT NULL,
    ADD CONSTRAINT products_reference_unique UNIQUE (reference);

ALTER TABLE products
ALTER COLUMN description TYPE TEXT,
    ALTER COLUMN validation TYPE TEXT,
    ALTER COLUMN rejection_reason TYPE TEXT;

-- Réduction des tailles surdimensionnées
ALTER TABLE products
ALTER COLUMN category TYPE VARCHAR(100),
    ALTER COLUMN subcategory TYPE VARCHAR(100),
    ALTER COLUMN lot TYPE VARCHAR(50),
    ALTER COLUMN certification TYPE VARCHAR(100),
    ALTER COLUMN country TYPE VARCHAR(100);

-- Statut en enum + étape de workflow en SMALLINT
ALTER TABLE products
ALTER COLUMN status TYPE product_status USING status::product_status;

-- Timestamps avec fuseau horaire (évite l'ambiguïté TIMESTAMP sans TZ)
ALTER TABLE products
ALTER COLUMN created_at TYPE TIMESTAMPTZ,
    ALTER COLUMN created_at SET DEFAULT now(),
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ;

-- Traçabilité de la validation : qui a validé, et quand
ALTER TABLE products
    ADD COLUMN validated_by BIGINT REFERENCES users(id),
    ADD COLUMN validated_at TIMESTAMPTZ;

-- La raison de rejet ne doit exister que si le produit est effectivement rejeté
ALTER TABLE products
    ADD CONSTRAINT rejection_reason_requires_status
        CHECK (rejection_reason IS NULL OR status = 'REJECTED');