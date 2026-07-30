# Product Park Datacom - API Back-End

Bienvenue dans le dépôt du back-end de l'application **Product Park Datacom**. Cette application fournit une API RESTful robuste basée sur **Spring Boot**, dédiée à la gestion du catalogue de produits et au suivi d'un workflow complet de création, d'édition par étapes, de validation et de rejet de produits.

---

##  Stack Technique & Technologies

* **Langage :** Java 17+
* **Framework :** Spring Boot 3.x
* **Sécurité :** Spring Security (Authentification JWT & contrôle d'accès basé sur les rôles)
* **Accès aux données :** Spring Data JPA / Hibernate
* **Base de données :** PostgreSQL / H2 (selon le profil de configuration)
* **Outillage & Utilitaires :**
    * Lombok (réduction du code boilerplate)
    * MapStruct (mapping DTO <-> Entités)
    * Bean Validation (Jakarta Validation)
* **Documentation API :** Springdoc OpenAPI / Swagger UI
* **Gestionnaire de dépendances :** Apache Maven

---

##  Fonctionnalités Principales

###  1. Authentification & Sécurité
* **Connexion utilisateur (`/api/auth/login`) :** Authentification sécurisée des utilisateurs et génération de jetons d'accès.
* **Contrôle d'accès par rôle (`@PreAuthorize`) :**
    * `ROLE_ADMIN` : Création et modification d'étapes de produits.
    * `ROLE_VALIDATOR` : Validation et rejet de produits soumis.

###  2. Gestion du Workflow Produit (`/api/products`)
* **Création de brouillon :** Initialisation d'un produit à l'état de brouillon par un administrateur (`POST /api/products`).
* **Gestion par étapes :** Sauvegarde incrémentale des différentes étapes du formulaire produit (`PUT /api/products/{id}/step/{stepNumber}`).
* **Validation de produit :** Validation finale du produit par un rôle validateur (`POST /api/products/{id}/validate`).
* **Rejet de produit :** Rejet motivé d'un produit par un validateur avec motif obligatoire (`POST /api/products/{id}/reject`).
* **Consultation & Filtrage :**
    * Obtenir les détails complets d'un produit (`GET /api/products/{id}`).
    * Lister les produits avec possibilité de filtrage par statut (`GET /api/products?status=DRAFT|PENDING|VALIDATED|REJECTED`).

---

##  Structure du Projet

```text
src/main/java/com/productpark/datacom/
├── config/                  # Configuration Spring Security, Swagger / OpenAPI, Cors...
├── controller/              # Contrôleurs REST (AuthController, ProductController...)
├── dto/                     # Data Transfer Objects
│   ├── request/             # Request DTOs (LoginRequest, ProductStepRequest...)
│   └── response/            # Response DTOs (AuthResponse, ProductDetailResponse...)
├── exception/               # Gestion globale des exceptions (ResourceNotFoundException...)
├── mapper/                  # Mappers DTO <-> Entity (ProductMapper...)
├── model/                   # Modèles du domaine
│   ├── entity/              # Entités JPA (User, Product...)
│   └── enums/               # Énumérations (ProductStatus...)
├── repository/              # Interfaces Spring Data JPA (UserRepository, ProductRepository...)
└── service/                 # Logique métier & Services (AuthService, ProductWorkflowService...)
```

---

##  Installation et Démarrage

### Prérequis
* **JDK 17** ou supérieur installé.
* **Maven 3.8+** installé (ou utilisation du wrapper `./mvnw`).
* Base de données configurée (PostgreSQL ou H2).

### 1. Cloner le projet
```bash
git clone https://github.com/votre-org/product-park-datacom-backend.git
cd product-park-datacom-backend
```

### 2. Configuration (`application.yml` ou `application.properties`)
Assurez-vous de configurer correctement les accès à la base de données et les clés JWT dans le fichier de configuration :

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/productpark_db
    username: postgres
    password: yourpassword
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: your_jwt_secret_key_here
  expiration: 86400000 # 24 heures en ms
```

### 3. Compiler et lancer l'application

#### Avec Maven :
```bash
mvn clean install
mvn spring-boot:run
```

#### Avec le Maven Wrapper :
```bash
./mvnw clean spring-boot:run
```

L'application démarrera par défaut sur le port **`8080`**.

---

##  Documentation de l'API (Swagger UI)

Une fois l'application démarrée, vous pouvez consulter et tester l'API de manière interactive via l'interface Swagger :

*  **Swagger UI :** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
*  **Spécification OpenAPI (JSON) :** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> **Remarque Authentification dans Swagger :** Pour tester les endpoints protégés (`ADMIN` ou `VALIDATOR`), authentifiez-vous d'abord via `/api/auth/login`, récupérez le token Bearer puis cliquez sur le bouton **Authorize** en haut à droite dans Swagger UI pour y insérer votre token.

---

##  Tests

Pour exécuter la suite de tests unitaires et d'intégration :

```bash
mvn test
```

---

##  Licence
Ce projet est privé et réservé à l'usage exclusif de Product Park Datacom.