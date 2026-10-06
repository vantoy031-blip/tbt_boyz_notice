package com.example.data.firebase

/**
 * ============================================================================
 * TBT NOTICE BOARD - FIREBASE CONFIGURATION & ARCHITECTURE MODULE
 * ============================================================================
 * 
 * Requirement 18:
 * Clearly separate:
 * - Firebase configuration (FirebaseConfig.kt)
 * - Firestore functions & collection schema ("notices")
 * - Authentication functions (Admin email/password)
 * 
 * Instructions:
 * 1. To connect to an external live Firebase project:
 *    - Place google-services.json in the /app directory or specify credentials below.
 *    - Firestore collection name: "notices"
 *    - Firestore security rules (see /firestore.rules):
 *        allow read: if resource.data.isArchived == false || request.auth != null;
 *        allow create, update, delete: if request.auth != null;
 * 2. Normal users need NO AUTHENTICATION (read-only access).
 * 3. Admins authenticate via Firebase Auth (email/password) or Admin Console.
 */
object FirebaseConfig {
    const val COLLECTION_NOTICES = "notices"

    // Optional environment/project configuration defaults
    var projectId: String = "tbt-notice-board"
    var isFirestoreSyncEnabled: Boolean = false

    // Field names matching the required Firestore schema
    object Fields {
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val CATEGORY = "category"
        const val IS_IMPORTANT = "isImportant"
        const val IS_PINNED = "isPinned"
        const val IS_ARCHIVED = "isArchived"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val PUBLISHED_AT = "publishedAt"
    }
}
