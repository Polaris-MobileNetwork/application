# Polaris Android Network Monitoring App

## 📄 Introduction

Polaris is a modern Android application designed for comprehensive mobile network performance monitoring. It functions as a "drive test" tool, continuously collecting detailed cellular network metrics, executing various network-related tests, and syncing the data with a central server for analysis and visualization. The app is built with a focus on modern Android development practices, ensuring it is robust, scalable, and efficient.

---

## ✨ Features

* **Continuous Background Collection**: Utilizes a foreground service (`NetworkMetricService`) to reliably collect network data even when the app is closed or in the background.
* **Detailed Metric Gathering**: Collects a wide range of cellular network parameters, including:
    * **Signal Strength & Quality**: RSRP, RSRQ, RSCP, RxLev, etc.
    * **Cellular Identity**: PLMN ID, Cell ID, TAC, LAC.
    * **Network Technology**: Differentiates between GSM, WCDMA, LTE, 5G NSA, and 5G SA.
    * **Location Data**: Uses Google's Fused Location Provider for precise GPS coordinates for each data point.
* **Local Data Persistence**: All collected metrics and test results are stored locally in a robust Room database, ensuring no data is lost.
* **Active Network Testing**:
    * **Manual & Scheduled Tests**: Supports user-initiated (manual), one-time scheduled, and periodic tests.
    * **Test Suite**: Implements a variety of network tests, including Ping, DNS Lookup, HTTP Web Load, Download Speed, and Upload Speed.
    * **Background Execution**: Uses `WorkManager` to reliably execute scheduled and periodic tests, even if the app is closed or the device is restarted.
* **Server Synchronization**:
    * **Metrics Sync**: Periodically syncs collected network metrics with a remote server.
    * **Test Configuration Sync**: Fetches new and updated test configurations from the server and handles deletions.
    * **Test Result Sync**: Uploads the results of all executed tests to the server.
* **Modern UI & UX**:
    * Built entirely with **Jetpack Compose** and Material 3 design principles.
    * Features a clean, tab-based navigation system.
    * Provides real-time feedback on service status, collection duration, and sync operations.
* **User-Configurable Settings**:
    * Allows users to customize the app theme (Light, Dark, System).
    * Provides options to control data collection and sync intervals.
    * Settings are persisted locally using Jetpack DataStore.

---

## 🏗️ Architecture & Tech Stack

The application is built following Google's recommended architecture for modern Android development, emphasizing a reactive UI and a clear separation of concerns.

* **UI Layer**: Built with **Jetpack Compose**. Screens observe state from ViewModels and are designed to be stateless and reusable.
* **ViewModel Layer**: Uses AndroidX `ViewModel`s to manage UI-related state and handle user interactions.
* **Repository Layer**: Implements the Repository pattern to abstract data sources, providing a clean API to the ViewModels.
* **Data Layer**:
    * **Local**: **Room** database for local persistence of network metrics, tests, and results. **Jetpack DataStore** for user settings.
    * **Remote**: **Retrofit** for making type-safe HTTP requests to the backend API. **Kotlinx Serialization** is used for JSON parsing.
* **Dependency Injection**: **Hilt** is used to manage dependencies throughout the app, simplifying the creation of objects and improving testability.
* **Asynchronous Operations**: **Kotlin Coroutines** and **Flow** are used extensively for all background tasks, database operations, and network calls.
* **Background Tasks**:
    * **Foreground Service**: For continuous, high-priority data collection.
    * **WorkManager**: For deferrable, guaranteed background tasks like scheduled tests and data synchronization.

---

## 📦 Modules & Key Components

* **`service/NetworkMetricService.kt`**: A foreground service that is the core of data collection. It runs a persistent timer, gathers location and cellular data, and saves it to the database.
* **`service/TestSchedulerWorker.kt`**: A `WorkManager` worker that runs periodically (every 15 mins) to execute one-time scheduled tests and sync test configurations with the server.
* **`service/TestExecutor.kt`**: A key class responsible for the actual logic of executing different types of tests (Ping, DNS, etc.).
* **`data/local/AppDatabase.kt`**: The Room database definition, including all entities (tables) and DAOs.
* **`data/repository/`**: Contains repository interfaces and their implementations, which abstract the data sources.
* **`ui/viewmodel/`**: Contains the ViewModels that provide state to the UI and handle logic.
* **`ui/screens/`**: Contains the top-level composable functions for each screen in the app.
* **`ui/components/`**: Contains smaller, reusable UI components used across different screens.

---

## 🌐 API & Data Models

The app communicates with a backend server for syncing test configurations and uploading data.

### Test Configuration Sync

* **Endpoint**: `POST /api/Test/except`
* **Request Body**:
    ```json
    {
      "excludedIds": ["server-id-1", "server-id-2"]
    }
    ```
* **Response Body**:
    ```json
    {
      "success": true,
      "code": 200,
      "message": "Optional message",
      "tests": [
        {
          "id": "server-id-3",
          "name": "New Scheduled Ping",
          "type": "PING",
          "parametersJson": "{\"host\":\"example.com\",\"count\":3}",
          "isEnabled": true,
          "scheduledTimestamp": 1755437473526,
          "intervalSeconds": 0,
          "isCompleted": false
        }
      ]
    }
    ```

### Test Deletion Sync

* **Endpoint**: `GET /api/Test/deleted`
* **Response Body**:
    ```json
    {
      "success": true,
      "code": 200,
      "message": null,
      "deletedTestIds": ["server-id-to-delete"]
    }
    ```

### Test Result Sync

* **Endpoint**: `POST /api/TestResult/Save`
* **Request Body**:
    ```json
    {
      "results": [
        {
          "timestamp": 1755437473526,
          "testType": "PING",
          "targetHost": "8.8.8.8",
          "resultValue": "25.3 ms",
          "isSuccess": true,
          "details": "Ping successful...",
          "testId": "server-id-of-parent-test"
        }
      ]
    }
    ```

### Network Metric Sync

* **Endpoint**: `POST /your/endpoint/path/for/metrics`
* **Request Body**:
    ```json
    {
      "measurements": [
        {
          "timeStamp": 1755437473526,
          "latitude": 35.1234,
          "longitude": 51.4321,
          "networkType": "LTE",
          "plmnId": "432-11",
          "lac": 1234,
          "tac": 5678,
          "rac": null,
          "cellId": "12345678",
          "arfcn": 1500,
          "frequencyBand": "Band 3",
          "actualFrequencyMhz": 1800.5,
          "signalStrength": -85,
          "rsrp": -95,
          "rsrq": -12,
          "rscp": null,
          "rxlev": null,
          "ecno": null
        }
      ]
    }
    ```

---

## 🚀 Setup & Build

1.  **Clone the repository.**
2.  **Open the project in the latest stable version of Android Studio.**
3.  **Permissions**: The app will request the following runtime permissions on first launch:
    * `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`
    * `READ_PHONE_STATE`
    * `POST_NOTIFICATIONS` (on Android 13+)
    * `FOREGROUND_SERVICE_LOCATION` (on Android 14+)
    * `SEND_SMS`
4.  **Configuration**: Before building, you may need to update the `BASE_URL` constant in `di/AppModule.kt` to point to your backend server.
5.  **Build and run** the app on an emulator or a physical device.