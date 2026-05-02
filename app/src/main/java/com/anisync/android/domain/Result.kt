package com.anisync.android.domain

/**
 * A wrapper for operation results that can be either Success or Error.
 * Used throughout the data layer to provide explicit error handling.
 */
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val message: String, val code: Int? = null, val countdownSeconds: Long? = null, val exception: Throwable? = null) : Result<Nothing>
}

/**
 * Folds a Result into a single value by applying the appropriate function.
 */
inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onError: (String) -> R
): R = when (this) {
    is Result.Success -> onSuccess(data)
    is Result.Error -> onError(message)
}

/**
 * Maps a successful result to a new type.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
}

/**
 * Returns the data if successful, or null if error.
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    is Result.Error -> null
}

/**
 * Returns the data if successful, or the default value if error.
 */
fun <T> Result<T>.getOrDefault(default: T): T = when (this) {
    is Result.Success -> data
    is Result.Error -> default
}

/**
 * Performs the given action on the data if this is a Success.
 * Returns the original Result unchanged.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/**
 * Performs the given action on the error if this is an Error.
 * Returns the original Result unchanged.
 */
inline fun <T> Result<T>.onError(action: (message: String, exception: Throwable?) -> Unit): Result<T> {
    if (this is Result.Error) action(message, exception)
    return this
}

/**
 * FlatMaps a successful result to another Result.
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
    is Result.Success -> transform(data)
    is Result.Error -> this
}

/**
 * Returns true if this is a Success.
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Returns true if this is an Error.
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error
