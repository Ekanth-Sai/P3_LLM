CREATE OR REPLACE FUNCTION get_child_roles(p_role_id BIGINT)
RETURNS TABLE(id BIGINT, role_name VARCHAR, parent_role_id BIGINT, display_name VARCHAR, description TEXT, allowed_sensitivity TEXT[]) AS $$
BEGIN
    RETURN QUERY
    WITH RECURSIVE child_roles_cte AS (
        -- Direct child roles
        SELECT r.id, r.role_name, r.parent_role_id, r.display_name, r.description, r.allowed_sensitivity
        FROM roles r
        WHERE r.parent_role_id = p_role_id
        UNION ALL
        -- Recursive step for child roles
        SELECT r.id, r.role_name, r.parent_role_id, r.display_name, r.description, r.allowed_sensitivity
        FROM roles r
        JOIN child_roles_cte crc ON r.parent_role_id = crc.id
    )
    -- Select all child roles from the CTE
    SELECT crc.id, crc.role_name, crc.parent_role_id, crc.display_name, crc.description, crc.allowed_sensitivity FROM child_roles_cte crc
    UNION ALL
    -- and all sub_roles for the given role and all its children
    SELECT sr.id, sr.sub_role_name, sr.parent_role_id, sr.display_name, sr.description, sr.allowed_sensitivity
    FROM sub_roles sr
    WHERE sr.parent_role_id IN (SELECT id FROM child_roles_cte UNION SELECT p_role_id);
END;
$$ LANGUAGE plpgsql;
