-- Тестовые пользователи. Хранятся только bcrypt-хэши паролей (cost 12).
-- alice / Str0ng-Passw0rd, bob / Bob-Passw0rd-123
INSERT INTO users (username, password_hash, role, created_at) VALUES
    ('alice', '$2a$12$NIHNkMA9zGheqGChz3gvE.N9A2DrG9yX.kXQUW4Abxvlx8ItuKyCO', 'USER', CURRENT_TIMESTAMP),
    ('bob',   '$2a$12$KJ/7NRpE1MreDzaYvi/CVOMrtvZq9vW2sGS2hLal0.vqzOIVt9uoe', 'USER', CURRENT_TIMESTAMP);
