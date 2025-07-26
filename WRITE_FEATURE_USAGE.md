# Write Feature Documentation

## Overview
The WriteHandler allows you to generate and automatically type text content into text fields using AI. When you select a text field in any app (WhatsApp, email, notes, etc.) and say a write command, the app will generate appropriate content and type it into the field.

## How to Use

### Basic Commands
- **"write code"** - Generates a simple code example
- **"write email"** - Generates a professional email template
- **"write message"** - Generates a casual message
- **"write letter"** - Generates a formal letter
- **"write response"** - Generates a thoughtful response

### Advanced Commands
- **"compose email"** - Same as "write email"
- **"draft message"** - Same as "write message"
- **"write a function"** - Generates a programming function
- **"write a hello world program"** - Generates a basic programming example

### Step-by-Step Usage

1. **Open any app with text input** (WhatsApp, Gmail, Notes, etc.)
2. **Tap on a text field** to focus it (the cursor should be blinking in the text field)
3. **Say your write command** (e.g., "write code" or "write email")
4. **Wait for generation** - The app will say "Writing [content type]..."
5. **Content is automatically typed** - The generated text will appear in the text field

### Examples

#### Writing Code in a Text Editor
1. Open a text editor or code editor app
2. Focus on a text area
3. Say: "write code"
4. AI generates a Python example with comments and types it automatically

#### Writing an Email in Gmail
1. Open Gmail and start composing an email
2. Focus on the email body text field
3. Say: "write email" 
4. AI generates a professional email template and types it in

#### Writing a WhatsApp Message
1. Open WhatsApp and select a chat
2. Focus on the message input field
3. Say: "write message"
4. AI generates a casual message and types it in

### Content Types Generated

- **Code**: Python examples with comments (default), practical and concise
- **Email**: Professional format with subject, greeting, body, and closing
- **Message**: Casual, conversational text suitable for texting/messaging
- **Letter**: Formal business letter with proper formatting
- **Response**: Thoughtful replies that address main points

### Technical Details

- Uses Google Gemini AI API for content generation
- Works with any text field that supports accessibility services
- Content is generated in the background and typed automatically
- Provides voice feedback during the process
- Handles errors gracefully with user feedback

### Troubleshooting

**Text not typing into field:**
- Make sure the text field is focused (cursor blinking)
- Ensure accessibility services are enabled for the app
- Try tapping the text field again before giving the command

**No content generated:**
- Check internet connection (requires API access)
- Wait longer - AI generation can take a few seconds
- Try the command again

**Wrong type of content:**
- Use more specific commands (e.g., "write Python code" instead of just "write code")
- The AI adapts based on your specific request

### Privacy Note
Content generation requires internet connection and uses Google's Gemini API. Generated content is processed according to Google's privacy policies.