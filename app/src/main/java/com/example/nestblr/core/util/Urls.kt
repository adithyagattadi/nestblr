package com.example.nestblr.core.util

import com.example.nestblr.BuildConfig

/**
 * Resolves a photo URL from the backend to a full URL usable by Coil.
 *
 * Handles three cases:
 * - null / blank: return null (upstream shows a placeholder)
 * - absolute URL (starts with http:// or https://): return as-is.
 *   Used by Supabase Storage photos in production:
 *   "https://<project>.supabase.co/storage/v1/object/public/listing-photos/abc.jpg"
 * - relative URL (starts with /): prepend BASE_URL.
 *   Used by legacy local-disk photos: "/uploads/abc.jpg"
 */
fun resolveBackendUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    return BuildConfig.BASE_URL.trimEnd('/') + url
}
