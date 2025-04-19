# CameraX Guide - Expiration Date Checker

An Android application that helps users check product expiration dates using camera recognition technology. The app provides both automated date recognition through the camera and manual entry options for maximum flexibility.

## Features

- 📅 Scan and recognize expiration dates on product packaging of Eucerin products
- ✏️ Manual date entry option when camera recognition is challenging
- 📸 High-quality photo capture using CameraX API
- 🔍 Text recognition powered by ML Kit
- 🔄 Switch between front and back cameras
- 💡 Toggle camera flash/torch for better scanning in low light
- 🎨 Modern UI built with Jetpack Compose
- 📱 View captured scans in an integrated gallery


## Main Functionality

This application serves as an expiration date checker with dual input methods:

1. **Camera Recognition**: Point your camera at a product's expiration date and the app will automatically detect and process the date information using ML Kit's text recognition capabilities.
<img src="https://github.com/user-attachments/assets/2dde7dd2-e0e9-4b2e-b7bf-b8d702e94548" width=30% height=30%>

3. **Manual Entry**: If the camera has difficulty recognizing the date or if you prefer manual input, the app provides a simple interface to manually enter expiration dates.
<img src="https://github.com/user-attachments/assets/93e79d6f-bea3-41f4-812d-cf754257e4b3" width=30% height=30%>

## Requirements

- Android Studio Flamingo or higher
- Android SDK 24+ (Android 7.0 Nougat or higher)
- Kotlin 1.8+
- Device with camera functionality

## Setup

1. Clone this repository
```bash
git clone https://github.com/yourusername/CameraXGuide-taking-photos.git
```

2. Open the project in Android Studio

3. Sync Gradle files and build the project

4. Run the app on a physical device or emulator (camera functionality works best on a physical device)

## Technologies Used

- **Jetpack Compose**: Modern UI toolkit for building native Android UI
- **CameraX**: Jetpack library that makes camera development easier
- **ML Kit**: Google's machine learning SDK for mobile developers
- **Kotlin Coroutines**: For asynchronous programming
- **ViewModel**: For UI-related data handling
- **Material Design 3**: For modern UI components

## Application Structure

- `MainActivity.kt`: Main entry point that handles camera functionality, text recognition, permissions, and UI
- `TextAnalyzer.kt`: Handles expiration date recognition from camera feed
- `CameraPreview.kt`: Composable function for camera preview
- `PhotoBottomSheetContent.kt`: UI for displaying captured photos and recognized dates
- `MainViewModel.kt`: Manages UI state and expiration date data

## Permissions

The application requires the following permissions:
- Camera access
- External storage (for saving photos)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgements

- This project was inspired by the CameraX documentation and sample applications
- Thanks to the Jetpack Compose and CameraX teams for their excellent libraries 
