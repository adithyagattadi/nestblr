package com.example.nestblr.core.util

import com.example.nestblr.BuildConfig

/**
 * Backend returns photo URLs as relative paths like "/uploads/abc.jpg".
 * Prefix with BuildConfig.BASE_URL so Coil can load them.
 * If the URL already has a scheme (http/https), return it unchanged.
 */
fun resolveBackendUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    return BuildConfig.BASE_URL.trimEnd('/') + url
}
