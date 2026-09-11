package com.skedgo.tripkit.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuff.Mode.SRC_IN
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.LruCache
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.skedgo.tripkit.common.model.stop.ScheduledStop
import com.skedgo.tripkit.common.model.stop.StopType
import com.skedgo.tripkit.routing.ModeInfo
import com.skedgo.tripkit.ui.BuildConfig
import com.skedgo.tripkit.ui.utils.BindingConversions
import com.skedgo.tripkit.ui.utils.DeviceInfo
import com.skedgo.tripkit.ui.utils.StopMarkerUtils.getLocalMapIconUrlForModeInfo
import com.skedgo.tripkit.ui.utils.StopMarkerUtils.getRemoteMapIconUrlForModeInfo
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.lang.ref.WeakReference
import javax.inject.Inject

class RemoteMarkerIconFetcher @Inject constructor(
    private val picasso: Picasso
) {

    companion object {
        const val SIZE_CIRCULAR_BITMAP = 30
        const val TINT_BITMAP_RGB = 255

        private const val DESCRIPTOR_CACHE_MAX_ENTRIES = 200
        private const val CACHE_SOURCE_REMOTE = "remote"
        private const val CACHE_SOURCE_RESOURCE = "resource"
        private const val CACHE_SOURCE_DEFAULT = "default"
        private val descriptorCacheLock = Any()
        private val descriptorCache =
            LruCache<MarkerIconDescriptorCacheKey, BitmapDescriptor>(DESCRIPTOR_CACHE_MAX_ENTRIES)

        // Coalesces concurrent requests for the same icon so a burst of markers
        // sharing an icon (e.g. many-segment routes) decodes the bitmap once
        // instead of once per marker, avoiding the allocation spike that triggers
        // blocking-GC ANRs / OOM.
        private val inFlightLock = Any()
        private val inFlightRequests =
            HashMap<MarkerIconDescriptorCacheKey, Single<BitmapDescriptor>>()
        private val inFlightTargets =
            HashMap<MarkerIconDescriptorCacheKey, Target>()
    }

    fun call(markerOptions: MarkerOptions, modeInfo: ModeInfo?) {
        modeInfo?.let {
            val url = getRemoteMapIconUrlForModeInfo(DeviceInfo.getDensityDpiName(), it)
            picasso.load(url).into(MarkerOptionsTarget(WeakReference(markerOptions)))
        }
    }

    fun callAsync(markerOptions: MarkerOptions, stop: ScheduledStop): Single<MarkerOptions> {
        val modeInfo = stop.modeInfo
        val densityDpiName = DeviceInfo.getDensityDpiName()
        val iconUrl =
            if(modeInfo?.remoteIconIsTemplate == true) {
                getRemoteMapIconUrlForModeInfo(densityDpiName, modeInfo)
            } else {
                getLocalMapIconUrlForModeInfo(densityDpiName, modeInfo)
            }
        val tintColor = Color.rgb(TINT_BITMAP_RGB, TINT_BITMAP_RGB, TINT_BITMAP_RGB)
        val circleColor = Color.rgb(
            modeInfo?.getServiceColor()?.red ?: 0,
            modeInfo?.getServiceColor()?.green ?: 0,
            modeInfo?.getServiceColor()?.blue ?: 0
        )
        val cacheKey = MarkerIconDescriptorCacheKey(
            source = CACHE_SOURCE_REMOTE,
            sourceId = iconUrl.orEmpty(),
            densityDpiName = densityDpiName,
            circleRadius = SIZE_CIRCULAR_BITMAP,
            tintColor = tintColor,
            circleColor = circleColor
        )
        TripGoMapMarkerDiag.recordIconFetch()
        return Single.defer {
            getCachedDescriptor(cacheKey)?.let { cachedIcon ->
                TripGoMapMarkerDiag.recordBitmapDescriptorCacheHit(descriptorCacheSize())
                markerOptions.icon(cachedIcon)
                return@defer Single.just(markerOptions)
            }

            TripGoMapMarkerDiag.recordBitmapDescriptorCacheMiss(descriptorCacheSize())
            loadRemoteDescriptor(cacheKey, iconUrl, tintColor, circleColor)
                .map { icon ->
                    markerOptions.icon(icon)
                    markerOptions
                }
                .onErrorResumeNext {
                    // Fallback to local resource-based marker icon, drawn in the current
                    // circular style so it stays visually consistent (#25936).
                    getMapIconFromResource(
                        markerOptions,
                        stop.type,
                        densityDpiName,
                        tintColor,
                        circleColor
                    )
                }
        }.subscribeOn(AndroidSchedulers.mainThread())
    }

    /**
     * Returns a shared [Single] that decodes the remote icon and builds a
     * [BitmapDescriptor] exactly once per [cacheKey]. Concurrent callers reuse the
     * same in-flight request (or the cached descriptor), so a burst of markers
     * sharing an icon no longer spawns one Picasso decode + multiple bitmap
     * allocations per marker.
     */
    private fun loadRemoteDescriptor(
        cacheKey: MarkerIconDescriptorCacheKey,
        iconUrl: String?,
        tintColor: Int,
        circleColor: Int
    ): Single<BitmapDescriptor> = loadCircularDescriptor(
        cacheKey = cacheKey,
        tintColor = tintColor,
        circleColor = circleColor,
        picassoRequest = { picasso.load(iconUrl) }
    )

    /**
     * Builds the current-style circular marker descriptor from whatever Picasso request
     * [picassoRequest] supplies - a remote icon URL, or a bundled drawable when the remote icon is
     * unavailable. Both go through [createCircularMarkerBitmap] so a fallback marker keeps the
     * same size, border and service colour as every other marker (#25936).
     */
    private fun loadCircularDescriptor(
        cacheKey: MarkerIconDescriptorCacheKey,
        tintColor: Int,
        circleColor: Int,
        picassoRequest: () -> com.squareup.picasso.RequestCreator
    ): Single<BitmapDescriptor> {
        synchronized(inFlightLock) {
            getCachedDescriptor(cacheKey)?.let { return Single.just(it) }
            inFlightRequests[cacheKey]?.let { return it }

            val request = Single.create<BitmapDescriptor> { emitter ->
                val target = object : Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        bitmap?.let {
                            getCachedDescriptor(cacheKey)?.let { cachedIcon ->
                                TripGoMapMarkerDiag.recordBitmapDescriptorCacheHit(descriptorCacheSize())
                                emitter.onSuccess(cachedIcon)
                                return
                            }

                            val markerBitmap = createCircularMarkerBitmap(
                                it,
                                tintColor,
                                circleColor,
                                SIZE_CIRCULAR_BITMAP
                            )

                            val icon = BitmapDescriptorFactory.fromBitmap(markerBitmap.bitmap)
                            TripGoMapMarkerDiag.recordBitmapDescriptorFromBitmap()
                            putCachedDescriptor(cacheKey, icon)
                            markerBitmap.recycleOwnedBitmaps()
                            emitter.onSuccess(icon)
                        } ?: run {
                            emitter.onError(Throwable("Bitmap is null"))
                        }
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        if(BuildConfig.DEBUG) {
                            e?.printStackTrace()
                        }
                        emitter.onError(e ?: Throwable("Bitmap failed to load"))
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        // Placeholder if needed
                    }
                }
                // Picasso keeps Target instances weakly. Retain this target until the
                // shared request terminates so its callback can complete the Single.
                synchronized(inFlightLock) {
                    inFlightTargets[cacheKey] = target
                }
                picassoRequest().into(target)
            }
                .doFinally {
                    synchronized(inFlightLock) {
                        inFlightRequests.remove(cacheKey)
                        inFlightTargets.remove(cacheKey)
                    }
                }
                .subscribeOn(AndroidSchedulers.mainThread())
                .cache()

            inFlightRequests[cacheKey] = request
            return request
        }
    }

    /**
     * Fallback used when the remote mode icon cannot be loaded.
     *
     * The bundled drawable is rendered through the same circular-marker pipeline as the remote
     * icon, so a stop whose icon failed to load still looks like every other marker - same
     * size, same white border, same service colour - instead of reverting to the obsolete flat
     * pin (#25936). Only if even the bundled glyph cannot be drawn do we fall back further.
     */
    private fun getMapIconFromResource(
        markerOptions: MarkerOptions,
        type: StopType?,
        densityDpiName: String,
        tintColor: Int,
        circleColor: Int
    ): Single<MarkerOptions> {
        val iconRes = BindingConversions.convertStopTypeToMapIconRes(type)
        if (iconRes != 0) {
            val circularKey = MarkerIconDescriptorCacheKey(
                source = CACHE_SOURCE_RESOURCE,
                sourceId = "circular:$iconRes",
                densityDpiName = densityDpiName,
                circleRadius = SIZE_CIRCULAR_BITMAP,
                tintColor = tintColor,
                circleColor = circleColor
            )
            getCachedDescriptor(circularKey)?.let { cachedIcon ->
                markerOptions.icon(cachedIcon)
                return Single.just(markerOptions)
            }
            return loadCircularDescriptor(
                cacheKey = circularKey,
                tintColor = tintColor,
                circleColor = circleColor,
                picassoRequest = { picasso.load(iconRes) }
            )
                .map { icon ->
                    markerOptions.icon(icon)
                    markerOptions
                }
                .onErrorResumeNext { rawResourceIcon(markerOptions, iconRes, densityDpiName) }
        }
        return rawResourceIcon(markerOptions, iconRes, densityDpiName)
    }

    private fun rawResourceIcon(
        markerOptions: MarkerOptions,
        iconRes: Int,
        densityDpiName: String
    ): Single<MarkerOptions> {
        return Single.fromCallable {
            val cacheKey = MarkerIconDescriptorCacheKey(
                source = if (iconRes == 0) CACHE_SOURCE_DEFAULT else CACHE_SOURCE_RESOURCE,
                sourceId = iconRes.toString(),
                densityDpiName = densityDpiName,
                circleRadius = 0,
                tintColor = 0,
                circleColor = 0
            )
            getCachedDescriptor(cacheKey)?.let { cachedIcon ->
                TripGoMapMarkerDiag.recordBitmapDescriptorCacheHit(descriptorCacheSize())
                markerOptions.icon(cachedIcon)
                return@fromCallable markerOptions
            }

            TripGoMapMarkerDiag.recordBitmapDescriptorCacheMiss(descriptorCacheSize())
            val icon: BitmapDescriptor = if (iconRes == 0) {
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            } else {
                BitmapDescriptorFactory.fromResource(iconRes)
            }
            putCachedDescriptor(cacheKey, icon)
            markerOptions.icon(icon)
            markerOptions
        }
    }

    private fun createCircularMarkerBitmap(
        bitmap: Bitmap,
        tintColor: Int,
        circleColor: Int,
        circleRadius: Int
    ): CreatedMarkerBitmap {
        // Create a new bitmap for the output
        val output = Bitmap.createBitmap(circleRadius * 2, circleRadius * 2, Bitmap.Config.ARGB_8888)
        TripGoMapMarkerDiag.recordBitmapCreation()
        val canvas = Canvas(output)

        // Draw the white border
        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = circleRadius * 0.1f // Border thickness is 10% of the radius
        }
        canvas.drawCircle(circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat() - (borderPaint.strokeWidth / 2), borderPaint)

        // Draw the circular background inside the border
        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            color = circleColor
        }
        canvas.drawCircle(circleRadius.toFloat(), circleRadius.toFloat(), circleRadius.toFloat() - borderPaint.strokeWidth, backgroundPaint)

        // Apply tint to the bitmap
        val tintedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        TripGoMapMarkerDiag.recordBitmapCreation(scaleOrCopy = 1)
        val bitmapCanvas = Canvas(tintedBitmap)
        val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        }
        bitmapCanvas.drawBitmap(tintedBitmap, 0f, 0f, tintPaint)

        // Scale the bitmap to fit inside the circle while maintaining the aspect ratio
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (aspectRatio > 1) {
            // Landscape orientation: Width is greater than height
            targetWidth = circleRadius * 2
            targetHeight = (targetWidth / aspectRatio).toInt()
        } else if(aspectRatio == 1f) {
            targetHeight = circleRadius
            targetWidth = circleRadius
        } else {
            // Portrait orientation: Height is greater than or equal to width
            targetHeight = circleRadius * 2
            targetWidth = (targetHeight * aspectRatio).toInt()
        }

        val scaledBitmap = Bitmap.createScaledBitmap(
            tintedBitmap,
            targetWidth,
            targetHeight,
            true
        )
        TripGoMapMarkerDiag.recordBitmapCreation(scaleOrCopy = 1)

        // Draw the scaled bitmap at the center of the circular background
        val left = (output.width - scaledBitmap.width) / 2f
        val top = (output.height - scaledBitmap.height) / 2f
        canvas.drawBitmap(scaledBitmap, left, top, null)

        return CreatedMarkerBitmap(
            bitmap = output,
            sourceBitmap = bitmap,
            tintedBitmap = tintedBitmap,
            scaledBitmap = scaledBitmap
        )
    }

    private fun getCachedDescriptor(key: MarkerIconDescriptorCacheKey): BitmapDescriptor? =
        synchronized(descriptorCacheLock) {
            descriptorCache.get(key)
        }

    private fun putCachedDescriptor(key: MarkerIconDescriptorCacheKey, descriptor: BitmapDescriptor) {
        synchronized(descriptorCacheLock) {
            descriptorCache.put(key, descriptor)
        }
    }

    private fun descriptorCacheSize(): Int =
        synchronized(descriptorCacheLock) {
            descriptorCache.size()
        }

    private data class CreatedMarkerBitmap(
        val bitmap: Bitmap,
        val sourceBitmap: Bitmap,
        val tintedBitmap: Bitmap,
        val scaledBitmap: Bitmap
    ) {
        fun recycleOwnedBitmaps() {
            // Picasso owns the source bitmap. Only recycle bitmaps allocated in this class.
            recycleIfOwned(scaledBitmap)
            recycleIfOwned(tintedBitmap)
            recycleIfOwned(bitmap)
        }

        private fun recycleIfOwned(candidate: Bitmap) {
            if (
                candidate !== sourceBitmap &&
                !candidate.isRecycled
            ) {
                candidate.recycle()
                TripGoMapMarkerDiag.recordBitmapRecycle()
            }
        }
    }

    internal data class MarkerIconDescriptorCacheKey(
        val source: String,
        val sourceId: String,
        val densityDpiName: String,
        val circleRadius: Int,
        val tintColor: Int,
        val circleColor: Int
    )
}
