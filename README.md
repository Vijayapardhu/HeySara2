<<<<<<< HEAD
# HeySara
Powerful Voice Assistant
=======
# Sara Voice Assistant

A voice-controlled Android assistant that responds to "Hey Sara" and can perform various device operations.

## New Feature: Media Interruption Control

Sara now properly manages audio focus to prevent unwanted interruption of media playback. You can control whether Sara should interrupt your music, videos, or other media.

### Voice Commands for Media Settings

- **"Sara settings"** or **"Voice settings"** - Check current media interruption settings
- **"Allow media interruption"** - Enable Sara to interrupt media playback when listening
- **"Disable media interruption"** or **"Don't interrupt media"** - Prevent Sara from interrupting media playback

### How It Works

**Default Behavior (Recommended):**
- Sara will NOT interrupt your media playback
- When media is playing, Sara will tell you to pause it first before saying "Hey Sara"
- This ensures uninterrupted music/video experience

**Alternative Behavior:**
- If you enable media interruption, Sara will pause your media when you say "Hey Sara"
- This allows immediate voice commands even during media playback

### Technical Details

The app now uses Android's AudioFocus system to:
- Properly request audio focus before speech recognition
- Respect other apps' audio playback
- Provide user control over interruption behavior
- Use appropriate audio focus levels based on user preferences

### Usage Tips

1. **For uninterrupted media experience:** Keep media interruption disabled (default)
2. **For immediate voice access:** Enable media interruption
3. **When media is playing:** Either pause it first, or enable interruption in settings
4. **Check current settings:** Say "Sara settings" to see your current preference

### Other Voice Commands

Sara supports many other commands including:
- Volume control
- Brightness adjustment
- Flashlight toggle
- App launching
- SMS sending
- And many more!

Say "Hey Sara" followed by your command to get started. 
>>>>>>> a2b1706 (Initial commit)
"# HeySara2" 
