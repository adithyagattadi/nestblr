package com.example.nestblr.feature.search

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.nestblr.domain.model.ListingSummary
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Compose wrapper around osmdroid's MapView showing OpenStreetMap tiles.
 * Initial centre is set once via the factory; the update block re-syncs
 * markers when [listings] changes. The map recenters only when
 * [centerLat]/[centerLng] actually change (e.g. the tenant picks a new
 * locality) — not on every marker refresh, so user pans aren't reset.
 */
@Composable
fun NestBlrMap(
    centerLat: Double,
    centerLng: Double,
    listings: List<ListingSummary>,
    onMarkerClick: (String) -> Unit,
    showUserLocation: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    // Tracks the last center we animated to. Seeded with the initial center
    // (set by the factory) so the first update doesn't re-animate.
    val lastCenter = remember { mutableStateOf(GeoPoint(centerLat, centerLng)) }

    // osmdroid needs explicit onResume/onPause hooks — without these, tiles
    // silently stop loading after rotation/foregrounding. Single most common
    // gotcha with this library.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.overlays.clear()
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(14.0)
                controller.setCenter(GeoPoint(centerLat, centerLng))
            }
        },
        update = { mv ->
            // Recenter only when the center actually changed (locality switch),
            // not on every marker refresh — otherwise user pans would reset.
            val target = GeoPoint(centerLat, centerLng)
            if (lastCenter.value != target) {
                mv.controller.animateTo(target)
                lastCenter.value = target
            }

            // Refresh only marker overlays — leaves user pan/zoom untouched.
            mv.overlays.removeAll { it is Marker }
            listings.forEach { listing ->
                val marker = Marker(mv).apply {
                    position = GeoPoint(listing.latitude, listing.longitude)
                    title = listing.title
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        onMarkerClick(listing.id)
                        true
                    }
                }
                mv.overlays.add(marker)
            }

            // "You are here" dot — added LAST so it draws on top of listing
            // markers. The removeAll above also clears this each cycle, so it
            // never stacks. Anchored center-center and non-tappable.
            if (showUserLocation) {
                val userMarker = Marker(mv).apply {
                    position = GeoPoint(centerLat, centerLng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = createUserLocationDrawable(context)
                    title = null
                    setOnMarkerClickListener { _, _ -> false }
                }
                mv.overlays.add(userMarker)
            }
            mv.invalidate()
        }
    )
}

/**
 * A static "you are here" marker icon, drawn programmatically (no XML/PNG):
 * a coral filled dot inside a white ring for contrast against any tile color.
 */
private fun createUserLocationDrawable(context: Context): Drawable {
    val sizeDp = 18 // overall diameter in dp
    val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // White outer ring
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx / 2f, ringPaint)

    // Coral inner dot (Rose600 / #E11D48)
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#E11D48")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, sizePx / 2f * 0.7f, dotPaint)

    return BitmapDrawable(context.resources, bitmap)
}
