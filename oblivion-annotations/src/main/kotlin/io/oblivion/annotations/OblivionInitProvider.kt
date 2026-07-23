package io.oblivion.annotations

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import io.oblivion.runtime.OblivionCore

/**
 * Auto-initialization ContentProvider for Oblivion.
 * Runs prior to Application.onCreate() / Activity.onCreate() to ensure native security
 * daemons and libraries are loaded even if the target app has no custom Application class.
 */
class OblivionInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        try {
            OblivionCore.init()
        } catch (e: Throwable) {
            // Silently handle load failure in non-supported environments
        }
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
