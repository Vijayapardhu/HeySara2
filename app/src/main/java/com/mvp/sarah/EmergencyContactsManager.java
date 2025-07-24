package com.mvp.sarah;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsManager {
    private static final String PREFS_NAME = "SaraEmergencyPrefs";
    private static final String KEY_CONTACTS = "emergency_contacts";

    public static class Contact {
        public String name;
        public String phone;
        public Contact(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }
    }

    public static List<Contact> getContacts(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_CONTACTS, "[]");
        List<Contact> contacts = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                contacts.add(new Contact(obj.getString("name"), obj.getString("phone")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return contacts;
    }

    public static void saveContacts(Context context, List<Contact> contacts) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray arr = new JSONArray();
        for (Contact c : contacts) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", c.name);
                obj.put("phone", c.phone);
                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(KEY_CONTACTS, arr.toString()).apply();
    }

    public static void addContact(Context context, Contact contact) {
        List<Contact> contacts = getContacts(context);
        contacts.add(contact);
        saveContacts(context, contacts);
    }

    public static void removeContact(Context context, Contact contact) {
        List<Contact> contacts = getContacts(context);
        contacts.removeIf(c -> c.phone.equals(contact.phone));
        saveContacts(context, contacts);
    }

    public static void updateContact(Context context, Contact oldContact, Contact newContact) {
        List<Contact> contacts = getContacts(context);
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).phone.equals(oldContact.phone)) {
                contacts.set(i, newContact);
                break;
            }
        }
        saveContacts(context, contacts);
    }
} 