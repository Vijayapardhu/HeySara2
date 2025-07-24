package com.mvp.sarah.handlers;

public interface SmsPrompt {
    void promptForRecipient();
    void promptForMessage(String recipient);
    void sendSms(String recipient, String message);
} 