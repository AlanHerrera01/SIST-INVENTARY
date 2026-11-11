package com.veris.inventory.identity.application;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.veris.inventory.common.error.ApiException;
import com.veris.inventory.config.SecurityProperties;
import com.veris.inventory.identity.infrastructure.SecurityEvents;
import com.veris.inventory.identity.infrastructure.SessionGuard;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Inicio de sesión con credenciales en el frontend: el backend canjea
 * usuario/clave contra el token endpoint de Keycloak (password grant del
 * cliente confidencial) y materializa la misma sesión OIDC que produce el
 * flujo authorization code. Keycloak sigue siendo quien valida; el navegador
 * nunca ve la página de login del proveedor.
 */
@Service
public class PasswordLoginService {

    private static final String REGISTRATION_ID = "inventory";

    private final ClientRegistrationRepository registrations;
    private final OAuth2AuthorizedClientRepository authorizedClients;
    private final LocalProfiles profiles;
    private final SecurityProperties security;
    private final org.springframework.security.web.context.SecurityContextRepository securityContextRepository;
    private final RestClient http;

    public PasswordLoginService(ClientRegistrationRepository registrations,
                                OAuth2AuthorizedClientRepository authorizedClients,
                                LocalProfiles profiles,
                                SecurityProperties security,
                                org.springframework.security.web.context.SecurityContextRepository securityContextRepository) {
        this.registrations = registrations;
        this.authorizedClients = authorizedClients;
        this.profiles = profiles;
        this.security = security;
        this.securityContextRepository = securityContextRepository;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * Valida las credenciales contra Keycloak, crea el cliente autorizado en la
     * sesión y deja el {@link SecurityContextHolder} autenticado con el
     * {@link OidcUser} como en el flujo OIDC.
     */
    public void login(String username, String password,
                      HttpServletRequest request, HttpServletResponse response) {
        var registration = registrations.findByRegistrationId(REGISTRATION_ID);
        if (registration == null) {
            throw ApiException.unavailable("IDENTITY_PROVIDER_UNAVAILABLE", "Cliente OIDC no configurado.");
        }
        var tokens = exchangeCredentials(registration, username, password);
        var now = Instant.now();
        var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                tokens.accessToken(), now, now.plusSeconds(tokens.expiresIn()));
        var user = toOidcUser(tokens, now);
        var profile = profiles.require(user); // perfil local + política MFA admin
        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authorizedClients.saveAuthorizedClient(
                new OAuth2AuthorizedClient(registration, user.getName(), accessToken, null),
                authentication, request, response);
        request.getSession().setAttribute(SessionGuard.START, Instant.now());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        SecurityEvents.record("LOGIN_SUCCESS", profile.id());
    }

    private TokenSet exchangeCredentials(ClientRegistration registration, String username, String password) {
        var tokenUri = java.net.URI.create(security.oidcInternalUri().replaceAll("/$", ""))
                + "/protocol/openid-connect/token";
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);
        form.add("client_id", registration.getClientId());
        form.add("client_secret", security.clientSecret());
        form.add("scope", "openid profile email roles");
        try {
            var response = http.post().uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                    .retrieve().body(TokenSet.class);
            if (response == null || response.accessToken() == null) {
                throw new ApiException(401, "AUTHENTICATION_FAILED", "Credenciales inválidas.");
            }
            return response;
        } catch (RestClientResponseException denied) {
            if (denied.getStatusCode().value() == 400 || denied.getStatusCode().value() == 401) {
                SecurityEvents.record("LOGIN_FAILURE", "unknown");
                throw new ApiException(401, "AUTHENTICATION_FAILED", "Credenciales inválidas.");
            }
            throw ApiException.unavailable("IDENTITY_PROVIDER_UNAVAILABLE", "Proveedor de identidad no disponible.");
        } catch (org.springframework.web.client.RestClientException failure) {
            throw ApiException.unavailable("IDENTITY_PROVIDER_UNAVAILABLE", "Proveedor de identidad no disponible.");
        }
    }

    private record TokenSet(@JsonProperty("access_token") String accessToken,
                            @JsonProperty("refresh_token") String refreshToken,
                            @JsonProperty("id_token") String idToken,
                            @JsonProperty("expires_in") long expiresIn) {}

    /**
     * Construye el {@link OidcUser} fusionando los claims del access token y del
     * id_token: Keycloak solo expone {@code realm_access} en el access token,
     * mientras que el perfil local lo necesita para la política de administradores.
     */
    private OidcUser toOidcUser(TokenSet tokens, Instant now) {
        var claims = decodeClaims(tokens.accessToken());
        claims.putAll(decodeClaims(tokens.idToken()));
        var idToken = new org.springframework.security.oauth2.core.oidc.OidcIdToken(
                tokens.idToken(), now, now.plusSeconds(tokens.expiresIn()), claims);
        try {
            return new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(
                    java.util.List.of(), idToken,
                    (org.springframework.security.oauth2.core.oidc.OidcUserInfo) null);
        } catch (RuntimeException failure) {
            SecurityEvents.record("LOGIN_FAILURE", "unknown");
            throw new ApiException(401, "AUTHENTICATION_FAILED", "Credenciales inválidas.");
        }
    }

    /** Decodifica el payload JWT sin verificar firma: el token ya viene del proveedor por TLS. */
    private static java.util.Map<String, Object> decodeClaims(String token) {
        try {
            if (token == null) return new java.util.HashMap<>();
            var parts = token.split("\\.");
            var payload = parts.length > 1 ? parts[1] : "";
            payload = payload.replace('-', '+').replace('_', '/');
            while (payload.length() % 4 != 0) payload += "=";
            @SuppressWarnings("unchecked")
            var claims = new tools.jackson.databind.ObjectMapper()
                    .readValue(java.util.Base64.getDecoder().decode(payload), java.util.Map.class);
            return new java.util.HashMap<>(claims);
        } catch (Exception failure) {
            return new java.util.HashMap<>();
        }
    }
}
