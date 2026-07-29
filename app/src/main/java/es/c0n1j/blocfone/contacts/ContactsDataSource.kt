package es.c0n1j.blocfone.contacts

import android.content.ContentResolver
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import es.c0n1j.blocfone.data.ContactContribution
import es.c0n1j.blocfone.data.ContactRef
import es.c0n1j.blocfone.data.escapeLikeLiteral
import es.c0n1j.blocfone.domain.PhoneNumberCanonicalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactCandidate(
    val contact: ContactRef,
    val numbers: Set<String>,
)

data class SyncSnapshot(
    val contributions: Set<ContactContribution>,
)

interface ContactsDataSource {
    suspend fun listContacts(query: String): List<ContactCandidate>

    suspend fun resolveAll(refs: Set<ContactRef>): SyncSnapshot
}

class AndroidContactsDataSource(
    private val contentResolver: ContentResolver,
) : ContactsDataSource {
    override suspend fun listContacts(query: String): List<ContactCandidate> = withContext(Dispatchers.IO) {
        val searchQuery = query.trim()
        val selection = buildList {
            add("${Contacts.HAS_PHONE_NUMBER} != 0")
            if (searchQuery.isNotEmpty()) add("${Contacts.DISPLAY_NAME_PRIMARY} LIKE ? ESCAPE '\\'")
        }.joinToString(" AND ")
        val selectionArgs = searchQuery
            .takeIf(String::isNotEmpty)
            ?.let { arrayOf("%${escapeLikeLiteral(it)}%") }
        val contacts = queryContacts(
            uri = Contacts.CONTENT_URI,
            selection = selection,
            selectionArgs = selectionArgs,
            sortOrder = "${Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC",
        )
        val numbersByContactId = queryPhoneNumbers(contacts.mapTo(mutableSetOf(), ContactRef::contactId))

        contacts.map { contact ->
            ContactCandidate(
                contact = contact,
                numbers = numbersByContactId[contact.contactId].orEmpty(),
            )
        }
    }

    override suspend fun resolveAll(refs: Set<ContactRef>): SyncSnapshot = withContext(Dispatchers.IO) {
        val resolvedContacts = refs.mapNotNullTo(mutableSetOf(), ::resolveContact)
        val numbersByContactId = queryPhoneNumbers(
            resolvedContacts.mapTo(mutableSetOf(), ContactRef::contactId),
        )

        SyncSnapshot(
            contributions = resolvedContacts.mapTo(mutableSetOf()) { contact ->
                ContactContribution(
                    contact = contact,
                    numbers = numbersByContactId[contact.contactId].orEmpty(),
                )
            },
        )
    }

    private fun resolveContact(ref: ContactRef): ContactRef? {
        val lookupUri = Contacts.getLookupUri(ref.contactId, ref.lookupKey)
        return queryContacts(
            uri = lookupUri,
            selection = null,
            selectionArgs = null,
            sortOrder = null,
        ).firstOrNull()
    }

    private fun queryContacts(
        uri: android.net.Uri,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): List<ContactRef> {
        val cursor = contentResolver.query(
            uri,
            CONTACT_PROJECTION,
            selection,
            selectionArgs,
            sortOrder,
        ) ?: throw ContactQueryException("Contacts query returned a null cursor")

        cursor.use {
            val idIndex = it.getColumnIndexOrThrow(Contacts._ID)
            val lookupKeyIndex = it.getColumnIndexOrThrow(Contacts.LOOKUP_KEY)
            val displayNameIndex = it.getColumnIndexOrThrow(Contacts.DISPLAY_NAME_PRIMARY)
            return buildList {
                while (it.moveToNext()) {
                    val lookupKey = it.getString(lookupKeyIndex).orEmpty()
                    if (lookupKey.isBlank()) continue
                    add(
                        ContactRef(
                            lookupKey = lookupKey,
                            contactId = it.getLong(idIndex),
                            displayName = it.getString(displayNameIndex).orEmpty(),
                        ),
                    )
                }
            }
        }
    }

    private fun queryPhoneNumbers(contactIds: Set<Long>): Map<Long, Set<String>> {
        if (contactIds.isEmpty()) return emptyMap()

        val numbersByContactId = mutableMapOf<Long, MutableSet<String>>()
        contactIds.chunked(PHONE_QUERY_BATCH_SIZE).forEach { batch ->
            val placeholders = List(batch.size) { "?" }.joinToString(",")
            val cursor = contentResolver.query(
                Phone.CONTENT_URI,
                PHONE_PROJECTION,
                "${Phone.CONTACT_ID} IN ($placeholders)",
                batch.map { it.toString() }.toTypedArray(),
                null,
            ) ?: throw ContactQueryException("Phone query returned a null cursor")

            cursor.use {
                val contactIdIndex = it.getColumnIndexOrThrow(Phone.CONTACT_ID)
                val normalizedNumberIndex = it.getColumnIndexOrThrow(Phone.NORMALIZED_NUMBER)
                val numberIndex = it.getColumnIndexOrThrow(Phone.NUMBER)
                while (it.moveToNext()) {
                    val normalizedNumber = sequenceOf(
                        it.getString(normalizedNumberIndex),
                        it.getString(numberIndex),
                    ).mapNotNull { number -> number?.let(PhoneNumberCanonicalizer::identity) }
                        .firstOrNull()
                        ?: continue
                    numbersByContactId
                        .getOrPut(it.getLong(contactIdIndex), ::mutableSetOf)
                        .add(normalizedNumber)
                }
            }
        }
        return numbersByContactId
    }

    private companion object {
        val CONTACT_PROJECTION = arrayOf(
            Contacts._ID,
            Contacts.LOOKUP_KEY,
            Contacts.DISPLAY_NAME_PRIMARY,
        )
        val PHONE_PROJECTION = arrayOf(
            Phone.CONTACT_ID,
            Phone.NORMALIZED_NUMBER,
            Phone.NUMBER,
        )
        const val PHONE_QUERY_BATCH_SIZE = 500
    }
}

class ContactQueryException(message: String) : IllegalStateException(message)
