<p align="center">
  <img src="docs/assets/banner.png" alt="CatMap Banner" width="100%">
</p>

<h1 align="center">CatMap</h1>

<p align="center">
  <strong>Where Every Cat Finds Its Place.</strong>
</p>

<p align="center">
  An open-source Android application for discovering, photographing, and sharing cats on an interactive map.
</p>

<p align="center">
Discover nearby cats • Capture memorable moments • Share with the community
</p>

<p align="center">
Android • Firebase • Google Maps • CameraX • MVVM
</p>


<p align="center">

![Android](https://img.shields.io/badge/Android-API%2024+-34A853?logo=android&logoColor=white)

![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)

![Firebase](https://img.shields.io/badge/Firebase-Backend-FFCA28?logo=firebase&logoColor=black)

![Google Maps](https://img.shields.io/badge/Google%20Maps-SDK-4285F4?logo=googlemaps&logoColor=white)

![Architecture](https://img.shields.io/badge/Architecture-MVVM-2563EB)

![License](https://img.shields.io/github/license/WhalEmo/CatMap)

</p>

---

## Table of Contents

- [Overview](#overview)
- [Why CatMap?](#why-catmap)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Application Preview](#application-preview)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Lessons Learned](#lessons-learned)
- [Roadmap](#roadmap)
- [Contributors](#contributors)
- [License](#license)

---

## Overview

CatMap is an open-source Android platform built for cat lovers who want to discover, document, and share cats through an interactive map.

Instead of leaving memorable encounters as forgotten photos in a gallery, CatMap turns each sighting into a location-aware community post.

The application combines Google Maps, CameraX, Firebase Authentication, Cloud Firestore, Firebase Storage, and a real-time messaging system to create a social experience centered around location and photography.

The project follows the MVVM architectural pattern and focuses on maintainability, modularity, and modern Android development practices.

## Why CatMap?

Every day, countless cats are encountered in streets, campuses, neighborhoods, and parks. Most of these encounters disappear as isolated photos stored on personal devices.

CatMap transforms these moments into a shared community experience.

By combining location services, photography, and social interaction, the application allows every cat sighting to become part of a collaborative map maintained by the community.

Instead of asking:

> "I saw a cat today."

CatMap allows users to say:

> "I shared its story with everyone."


## Features

CatMap brings together interactive mapping, mobile photography, real-time communication and cloud services into a single Android application designed for cat lovers.

| Feature             | Description                                                        |
| ------------------- | ------------------------------------------------------------------ |
| **Authentication**  | User registration, login and password recovery.                    |
| **Interactive Map** | Explore nearby cats using Google Maps and GeoFire.                 |
| **Photography**     | Capture and upload photos with CameraX.                            |
| **Community**       | Profiles, comments, follows and cat posts.                         |
| **Realtime Chat**   | Instant one-to-one messaging.                                      |
| **Cloud Backend**   | Firebase Authentication, Firestore, Storage and Realtime Database. |


## Technology Stack

| Category | Technology |
|-----------|------------|
| Language | Kotlin + Java |
| UI | XML |
| Architecture | MVVM |
| Backend | Firebase |
| Database | Firestore |
| Storage | Firebase Storage |
| Authentication | Firebase Auth |
| Maps | Google Maps SDK |
| Camera | CameraX |
| Async | Coroutines |
| Build | Gradle KTS |
| Min SDK | 24 |
| Target SDK | 34 |

CatMap is built using modern Android development tools with a strong focus on scalability, maintainability and modular architecture.

## Application Preview

CatMap is designed around a simple user journey.

Discover nearby cats.

Capture memorable moments.

Share them with the community.

---

| <p align="center">**Discover**</p> | <p align="center">**Capture**</p> |
|-----------|----------|
| <p align="center"><img src="docs/screenshots/map_view.jpeg"></p> | <p align="center"><img src="docs/screenshots/camera_view.jpeg"></p> |
| <p align="center">Explore nearby cats shared by the community.</p> | <p align="center">Take high-quality photos using CameraX.</p> |

| <p align="center">**Share**</p> | <p align="center">**Connect**</p> |
|---------|---------|
| <p align="center"><img src="docs/screenshots/upload_view.jpeg"></p> | <p align="center"><img src="docs/screenshots/chat_view.jpeg"></p> |
| <p align="center">Publish your cat sightings with precise location.</p> | <p align="center">Start real-time conversations with other users.</p> |

| <p align="center">**Profile**</p> | <p align="center">**Details**</p> |
|-----------|----------|
| <p align="center"><img src="docs/screenshots/profile_view.jpeg"></p> | <p align="center"><img src="docs/screenshots/marker_detail_view.jpeg"></p> |
| <p align="center">Manage your posts and personal profile.</p> | <p align="center">View detailed information about every cat marker.</p> |

---

## Project Structure

The project is organized into feature-based modules and shared infrastructure layers to improve maintainability and scalability.

```text
app
├── auth/                # User authentication
├── maps/                # Google Maps & location features
├── chat/                # Realtime messaging
├── profile/             # User profiles and social features
├── models/              # Data models
├── repository/          # Data access layer
├── comments/            # Comments and replies
└── ui/
    ├── camera/          # CameraX implementation
    ├── navigation/      # Navigation engine
    ├── upload/          # Post publishing
    ├── manager/         # Shared managers
    ├── map/             # Map UI components
    └── extensions/      # Kotlin extensions
```

| Package | Responsibility |
|---------|----------------|
| `auth` | User authentication and account management |
| `maps` | Google Maps integration and location features |
| `chat` | Realtime messaging infrastructure |
| `profile` | User profile and social interactions |
| `repository` | Data access and Firebase communication |
| `models` | Application data models |
| `ui` | Shared UI components and navigation |

## Architecture

CatMap follows the **MVVM (Model–View–ViewModel)** architectural pattern.

The application separates presentation logic, business logic, and data access into dedicated layers, making the project easier to understand and maintain.

```text
            UI Layer
    (Activity / Fragment / XML)
              │
              ▼
          ViewModel
              │
              ▼
          Repository
              │
      ┌───────┼────────┬───────────┐
      ▼       ▼        ▼           ▼
 Firebase  Firestore Storage Google Maps
   Auth
              │
              ▼
           GeoFire
```

This architecture allows UI components to remain independent from backend implementations while improving scalability and code organization.

## Getting Started

Follow these instructions to set up the project on your local machine for development and testing.

### Prerequisites

Ensure you have the following installed before proceeding:
* **Android Studio** (Latest version recommended)
* **Java Development Kit (JDK)** 17 or higher
* A **Firebase** Account
* A **Google Cloud Platform** Account (for Maps SDK)

### Installation

1. Clone the repository.

```bash
git clone https://github.com/WhalEmo/CatMap.git
```

2. Open the project in Android Studio.

3. Create a Firebase project.

4. Download `google-services.json`.

5. Place `google-services.json` inside the `app/` directory.

6. Enable:
   - Authentication
   - Cloud Firestore
   - Firebase Storage
   - Realtime Database

7. Create a Google Maps API Key.

8. Add your Maps API key to `local.properties` or your preferred secrets configuration.

9. Sync Gradle.

10. Run the application.

## Lessons Learned

Developing CatMap provided valuable experience in modern Android application architecture and mobile software engineering.

Key lessons learned throughout the project include:

- MVVM significantly improves maintainability.
- Navigation architecture should be designed before feature implementation.
- Kotlin-only development simplifies project consistency.
- StateFlow would be preferred over LiveData for modern state management.
- Jetpack Compose would be the preferred UI toolkit for future development.

If CatMap were started again today, it would adopt a Compose-first architecture, a cleaner repository layer and a redesigned navigation engine.

## Roadmap

- [x] Authentication
- [x] Google Maps
- [x] CameraX
- [x] Firebase Storage
- [x] Realtime Chat
- [x] Profile System
- [x] Nearby Cats

### Next Version

- [ ] Jetpack Compose
- [ ] StateFlow
- [ ] Complete Kotlin Migration
- [ ] Improved Navigation Engine
- [ ] Clean Architecture
- [ ] Unit Tests
- [ ] Dark Theme
- [ ] CI/CD Pipeline
- [ ] Offline Mode


## Contributors

| Name | Role |
|------|------|
| **Beyza Nur Takça** | Lead Android Developer |
| **Emrullah Uygun** | Android Developer |

---

<p align="center">

Built with Kotlin, Java, Firebase and ❤️ for the cat community.

</p>

## License

This project is licensed under the MIT License. See the `LICENSE` file for more information.