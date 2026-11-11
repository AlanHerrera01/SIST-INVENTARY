package com.veris.inventory.identity.infrastructure;

import java.time.Instant;
import java.util.Map;
import com.veris.inventory.config.IdentityProperties;
import com.veris.inventory.config.SecurityProperties;
import com.veris.inventory.identity.application.LocalProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration
public class SecurityConfiguration {
    @Bean ClientRegistrationRepository clients(IdentityProperties identity,
            SecurityProperties security) {
        var issuer = identity.issuerUri().toString();
        var internal = security.oidcInternalUri();
        var publicUrl = security.publicUrl();
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("inventory")
                .clientId("inventory-bff").clientSecret(security.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(publicUrl + "/login/oauth2/code/inventory")
                .scope("openid", "profile", "email", "roles")
                .authorizationUri(issuer + "/protocol/openid-connect/auth")
                .tokenUri(internal + "/protocol/openid-connect/token")
                .jwkSetUri(internal + "/protocol/openid-connect/certs")
                .issuerUri(issuer)
                .providerConfigurationMetadata(Map.of(
                        "end_session_endpoint", issuer + "/protocol/openid-connect/logout",
                        "user_info_endpoint", internal + "/protocol/openid-connect/userinfo"))
                .userNameAttributeName("sub").clientName("Inventario").build());
    }
    @Bean OAuth2AuthorizedClientRepository authorizedClients() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }
    @Bean org.springframework.security.web.context.SecurityContextRepository securityContextRepository() {
        return new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
    }
    @Bean SecurityFilterChain security(HttpSecurity http, ClientRegistrationRepository clients,
            OAuth2AuthorizedClientRepository authorizedClients, LocalProfiles profiles,
            SecurityProperties security, IdentityProperties identity,
            org.springframework.security.web.context.SecurityContextRepository securityContextRepository)
            throws Exception {
        var publicUrl = security.publicUrl();
        var issuer = identity.issuerUri().toString();
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(clients, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce()
                .andThen(builder -> builder.additionalParameters(params -> params.put("prompt", "login"))));
        var oidc = new OidcUserService();
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/api/auth/csrf", "/api/auth/login",
                        "/oauth2/authorization/inventory",
                        "/login/oauth2/code/inventory", "/error").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll());
        http.requestCache(cache -> cache.requestCache(new NullRequestCache()));
        http.exceptionHandling(errors -> errors
                .authenticationEntryPoint((req, res, ex) -> SecurityEvents.problem(res, 401, "AUTHENTICATION_REQUIRED"))
                .accessDeniedHandler((req, res, ex) -> {
                    SecurityEvents.record("ACCESS_DENIED", "unknown");
                    SecurityEvents.problem(res, 403, "ACCESS_DENIED");
                }));
        http.securityContext(context -> context.securityContextRepository(securityContextRepository));
        http.csrf(Customizer.withDefaults());
        http.sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()));
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                .referrerPolicy(referrer -> referrer.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)));
        http.oauth2Login(login -> login
                .authorizedClientRepository(authorizedClients)
                .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                .userInfoEndpoint(info -> info.oidcUserService(request -> {
                    var user = oidc.loadUser(request);
                    profiles.require(user);
                    return user;
                }))
                .successHandler((req, res, authentication) -> {
                    req.getSession().setAttribute(SessionGuard.START, Instant.now());
                    SecurityEvents.record("LOGIN_SUCCESS", profiles.require(
                            (org.springframework.security.oauth2.core.oidc.user.OidcUser) authentication.getPrincipal()).id());
                    res.sendRedirect(publicUrl + "/");
                })
                .failureHandler((req, res, exception) -> {
                    var session = req.getSession(false);
                    if (session != null) session.invalidate();
                    SecurityEvents.record("LOGIN_FAILURE", "unknown");
                    res.sendRedirect(publicUrl + "/?auth=failed");
                }));
        http.logout(logout -> logout.logoutUrl("/api/auth/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> {
                    SecurityEvents.record("LOGOUT", "unknown");
                    // Do not send id_token_hint through the browser. Keycloak asks for confirmation.
                    var destination = org.springframework.web.util.UriComponentsBuilder
                            .fromUriString(issuer + "/protocol/openid-connect/logout")
                            .queryParam("client_id", "inventory-bff")
                            .queryParam("post_logout_redirect_uri", publicUrl + "/").build().encode().toUriString();
                    res.setContentType("application/json");
                    res.setHeader("Cache-Control", "no-store");
                    res.getWriter().write("{\"logoutUrl\":\"" + destination + "\"}");
                }));
        http.oidcLogout(logout -> logout.backChannel(Customizer.withDefaults()));
        http.addFilterBefore(new SessionGuard(profiles, security.absoluteSessionTimeout()),
                AuthorizationFilter.class);
        return http.build();
    }
}
