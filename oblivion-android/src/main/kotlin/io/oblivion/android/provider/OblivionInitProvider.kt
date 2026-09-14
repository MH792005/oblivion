package io.oblivion.android.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import io.oblivion.android.Oblivion

/**
 * Early initializer for Oblivion.
 * Runs during application process bootstrap before [android.app.Application.onCreate]
 * to establish security controls as early as possible.
 */
class OblivionInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let { ctx ->
            Oblivion.init(ctx)
        }
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
