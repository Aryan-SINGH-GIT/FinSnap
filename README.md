# 📱 FinSnap - Smart Finance Tracker for Gen Z & Young Adults

FinSnap is a modern and intuitive personal finance tracking app designed to help users manage their expenses, understand spending habits, and make smarter financial decisions — all in one tap. Whether you're tracking your monthly budget, analyzing where your money goes, or checking a suspicious SMS — FinSnap has your back.

## 🚀 Features

- 🔐 **Secure Local Storage** – Your data stays on your device.
- 📩 **Smart SMS Parsing** – Automatically detects and extracts transaction details from bank-related SMS messages.
- 🚫 **Scam Filter** – Filters out fraudulent or irrelevant messages using intelligent heuristics.
- 📝 **Manual Entry** – Add expenses or income manually in a few taps.
- 📊 **Interactive Analytics** – View spending categories, trends, and comparison graphs.
- 💳 **Categorization** – Assign categories to each transaction (e.g., Food, Transport, Bills).
- 🧠 **Insights for Action** – Personalized tips based on your spending patterns.
- 📅 **Monthly Summary** – Know your income, expenses, and savings in one glance.
## 📷 Screenshots
<img src="https://github.com/user-attachments/assets/76436c98-19ed-4741-969a-c3964ea0c8e3" alt="WhatsApp Image 2025-09-22 at 12 34 00_e256c417" width="300" />

<img src="https://github.com/user-attachments/assets/740ba339-0a34-473a-9006-6b4fa6a6bd09" alt="WhatsApp Image 2025-09-22 at 12 34 00_84c5e08c" width="300" />

<img src="https://github.com/user-attachments/assets/f6d54c6e-025b-4c73-9695-858b8b706921" alt="WhatsApp Image 2025-09-22 at 12 34 01_7cae8644" width="300" />

<img src="https://github.com/user-attachments/assets/8a4e52af-dab3-4fe4-99f9-7705d6b75298" alt="WhatsApp Image 2025-09-22 at 12 34 01_0e5e9f8f" width="300" />

<img src="https://github.com/user-attachments/assets/7b52e60e-70d4-491e-933d-534c7e971756" alt="WhatsApp Image 2025-09-22 at 12 34 00_dc12c246" width="300" /> 

<img src="https://github.com/user-attachments/assets/e18a5b13-77de-404f-9703-cc7b43deec44" alt="WhatsApp Image 2025-09-22 at 12 34 00_dc12c246" width="300" /> 



## 🛠️ Tech Stack

- **Language:** Kotlin
- **IDE:** Android Studio
- **Database:** Room DB
- **Architecture:** MVVM (Model-View-ViewModel)
- **Libraries:**
  - Retrofit + Coroutines (for network calls if future online sync is added)
  - MPAndroidChart (for graphs)
  - LiveData & ViewModel
  - SMS Retriever API (for parsing SMS)

## 🧱 Architecture Overview

- **Platform**: Native Android (Kotlin), single app module `app`
- **Pattern**: MVVM with Repository and Room persistence
- **Layers**:
  - **View (UI)**: Activities/Fragments under `app/src/main/java/com/example/finsnap/view` using ViewBinding, Navigation, RecyclerView, and Material components.
  - **ViewModel**: `app/src/main/java/com/example/finsnap/viewmodel` contains `FinanceViewModel` orchestrating business logic, `SessionManager` for simple session state, and managers for DB/network.
  - **Data**: `app/src/main/java/com/example/finsnap/model` contains entities and repository; Room (`UserDatabase`, `UsersDao`) is in `viewmodel` package.

## 📈 Graphs Included

- 📉 **Credit vs Debit Chart**
- 🧾 **Expense by Category Chart**

## 📦 Installation

1. Clone this repo:
   ```bash
   git clone https://github.com/yourusername/finsnap.git
