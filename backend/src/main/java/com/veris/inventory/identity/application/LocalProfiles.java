package com.veris.inventory.identity.application;

import java.util.List;
import java.util.Map;
import com.veris.inventory.config.SecurityProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

/** Identity is issuer + subject within the configured organization, never email. */
@Service
public class LocalProfiles {
    public record Grant(String role, String scope, String centerId, String permission) {}
    public record Profile(String id, String organizationId, String displayName, List<Grant> grants) {
        public boolean administrator() { return grants.stream().anyMatch(g -> g.role().equals("ADMINISTRADOR")); }
        public boolean allows(String permission, String centerId) {
            return grants.stream().anyMatch(g -> permission.equals(g.permission()) &&
                    (g.scope().equals("GLOBAL") || (centerId != null && centerId.equals(g.centerId()))));
        }
    }
    private final JdbcTemplate jdbc;
    private final String organization;
    public LocalProfiles(JdbcTemplate jdbc, SecurityProperties security) {
        this.jdbc = jdbc;
        this.organization = java.util.UUID.fromString(security.organizationId()).toString();
    }
    public Profile require(OidcUser identity) {
        var profiles = jdbc.query("""
            SELECT BIN_TO_UUID(id,1), display_name FROM app_user
            WHERE organization_id=UUID_TO_BIN(?,1) AND identity_issuer=?
              AND identity_subject=? AND user_state='ACTIVO'
            """, (rs, n) -> new Profile(rs.getString(1), organization, rs.getString(2), List.of()),
                organization, identity.getIssuer().toString(), identity.getSubject());
        if (profiles.size() != 1) throw new OAuth2AuthenticationException("local_profile_unavailable");
        var user = profiles.getFirst();
        var grants = jdbc.query("""
            SELECT r.code, a.scope_kind, BIN_TO_UUID(c.center_id,1), p.code
            FROM user_role_assignment a JOIN app_role r ON r.id=a.role_id AND r.organization_id=a.organization_id
            LEFT JOIN user_role_assignment_center c ON c.role_assignment_id=a.id AND c.organization_id=a.organization_id
            LEFT JOIN role_permission rp ON rp.role_id=r.id AND rp.organization_id=r.organization_id
            LEFT JOIN permission p ON p.id=rp.permission_id
            WHERE a.organization_id=UUID_TO_BIN(?,1) AND a.user_id=UUID_TO_BIN(?,1)
              AND a.revoked_at IS NULL AND a.valid_from<=UTC_TIMESTAMP(6)
              AND (a.valid_until IS NULL OR a.valid_until>UTC_TIMESTAMP(6))
            """, (rs,n) -> new Grant(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                organization, user.id());
        var profile = new Profile(user.id(), organization, user.displayName(), List.copyOf(grants));
        // A local administrator must also pass the realm's mandatory administrator OTP flow.
        if (profile.administrator()) {
            Object realm = identity.getClaim("realm_access");
            if (!(realm instanceof Map<?,?> access) || !(access.get("roles") instanceof List<?> roles)
                    || !roles.contains("ADMINISTRADOR"))
                throw new OAuth2AuthenticationException("administrator_mfa_policy_required");
        }
        return profile;
    }
}
