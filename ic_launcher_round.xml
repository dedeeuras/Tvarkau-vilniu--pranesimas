package lt.pilietis.greitaspranesimas

import android.content.Context
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/**
 * Nemokamas OpenStreetMap žemėlapis (osmdroid, be API rakto).
 * Naudotojas bakstelėjimu pažymi tikslią vietą.
 *
 * @param initial pradinis centras (jei turim apytikslę vietą), kitaip Vilniaus centras
 * @param onPick grąžina pasirinktas koordinates
 */
@Composable
fun MapPicker(
    initial: Pair<Double, Double>?,
    onPick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth().height(260.dp),
        factory = { context ->
            Configuration.getInstance().load(
                context,
                PreferenceManager.getDefaultSharedPreferences(context)
            )
            Configuration.getInstance().userAgentValue = context.packageName

            MapView(context).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(17.0)

                val start = initial?.let { GeoPoint(it.first, it.second) }
                    ?: GeoPoint(54.6872, 25.2797) // Vilniaus centras
                controller.setCenter(start)

                val marker = Marker(this).apply {
                    position = start
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    isDraggable = true
                }
                overlays.add(marker)

                // Iš karto pasiūlom centrą, jei jį turėjom
                initial?.let { onPick(it.first, it.second) }

                marker.setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                    override fun onMarkerDrag(m: Marker) {}
                    override fun onMarkerDragEnd(m: Marker) {
                        onPick(m.position.latitude, m.position.longitude)
                    }
                    override fun onMarkerDragStart(m: Marker) {}
                })

                val tap = object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                        marker.position = p
                        onPick(p.latitude, p.longitude)
                        invalidate()
                        return true
                    }
                    override fun longPressHelper(p: GeoPoint) = false
                }
                overlays.add(0, MapEventsOverlay(tap))
            }
        },
        onRelease = { it.onDetach() }
    )
}
