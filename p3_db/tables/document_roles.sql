CREATE TABLE IF NOT EXISTS document_roles (
    id SERIAL PRIMARy KEY,
    file_id INTEGER REFERENCES user_file(id) ON DELETE CASCADE,
    role_id INTEGER REFERENCES roles(id) on DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(file_id, role_id)
);