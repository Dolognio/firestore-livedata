package ch.jn.firestorelivedata

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot

fun <T> CollectionReference.livedata(clazz: Class<T>, defaultValue: List<T>): LiveData<List<T>> {
    return CollectionLiveDataNative(this, clazz, defaultValue)
}

fun <T> CollectionReference.livedata(parser: (documentSnapshot: DocumentSnapshot) -> T, defaultValue: List<T>): LiveData<List<T>> {
    return CollectionLiveDataCustom(this, parser, defaultValue)
}

fun CollectionReference.livedata(): LiveData<QuerySnapshot> {
    return CollectionLiveDataRaw(this)
}

private sealed class CollectionLiveDataBasic<T>(
    private val collectionReference: CollectionReference,
    private val documentToObject: (documentSnapshot: DocumentSnapshot) -> T,
    defaultValue: List<T>
) :
    LiveData<List<T>>() {

    private var listener: ListenerRegistration? = null

    init {
        value = defaultValue
    }

    override fun onActive() {
        super.onActive()

        if (listener == null) {
            listener = collectionReference.addSnapshotListener { querySnapshot, exception ->
                if (exception == null) {
                    querySnapshot?.let {
                        value = it.documents.map { documentSnapshot ->
                            documentToObject.invoke(documentSnapshot)
                        }
                    }
                } else {
                    Log.e("FireStoreLiveData", "", exception)
                }
            }
        }
    }

    override fun onInactive() {
        super.onInactive()

        listener?.remove()
        listener = null
    }
}

private class CollectionLiveDataNative<T>(
    collectionReference: CollectionReference,
    clazz: Class<T>,
    defaultValue: List<T>
) : CollectionLiveDataBasic<T>(
    collectionReference = collectionReference,
    documentToObject = { documentSnapshot ->
        documentSnapshot.toObject(clazz)!!
    },
    defaultValue = defaultValue
)

private class CollectionLiveDataCustom<T>(
    collectionReference: CollectionReference,
    parser: (documentSnapshot: DocumentSnapshot) -> T,
    defaultValue: List<T>
) : CollectionLiveDataBasic<T>(
    collectionReference = collectionReference,
    documentToObject = { documentSnapshot ->
        parser.invoke(documentSnapshot)
    },
    defaultValue = defaultValue
)

private class CollectionLiveDataRaw(private val collectionReference: CollectionReference) :
    LiveData<QuerySnapshot>() {

    private var listener: ListenerRegistration? = null

    override fun onActive() {
        super.onActive()

        listener = collectionReference.addSnapshotListener { querySnapshot, exception ->
            if (exception == null) {
                value = querySnapshot
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