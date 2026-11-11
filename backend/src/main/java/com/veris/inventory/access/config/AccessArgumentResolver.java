package com.veris.inventory.access.config;

import com.veris.inventory.access.services.AccessGuard;
import com.veris.inventory.identity.application.LocalProfiles;
import com.veris.inventory.identity.infrastructure.SessionGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Resuelve {@code @CurrentAccess AccessContext} desde el perfil del request. */
public class AccessArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentAccess.class)
                && AccessGuard.AccessContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var request = webRequest.getNativeRequest(HttpServletRequest.class);
        var profile = request == null ? null
                : (LocalProfiles.Profile) request.getAttribute(SessionGuard.PROFILE);
        return new AccessGuard.AccessContext(profile);
    }
}
