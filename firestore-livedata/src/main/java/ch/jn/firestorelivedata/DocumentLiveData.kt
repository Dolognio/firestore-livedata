package ch.jn.firestorelivedata

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration

fun <T> DocumentReference.livedata(clazz: Class<T>, defaultValue: T?): LiveData<T> {
    return DocumentLiveDataNative(this, clazz, defaultValue)
}

fun <T> DocumentReference.livedata(parser: (documentSnapshot: DocumentSnapshot) -> T, defaultValue: T?): LiveData<T> {
    return DocumentLiveDataCustom(this, parser, defaultValue)
}

fun DocumentReference.livedata(): LiveData<DocumentSnapshot> {
    return DocumentLiveDataRaw(this)
}

private sealed class DocumentLiveDataBasic<T>(
    private val documentReference: DocumentReference,
    private val documentToObject: (documentSnapshot: DocumentSnapshot) -> T,
    defaultValue: T?
) :
    LiveData<T>() {

    private var listener: ListenerRegistration? = null

    init {
        value = defaultValue
    }

    override fun onActive() {
        super.onActive()

        listener = documentReference.addSnapshotListener { documentSnapshot, exception ->
            if (exception == null) {
                documentSnapshot?.let {
                    value = documentToObject.invoke(it)
                }
            } else {
                Log.e("FireStoreLiveData", "", exception)
            }
        }
    }

    override fun onInactive() {
        super.onInactive()

        listener?.remove()
        listener = null
    }
}

private class DocumentLiveDataNative<T>(
    documentReference: DocumentReference,
    clazz: Class<T>,
    defaultValue: T?
) : DocumentLiveDataBasic<T>(
    documentReference = documentReference,
    documentToObject = { documentSnapshot ->
        documentSnapshot.toObject(clazz)!!
    },
    defaultValue = defaultValue
)

private class DocumentLiveDataCustom<T>(
    documentReference: DocumentReference,
    parser: (documentSnapshot: DocumentSnapshot) -> T,
    defaultValue: T?
) : DocumentLiveDataBasic<T>(
    documentReference = documentReference,
    documentToObject = { documentSnapshot ->
        parser.invoke(documentSnapshot)
    },
    defaultValue = defaultValue
)

class DocumentLiveDataRaw(private val documentReference: DocumentReference) :
    LiveData<DocumentSnapshot>() {

    private var listener: ListenerRegistration? = null

    override fun onActive() {
        super.onActive()

        listener = documentReference.addSnapshotListener { documentSnapshot, exception ->
            if (exception == null) {
                value = documentSnapshot
            } else {
                Log.e("FireStoreLiveData", "", exception)
            }
        }
    }

    override fun onInactive() {
        super.onInactive()

        listener?.remove()
        listener = null
    }
}