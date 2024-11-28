# TV Ad Volume Control

An Android application that automatically detects and reduces volume during television advertisements using audio pattern analysis.

## Features

- Real-time advertisement detection using audio analysis
- Automatic volume reduction during detected ads
- User-configurable sensitivity settings
- Support for multiple audio output devices
- Battery-efficient implementation
- Clean, intuitive user interface

## Technical Details

- Minimum SDK: Android 9.0 (API level 28)
- Target SDK: Android 13 (API level 33)
- Language: Kotlin
- Architecture: MVVM

### Key Components

- **Audio Analysis**: Uses Fast Fourier Transform (FFT) for frequency analysis
- **Pattern Detection**: Analyzes multiple frequency bands and volume patterns
- **Volume Control**: Smooth volume adjustment with configurable thresholds

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Build and run on a device with Android 9.0 or later

## Required Permissions

- `RECORD_AUDIO`: For analyzing audio patterns
- `MODIFY_AUDIO_SETTINGS`: For adjusting volume

## Configuration

The app provides several configurable settings:

- Ad Detection Sensitivity (10-90)
- Enable/Disable Feature
- Volume Reduction Level

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
