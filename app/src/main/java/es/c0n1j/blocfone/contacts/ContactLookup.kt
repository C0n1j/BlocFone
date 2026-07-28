package es.c0n1j.blocfone.contacts

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract

class ContactLookup(private val contentResolver: ContentResolver) {
    fun exists(normalizedNumber: String): Boolean {
        val lookupUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalizedNumber),
        )
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)

        val cursor = contentResolver.query(lookupUri, projection, null, null, null)
            ?: error("Contact lookup returned no cursor")
        return cursor.use { it.moveToFirst() }
    }
}
