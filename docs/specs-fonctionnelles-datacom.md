# DATACOM — Spécifications fonctionnelles détaillées (v2)

Ce document décrit le comportement attendu de l'application du point de vue de l'utilisateur.
Il s'appuie sur les décisions actées dans le document d'architecture technique et les complète.

---

## 1. Acteurs et permissions

| Action | ADMIN | VALIDATOR |
|---|---|---|
| Se connecter | ✅ | ✅ |
| Voir la liste de tous les produits, tout statut confondu | ✅ | ✅ |
| Voir le détail d'un produit | ✅ | ✅ |
| Créer un nouveau produit | ✅ | ❌ |
| Éditer un produit (steps 1 à 4), y compris ceux créés par un autre ADMIN | ✅ (uniquement si statut `DRAFT` ou `REJECTED`) | ❌ (jamais, lecture seule stricte) |
| Valider un produit | ❌ | ✅ (uniquement si statut `PENDING`) |
| Refuser un produit avec commentaire | ❌ | ✅ (uniquement si statut `PENDING`) |

Le rôle `USER` de l'existant est supprimé : chaque compte doit désormais être explicitement `ADMIN` ou `VALIDATOR`, sans rôle intermédiaire ambigu.

---

## 2. Cycle de vie d'un produit

```
                 [ADMIN termine step 4]
   ┌────────┐  ─────────────────────────►  ┌─────────┐
   │ DRAFT  │                               │ PENDING │
   └────────┘  ◄─────────────────────────   └─────────┘
        ▲       [VALIDATOR refuse                │  │
        │        + rejectionReason]               │  │ [VALIDATOR valide]
        │                                          │  ▼
        └──────────────────────────────────   ┌───────────┐
         [ADMIN termine step 4 après refus]    │ VALIDATED │
                                                └───────────┘
   REJECTED ──────────────────────────────────────────┘
   (état intermédiaire, l'ADMIN reprend au step 1)
```

- **`DRAFT`** : produit en cours de création par un ADMIN, steps 1 à 3 non finalisés (ou premier passage).
- **`PENDING`** : step 4 terminé et soumis, en attente d'une décision du VALIDATOR.
- **`VALIDATED`** : état terminal, **définitivement figé** — aucune modification possible ensuite, pour aucun rôle.
- **`REJECTED`** : état transitoire — dès que l'ADMIN commence à éditer un produit `REJECTED`, il repasse en `DRAFT` et `currentstep` est remis à 1.

---

## 3. Règles de gestion détaillées, écran par écran

### 3.1 Écran de connexion (Login)
- Champs : login, mot de passe.
- En cas d'échec : message générique **"Identifiants incorrects"** (ne pas préciser si c'est le login ou le mot de passe qui est faux — bonne pratique de sécurité, à la différence de l'existant).
- Pas de mécanisme de "mot de passe oublié" prévu dans le scope actuel — **confirmé, hors scope**.

### 3.2 Liste des produits
- Colonnes affichées : Id, Nom, Statut, Step courant, Créé par, Date de création.
- Tri par défaut : les plus récents en premier (comme l'existant).
- Un badge visuel différencie les 4 statuts (couleur), pour un repérage rapide en un coup d'œil.
- **Filtre disponible : par statut uniquement** (pas de filtre par créateur, décision confirmée).

### 3.3 Formulaire produit (ADMIN) — steps 1 à 4

Rappel : progression uniquement vers l'avant, pas de retour en arrière (sauf refus, qui remet tout à zéro).

**Step 1 — Informations générales**
| Champ | Obligatoire | Contrainte proposée (⚠️ À VALIDER) |
|---|---|---|
| Nom | Oui | 3 à 100 caractères |
| Référence | Oui | Format libre, unique — **pas de format imposé, confirmé** |
| Description | Non | Max 1000 caractères |

**Step 2 — Classification**
| Champ | Obligatoire | Contrainte proposée |
|---|---|---|
| Catégorie | Oui | **Texte libre, confirmé** (pas de liste prédéfinie en base) |
| Sous-catégorie | Non | **Texte libre, confirmé** |
| Fabricant | Oui | Texte libre, max 150 caractères |
| Pays | Oui | Liste fermée (reprise de l'existant : France, Germany, Spain, Italy, China, USA) — **confirmé sans modification** |

**Step 3 — Conformité**
| Champ | Obligatoire | Contrainte proposée |
|---|---|---|
| Numéro de lot | Oui | Format libre |
| Certification | Non | Texte libre |
| Commentaire (`validation`) | Non | Champ hérité de l'existant. **Usage métier réel inconnu** — documenté par défaut comme "note libre de l'ADMIN à destination du VALIDATOR", sans logique métier attachée. À réévaluer si l'usage réel est identifié plus tard (ex. auprès des utilisateurs finaux de l'existant). |

**Step 4 — Récapitulatif et soumission**
- Affiche l'ensemble des champs saisis en lecture seule.
- Bouton "Soumettre" : passe le statut à `PENDING`.
- Si le produit était `REJECTED`, affiche en bandeau persistant (sur tous les steps) le motif de refus.

### 3.4 Écran de consultation/validation (VALIDATOR)
- Vue en lecture seule : tous les champs du produit, non modifiables.
- Si `status = PENDING` : deux boutons, "Valider" et "Refuser".
    - "Refuser" ouvre une saisie obligatoire de `rejectionReason`, avec une **longueur minimale de 10 caractères imposée** (validation côté formulaire et côté serveur), pour éviter un motif du type "non" peu exploitable par l'ADMIN — ⚠️ à ajuster si 10 caractères te semble trop court/long une fois en pratique.
- Si `status` autre que `PENDING` : aucun bouton d'action, juste consultation (ex: produit déjà `VALIDATED` ou en cours de re-soumission par l'ADMIN).

---

## 4. Modèle de données fonctionnel

| Champ | Step | Obligatoire | Éditable après soumission (PENDING) |
|---|---|---|---|
| name | 1 | Oui | Non (verrouillé tant que PENDING/VALIDATED) |
| reference | 1 | Oui | Non |
| description | 1 | Non | Non |
| category | 2 | Oui | Non |
| subcategory | 2 | Non | Non |
| manufacturer | 2 | Oui | Non |
| country | 2 | Oui | Non |
| lot | 3 | Oui | Non |
| certification | 3 | Non | Non |
| validation (commentaire libre) | 3 | Non | Non |
| rejectionReason | — (rempli par le VALIDATOR) | Oui si refus | N/A (rempli une seule fois par refus) |
| status | — | — | Géré uniquement par les transitions (jamais saisi directement) |
| currentstep | — | — | Géré automatiquement |
| createdby / createdat / updatedat | — | — | Automatique, jamais éditable |

---

## 5. Cas particuliers et gestion d'erreurs

| Cas | Comportement attendu |
|---|---|
| Un ADMIN tente d'éditer un produit `PENDING` ou `VALIDATED` | Refus, message **"Ce produit ne peut pas être modifié dans son état actuel"** |
| Un VALIDATOR tente de valider/refuser un produit qui n'est pas `PENDING` | Refus (erreur 409/403 côté API), message équivalent |
| Un utilisateur accède à un produit inexistant (id invalide) | Erreur 404, message **"Produit introuvable"** |
| Un VALIDATOR tente de refuser sans motif | Blocage côté formulaire (champ obligatoire) + revalidation côté serveur |
| Token expiré en cours de session | Redirection automatique vers le login, message **"Votre session a expiré, veuillez vous reconnecter"** |
| Un ADMIN reprend l'édition d'un produit `REJECTED` | Reset automatique à `currentstep = 1`, passage implicite en `DRAFT`, bandeau de refus affiché |

---