package com.mvp.sarah.handlers;

import android.content.Context;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class CalculatorHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "what is",
            "calculate",
            "plus",
            "minus",
            "times",
            "divided by"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase().trim();
        // Only match if it looks like a calculation
        if (lower.startsWith("calculate") || lower.contains("plus") || lower.contains("minus") || lower.contains("times") || lower.contains("divided by")) {
            return true;
        }
        // Only match 'what is' if it contains numbers and math operators
        if (lower.startsWith("what is the value of ") || lower.startsWith("how much is")) {
            return lower.matches(".*\\d.*[+\\-*/].*\\d.*");
        }
        return false;
    }

    @Override
    public void handle(Context context, String command) {
        String expr = extractExpression(command);
        if (expr == null) {
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't understand the calculation.");
            return;
        }
        try {
            double result = eval(expr);
            String resultStr;
            if (result == Math.rint(result)) {
                // Whole number, show as integer
                resultStr = String.valueOf((long) result);
            } else {
                // Decimal, show as is
                resultStr = String.valueOf(result);
            }
            FeedbackProvider.speakAndToast(context, "The answer is " + resultStr);
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't calculate that.");
        }
    }

    private String extractExpression(String command) {
        command = command.toLowerCase().replace("what is the value of ", "").replace("calculate", "").trim();
        command = command.replace("plus", "+").replace("minus", "-").replace("times", "*").replace("multiplied by", "*").replace("divided by", "/");
        command = command.replaceAll("[^0-9+*/. -]", "");
        return command.trim().isEmpty() ? null : command.trim();
    }

    // Simple eval for +, -, *, /
    private double eval(String expr) {
        // This is a very basic parser for simple expressions
        String[] tokens = expr.split(" ");
        double result = Double.parseDouble(tokens[0]);
        for (int i = 1; i < tokens.length; i += 2) {
            String op = tokens[i];
            double val = Double.parseDouble(tokens[i + 1]);
            switch (op) {
                case "+": result += val; break;
                case "-": result -= val; break;
                case "*": result *= val; break;
                case "/": result /= val; break;
            }
        }
        return result;
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 