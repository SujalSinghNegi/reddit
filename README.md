# Reddit Clone - Android Social Platform

![Banner Image][WhatsApp Image 2026-03-08 at 1 11 33 PM](https://github.com/user-attachments/assets/f34c5fe9-648e-47a9-8f5f-4dec5e4b3685) 

A fully functional, high-performance Reddit clone built natively for Android. This project demonstrates modern Android development practices, real-time database synchronization, and complex state management. 

## 🚀 Features

* **Robust Authentication:** Secure email/password login and registration.
* **Real-time Feed:** Scrollable, dynamic feed of user-generated posts ordered by timestamp.
* **Optimistic UI Voting System:** Lightning-fast upvote/downvote interactions. The UI updates instantly while Firestore transactions sync in the background to prevent race conditions and duplicate votes.
* **Media Uploads:** Users can upload images with their posts. Includes background image compression (WebP) to minimize bandwidth and storage costs.
* **Smart Throttling:** Built-in debounce and cooldown logic to prevent database spamming during rapid user interactions.
* **Seamless State Management:** Handles Android lifecycle events smoothly, pausing the task in the background rather than destroying it when navigating away.

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI:** XML / ViewBinding
* **Backend:** Firebase (Authentication, Firestore, Storage)
* **Image Loading:** Glide
* **Image Compression:** Zelory Compressor
* **Asynchronous Programming:** Kotlin Coroutines & Lifecycle Scopes

## 📸 Screenshots

| Feed Layout | Upvote/Downvote Logic | Image Upload |
| :---: | :---: | :---: |
| ![Feed] ![WhatsApp Image 2026-03-08 at 1 11 32 PM (1)](https://github.com/user-attachments/assets/5c310fd7-1766-493c-8333-6dd979b537b0) | ![Voting] ![upvote and downvote](https://github.com/user-attachments/assets/8121abf9-0a2b-4bad-9b1d-fd5ad11ea46d) | ![Upload] ![upload](https://github.com/user-attachments/assets/c406bbae-5a4f-4b06-8c0d-73c50dd2b913)
 |

## 🧠 Architecture Highlights

### Firestore Transactional Voting
To ensure mathematical accuracy when multiple users vote on a viral post simultaneously, this app uses Firebase Transactions. It stores individual votes in a subcollection (`/posts/{postId}/votes/{userId}`) rather than just incrementing a single integer, preventing duplicate voting and race conditions.

### Optimistic UI & Cooldowns
To provide a zero-latency experience, UI elements (like the Reddit Orange upvote arrow) update instantly upon clicking. A 10-second local lockout prevents users from spamming the database, while background coroutines handle the server synchronization.

## 💻 Installation & Setup

To run this project locally, you will need to connect it to your own Firebase project.

1.  Clone the repository:
    ```bash
    git clone [https://github.com/YourUsername/YourRepoName.git](https://github.com/YourUsername/YourRepoName.git)
    ```
2.  Open the project in **Android Studio**.
3.  Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
4.  Add an Android app to the Firebase project and download the `google-services.json` file.
5.  Place the `google-services.json` file into the `app/` directory of this project.
6.  Enable **Authentication** (Email/Password), **Firestore**, and **Storage** in your Firebase console.
7.  Sync the Gradle files and run the app on an emulator or physical device.

## 🔒 Firestore Rules

For the app to function correctly, ensure your Firestore rules are set up to handle the shallow subcollections:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allows public read, authenticated create, and author-only delete
    match /posts/{postId} {
      allow read: if true;
      allow create, update: if request.auth != null;
      allow delete: if request.auth != null && request.auth.uid == resource.data.authorId;
    }
    // Secures individual user votes
    match /posts/{postId}/votes/{voteUserId} {
      allow read: if true;
      allow write: if request.auth != null && request.auth.uid == voteUserId;
    }
  }
}
