DATACOM Backend — Backlog de développement (US)
Découpage pensé pour un développement step-by-step avec un commit/push par US (ou par petit groupe d'US cohérent). Chaque US indique les fichiers concernés (déjà présents dans le squelette fourni) et une suggestion de message de commit.
Note : le code existe déjà dans le zip fourni précédemment. Ce backlog te permet soit de le committer progressivement en le "rejouant" dans cet ordre, soit de t'en servir de plan pour les prochaines évolutions (tests, ajustements, nouvelles features).

Epic 0 — Setup du projet
US-00.1 : Initialiser le squelette Maven
En tant que développeur, je veux un projet Spring Boot vide et buildable, afin de partir sur une base saine.
Fichiers : pom.xml, DatacomApplication.java, .gitignore
Critère d'acceptation : mvn clean install passe (sans logique métier encore)
Commit : chore: init spring boot project skeleton (java 17, spring boot 3.3.4)
US-00.2 : Configuration de base et connexion PostgreSQL
En tant que développeur, je veux que l'application se connecte à PostgreSQL via des variables d'environnement, afin de ne jamais committer de credentials en dur (cf. bug de l'existant).
Fichiers : application.yml
Critère d'acceptation : l'app démarre et se connecte à une base locale (via DB_URL/DB_USER/DB_PASSWORD)
Commit : chore: configure datasource via environment variables

Epic 1 — Modèle de données
US-01.1 : Entité User + enum Role
Fichiers : model/entity/User.java, model/enums/Role.java
Critère d'acceptation : table users générée par Hibernate au démarrage (ddl-auto: update), avec les colonnes attendues
Commit : feat: add User entity and Role enum
US-01.2 : Entité Product + enum ProductStatus
Fichiers : model/entity/Product.java, model/enums/ProductStatus.java
Critère d'acceptation : table products générée avec tous les champs des specs (steps 1 à 3 + workflow)
Commit : feat: add Product entity and ProductStatus enum
US-01.3 : Repositories JPA
Fichiers : repository/UserRepository.java, repository/ProductRepository.java
Critère d'acceptation : findByLogin, findByStatusOrderByIdDesc, findAllByOrderByIdDesc fonctionnels (testables via un test d'intégration simple)
Commit : feat: add JPA repositories for User and Product
US-01.4 : Données de démo
Fichiers : database/seed-data.sql
Critère d'acceptation : un admin et un validator sont chargés, mots de passe en BCrypt (jamais en clair)
Commit : chore: add seed data with bcrypt-hashed passwords

Epic 2 — Sécurité et authentification (le cœur des correctifs de sécurité)
US-02.1 : Config Spring Security de base + BCrypt
Fichiers : config/SecurityConfig.java
Critère d'acceptation : toutes les routes /api/** renvoient 401 sans authentification ; PasswordEncoder disponible en bean
Commit : feat(security): base Spring Security config with BCrypt
US-02.2 : Génération et validation de JWT
Fichiers : security/JwtTokenProvider.java
Critère d'acceptation : un token généré contient sub (login) et role, expire après la durée configurée, signature vérifiable
Commit : feat(security): JWT token provider (generate/validate)
US-02.3 : Filtre d'authentification JWT
Fichiers : security/JwtAuthenticationFilter.java, security/CustomUserDetailsService.java
Critère d'acceptation : une requête avec Authorization: Bearer <token> valide peuple le SecurityContext avec le rôle correct, extrait du token signé (jamais fait confiance à une valeur client)
Commit : feat(security): JWT authentication filter
US-02.4 : Endpoint de login
Fichiers : dto/request/LoginRequest.java, dto/response/AuthResponse.java, service/AuthService.java, controller/AuthController.java
Critère d'acceptation :
Login valide → 200 + token JWT
Login/mot de passe invalide → 401, message générique "Identifiants incorrects" (ne pas préciser lequel des deux est faux)
Commit : feat(auth): login endpoint returning JWT
Test manuel suggéré : curl -X POST localhost:8080/api/auth/login -d '{"login":"admin","password":"admin123"}' -H "Content-Type: application/json"
US-02.5 : CORS pour le frontend Vue
Fichiers : config/CorsConfig.java
Critère d'acceptation : une requête depuis http://localhost:5173 passe sans erreur CORS
Commit : feat(security): configure CORS for Vue frontend

Epic 3 — Gestion centralisée des erreurs
US-03.1 : Exceptions métier
Fichiers : exception/ResourceNotFoundException.java, exception/ForbiddenTransitionException.java
Commit : feat(error-handling): add business exceptions
US-03.2 : Handler global
Fichiers : exception/GlobalExceptionHandler.java
Critère d'acceptation : aucune stack trace ne fuite jamais dans une réponse HTTP, quel que soit le type d'erreur (404, 409, 400, 401, 500)
Commit : feat(error-handling): global exception handler with uniform error responses

Epic 4 — Création et édition d'un produit (ADMIN)
US-04.1 : Création d'un produit (statut DRAFT)
Fichiers : service/ProductWorkflowService.java (méthode createDraft), endpoint POST /api/products dans ProductController
Critère d'acceptation : un ADMIN authentifié crée un produit vide en DRAFT, step 1 ; un VALIDATOR reçoit 403
Commit : feat(product): create empty draft product (admin only)
US-04.2 : DTO et validation du step 1
Fichiers : dto/request/ProductStepRequest.java (partiel), logique applyStepFields case 1
Critère d'acceptation : PUT /api/products/{id}/step/1?next=true renseigne name/reference/description et passe currentStep=2
Commit : feat(product): step 1 (general info) save + advance
US-04.3 : Step 2 (classification)
Critère d'acceptation : idem sur category/subcategory/manufacturer/country
Commit : feat(product): step 2 (classification) save + advance
US-04.4 : Step 3 (conformité)
Critère d'acceptation : idem sur lot/certification/validation (commentaire)
Commit : feat(product): step 3 (compliance) save + advance
US-04.5 : Step 4 (soumission finale)
Critère d'acceptation : PUT .../step/4?next=true passe le statut de DRAFT/REJECTED à PENDING, purge rejectionReason
Commit : feat(product): step 4 submission → status PENDING
US-04.6 : Blocage d'édition hors DRAFT/REJECTED
Fichiers : getEditableOrThrow dans ProductWorkflowService
Critère d'acceptation : tenter d'éditer un produit PENDING ou VALIDATED renvoie 409 avec message "Ce produit ne peut pas être modifié dans son état actuel"
Commit : feat(product): forbid editing products not in DRAFT/REJECTED

Epic 5 — Validation / Refus (VALIDATOR)
US-05.1 : Validation d'un produit
Fichiers : endpoint POST /api/products/{id}/validate
Critère d'acceptation : uniquement si status=PENDING → passe à VALIDATED ; sinon 409 ; un ADMIN reçoit 403
Commit : feat(product): validate endpoint (validator only)
US-05.2 : Refus d'un produit avec motif
Fichiers : dto/request/ProductRejectRequest.java, endpoint POST /api/products/{id}/reject
Critère d'acceptation : motif obligatoire, ≥10 caractères, sinon 400 ; passe le statut à REJECTED
Commit : feat(product): reject endpoint with mandatory rejectionReason (validator only)
US-05.3 : Reprise en édition après refus
Fichiers : logique dans saveStep (reset REJECTED → DRAFT, currentStep=1)
Critère d'acceptation : rééditer un produit REJECTED le repasse en DRAFT avec currentStep=1, rejectionReason conservé jusqu'à la re-soumission du step 4
Commit : feat(product): reset to step 1 when resubmitting a rejected product

Epic 6 — Consultation (liste et détail)
US-06.1 : DTOs et mapper
Fichiers : dto/response/ProductListResponse.java, dto/response/ProductDetailResponse.java, mapper/ProductMapper.java
Commit : feat(product): response DTOs and mapper
US-06.2 : Liste des produits avec filtre statut
Fichiers : service/ProductService.java (méthode list), endpoint GET /api/products
Critère d'acceptation : GET /api/products renvoie tout ; GET /api/products?status=PENDING filtre ; visible ADMIN et VALIDATOR sans restriction par créateur
Commit : feat(product): list endpoint with optional status filter
US-06.3 : Détail d'un produit
Fichiers : endpoint GET /api/products/{id}
Critère d'acceptation : 404 si id inexistant, avec message "Produit introuvable"
Commit : feat(product): detail endpoint

Epic 7 — Qualité et déploiement (pas encore traité, à faire ensuite)
US-07.1 : Tests unitaires ProductWorkflowService
Couvrir : transitions valides/invalides, reset après refus, edition croisée ADMIN
Commit : test(product): unit tests for ProductWorkflowService
US-07.2 : Tests d'intégration contrôleurs (@SpringBootTest + MockMvc)
Couvrir : 401 sans token, 403 mauvais rôle, scénarios nominaux de bout en bout
Commit : test(product): controller integration tests
US-07.3 : Dockerisation
Fichiers : Dockerfile
Critère d'acceptation : docker build produit une image fonctionnelle, lancée avec les bonnes variables d'env
Commit : chore: add multi-stage Dockerfile
US-07.4 : Décision Flyway
À faire seulement après validation de ta part (cf. point ouvert du README) — remplacer ddl-auto: update par des migrations versionnées avant tout déploiement réel

