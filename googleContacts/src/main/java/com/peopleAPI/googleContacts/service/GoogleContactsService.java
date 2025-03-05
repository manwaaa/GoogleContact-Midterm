package com.peopleAPI.googleContacts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.people.v1.model.*;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class GoogleContactsService {
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public GoogleContactsService() throws GeneralSecurityException, IOException {

    }

    public PeopleService getPeopleService(Credential credential) throws GeneralSecurityException, IOException {
        return new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("midterms")
                .build();
    }
    // GET CONTACTS
    public String getAllContacts(Credential accessToken) throws GeneralSecurityException, IOException {
        PeopleService peopleService = getPeopleService(accessToken);
        ListConnectionsResponse response = peopleService.people().connections()
                .list("people/me")
                .setPersonFields("names,emailAddresses,phoneNumbers")
                .setPageSize(10)
                .execute();
        List<Map<String, Object>> contacts = response.getConnections().stream()
                .map(person -> {
                    String name = (person.getNames() != null && !person.getNames().isEmpty()) ?
                            person.getNames().get(0).getDisplayName() : "No Name";
                    String email = (person.getEmailAddresses() != null && !person.getEmailAddresses().isEmpty()) ?
                            person.getEmailAddresses().get(0).getValue() : "No Email";
                    String phone = (person.getPhoneNumbers() != null && !person.getPhoneNumbers().isEmpty()) ?
                            person.getPhoneNumbers().get(0).getValue() : "No Phone";
                    String resourceName = (person.getResourceName() != null && !person.getResourceName().isEmpty()) ?
                            person.getResourceName() : "No Resource Name";
                    return Map.<String, Object>of("name", name, "email", email, "phone", phone, "resourceName", resourceName);
                }).collect(Collectors.toList());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(contacts);
    }

    // CREATE CONTACTS
    public Person createContact(Credential accessToken, String name, String email, String phone) throws GeneralSecurityException, IOException {
        PeopleService peopleService = getPeopleService(accessToken);
        Person newContact = new Person();

        String firstName = name;
        String lastName = "";
        if(name.contains(" ")) {
            String[] names = name.split(" ");
            firstName = names[0];
            lastName = names[1];
        }

        Name contactName = new Name()
                .setGivenName(firstName)
                .setFamilyName(lastName);
        newContact.setNames(Collections.singletonList(contactName));

        if(phone != null && !phone.isEmpty()) {
            PhoneNumber phoneNumber = new PhoneNumber()
                    .setValue(phone);
            newContact.setPhoneNumbers(Collections.singletonList(phoneNumber));
        }
        if(email != null && !email.isEmpty()) {
            EmailAddress emailAddress = new EmailAddress().setValue(email);
            newContact.setEmailAddresses(Collections.singletonList(emailAddress));
        }

        return peopleService.people().createContact(newContact).execute();
    }

    // DELETE CONTACTS
    public void deleteContact(Credential accessToken, String resourceName) throws GeneralSecurityException, IOException {
        PeopleService peopleService = getPeopleService(accessToken);
        peopleService.people().deleteContact(resourceName).execute();
    }

    // UPDATE CONTACTS
    public Person updateContact(Credential accessToken, String resourceName, String name, String email, String phone) throws GeneralSecurityException, IOException {
        PeopleService peopleService = getPeopleService(accessToken);
        Person existingContact = peopleService.people().get(resourceName).setPersonFields("names,emailAddresses,phoneNumbers,metadata").execute();
        String etag = existingContact.getEtag();
        Person updateContact = new Person().setEtag(etag);
        String firstName = name;
        String lastName = "";
        if (name.contains(" ")) {
            String[] names = name.split(" ");
            firstName = names[0];
            lastName = names[1];
        }
        List<String> updateFields = new ArrayList<>();
        Name contactName = new Name()
                .setGivenName(firstName)
                .setFamilyName(lastName);
        if(!firstName.isEmpty() || !lastName.isEmpty() ) {
            updateContact.setNames(Collections.singletonList(new Name().setGivenName(firstName).setFamilyName(lastName)));
            updateFields.add("names");
        }

        if (phone != null && !phone.isEmpty()) {
            updateContact.setPhoneNumbers(Collections.singletonList(new PhoneNumber().setValue(phone)));
            updateFields.add("phoneNumbers");
        }
        if (email != null && !email.isEmpty()) {
            updateContact.setEmailAddresses(Collections.singletonList(new EmailAddress().setValue(email)));
            updateFields.add("emailAddresses");
        }
        String updateMask = String.join(",", updateFields);
        if(updateMask.isEmpty()){
            throw new IllegalArgumentException("Update mask is empty");
        }
        return peopleService.people().updateContact(resourceName, updateContact).setUpdatePersonFields(updateMask).execute();
    }

}