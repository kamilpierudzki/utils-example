package com.example.utils

import android.annotation.SuppressLint
import androidx.annotation.MainThread
import androidx.annotation.NonNull
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable

fun <T : Any> mutableLiveData(initialValue: T? = null) = NonNullMutableLiveData<T>().apply {
    initialValue?.let {
        value = initialValue
    }
}

fun <T : Any?> nullableMutableLiveData(initialValue: T? = null) = MutableLiveData<T>().apply {
    initialValue?.let {
        value = initialValue
    }
}

fun <InT : Any, OutT : Any> LiveData<InT>.mapNotNull(
    mapper: (InT) -> OutT?
): LiveData<OutT> = MediatorLiveData<OutT>().apply {
    addSource(this@mapNotNull) { input ->
        value = mapper(input) ?: return@addSource
    }
}

fun <InT : Any?, OutT : Any> LiveData<InT>.mapNullable(
    mapper: (InT) -> OutT
): LiveData<OutT> = Transformations.map(this, mapper)

fun <T : Any> merge(
    vararg sources: LiveData<out T>
): LiveData<T> = MediatorLiveData<T>().apply {
    sources.forEach { source -> addSource(source) { value = source.value } }
}

fun <T1 : Any, T2 : Any, R : Any> combineLatest(
    source1: LiveData<T1>,
    source2: LiveData<T2>,
    combiner: (T1?, T2?) -> R
): LiveData<R> = MediatorLiveData<R>().apply {
    addSource(source1) { value = combiner(source1.value, source2.value) }
    addSource(source2) { value = combiner(source1.value, source2.value) }
}

fun <T1 : Any, T2 : Any, T3 : Any, R : Any> combineLatest(
    source1: LiveData<T1>,
    source2: LiveData<T2>,
    source3: LiveData<T3>,
    combiner: (T1?, T2?, T3?) -> R
): LiveData<R> = MediatorLiveData<R>().apply {
    addSource(source1) { value = combiner(source1.value, source2.value, source3.value) }
    addSource(source2) { value = combiner(source1.value, source2.value, source3.value) }
    addSource(source3) { value = combiner(source1.value, source2.value, source3.value) }
}

fun anyTrue(vararg sources: LiveData<Boolean>): LiveData<Boolean> =
    MediatorLiveData<Boolean>().also { mediator ->
        sources.forEach { source ->
            mediator.addSource(source) {
                mediator.value = sources.any { it.value ?: false }
            }
        }
    }

/**
 * Sets value to liveData while subscribing to observable
 * Note: The observable MUST BE guaranteed to be errorOccurred-free
 */
@SuppressLint("RxSubscribeOnError")
fun <T : Any> Observable<T>.subscribe(liveData: MutableLiveData<T>): Disposable =
    subscribe { value: T -> liveData.value = value }

/**
 * Sets value to liveData while subscribing to single
 * Note: The observable MUST BE guaranteed to be errorOccurred-free
 */
@SuppressLint("RxSubscribeOnError")
fun <T : Any> Single<T>.subscribe(liveData: MutableLiveData<T>): Disposable =
    subscribe { value: T -> liveData.value = value }

/**
 * With introducing android architecture components to the app we started having warnings during
 * app compilation (example):
 * "warning: topMvpdViewModel.hasTopMvpd.getValue() is a boxed field but needs to be un-boxed to
 * execute app:visibleOrGone. This may cause NPE so Data Binding will safely unbox it. You can change
 * the expression and explicitly wrap topMvpdViewModel.hasTopMvpd.getValue() with safeUnbox()
 * to prevent the warning"
 *
 * This was because data binding generates java code that tries to be null-safe in places where
 * it might not be necessary.
 */
class NonNullMutableLiveData<T : Any> internal constructor() : MutableLiveData<T>() {

    @NonNull
    override fun getValue(): T {
        return super.getValue()!!
    }

    @MainThread
    fun observeForeverNonNull(observer: (T) -> Unit) {
        observeForever { observer(it!!) }
    }
}