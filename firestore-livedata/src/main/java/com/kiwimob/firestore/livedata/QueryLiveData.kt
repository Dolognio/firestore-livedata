package com.kiwimob.firestore.livedata

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

fun <T> Query.livedata(clazz: Class<T>): LiveData<List<T>> {
    return QueryLiveDataNative(this, clazz)
}

fun <T> Query.livedata(parser: (documentSnapshot: DocumentSnapshot) -> T): LiveData<List<T>> {
    return QueryLiveDataCustom(this, parser)
}

fun Query.livedata(): LiveData<QuerySnapshot> {
    return QueryLiveDataRaw(this)
}

private sealed class QueryLiveDataBasic<T>(
    private val query: Query,
    private val documentToObject: (documentSnapshot: DocumentSnapshot) -> T
) :
    LiveData<List<T>>() {

    private var listener: ListenerRegistration? = null

    override fun onActive() {
        super.onActive()

        listener = query.addSnapshotListener { querySnapshot, exception ->
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

    override fun onInactive() {
        super.onInactive()

        listener?.remove()
        listener = null
    }
}

private class QueryLiveDataNative<T>(
    query: Query,
    private val clazz: Class<T>
) : QueryLiveDataBasic<T>(
    query = query,
    documentToObject = { documentSnapshot ->
        documentSnapshot.toObject(clazz)!!
    }
)

private class QueryLiveDataCustom<T>(
    query: Query,
    private val parser: (documentSnapshot: DocumentSnapshot) -> T
) : QueryLiveDataBasic<T>(
    query = query,
    documentToObject = { documentSnapshot ->
        parser.invoke(documentSnapshot)
    }
)

private class QueryLiveDataRaw(private val query: Query) : LiveData<QuerySnapshot>() {

    private var listener: ListenerRegistration? = null

    override fun onActive() {
        super.onActive()

        listener = query.addSnapshotListener { querySnapshot, exception ->
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