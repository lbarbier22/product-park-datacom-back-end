# DATACOM — Architecture détaillée (v2)

Stack validée : **Vue.js (SPA) + Spring Boot (API REST) + PostgreSQL + JWT**, backend et frontend séparés.

---

## 1. Architecture backend (Spring Boot)

### 1.1 Structure des packages

```
com.productpark.datacom
│
├── DatacomApplication.java          // point d'entrée Spring Boot
│
├── config/
│   ├── SecurityConfig.java          // config Spring Security (routes publiques/protégées, filtre JWT)
│   ├── CorsConfig.java              // autoriser les appels depuis le frontend Vue
│   └── OpenApiConfig.java           // (optionnel) doc Swagger/OpenAPI
│
├── security/
│   ├── JwtTokenProvider.java        // génération / validation des tokens JWT
│   ├── JwtAuthenticationFilter.java // filtre qui lit le token à chaque requête
│   └── CustomUserDetailsService.java
│
├── controller/                      // couche API (REST), pas de logique métier ici
│   ├── AuthController.java          // /api/auth/**
│   └── ProductController.java       // /api/products/**
│
├── service/                          // logique métier
│   ├── AuthService.java
│   ├── ProductService.java
│   └── ProductWorkflowService.java   // logique spécifique des steps (1→4) et transitions de statut
│
├── repository/                       // accès données (Spring Data JPA)
│   ├── UserRepository.java
│   └── ProductRepository.java
│
├── model/
│   ├── entity/
│   │   ├── User.java
│   │   └── Product.java
│   └── enums/
│       ├── Role.java                 // ADMIN, VALIDATOR
│       ├── ProductStatus.java        // DRAFT, PENDING, VALIDATED, REJECTED
│       └── ProductStep.java          // 1 à 4
│
├── dto/                              // objets exposés à l'API (jamais les entités directement)
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── ProductStepRequest.java   // un DTO par step, pour valider precisément les champs attendus
│   │   └── ProductRejectRequest.java // contient rejectionReason (obligatoire)
│   └── response/
│       ├── AuthResponse.java
│       ├── ProductListResponse.java
│       └── ProductDetailResponse.java // inclut rejectionReason si status=REJECTED, pour affichage côté ADMIN
│
├── mapper/                           // conversion entité <-> DTO (ex: MapStruct ou mapping manuel)
│   └── ProductMapper.java
│
└── exception/
    ├── GlobalExceptionHandler.java   // @ControllerAdvice, réponses d'erreur uniformes
    ├── ResourceNotFoundException.java
    └── ForbiddenTransitionException.java  // ex: le VALIDATOR tente de valider/refuser un produit qui n'est pas au statut PENDING
```

**Pourquoi ce découpage règle plusieurs bugs de l'existant :**
- Les DTOs empêchent d'exposer les champs sensibles (mot de passe) et empêchent le client de forcer des champs qu'il ne devrait pas contrôler (ex: `status`, `id`, `createdby`).
- `ProductWorkflowService` centralise la logique des steps : plus possible de "sauter" un step ou de forcer un step invalide depuis le front, car c'est vérifié côté serveur avant chaque transition.
- `Role.java` en enum + Spring Security (`@PreAuthorize`) : le contrôle de rôle devient systématique, plus un oubli comme dans l'existant.

### 1.2 Sécurité — points clés à valider avec toi

- Mot de passe hashé avec **BCrypt** (`PasswordEncoder` Spring Security).
- Chaque endpoint sensible protégé par annotation, ex :
    - `@PreAuthorize("hasRole('ADMIN')")` sur la création de fiche
    - `@PreAuthorize("hasRole('VALIDATOR')")` sur la validation
- Le token JWT stocke `sub` (login) + `role`, signé côté serveur (secret ou clé RSA — à décider).
- **Stockage du token côté Vue : DÉCIDÉ → localStorage.**
    - Le token JWT est retourné dans le corps de la réponse JSON de `POST /api/auth/login`.
    - Le frontend l'injecte dans le header `Authorization: Bearer <token>` via un intercepteur axios, à chaque requête.
    - Risque assumé : vol du token en cas de faille XSS. Ça renforce l'importance de bien échapper tout contenu utilisateur affiché (cf. bug XSS n°7 de l'existant, à corriger impérativement — c'est maintenant la principale ligne de défense).
    - Pas de cookie, donc pas de configuration CORS `allowCredentials` particulière, ni d'endpoint `/api/auth/me` nécessaire (le rôle peut être décodé depuis le JWT côté client, ex: librairie `jwt-decode`, à valider quand même côté serveur à chaque requête bien sûr).

---

## 2. Architecture frontend (Vue.js)

### 2.1 Structure des dossiers

```
src/
├── main.js
├── App.vue
├── router/
│   └── index.js                  // routes + guards (redirection si non connecté / rôle insuffisant)
├── stores/                       // Pinia (state management)
│   ├── auth.store.js              // token, user courant, rôle
│   └── product.store.js
├── services/                      // appels API (axios ou fetch)
│   ├── api.js                     // instance axios configurée (baseURL, intercepteur JWT)
│   ├── auth.service.js
│   └── product.service.js
├── views/
│   ├── LoginView.vue
│   ├── ProductListView.vue
│   ├── ProductFormView.vue        // gère l'affichage des 4 steps (ADMIN uniquement, édition)
│   └── ProductReviewView.vue      // vue lecture seule (VALIDATOR) : récap complet + boutons "Valider" / "Refuser" (avec saisie du motif) si status=PENDING
├── components/
│   ├── product/
│   │   ├── ProductStep1.vue       // Nom, référence, description
│   │   ├── ProductStep2.vue       // Catégorie, sous-catégorie, fabricant, pays
│   │   ├── ProductStep3.vue       // Lot, certification, commentaire validation
│   │   └── ProductStep4.vue       // Récap final avant soumission (ADMIN) : passe le statut à PENDING
│   └── common/
│       ├── AppHeader.vue
│       ├── StepIndicator.vue
│       └── RejectionBanner.vue    // bandeau persistant affichant le rejectionReason, visible sur tous les steps du ProductFormView quand status=REJECTED
└── assets/
```

### 2.2 Points clés

- **Router guards** : vérifient à la fois "connecté ?" et "rôle autorisé ?" avant d'afficher une vue. `ProductFormView` (édition en steps) est accessible **uniquement à l'ADMIN** ; `ProductReviewView` (lecture seule) est accessible **uniquement au VALIDATOR**. Ce routage par rôle reste un confort UX — la vraie protection est côté API (rappel du point de vigilance n°2 de l'existant).
- **Un composant par step** : respecte le fonctionnement "sans retour arrière" en avançant, tout en gardant le code lisible.
- **Store Pinia `product.store.js`** : garde l'état du produit en cours d'édition entre les steps, évite de tout recharger à chaque navigation.

---

## 3. Endpoints API (proposition)

| Méthode | Endpoint | Rôle requis | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public | Authentification, retourne le JWT |
| POST | `/api/auth/logout` | authentifié | Suppression du token côté client (`localStorage.removeItem`), pas d'invalidation serveur par défaut (à revoir si besoin d'une blacklist plus tard) |
| GET | `/api/products` | ADMIN, VALIDATOR | Liste des fiches produits — **visible intégralement par les deux rôles, quel que soit le créateur ou le statut** (l'ADMIN voit les produits créés par n'importe quel ADMIN, pas seulement les siens) |
| GET | `/api/products/{id}` | ADMIN, VALIDATOR | Détail d'une fiche (lecture seule pour le VALIDATOR) |
| POST | `/api/products` | ADMIN | Création d'une fiche (step 1, statut `DRAFT`) |
| PUT | `/api/products/{id}/step/{stepNumber}` | ADMIN | Mise à jour des champs d'un step + passage au suivant. Le dernier step passe le statut de `DRAFT`/`REJECTED` à `PENDING` |
| POST | `/api/products/{id}/validate` | VALIDATOR | Validation finale — autorisé uniquement si `status = PENDING`, passe le statut à `VALIDATED` |
| POST | `/api/products/{id}/reject` | VALIDATOR | Refus — autorisé uniquement si `status = PENDING`, passe le statut à `REJECTED`, nécessite un `rejectionReason` (commentaire obligatoire) |

**Statuts (`ProductStatus`) : 4 valeurs validées** → `DRAFT` (en cours de création par l'ADMIN, steps 1 à 3) → `PENDING` (step 4 terminé, en attente de validation) → `VALIDATED` (validé par le VALIDATOR) **ou** `REJECTED` (refusé par le VALIDATOR avec commentaire, l'ADMIN doit re-soumettre une nouvelle version qui repasse en `PENDING`).

**Règle d'autorisation à retenir pour `ProductWorkflowService`** :
- L'ADMIN peut éditer un produit tant qu'il est en `DRAFT` **ou** `REJECTED` (re-soumission après refus) — **n'importe quel ADMIN peut éditer n'importe quel produit, pas seulement celui qu'il a créé lui-même** (pas de restriction par `createdby`).
- **Lors du passage d'un produit `REJECTED` vers l'édition, `currentstep` est réinitialisé à 1** : l'ADMIN revoit l'intégralité des 4 steps, pas seulement le point ayant motivé le refus.
- Le `rejectionReason` doit rester **visible à chaque step** du formulaire pendant cette re-soumission (bandeau d'information persistant, ex: "Produit refusé — motif : ..."), pour que l'ADMIN garde le contexte pendant toute la ressaisie, même si techniquement il peut modifier n'importe quel champ.
- Le VALIDATOR ne peut **jamais** modifier les champs métier d'un produit (accès lecture seule strict sur ces champs). Il peut uniquement déclencher une transition de statut : `PENDING` → `VALIDATED`, ou `PENDING` → `REJECTED` (avec un commentaire de refus obligatoire).
- Le champ `validation` (commentaire du step 3, hérité de l'existant) est conservé dans le modèle mais **n'est pas utilisé** pour le motif de refus — un nouveau champ dédié (ex: `rejectionReason`) doit être introduit pour ce cas, distinct du champ `validation` existant, pour ne pas mélanger les deux usages.

---