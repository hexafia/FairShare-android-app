# How to Get Your FCM Server Key

Since your Firebase project has FCM V1 enabled but the legacy API disabled, here are two options:

## Option 1: Enable Legacy API (Recommended for this use case)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to Project Settings (gear icon) > Cloud Messaging
4. Find "Cloud Messaging API (Legacy)" 
5. Click the **3-dot menu** and select **"Manage API in Google Cloud Console"**
6. In the Google Cloud Console, **enable** the "Cloud Messaging API (Legacy)"
7. Go back to Firebase Console > Project Settings > Cloud Messaging
8. The **Server Key** should now be visible in the Legacy API section
9. Copy this Server Key and use it in `FCMHelper.java`

## Option 2: Use FCM V1 (More Complex)

If you prefer to use FCM V1, you'll need to:

1. Go to Firebase Console > Project Settings > Service accounts
2. Click "Generate new private key" 
3. Download the JSON file
4. Place it in your app's `assets` folder
5. Update the FCMHelper to use OAuth2 authentication

## Recommendation

For client-to-client messaging (Android app sending directly to other Android apps), **Option 1 is recommended** because:

- Simpler implementation
- Less security overhead
- No need to manage service account keys in the client app
- The current FCMHelper.java is designed for this approach

The legacy API is still fully supported by Firebase and works perfectly for this use case.

## Next Steps

1. Try Option 1 first to get your Server Key
2. Replace `YOUR_SERVER_KEY_HERE` in `FCMHelper.java` 
3. Test the notifications
4. If you prefer FCM V1, let me know and I'll update the implementation
