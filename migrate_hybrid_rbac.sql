ALTER TABLE users
ADD COLUMN IF NOT EXISTS allowed_projects TEXT[] DEFAULT '{}',
ADD COLUMN IF NOT EXISTS allowed_sensitivity TEXT[] DEFAULT '{}';

CREATE TABLE IF NOT Exists projects (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    department VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO projects (name, department)
SELECT DISTINCT project, department 
FROM user_file
WHERE project IS NOT NULL AND department IS NOT NULL 
ON CONFLICT (name) DO NOTHING;

UPDATE users u 
SET allowed_projects = ARRAY[u.project]
WHERE u.project IS NOT NULL 
AND (u.allowed_projects IS NULL OR array_length(u.allowed_projects, 1) IS NULL);

UPDATE users 
SET allowed_sensitivity = 
    CASE 
        WHEN role = 'ADMIN' THEN
            ARRAY['Public', 'Internal', 'Confidential', 'Highly Confidential']
        WHEN role = 'USER' THEN 
            ARRAY['Public', 'Internal']
        ELSE
            ARRAY['Public']
    END 
WHERE allowed_sensitivity IS NULL OR array_length(allowed_sensitivity, 1) IS NULL;

CREATE INDEX IF NOT EXISTS idx_users_department ON users(department);
CREATE INDEX IF NOT EXISTS idx_users_allowed_projects ON users USING GIN(allowed_projects);
CREATE INDEX IF NOT EXISTS idx_userfile_department ON user_file(department);
CREATE INDEX IF NOT EXISTS idx_userfile_project ON user_file(project);
CREATE INDEX IF NOT EXISTS idx_projects_department ON projects(department);

SELECT 
    email,
    department,
    project,
    allowed_projects,
    allowed_sensitivity
FROM users
LIMIT 5;

SELECT department, COUNT(*) as project_count
FROM projects 
GROUP BY department;