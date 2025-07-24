package com.mvp.sarah;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import android.content.SharedPreferences;

public class EmergencyContactsActivity extends Activity {
    private static final int PICK_CONTACT_REQUEST = 1;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<EmergencyContactsManager.Contact> contacts;
    private List<String> contactNames;
    private static final String PREFS_NAME = "SaraEmergencyPrefs";
    private static final String KEY_MESSAGE = "emergency_message";
    private EditText editMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);

        listView = findViewById(R.id.list_contacts);
        Button btnAdd = findViewById(R.id.btn_add_contact);
        Button btnRemove = findViewById(R.id.btn_remove_contact);
        Button btnEdit = findViewById(R.id.btn_edit_contact);
        editMessage = findViewById(R.id.edit_emergency_message);
        Button btnSaveMessage = findViewById(R.id.btn_save_message);

        contacts = EmergencyContactsManager.getContacts(this);
        contactNames = new ArrayList<>();
        for (EmergencyContactsManager.Contact c : contacts) {
            contactNames.add(c.name + " (" + c.phone + ")");
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, contactNames);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedMsg = prefs.getString(KEY_MESSAGE, "I'm in trouble. Please help me! My location: {location_link}");
        editMessage.setText(savedMsg);
        btnSaveMessage.setOnClickListener(v -> {
            String msg = editMessage.getText().toString();
            prefs.edit().putString(KEY_MESSAGE, msg).apply();
            Toast.makeText(this, "Emergency message saved!", Toast.LENGTH_SHORT).show();
        });

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT_REQUEST);
        });

        btnRemove.setOnClickListener(v -> {
            int pos = listView.getCheckedItemPosition();
            if (pos != ListView.INVALID_POSITION) {
                EmergencyContactsManager.removeContact(this, contacts.get(pos));
                contacts.remove(pos);
                contactNames.remove(pos);
                adapter.notifyDataSetChanged();
                listView.clearChoices();
            } else {
                Toast.makeText(this, "Select a contact to remove", Toast.LENGTH_SHORT).show();
            }
        });

        btnEdit.setOnClickListener(v -> {
            int pos = listView.getCheckedItemPosition();
            if (pos != ListView.INVALID_POSITION) {
                EmergencyContactsManager.Contact oldContact = contacts.get(pos);
                showEditDialog(oldContact, pos);
            } else {
                Toast.makeText(this, "Select a contact to edit", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditDialog(EmergencyContactsManager.Contact contact, int pos) {
        EditText inputName = new EditText(this);
        inputName.setText(contact.name);
        EditText inputPhone = new EditText(this);
        inputPhone.setText(contact.phone);
        inputPhone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Edit Contact")
                .setView(inputName)
                .setPositiveButton("Next", (d, w) -> {
                    String newName = inputName.getText().toString();
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("Edit Phone")
                        .setView(inputPhone)
                        .setPositiveButton("Save", (d2, w2) -> {
                            String newPhone = inputPhone.getText().toString();
                            EmergencyContactsManager.Contact newContact = new EmergencyContactsManager.Contact(newName, newPhone);
                            EmergencyContactsManager.updateContact(this, contact, newContact);
                            contacts.set(pos, newContact);
                            contactNames.set(pos, newName + " (" + newPhone + ")");
                            adapter.notifyDataSetChanged();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            android.net.Uri contactUri = data.getData();
            String[] projection = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
            try (android.database.Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String phone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    EmergencyContactsManager.Contact contact = new EmergencyContactsManager.Contact(name, phone);
                    EmergencyContactsManager.addContact(this, contact);
                    contacts.add(contact);
                    contactNames.add(name + " (" + phone + ")");
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }
} 