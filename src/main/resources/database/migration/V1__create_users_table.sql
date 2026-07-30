CREATE TABLE users (
                       id        BIGSERIAL PRIMARY KEY,
                       login     VARCHAR(255) NOT NULL UNIQUE,
                       password  VARCHAR(255) NOT NULL,
                       firstname VARCHAR(255) NOT NULL,
                       lastname  VARCHAR(255) NOT NULL,
                       role      VARCHAR(50)  NOT NULL
);