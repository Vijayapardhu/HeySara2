package com.mvp.sarah;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.util.Log;
import android.widget.ProgressBar;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.content.SharedPreferences;

public class AuthActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText editEmail, editPassword, editConfirmPassword, editUsername;
    private Button btnLogin, btnRegister, btnCreateAccount;
    private TextView textForgotPassword;
    private LinearLayout layoutProfileFields;
    private TextView textProfileUsername, textProfilePicovoice;
    private boolean isRegisterMode = false;
    private String googleUid;
    private ProgressBar progressBar;
    private static final int REQ_APP_LOCK_SETUP = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        editConfirmPassword = findViewById(R.id.edit_confirm_password);
        editUsername = findViewById(R.id.edit_username);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        textForgotPassword = findViewById(R.id.text_forgot_password);
        layoutProfileFields = findViewById(R.id.layout_profile_fields);
        textProfileUsername = findViewById(R.id.text_profile_username);
        textProfilePicovoice = findViewById(R.id.text_profile_picovoice);
        progressBar = findViewById(R.id.progress_bar);

        // Hide register fields by default
        editConfirmPassword.setVisibility(View.GONE);
        editUsername.setVisibility(View.GONE);
        btnRegister.setVisibility(View.GONE);
        layoutProfileFields.setVisibility(View.GONE);

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> registerUser());
        btnCreateAccount.setOnClickListener(v -> showRegisterFields());
        textForgotPassword.setOnClickListener(v -> sendPasswordReset());

        setupPasswordVisibilityToggle(editPassword);
        setupPasswordVisibilityToggle(editConfirmPassword);

        // If already logged in, show profile fields
        if (mAuth.getCurrentUser() != null) {
            fetchUserProfileIfExists();
        }
    }

    private void fetchUserProfileIfExists() {
        if (googleUid == null && mAuth.getCurrentUser() != null) {
            googleUid = mAuth.getCurrentUser().getUid();
        }
        if (googleUid == null) return;
        db.collection("users").document(googleUid).get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    String username = document.getString("username");
                    String storedDeviceId = document.getString("device_id");
                    String currentDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                    if (storedDeviceId != null && !storedDeviceId.equals(currentDeviceId)) {
                        Toast.makeText(this, "This account is already used on another device.", Toast.LENGTH_LONG).show();
                        FirebaseAuth.getInstance().signOut();
                        finish();
                        return;
                    }
                    if (username != null) {
                        showProfileFields(username);
                    }
                }
            });
    }

    private void showRegisterFields() {
        isRegisterMode = true;
        editConfirmPassword.setVisibility(View.VISIBLE);
        editUsername.setVisibility(View.VISIBLE);
        btnRegister.setVisibility(View.VISIBLE);
        btnLogin.setVisibility(View.GONE);
        btnCreateAccount.setVisibility(View.GONE);
    }

    private void setUiEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        btnRegister.setEnabled(enabled);
        btnCreateAccount.setEnabled(enabled);
        editEmail.setEnabled(enabled);
        editPassword.setEnabled(enabled);
        editConfirmPassword.setEnabled(enabled);
        editUsername.setEnabled(enabled);
        textForgotPassword.setEnabled(enabled);
        progressBar.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    private void loginUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        setUiEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                setUiEnabled(true);
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        googleUid = user.getUid();
                        fetchUserProfileIfExists();
                        proceedToMainOrAppLock();
                    }
                } else {
                    String message = "Login failed: ";
                    Exception e = task.getException();
                    if (e != null) {
                        Log.e("AuthActivity", "Login error", e);
                        if (e.getMessage() != null) message += e.getMessage();
                        if (e.getMessage() != null && e.getMessage().contains("password is invalid")) {
                            message = "Incorrect password.";
                        } else if (e.getMessage() != null && e.getMessage().contains("no user record")) {
                            message = "No account found with this email.";
                        }
                    } else {
                        message += "Unknown error";
                    }
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void registerUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();
        String username = editUsername.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword) || TextUtils.isEmpty(username)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        setUiEnabled(false);
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        db.collection("users").whereEqualTo("device_id", deviceId).get()
            .addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    setUiEnabled(true);
                    Toast.makeText(this, "This device is already registered with another account.", Toast.LENGTH_LONG).show();
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    googleUid = user.getUid();
                                    Map<String, Object> userProfile = new java.util.HashMap<>();
                                    userProfile.put("uid", googleUid);
                                    userProfile.put("username", username);
                                    userProfile.put("role", "user");
                                    userProfile.put("subscription", "normal");
                                    userProfile.put("device_id", deviceId);
                                    db.collection("users").document(googleUid).set(userProfile)
                                        .addOnSuccessListener(aVoid -> {
                                            setUiEnabled(true);
                                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_LONG).show();
                                            resetFields();
                                            proceedToMainOrAppLock();
                                        })
                                        .addOnFailureListener(e -> {
                                            setUiEnabled(true);
                                            Log.e("AuthActivity", "Firestore save error", e);
                                            Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                                }
                            } else {
                                setUiEnabled(true);
                                String message = "Registration failed: ";
                                Exception e = task.getException();
                                if (e != null) {
                                    Log.e("AuthActivity", "Registration error", e);
                                    if (e.getMessage() != null) message += e.getMessage();
                                    if (e.getMessage() != null && e.getMessage().contains("email address is already in use")) {
                                        message = "This email is already registered.";
                                    } else if (e.getMessage() != null && e.getMessage().contains("Password should be at least")) {
                                        message = "Password is too weak.";
                                    } else if (e.getMessage() != null && e.getMessage().contains("badly formatted")) {
                                        message = "Invalid email address format.";
                                    }
                                } else {
                                    message += "Unknown error";
                                }
                                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                setUiEnabled(true);
                Log.e("AuthActivity", "Device check error", e);
                Toast.makeText(this, "Error checking device registration: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void sendPasswordReset() {
        String email = editEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showProfileFields(String username) {
        layoutProfileFields.setVisibility(View.VISIBLE);
        textProfileUsername.setText("Username: " + username);
        textProfilePicovoice.setVisibility(View.GONE);
        // Hide login/register fields
        findViewById(R.id.btn_login).setVisibility(View.GONE);
        findViewById(R.id.btn_register).setVisibility(View.GONE);
        findViewById(R.id.btn_create_account).setVisibility(View.GONE);
        findViewById(R.id.edit_email).setVisibility(View.GONE);
        findViewById(R.id.edit_password).setVisibility(View.GONE);
        findViewById(R.id.edit_confirm_password).setVisibility(View.GONE);
        findViewById(R.id.text_forgot_password).setVisibility(View.GONE);
        findViewById(R.id.edit_username).setVisibility(View.GONE);
    }

    private void copyWakeword() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Wakeword", "Hey Sarah");
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Wakeword copied!", Toast.LENGTH_SHORT).show();
    }

    private void openPicovoiceLink() {
        String url = "https://picovoice.ai/platform/porcupine/";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }

    private void resetFields() {
        editEmail.setText("");
        editPassword.setText("");
        editConfirmPassword.setText("");
        editUsername.setText("");
    }

    private void proceedToMainOrAppLock() {
        SharedPreferences prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE);
        boolean isPinSet = prefs.getBoolean("app_lock_pin_set", false);
        if (!isPinSet) {
            Intent intent = new Intent(this, AppLockActivity.class);
            intent.putExtra("setup", true);
            startActivityForResult(intent, REQ_APP_LOCK_SETUP);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void setupPasswordVisibilityToggle(EditText editText) {
        editText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Drawable drawableEnd = editText.getCompoundDrawables()[2];
                if (drawableEnd != null && event.getRawX() >= (editText.getRight() - drawableEnd.getBounds().width())) {
                    if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                        editText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    } else {
                        editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    }
                    editText.setSelection(editText.getText().length());
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_APP_LOCK_SETUP) {
            // After setting PIN, go to MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
} 