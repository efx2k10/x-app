package com.efx.twitterwrapper.core.update

internal class UpdateException(
    val kind: UpdateState.Error.Kind,
    val detail: String? = null,
    cause: Throwable? = null,
) : Exception(detail ?: kind.name, cause)
