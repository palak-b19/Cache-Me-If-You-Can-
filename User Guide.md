# USER GUIDE — Permissions Manager App

## 1. Introduction
The **Permissions Manager App** helps users understand, evaluate, and control the permissions requested by applications on their device. Modern apps often request sensitive access such as location, camera, microphone, contacts, or files—permissions users often accept without fully knowing the risks.

This app solves that problem by:

- Scanning app permissions  
- Explaining why an app may need them  
- Generating a **risk score** using a Large Language Model (LLM)  
- Providing actionable recommendations to improve privacy and security  

This guide explains how to use the app effectively.

---

## 2. Installation & Setup

### 2.1 Requirements
- Android device  
- Internet connection (required for LLM-based scoring)

### 2.2 Installation
1. Install the app using the provided APK.  
2. On first launch, the app will request:  
   - Permission to list installed apps  
   - Permission to read app permissions (system-level)  
3. Grant these permissions to enable full functionality.

---

## 3. Home Screen Overview
Upon opening the app, the Home Screen displays:

### a. Search and Filter Tools
- A **search bar** to quickly find any installed app.

### b. App List
A scrollable list of all installed applications.  
After clicking on an app, it typically shows:
- App name  
- Icon  
- Short summary of permission status  
- Risk score and analysis  

### c. Theme Setting
- Option to switch between **Dark mode** and **Light mode**.

---

## 4. App Details Screen
Selecting any app opens its details view, which includes:

### 4.1 Permissions Breakdown
A list of all permissions requested by the selected app.

(Not grouped here manually, because the LLM-based analysis already gives category-wise distinction.)

#### High-sensitivity permissions:
- Camera  
- Microphone  
- Location  
- SMS / Call logs  
- Contacts  
- Storage access  

#### Moderate permissions:
- Bluetooth  
- Wi-Fi  
- Background activity  

#### Low-sensitivity permissions:
- Vibration  
- Notifications  
- Network access (basic)

### 4.2 LLM Powered Risk Scoring
Explained in detail in the next section.

---

## 5. LLM-Powered Risk Scoring
This is the core feature of the app.

### 5.1 What the LLM Does
The Large Language Model analyzes:

- App category  
- Types of permissions  
- Number of sensitive permissions  
- Permission combinations (e.g., camera + microphone + location)  
- Known risk patterns  
- User-provided contextual information  

### 5.2 Understanding the Risk Score
The LLM generates:
- A **Risk Score** (0–100)  
- A **Risk Level Category**:
  - **0–30 → Low Risk**  
  - **31–60 → Medium Risk**  
  - **61–100 → High Risk**

### 5.3 Explanation Section
The LLM provides a human-readable explanation, including:

- Why the permission set is risky  
- Whether the permissions are normal for this app category  
- Confidence score (how confident the model is)  
- Source used to evaluate the risk  

**Example output:**

> “The risk score is 61/100 — High risk.  
> Permissions like Precise Location and Microphone are sensitive.  
> Microphone can record live conversations, and precise location can leak user location.”

---

## 6. Recommendations Screen
After generating the risk score, the app provides a **Recommendations Panel**, including:

- Permissions you should consider revoking  
- Permissions that are not essential  
- Suggested safer alternatives (if any)  
- Category-specific advice (e.g., *Messaging apps usually need contacts, but games do not.*)

---

## 7. Managing Permissions — “Manage Permissions”
You can directly control permissions from the app.  
This option opens your device’s built-in permissions manager, where you can edit permissions and then return to the app to check the updated risk score.

### 7.1 Revoking a Permission
- Tap the permission you want to revoke.  
- Tap **Deny**, **Don’t Allow**, or toggle it off.

### 7.2 Granting a Permission
- Tap **Enable Permission**.  
- Tap **Allow**.

### 7.3 Reset to Default
Resets all permissions to your device’s recommended defaults.

---

## 8. Troubleshooting

### Risk Score Not Loading
- Check your internet connection.  
- Retry scanning.  
- Ensure the app has permission to read installed apps.

### App Not Appearing in List
- Restart the Permissions Manager app.  
- Trigger a re-scan.

### LLM Error
- This may occur due to temporary service issues.  
- Retry after a few minutes.

---

## 9. Conclusion
The Permissions Manager app empowers users to make informed decisions about their digital privacy. By combining transparent permission reporting with an LLM-driven risk analysis engine, it helps users understand exactly what apps are doing and whether those actions align with security expectations.

---

