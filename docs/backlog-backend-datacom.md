DATACOM Backend - Backlog de développement (US)
Découpage pensé pour un développement step-by-step avec un commit/push par US (ou par petit groupe d'US cohérent). Chaque US indique les fichiers concernés (déjà présents dans le squelette fourni) et une suggestion de message de commit.

Epic 0 - Setup du projet
US-00.1 : Initialiser le squelette Maven & Configuration de base et connexion PostgreSQL
En tant que développeur, je veux un projet Spring Boot vide et buildable, afin de partir sur une base saine et je veux que l'application se connecte à PostgreSQL via des variables d'environnement, afin de ne jamais committer de credentials en dur (cf. bug de l'existant).
Fichiers : pom.xml, DatacomApplication.java, .gitignore, application.yml
Critère d'acceptation :
mvn clean install passe (sans logique métier encore)
l'app démarre et se connecte à une base locale (via DB_URL/DB_USER/DB_PASSWORD)

Epic 1 - Modèle de données
US-01.1 : Entité User + enum Role & Entité Product + enum ProductStatus
Fichiers : model/entity/User.java, model/enums/Role.java, model/entity/Product.java,  model/enums/ProductStatus.java
Critère d'acceptation :
table users générée par Hibernate au démarrage (ddl-auto: update), avec les colonnes attendues
table products générée avec tous les champs des specs (steps 1 à 3 + workflow)
US-01.2 : Repositories JPA & Données de démo
Fichiers : repository/UserRepository.java, repository/ProductRepository.java, database/seed-data.sql
Critère d'acceptation : findByLogin, findByStatusOrderByIdDesc, findAllByOrderByIdDesc fonctionnels (testables via un test d'intégration simple), un admin et un validator sont chargés, mots de passe en BCrypt (jamais en clair)

Epic 2 - Sécurité et authentification
US-02.1 : Config Spring Security de base + BCrypt
Fichiers : config/SecurityConfig.java, security/JwtTokenProvider.java
Critère d'acceptation : toutes les routes /api/** renvoient 401 sans authentification ; PasswordEncoder disponible en bean, un token généré contient sub (login) et role, expire après la durée configurée, signature vérifiable
US-02.2 : Filtre d'authentification JWT & Endpoint de login
Fichiers : security/JwtAuthenticationFilter.java, security/CustomUserDetailsService.java, dto/request/LoginRequest.java, dto/response/AuthResponse.java, service/AuthService.java, controller/AuthController.java
Critère d'acceptation :
une requête avec Authorization: Bearer <token> valide peuple le SecurityContext avec le rôle correct, extrait du token signé (jamais fait confiance à une valeur client)
Login valide → 200 + token JWT
Login/mot de passe invalide → 401, message générique "Identifiants incorrects" (ne pas préciser lequel des deux est faux)
Test manuel suggéré : curl -X POST localhost:8080/api/auth/login -d '{"login":"admin","password":"admin123"}' -H "Content-Type: application/json"
US-02.3 : CORS pour le frontend Vue
Fichiers : config/CorsConfig.java
Critère d'acceptation : une requête depuis http://localhost:5173 passe sans erreur CORS

Epic 3 - Gestion centralisée des erreurs
US-03.1 : Exceptions métier & Handler global
Fichiers : exception/ResourceNotFoundException.java, exception/ForbiddenTransitionException.java, exception/GlobalExceptionHandler.java
Critère d'acceptation : aucune stack trace ne fuite jamais dans une réponse HTTP, quel que soit le type d'erreur (404, 409, 400, 401, 500)

Epic 4 - Création et édition d'un produit (ADMIN)
US-04.1 : Création d'un produit, DTO et validation du step 1,2,3,4
Fichiers : service/ProductWorkflowService.java (méthode createDraft), endpoint POST /api/products dans ProductController, dto/request/ProductStepRequest.java (partiel), logique applyStepFields case
Critère d'acceptation :
un ADMIN authentifié crée un produit vide en DRAFT, step 1 ; un VALIDATOR reçoit 403
PUT /api/products/{id}/step/1?next=true renseigne name/reference/description et passe currentStep=2
idem sur category/subcategory/manufacturer/country
idem sur lot/certification/validation (commentaire)
PUT .../step/4?next=true passe le statut de DRAFT/REJECTED à PENDING, purge rejectionReason
US-04.2 : Blocage d'édition hors DRAFT/REJECTED
Fichiers : getEditableOrThrow dans ProductWorkflowService
Critère d'acceptation : tenter d'éditer un produit PENDING ou VALIDATED renvoie 409 avec message "Ce produit ne peut pas être modifié dans son état actuel"

Epic 5 - Validation / Refus (VALIDATOR)
US-05.1 : Validation d'un produit, Refus d'un produit avec motif & Reprise en édition après refus
Fichiers : endpoint POST /api/products/{id}/validate, dto/request/ProductRejectRequest.java, endpoint POST /api/products/{id}/reject, logique dans saveStep (reset REJECTED → DRAFT, currentStep=1)
Critère d'acceptation :
uniquement si status=PENDING → passe à VALIDATED ; sinon 409 ; un ADMIN reçoit 403
motif obligatoire, ≥10 caractères, sinon 400 ; passe le statut à REJECTED
rééditer un produit REJECTED le repasse en DRAFT avec currentStep=1, rejectionReason conservé jusqu'à la re-soumission du step 4

Epic 6 - Consultation (liste et détail)
US-06.1 : DTOs, mapper, liste des produits avec filtre statut & détail d'un produit
Fichiers : dto/response/ProductListResponse.java, dto/response/ProductDetailResponse.java, mapper/ProductMapper.java, service/ProductService.java (méthode list), endpoint GET /api/products, endpoint GET /api/products/{id}
Critère d'acceptation :
GET /api/products renvoie tout
GET /api/products?status=PENDING filtre
visible ADMIN et VALIDATOR sans restriction par créateur
Critère d'acceptation : 404 si id inexistant, avec message "Produit introuvable"

Epic 7 - Qualité et déploiement
US-07.1 : Tests unitaires ProductWorkflowService
Couvrir :
transitions valides/invalides, reset après refus, edition croisée ADMIN
401 sans token, 403 mauvais rôle, scénarios nominaux de bout en bout
US-07.2 : Dockerisation
Fichiers : Dockerfile
Critère d'acceptation : docker build produit une image fonctionnelle, lancée avec les bonnes variables d'env
US-07.3 : Décision Flyway
À faire seulement après validation de ta part (cf. point ouvert du README) - remplacer ddl-auto: update par des migrations versionnées avant tout déploiement réel

