package com.veris.inventory.identity.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Only fixed event codes and local UUIDs; never claims, URLs, exceptions or credentials. */
public final class SecurityEvents {
    private static final Logger LOG = LoggerFactory.getLogger("security.audit");
    private SecurityEvents() {}
    public static void record(String event, String actor) {
        LOG.atInfo().addKeyValue("security_event", event).addKeyValue("local_actor", actor).log("Security event");
    }
    public static void problem(jakarta.servlet.http.HttpServletResponse response, int status, String code)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"status\":" + status + ",\"code\":\"" + code + "\"}");
    }
}
