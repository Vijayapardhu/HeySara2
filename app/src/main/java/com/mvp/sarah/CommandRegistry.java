package com.mvp.sarah;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.mvp.sarah.handlers.CallAnswerHandler;
import com.mvp.sarah.handlers.NoteHandler;
import com.mvp.sarah.handlers.ReminderHandler;
import com.mvp.sarah.handlers.AlarmHandler;
import com.mvp.sarah.handlers.DoNotDisturbHandler;
import com.mvp.sarah.handlers.PlayMusicHandler;
import com.mvp.sarah.handlers.BluetoothHandler;
import com.mvp.sarah.handlers.WikipediaHandler;
import com.mvp.sarah.handlers.EmergencyHandler;
import com.mvp.sarah.AppLockActivity;
import com.mvp.sarah.handlers.SetupEmergencyHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandRegistry {
    private static final List<CommandHandler> handlers = new ArrayList<>();
    private static FallbackHandler fallbackHandler = new FallbackHandler();
    private static CommandListener commandListener;

    public interface CommandListener {
        void onCommandHandled(String command);
        void onCommandError(String command);
    }

    public static void setCommandListener(CommandListener listener) {
        commandListener = listener;
    }
    public static void register(CommandHandler handler) {
        handlers.add(handler);
    }

    public static boolean handleCommand(Context context, String command) {
        String normalized = command.trim().toLowerCase();
        Log.d("CommandRegistry", "Processing command: '" + command + "' (normalized: '" + normalized + "')");
        boolean commandHandled = false;
        
        // First, check if this is a call-related command and prioritize CallAnswerHandler
        if (isCallCommand(normalized)) {
            for (CommandHandler handler : handlers) {
                if (handler instanceof CallAnswerHandler) {
                    Log.d("CommandRegistry", "Found CallAnswerHandler, processing call command");
                    handler.handle(context, command);
                    commandHandled = true;
                    break;
                }
            }
        }
        
        if (!commandHandled) {
            for (CommandHandler handler : handlers) {
                if (handler.canHandle(normalized)) {
                    try {
                        Log.d("CommandRegistry", "Handler " + handler.getClass().getSimpleName() + " can handle command");
                        handler.handle(context, command);
                        commandHandled = true;
                        break;
                    } catch (Exception e) {
                        Log.e("CommandRegistry", "Error executing command: " + command, e);
                        FeedbackProvider.speakAndToast(context, "Sorry, an error occurred while handling your request.");
                        commandHandled = false;
                        break;
                    }
                }
            }
        }

        if (commandHandled) {
            if (commandListener != null) {
                commandListener.onCommandHandled(command);
            }
        } else {
            Log.d("CommandRegistry", "No handler found for command: '" + command + "'");
            fallbackHandler.handle(context, command);
            if (commandListener != null) {
                commandListener.onCommandError(command);
            }
        }
        return commandHandled;
    }
    
    private static boolean isCallCommand(String command) {
        return command.contains("answer") || 
               command.contains("reject") ||
               command.contains("decline") ||
               command.contains("accept") ||
               command.contains("pick up") ||
               command.contains("hang up");
    }

    // Fallback handler for suggestions
    public static class FallbackHandler implements CommandHandler {
        @Override
        public boolean canHandle(String command) {
            return true;
        }
        @Override
        public void handle(Context context, String command) {
            List<String> suggestions = new ArrayList<>();
            String normalized = command.trim().toLowerCase();
            for (CommandHandler handler : handlers) {
                if (handler instanceof SuggestionProvider) {
                    for (String pattern : ((SuggestionProvider) handler).getSuggestions()) {
                        if (pattern.contains(normalized) || normalized.contains(pattern)) {
                            suggestions.add(pattern);
                        }
                    }
                }
            }
            if (!suggestions.isEmpty()) {
                FeedbackProvider.speakAndToast(context, "Did you mean: " + suggestions.get(0), Toast.LENGTH_LONG);
            } else {
                FeedbackProvider.speakAndToast(context, "Sorry, I didn't understand that!");
            }
        }
    }

    // Interface for handlers to provide suggestions
    public interface SuggestionProvider {
        List<String> getSuggestions();
    }

    public static List<CommandHandler> getAllHandlers() {
        return new ArrayList<>(handlers);
    }

    public static void init(Context context) {
        if (!handlers.isEmpty()) {
            return; // Already initialized
        }
        
        try {
            // Register all command handlers
            register(new com.mvp.sarah.handlers.OnlineTestHandler());
            register(new com.mvp.sarah.handlers.ShowSharedLocationHandler());
            register(new com.mvp.sarah.handlers.QuickSettingsHandler());
            register(new com.mvp.sarah.handlers.QuickSettingsDebugHandler());
            register(new com.mvp.sarah.handlers.AirplaneModeHandler());
            register(new com.mvp.sarah.handlers.AlarmHandler());
            register(new com.mvp.sarah.handlers.BatteryHandler());
            register(new com.mvp.sarah.handlers.BluetoothHandler());
            register(new com.mvp.sarah.handlers.BrightnessHandler());
            register(new com.mvp.sarah.handlers.CalculatorHandler());
            register(new com.mvp.sarah.handlers.CalendarHandler());
            register(new com.mvp.sarah.handlers.CallAnswerHandler());
            register(new com.mvp.sarah.handlers.CallContactHandler());
            register(new com.mvp.sarah.handlers.CameraTranslateHandler());
            register(new com.mvp.sarah.handlers.ChitChatHandler());
            register(new com.mvp.sarah.handlers.ClickLabelHandler());
            register(new com.mvp.sarah.handlers.DeviceInfoHandler());
            register(new com.mvp.sarah.handlers.DictionaryHandler());
            register(new com.mvp.sarah.handlers.DoNotDisturbHandler());
            register(new com.mvp.sarah.handlers.EmailHandler());
            register(new com.mvp.sarah.handlers.FindPhoneHandler());
            register(new com.mvp.sarah.handlers.FlashlightHandler());
            register(new com.mvp.sarah.handlers.HelpHandler());
            register(new com.mvp.sarah.handlers.ImageAnalysisHandler());
            register(new com.mvp.sarah.handlers.JokeHandler());
            register(new com.mvp.sarah.handlers.LocationHandler());
            register(new com.mvp.sarah.handlers.MusicControlHandler());
            register(new com.mvp.sarah.handlers.NavigationHandler());
            register(new com.mvp.sarah.handlers.NewsHandler());
            register(new com.mvp.sarah.handlers.NoteHandler());
            register(new com.mvp.sarah.handlers.OpenAppHandler());
            register(new com.mvp.sarah.handlers.OpenCameraHandler());
            // OpenSettingsHandler moved after OpenCameraHandler to avoid conflicts
            register(new com.mvp.sarah.handlers.OpenSettingsHandler());
            register(new com.mvp.sarah.handlers.OpenWebsiteHandler());
            register(new com.mvp.sarah.handlers.PaymentHandler());
            register(new com.mvp.sarah.handlers.PlayMusicHandler());
            register(new com.mvp.sarah.handlers.RandomUtilityHandler());
            register(new com.mvp.sarah.handlers.ReadScreenHandler());
            register(new com.mvp.sarah.handlers.ReminderHandler());
            register(new com.mvp.sarah.handlers.RingerModeHandler());
            register(new com.mvp.sarah.handlers.SaraSettingsHandler());
            register(new com.mvp.sarah.handlers.ScreenshotHandler());
            register(new com.mvp.sarah.handlers.SearchHandler());
            register(new com.mvp.sarah.handlers.SendSmsHandler());
            register(new com.mvp.sarah.handlers.ShareFileHandler());
            register(new com.mvp.sarah.handlers.SmsReaderHandler());
            register(new com.mvp.sarah.handlers.StopwatchHandler());
            register(new com.mvp.sarah.handlers.TakePhotoHandler());
            register(new com.mvp.sarah.handlers.TimerHandler());
            register(new com.mvp.sarah.handlers.TranslateHandler());
            register(new com.mvp.sarah.handlers.TypeTextHandler());
            register(new com.mvp.sarah.handlers.VolumeHandler());
            register(new com.mvp.sarah.handlers.WeatherHandler());
            register(new com.mvp.sarah.handlers.WhatsAppHandler());
            register(new com.mvp.sarah.handlers.WifiHandler());
            register(new com.mvp.sarah.handlers.WikipediaHandler());
            register(new com.mvp.sarah.handlers.EmergencyHandler());
            register(new com.mvp.sarah.handlers.SetupEmergencyHandler());
            register(new com.mvp.sarah.handlers.ImageGenerationHandler());
            register(new com.mvp.sarah.handlers.LockScreenHandler());
            register(new com.mvp.sarah.handlers.StopServicesHandler());
            register(new com.mvp.sarah.handlers.CurrentVersionHandler());
            register(new com.mvp.sarah.handlers.TellTimeHandler());
            register(new com.mvp.sarah.handlers.TodaysDateHandler());
            register(new com.mvp.sarah.handlers.BackToHomeHandler());
            register(new com.mvp.sarah.handlers.DeleteAppHandler());
            register(new com.mvp.sarah.handlers.LockUnlockAppHandler());
            register(new com.mvp.sarah.handlers.RecordVideoHandler());
            register(new com.mvp.sarah.handlers.VideoCallHandler());
            register(new com.mvp.sarah.handlers.RemoveWaterHandler());
            register(new com.mvp.sarah.handlers.TodaysNewsHandler());
            register(new com.mvp.sarah.handlers.WeatherReportHandler());
            register(new com.mvp.sarah.handlers.ShowLocationPersonHandler());
            register(new com.mvp.sarah.handlers.ShareLocationHandler());
            register(new com.mvp.sarah.handlers.StopShareLocationHandler());
            register(new com.mvp.sarah.handlers.ShowSharedLocationHandler());
            register(new com.mvp.sarah.handlers.ScrollHandler());
            register(new com.mvp.sarah.handlers.TranslateModeHandler());
            register(new com.mvp.sarah.handlers.ScanExplainHandler());
            register(new com.mvp.sarah.handlers.ReadNewNotificationHandler());
            register(new com.mvp.sarah.handlers.UpdateAssistantHandler());
            register(new com.mvp.sarah.handlers.GeminiHandler());
            register(new com.mvp.sarah.handlers.OnlineTestHandler());
            
            // Don't set fallbackHandler here, keep the default FallbackHandler
        } catch (Exception e) {
            Log.e("CommandRegistry", "Error initializing command handlers: " + e.getMessage());
            // Continue with basic functionality even if some handlers fail
        }
    }
} 
