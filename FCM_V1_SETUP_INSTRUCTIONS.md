# FCM V1 Implementation for FairShare - Setup Instructions

## Overview
I've updated your FairShare app to use Firebase Cloud Messaging V1 API with service account authentication, which is the current recommended approach. Here's what was implemented and what you need to do to complete the setup.

## What's Been Implemented

### 1. FCM V1 Helper Class
- **FCMV1Helper.java**: Complete helper class using FCM V1 API
- Uses OAuth2 authentication with service account credentials
- Includes specialized methods for nudges and payment confirmations
- Proper error handling and async operations

### 2. Updated Dependencies
- Added Google Auth library for OAuth2 authentication
- Added Google Cloud client libraries
- Updated build configuration

### 3. Integration Points
- **GroupLobbyActivity**: Updated to use FCMV1Helper
- **MainActivity**: Added initialization code for FCM V1
- **MyFirebaseMessagingService**: Still handles incoming messages (no changes needed)

## What You Need to Do

### 1. Get Your Firebase Project ID
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Project Settings** (gear icon) > **General**
4. Copy your **Project ID** (it looks like: `your-project-name-12345`)

### 2. Generate Service Account Key
1. In Firebase Console, go to **Project Settings** > **Service accounts**
2. Click **"Generate new private key"**
3. Select **"Firebase Admin SDK"** as the service account
4. Click **"Generate key"**
5. A JSON file will be downloaded - **keep this secure!**

### 3. Update the Code
You need to update two places in your code:

#### A. Update FCMV1Helper.java
Open `app/src/main/java/com/example/fairshare/FCMV1Helper.java`:
- Replace `your-project-id-here` on line 25 with your actual Project ID

#### B. Update MainActivity.java
Open `app/src/main/java/com/example/fairshare/MainActivity.java`:
- Replace `your-project-id-here` on line 93 with your actual Project ID
- Replace the placeholder service account JSON (lines 97-106) with your actual JSON content

**Example of what to replace in MainActivity.java:**
```java
String serviceAccountJson = "{\n" +
        "  \"type\": \"service_account\",\n" +
        "  \"project_id\": \"your-actual-project-id\",\n" +
        "  \"private_key_id\": \"actual-key-id\",\n" +
        "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nactual-private-key\\n-----END PRIVATE KEY-----\\n\",\n" +
        "  \"client_email\": \"firebase-adminsdk-xxx@your-project-id.iam.gserviceaccount.com\",\n" +
        "  \"client_id\": \"actual-client-id\",\n" +
        "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
        "  \"token_uri\": \"https://oauth2.googleapis.com/token\"\n" +
        "}";
```

### 4. Security Considerations
**IMPORTANT**: The service account JSON contains sensitive credentials. For production:
- Consider storing the JSON in encrypted storage
- Don't commit the JSON to version control
- Consider using a backend server for FCM sending in production

## How It Works

### Authentication Flow
1. FCMV1Helper initializes with service account credentials
2. OAuth2 token is automatically obtained and refreshed
3. FCM V1 API calls are authenticated with the token

### Notification Types
- **Nudge**: "Payment Reminder" notifications with sender and amount details
- **Payment Confirmed**: "Payment Confirmed" notifications with payment details
- **General**: Fallback for other notification types

### API Endpoints
- Uses FCM V1 endpoint: `https://fcm.googleapis.com/v1/projects/{project-id}/messages:send`
- Proper OAuth2 authentication headers
- JSON message payload format

## Testing the Implementation

### 1. Build and Run
```bash
./gradlew build
./gradlew installDebug
```

### 2. Check Logs
Look for these log messages:
- `FCM V1 Helper initialized successfully`
- `FCM V1 notification sent successfully`
- Any authentication errors

### 3. Test Notifications
1. Create a group with expenses
2. Try sending nudges between different users
3. Settle payments and check for confirmation notifications

## Troubleshooting

### Common Issues

#### "FCM V1 Helper not initialized"
- Ensure `initializeFCMV1()` is called in MainActivity
- Check that service account JSON is properly formatted
- Verify Project ID is set correctly

#### Authentication Errors
- Verify service account has proper permissions
- Check that the service account JSON is valid
- Ensure the project ID matches the service account

#### "No FCM token found for user"
- Check that users are logged in
- Verify FCM tokens are being saved to Firestore
- Check LoginActivity token management

#### Build Errors
- Ensure all dependencies are synced
- Check that Google Auth library is properly added
- Verify import statements are correct

### Debug Steps
1. Check Logcat for `FCMV1Helper` messages
2. Verify service account credentials in Google Cloud Console
3. Test with a simple notification first
4. Check Firestore for user FCM tokens

## Files Modified/Created

### New Files
- `app/src/main/java/com/example/fairshare/FCMV1Helper.java` - FCM V1 helper class
- `FCM_V1_SETUP_INSTRUCTIONS.md` - This setup guide

### Modified Files
- `app/src/main/java/com/example/fairshare/ui/groups/GroupLobbyActivity.java` - Updated to use FCMV1Helper
- `app/src/main/java/com/example/fairshare/MainActivity.java` - Added FCM V1 initialization
- `app/build.gradle.kts` - Added Google Auth dependency
- `gradle/libs.versions.toml` - Added dependency versions

### Existing Files Used
- `app/src/main/java/com/example/fairshare/MyFirebaseMessagingService.java` - Handles incoming messages
- `app/src/main/AndroidManifest.xml` - FCM service registration

## Migration from Legacy API

If you were previously using the legacy FCM API:
- The notification sending logic is now handled by FCMV1Helper
- Incoming message handling remains the same
- No changes needed to MyFirebaseMessagingService
- User experience is identical

## Next Steps

1. **Get your Project ID** from Firebase Console
2. **Generate service account key** from Service accounts section
3. **Update the code** with your actual credentials
4. **Test thoroughly** with multiple devices
5. **Consider security** improvements for production deployment

The FCM V1 implementation is more secure and future-proof than the legacy API, providing better authentication and more reliable delivery.
