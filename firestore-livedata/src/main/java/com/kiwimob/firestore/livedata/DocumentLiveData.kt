package com.kiwimob.firestore.livedata

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration

fun <T> DocumentReference.livedata(clazz: Class<T>): LiveData<T> {
    return DocumentLiveDataNative(this, clazz)
}

fun <T> DocumentReference.livedata(parser: (documentSnapshot: DocumentSnapshot) -> T): LiveData<T> {
    return DocumentLiveDataCustom(this, parser)
}

fun DocumentReference.livedata(): LiveData<DocumentSnapshot> {
    return DocumentLiveDataRaw(this)
}

private sealed class DocumentLiveDataBasic<T>(
    private val documentReference: DocumentReference,
    private val documentToObject: (documentSnapshot: DocumentSnapshot) -> T
) :
    LiveData<T>() {

    private var listener: ListenerRegistration? = null

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
    private val clazz: Class<T>
) : DocumentLiveDataBasic<T>(
    documentReference = documentReference,
    documentToObject = { documentSnapshot ->
        documentSnapshot.toObject(clazz)!!
    }
)

private class DocumentLiveDataCustom<T>(
    documentReference: DocumentReference,
    private val parser: (documentSnapshot: DocumentSnapshot) -> T
) : DocumentLiveDataBasic<T>(
    documentReference = documentReference,
    documentToObject = { documentSnapshot ->
        parser.invoke(documentSnapshot)
    }
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