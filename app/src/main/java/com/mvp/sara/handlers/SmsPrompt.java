package com.mvp.sara.handlers;

public interface SmsPrompt {
    void promptForRecipient();
    void promptForMessage(String recipient);
    void sendSms(String recipient, String message);
} 