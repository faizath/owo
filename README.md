# 🦉 OwO - Hotel & Flight Booking App

<div align="center">
  <img src="src/app/src/main/resources/com/owo/assets/logo_owo.png" alt="OwO Logo" width="200"/>
  <br>
  <strong>A Modern Desktop Application for Hotel & Flight Booking</strong>
  <br><br>
  
  ![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
  ![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=flat-square&logo=java)
  ![Gradle](https://img.shields.io/badge/Gradle-8.14.1-green?style=flat-square&logo=gradle)
  ![SQLite](https://img.shields.io/badge/SQLite-3.45.1.0-lightblue?style=flat-square&logo=sqlite)
</div>

## 📖 About

OwO is a comprehensive desktop application designed for seamless hotel and flight booking experiences. Built with modern Java technologies, it provides an intuitive interface powered by JavaFX with WebView integration, offering users a smooth and responsive booking platform.

### ✨ Key Features

- 🏨 **Hotel Booking** - Search and book accommodations with ease
- ✈️ **Flight Booking** - Find and reserve flight tickets
- 💰 **Refund Management** - Handle cancellations and refunds efficiently
- ✅ **Check-in Process** - Streamlined check-in for booked services
- 🔐 **Secure Authentication** - User login and account management
- 📱 **Modern UI** - Responsive web-based interface within desktop app

## 🛠️ Tech Stack

### Core Technologies
- **Java 21** - Modern Java runtime with latest features
- **JavaFX 21** - Rich desktop application framework
- **WebView** - Embedded web content rendering
- **Gradle 8.14.1** - Build automation and dependency management

### Database & Security
- **SQLite 3.45.1.0** - Lightweight embedded database
- **BCrypt (jBcrypt 0.4)** - Password hashing and security

### Testing & Libraries
- **JUnit Jupiter 5.8.2** - Unit testing framework
- **Google Guava 31.1-jre** - Core Java utilities

## 🎯 Implemented Use Cases

### 1. 🎫 Pemesanan Tiket (Ticket Booking)
Complete ticket booking system for both hotels and flights.
- **Classes**: `PemesananViewer`, `PemesananController`, `Pemesanan`, `Tiket`

### 2. 💸 Refund dan Pembatalan (Refund & Cancellation)
Comprehensive refund and cancellation management system.
- **Classes**: `RefundViewer`, `RefundController`, `Pemesanan`, `Refund`

### 3. ✅ Proses Check In (Check-in Process)
Streamlined check-in system for booked services.
- **Classes**: `RiwayatPemesananViewer`, `CheckInController`, `Pemesanan`

### 4. 🔐 Login/Autentikasi (Authentication)
Secure user authentication and account management.
- **Classes**: `LoginForm`, `AuthController`, `Akun`, `Notifikasi`

## 🏗️ Architecture

The application follows a clean architecture pattern with clear separation of concerns:

### 📱 Boundary Layer (UI Components)

The boundary is HTML, CSS and JavaScript rendered inside a single `WebView` — there are no
Java UI classes. `App.html` is the shell: it hosts one `#app` container and a router that
swaps screen fragments in place, so the JavaScript bridge is injected once and survives
navigation.

- `boundary/App.html` - Application shell, session bar, notifications
- `boundary/app.js` - Router, session, formatting and escaping helpers
- `boundary/screens.js` - One controller per screen
- `boundary/owo-bridge.js` - Promise wrapper over the Java bridge
- `boundary/screens/*.html` - Screen fragments (markup and styles only)

Screen fragments contain no `<script>` blocks and no inline event handlers; their
behaviour lives in `screens.js`. Java serves each fragment through the bridge, because a
packaged build runs from a jar where the page cannot fetch its own resources.

### 🎮 Controller Layer (Business Logic)
- `PemesananController` - Handles booking operations
- `RefundController` - Manages refund processes
- `CheckInController` - Controls check-in operations
- `AuthController` - Manages authentication

### 📊 Entity Layer (Data Models)
- `Pemesanan` - Booking entity
- `Tiket` - Ticket entity (with `TiketHotel` and `TiketPesawat` subclasses)
- `Refund` - Refund entity
- `Akun` - User account entity
- `Notifikasi` - Notification entity

### 🗄️ Data Access Layer (DAO)
- `PemesananDAO` - Booking data operations
- `TiketDAO` - Ticket data operations
- `RefundDAO` - Refund data operations
- `AkunDAO` - Account data operations
- `NotifikasiDAO` - Notification data operations

### 🔧 Utilities
- `DBHelper` - Database location and connections (a fresh connection per call)
- `JavaScriptBridge` - The only surface JavaScript can call; owns the session
- `BridgeInstaller` - Wires the bridge into the `WebEngine`
- `Json` - JSON reader/writer for the bridge, locale-independent
- `SqlDates` - Tolerant date parsing for stored values
- `PasswordUtil` - Password hashing (bcrypt)
- `NotifikasiHelper` - Notification polling, started on login
- `NotificationBridge` - Delivers notifications into the page
- `JsConsole` - Forwards page console output to the application log

### 🔐 Session and authorization

The session lives in Java, on `JavaScriptBridge`. **No bridge method accepts a user id** —
every operation on user data derives identity from the session, so a client cannot request
another user's records. Booking status transitions are decided by `PemesananController`
against an explicit state machine (`PemesananStatus`), and refund amounts are computed
server-side from the stored ticket price.

## 🚀 Installation & Setup

### Prerequisites
- Git
- Any JVM between 17 and 24 to launch the Gradle wrapper

Gradle selects a Java 21 JVM for its own daemon via `src/gradle/gradle-daemon-jvm.properties`,
and provisions one through the foojay resolver if none is installed. No `JAVA_HOME`
configuration is required.

### Steps to Run Locally

1. **Clone the repository**
   ```bash
   git clone https://github.com/faizathr/IF2050-2025-K1D-OwO
   ```

2. **Navigate to the project directory**
   ```bash
   cd IF2050-2025-K1D-OwO/src
   ```

3. **Build the application**
   ```bash
   ./gradlew build
   ```

4. **Seed the database** (optional; idempotent, safe to re-run)
   ```bash
   ./gradlew seed
   ```

5. **Run the application**
   ```bash
   ./gradlew run
   ```

### Demo Account

`./gradlew seed` creates a demo account alongside the sample flights and hotels:

| Email | Password |
|---|---|
| `demo@owo.id` | `demo1234` |

### Alternative Commands (Windows)
For Windows users, use `gradlew.bat` instead:
```cmd
gradlew.bat build
gradlew.bat seed
gradlew.bat run
```

### Troubleshooting: `Unsupported class file major version`

If the daemon JVM criteria cannot be satisfied, Gradle falls back to the launcher JVM and
the build fails while parsing the build script. Point `JAVA_HOME` at a Java 21 installation:

```bash
# Linux / macOS
JAVA_HOME=/path/to/jdk-21 ./gradlew build

# Windows
set JAVA_HOME=C:\path\to\jdk-21
gradlew.bat build
```

The number in the message identifies the JVM Gradle is running on: 65 = Java 21, 66 = 22,
67 = 23, 68 = 24, 69 = 25.

## 📁 Project Structure

```
owo/
├── doc/                    # Documentation files
├── img/                    # Image assets
├── README.md              # This file
└── src/
    ├── app/
    │   ├── build.gradle   # Build configuration
    │   ├── owo.db         # SQLite database
    │   └── src/
    │       ├── main/
    │       │   ├── java/com/owo/
    │       │   │   ├── App.java           # Main application entry
    │       │   │   ├── controller/        # Business logic controllers
    │       │   │   ├── dao/              # Data access objects
    │       │   │   ├── entity/           # Data models
    │       │   │   └── utils/            # Utility classes
    │       │   └── resources/com/owo/
    │       │       ├── assets/           # UI assets (images, icons)
    │       │       └── boundary/         # HTML/JS UI components
    │       └── test/                     # Unit tests
    ├── gradle/                           # Gradle wrapper files
    ├── gradlew                          # Gradle wrapper (Unix)
    ├── gradlew.bat                      # Gradle wrapper (Windows)
    └── settings.gradle                  # Gradle settings
```

## 🧪 Testing

Run the test suite:
```bash
./gradlew test
```

Test coverage includes:
- Entity model testing
- Controller logic testing
- Authentication flow testing
- Database operations testing

## 📝 License

This project is part of the Software Engineering Fundamentals course (IF2050) at Institut Teknologi Bandung.

## 👥 Team

- **Group**: K1D
- **Course**: IF2050 - Dasar Rekayasa Perangkat Lunak
- **Institution**: Institut Teknologi Bandung

| NIM | Nama Lengkap | Tugas |
|---|---|---|
| 18223061 | Naura Ayurachmani | PemesananViewer, LoginForm, notulensi asistensi, laporan HIUPL (bab 1, bab 2, bab 3, bab 4, bab 5) |
| 18222051 | Firsa Athaya Raissa Alifah | RiwayatPemesananViewer, PembayaranViewer, laporan HIUPL (bab 3, bab 4, bab 5) |
| 18222060 | Taufiq Ramadhan Ahmad | PemesananController, Pemesanan, Tiket, laporan HIUPL (bab 3, bab 5) |
| 18222063 | Muhammad Faiz A | AuthController, Akun, Notifikasi, Integrasi |
| 18222072 | Muhammad Kevinza Faiz | RefundController, CheckInController, Refund, laporan HIUPL (bab 3) |
---

<div align="center">
  Made with ❤️ by Team OwO
</div>
