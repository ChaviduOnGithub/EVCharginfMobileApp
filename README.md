# ⚡️ Bolt – EV Charging Station Mobile App

Bolt is a smart mobile application that helps EV owners easily locate, book, and manage electric vehicle charging stations.  
The system is powered by a **.NET Web API backend** and provides real-time access to station availability, bookings, and user sessions.

---

## 🚀 Features

- 🔐 **User Authentication**
  - Login & registration with NIC-based credentials.
  - Secure session management.

- 🗺️ **Interactive Map**
  - View all nearby charging stations.
  - See all available stations across the island.
  - Tap a station to view details or make a booking.

- 🕐 **Booking System**
  - Reserve charging slots by date and time.
  - Manage pending or confirmed bookings.

- 📍 **Live Location**
  - Detect your current position and highlight it on the map.
  - Use GPS permissions to center the map around your location.

- 🌙 **Modern UI**
  - Material Design components.
  - Lottie animations for smooth visuals.
  - Simple and intuitive layout.

---

## 🧱 System Architecture
Mobile App (Android - Kotlin)
▼
.NET Web API
▼
Mongo DB


- **Mobile App:** Kotlin, Android Jetpack, OSMDroid, Lottie
- **Backend API:** ASP.NET Core 8.0 hosted on Azure
- **Database:** SQL Server / Azure SQL
- **Authentication:** JWT-based token system

---

## 🧩 Tech Stack

| Component | Technology |
|------------|-------------|
| Mobile App | Kotlin, Android Jetpack, OSMDroid, Lottie |
| Backend API | ASP.NET Core 8.0 |
| Database | Microsoft SQL Server / Azure SQL |
| Hosting | Azure App Service |
| API Communication | REST (JSON) via HTTPS |

---

## ⚙️ Setup Instructions

### 🖥️ Prerequisites
- Android Studio (latest version)
- .NET SDK 8.0+
- Azure subscription (for hosting)
