package lt.pilietis.greitaspranesimas

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.net.Uri
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class PhotoMeta(
    val lat: Double?,
    val lng: Double?,
    val address: String?,
    val takenAt: java.util.Date?,
    val source: String, // "exif" | "gps" | "none"
)

object PhotoMetaReader {

    /**
     * Pagrindinis įėjimo taškas. Pirmiausia bandome EXIF (tiksliausia — tai vieta,
     * kur nuotrauka buvo padaryta, o ne kur naudotojas yra dabar), po to įrenginio GPS.
     */
    suspend fun read(context: Context, uri: Uri): PhotoMeta = withContext(Dispatchers.IO) {
        val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }

        val exifLatLng = exif?.latLong
        // Nuotraukos darymo laikas - jį siųsim kaip violation_date_time
        val takenAt = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { raw ->
            runCatching {
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(raw)
            }.getOrNull()
        }

        // ExifInterface.latLong gali grąžinti (NaN, NaN), kai GPS blokas yra,
        // bet tuščias (dažna po persiuntimo per pokalbių programėles). Tokį atmetam.
        var lat = exifLatLng?.get(0)?.takeIf { it.isFinite() && it != 0.0 }
        var lng = exifLatLng?.get(1)?.takeIf { it.isFinite() && it != 0.0 }
        var source = if (lat != null) "exif" else "none"

        if (lat == null) {
            currentLocation(context)?.let { (la, ln) ->
                lat = la; lng = ln; source = "gps"
            }
        }

        val address = if (lat != null && lng != null) geocode(context, lat!!, lng!!) else null

        PhotoMeta(lat, lng, address, takenAt, source)
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(context: Context): Pair<Double, Double>? {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null

        return runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val loc = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
            loc?.let { it.latitude to it.longitude }
        }.getOrNull()
    }

    /** Vieša versija žemėlapio parinkikliui. */
    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? =
        withContext(Dispatchers.IO) { geocode(context, lat, lng) }

    @Suppress("DEPRECATION") // sinchroninė versija veikia visose API versijose
    private fun geocode(context: Context, lat: Double, lng: Double): String? = runCatching {
        Geocoder(context, Locale("lt", "LT"))
            .getFromLocation(lat, lng, 1)
            ?.firstOrNull()
            ?.let { a ->
                listOfNotNull(a.thoroughfare, a.subThoroughfare)
                    .joinToString(" ")
                    .ifBlank { a.getAddressLine(0) }
            }
    }.getOrNull()

    /** Adresas -> koordinatės. Naudojama, kai nuotraukoje nėra GPS. */
    @Suppress("DEPRECATION")
    suspend fun forwardGeocode(context: Context, query: String): Triple<Double, Double, String>? =
        withContext(Dispatchers.IO) {
            runCatching {
                Geocoder(context, Locale("lt", "LT"))
                    .getFromLocationName("$query, Vilnius, Lietuva", 1)
                    ?.firstOrNull()
                    ?.let { a ->
                        val label = listOfNotNull(a.thoroughfare, a.subThoroughfare)
                            .joinToString(" ").ifBlank { a.getAddressLine(0) ?: query }
                        Triple(a.latitude, a.longitude, label)
                    }
            }.getOrNull()
        }

    /**
     * Sumažina nuotrauką prieš siunčiant į vaizdo modelį.
     * 1024 px ilgesniajai kraštinei — daugiau nieko nepagerina, tik brangina užklausą.
     */
    fun downscaleToBase64(context: Context, uri: Uri, maxSide: Int = 1024): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide * 2) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp: Bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val scale = maxSide.toFloat() / maxOf(bmp.width, bmp.height)
        val out = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true
            )
        } else bmp

        val stream = ByteArrayOutputStream()
        out.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
