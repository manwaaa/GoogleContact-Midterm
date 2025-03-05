package com.peopleAPI.googleContacts.controller;

import com.peopleAPI.googleContacts.service.GoogleContactsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Controller
public class MainController {
    @Autowired
    private GoogleContactsService googleContactsService;
    @Autowired
    private OAuth2AuthorizedClientService authorizeClientService;
    @Autowired
    private OAuth2AuthorizedClientManager authorizedClientManager;

    public String getToken() {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthorizedClient client = authorizeClientService.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getName());

        if (client == null || client.getAccessToken() == null) {
            throw new RuntimeException("OAuth2 client or access token not found.");
        }

        OAuth2AccessToken accessToken = client.getAccessToken();
        if (accessToken.getExpiresAt().isBefore(Instant.now())) {
            client = authorizedClientManager.authorize(OAuth2AuthorizeRequest.withClientRegistrationId(token.getAuthorizedClientRegistrationId())
                    .principal(token)
                    .build());
            if (client == null) {
                throw new RuntimeException("Failed to refresh the access token.");
            }
            accessToken = client.getAccessToken();
        }

        return accessToken.getTokenValue();
    }

    private Credential getCredential(String token){
        return new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(token);
    }

    @RequestMapping("/")
    public String index() {
        return "index";
    }
    @RequestMapping("/login.html")
    public String login() {
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        // Invalidate the session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear authentication context
        SecurityContextHolder.clearContext();

        // Redirect to login page
        return "redirect:/login.html";
    }
    @GetMapping("/display.html")
    public String display(Model model) throws GeneralSecurityException, IOException {
        Credential credential = getCredential(getToken());
        String jsonResponse = googleContactsService.getAllContacts(credential);

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, String>> contacts = objectMapper.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {});
        model.addAttribute("contacts", contacts);
        return "display";
    }
    @PostMapping("/create-contact")
    public String createContact(String name, String email, String phone) throws GeneralSecurityException, IOException {
        Credential credential = getCredential(getToken());
        googleContactsService.createContact(credential, name, email, phone);
        return "redirect:/display.html";
    }

    @PostMapping("/update-contact")
    public String updateContact(String resourceName, String name, String email, String phone) throws GeneralSecurityException, IOException {
        Credential credential = getCredential(getToken());
        googleContactsService.updateContact(credential, resourceName, name, email, phone);
        return "redirect:/display.html";
    }

    @PostMapping("/delete-contact")
    public String deleteContact(String resourceName) throws GeneralSecurityException, IOException {
        Credential credential = getCredential(getToken());
        googleContactsService.deleteContact(credential, resourceName);
        return "redirect:/display.html";
    }
}