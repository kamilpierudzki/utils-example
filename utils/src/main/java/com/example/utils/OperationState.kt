package com.example.utils

sealed class OperationState<out DataT : Any, out ErrorT : Any> {

    val inProgress get() = this is InProgress
    val idle get() = this is Idle
    val success get() = this is Success
    val error get() = this is Error
    val successData get() = (this as? Success)?.data

    object Idle : OperationState<Nothing, Nothing>()

    object InProgress : OperationState<Nothing, Nothing>()

    data class Success<out DataT : Any>(val data: DataT) :
        OperationState<DataT, Nothing>()

    data class Error<out ErrorT : Any>(val errorData: ErrorT) :
        OperationState<Nothing, ErrorT>()

    fun toOperationResult(): OperationResult<DataT, ErrorT>? = when (this) {
        is Success -> OperationResult.Success(data)
        is Error -> OperationResult.Error(errorData)
        is InProgress -> null
        is Idle -> null
    }

    fun <OutDataT : Any> mapSuccess(
        successMapper: (DataT) -> OutDataT
    ): OperationState<OutDataT, ErrorT> = when (this) {
        is Success -> Success(successMapper(data))
        is Error -> Error(errorData)
        is InProgress -> OperationState.InProgress
        is Idle -> OperationState.Idle
    }

    fun doOnSuccess(onSuccess: (OperationState.Success<DataT>) -> Unit) =
        this.also { if (it is OperationState.Success) onSuccess(it) }

    fun doOnError(onError: (OperationState.Error<ErrorT>) -> Unit) =
        this.also { if (it is OperationState.Error) onError(it) }

    fun doOnProgress(onProgress: (OperationState.InProgress) -> Unit) =
        this.also { if (it is OperationState.InProgress) onProgress(it) }
}