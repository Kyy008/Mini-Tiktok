package com.minitiktok.auth.controller;

import java.net.URI;
import java.net.URISyntaxException;

import com.minitiktok.auth.dto.RegisterRequest;
import com.minitiktok.auth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthPageController {

    private final UserService userService;
    private final RequestCache requestCache;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @ModelAttribute("frontendBaseUrl")
    public String frontendBaseUrl() {
        return frontendBaseUrl;
    }

    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request, HttpServletResponse response) {
        model.addAttribute("continueUrl", resolveContinueUrl(null, request, response));
        return "login";
    }

    @GetMapping("/register")
    public String register(
            @RequestParam(name = "continue", required = false) String continueUrl,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        model.addAttribute("registerRequest", new RegisterRequest());
        model.addAttribute("continueUrl", resolveContinueUrl(continueUrl, request, response));
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute RegisterRequest registerRequest,
            BindingResult bindingResult,
            @RequestParam(name = "continue", required = false) String continueUrl,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        String resolvedContinueUrl = resolveContinueUrl(continueUrl, request, response);
        model.addAttribute("continueUrl", resolvedContinueUrl);
        if (bindingResult.hasErrors()) {
            return "register";
        }

        try {
            userService.register(registerRequest.getUsername(), registerRequest.getPassword());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("registerError", ex.getMessage());
            return "register";
        }

        if (resolvedContinueUrl != null) {
            return "redirect:" + resolvedContinueUrl;
        }
        return "redirect:/login?registered";
    }

    private String resolveContinueUrl(
            String continueUrl,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (isSafeAuthorizationUrl(continueUrl, request)) {
            return continueUrl;
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null && isSafeAuthorizationUrl(savedRequest.getRedirectUrl(), request)) {
            return savedRequest.getRedirectUrl();
        }

        return null;
    }

    private boolean isSafeAuthorizationUrl(String value, HttpServletRequest request) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(value);
            if (!"/oauth2/authorize".equals(uri.getPath())) {
                return false;
            }
            if (!uri.isAbsolute()) {
                return true;
            }
            return sameRequestOrigin(uri, request);
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private boolean sameRequestOrigin(URI uri, HttpServletRequest request) {
        int requestPort = request.getServerPort();
        int uriPort = uri.getPort();
        if (uriPort == -1) {
            uriPort = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        }
        return request.getScheme().equalsIgnoreCase(uri.getScheme())
                && request.getServerName().equalsIgnoreCase(uri.getHost())
                && requestPort == uriPort;
    }
}
