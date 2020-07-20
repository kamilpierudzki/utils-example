package com.example.utils

import android.os.Parcel
import android.os.Parcelable

sealed class OperationResult<out DataT : Any, out ErrorT : Any> : Parcelable {

    val success get() = this is Success
    val error get() = this is Error
    val successData get() = (this as? Success)?.data

    data class Success<out DataT : Any> internal constructor(
        val data: DataT
    ) : OperationResult<DataT, Nothing>() {

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeValue(data)
        }

        override fun describeContents(): Int = 0

        internal companion object CREATOR : Parcelable.Creator<Success<Any>> {
            override fun createFromParcel(parcel: Parcel): Success<Any> = Success(parcel.read())

            override fun newArray(size: Int): Array<Success<Any>?> = arrayOfNulls(size)
        }
    }

    data class Error<ErrorT : Any> internal constructor(
        val errorData: ErrorT
    ) : OperationResult<Nothing, ErrorT>() {

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeValue(errorData)
        }

        override fun describeContents(): Int = 0

        internal companion object CREATOR : Parcelable.Creator<Error<Any>> {
            override fun createFromParcel(parcel: Parcel): Error<Any> = Error(parcel.read())

            override fun newArray(size: Int): Array<Error<Any>?> = arrayOfNulls(size)
        }
    }

    fun toOperationState(): OperationState<DataT, ErrorT> = when (this) {
        is Success -> OperationState.Success(data)
        is Error -> OperationState.Error(errorData)
    }

    fun <OutDataT : Any> mapSuccess(
        successMapper: (DataT) -> OutDataT
    ): OperationResult<OutDataT, ErrorT> = when (this) {
        is Success -> successMapper(data).toOperationSuccess()
        is Error -> errorData.toOperationError()
    }

    fun <OutErrorT : Any> mapError(
        errorMapper: (ErrorT) -> OutErrorT
    ): OperationResult<DataT, OutErrorT> = when (this) {
        is Success -> data.toOperationSuccess()
        is Error -> errorMapper(errorData).toOperationError()
    }

    fun doOnSuccess(onSuccess: (DataT) -> Unit): OperationResult<DataT, ErrorT> =
        this.also { if (it is Success) onSuccess.invoke(it.data) }

    fun doOnError(onError: (ErrorT) -> Unit): OperationResult<DataT, ErrorT> =
        this.also { if (it is Error) onError.invoke(it.errorData) }
}

fun <DataT : Any, ErrorT : Any> DataT.toOperationSuccess(): OperationResult<DataT, ErrorT> =
    OperationResult.Success(this)

fun <DataT : Any, ErrorT : Any> ErrorT.toOperationError(): OperationResult<DataT, ErrorT> =
    OperationResult.Error(this)

private inline fun <reified T> Parcel.read(): T = readValue(T::class.javaClass.classLoader) as T