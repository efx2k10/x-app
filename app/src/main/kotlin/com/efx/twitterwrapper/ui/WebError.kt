package com.efx.twitterwrapper.ui

/** Main frame load failure shown by [ErrorOverlay]. */
sealed interface WebError {

    data class Generic(val detail: String?) : WebError

    data class Http(val statusCode: Int) : WebError

    data object Ssl : WebError
}
