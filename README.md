# MedForms

<!-- ![GitHub Workflow Status](https://img.shields.io/github/workflow/status/chandlerklein/medforms/Android%20CI) -->
<!-- ![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/chandlerklein/medforms) -->
<!-- ![GitHub](https://img.shields.io/github/license/chandlerklein/medforms) -->

<img align="right" height="500" src="/documentation/images/screens.png" alt="MedForms UI">

MedForms is a HIPAA–compliant Android application for usage on a tablet that uses handwriting recognition and dynamically
generates/exports consultation forms. First, MedForms securely connects to a remote file server through its configuration menu and specifies the location of a JSON template file to import. It also specifies a path to export the final output to. When the user begins a consultation, the JSON template file is used to dynamically create Android View elements and populate the form. The form can be completely filled out with a stylus using MyScript, a handwriting recognition library. Once the form is complete, the final output is exported to the original file server for review prior to forwarding.

## Summary

- [Form Fields Configuration](#form-fields-configuration)
- [Getting Started](#getting-started)
- [Built With](#built-with)
- [Authors](#authors)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Form Fields Configuration

For a detailed overview of the JSON consultation form template structure as well as available field types and their representations, check out the [documentation](/documentation/README.md).

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

```
Android Studio 4.0.1
```

```
API level 29 (Android 10) tablet emulator or physical device
```

### Installing

A step by step series of examples that tell you how to get a development env running

Clone this repo

```
git clone https://github.com/C94/MedForms.git
```

Open the Application folder in Android Studio

```
Open an existing Android Studio project → Application/
```

Build the project and run on your device

```
Make Project → Choose emulator/physical device in Available Devices → Run 'App'
```

You're done!

## Built With

- [Android Studio](https://developer.android.com/studio) - Integrated Development Environment
- [Gradle](https://gradle.org/) - Build automation
- [MyScript](https://www.myscript.com/) - Handwriting recognition

## Authors

- **[Chandler Klein](https://github.com/C94)** — ck@chandlerklein.com

- **[Garrett Ruffner](https://github.com/xorplanet)**

## License

This project is licensed under Apache License, Version 2.0 - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

- [PurpleBooth README template](https://github.com/PurpleBooth/a-good-readme-template/blob/main/README.md)
