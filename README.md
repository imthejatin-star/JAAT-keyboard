# JAAT Keyboard

A native Android keyboard experiment built with Kotlin.

## Features
- Android Input Method Service
- Hacker-inspired dark interface
- Three local themes
- English and Hindi character layouts
- Developer symbols
- Local word suggestions
- Basic swipe gesture recognition
- User-triggered clipboard paste
- Offline text editing tools
- GitHub Actions APK build

## Build
GitHub Actions builds the debug APK on every push to main.

## Install
Download the APK from the GitHub Actions artifact.

Open JAAT Keyboard, enable it in Android keyboard settings, and select it using the input method picker.

## Privacy
The keyboard does not send typed text to a server. Clipboard content is read only when the clipboard button is tapped.

## Limitations
The current suggestion engine uses a small bundled dictionary. Swipe recognition is a basic prototype, not a full predictive swipe engine. The offline writing tools are text utilities, not a generative AI model.
