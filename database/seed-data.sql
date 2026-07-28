-- Données de démo pour DATACOM v2
-- Mots de passe hashés en BCrypt (jamais en clair, contrairement à l'existant).
--
-- admin / admin123
-- validator / validator123
--
-- Ces hash ont été générés avec bcrypt (10 rounds). Spring Security's BCryptPasswordEncoder
-- vérifie nativement les préfixes $2a$/$2b$/$2y$, donc compatible tel quel.

INSERT INTO users (login, password, firstname, lastname, role) VALUES
('admin', '$2b$10$T0HLQmESnzejSbnhjKpyDOEfijLGoWa.xFsbWCCKlcIUffY5e.dvq', 'Alice', 'Admin', 'ADMIN'),
('validator', '$2b$10$GiwzmxWYsOXZyNpbX0Q4WuQZ3DA64Evg.XmLvDRlVPMt.bHDeod7m', 'Bob', 'Validator', 'VALIDATOR');
