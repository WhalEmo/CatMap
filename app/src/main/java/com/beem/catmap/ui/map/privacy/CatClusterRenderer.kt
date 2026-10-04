package com.beem.catmap.ui.map.privacy

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.beem.catmap.R
import com.beem.catmap.ui.map.model.CatClusterItem
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import androidx.core.graphics.createBitmap

class CatClusterRenderer(
    context: Context,
    private val map: GoogleMap,
    private val clusterManager: ClusterManager<CatClusterItem>
) : DefaultClusterRenderer<CatClusterItem>(context, map, clusterManager) {

    private val iconCache = mutableMapOf<Int, BitmapDescriptor>()
    private val activeZoneCircles = mutableMapOf<String, Circle>()

    private val brandColor = ContextCompat.getColor(context, R.color.catmap_accent)
    // %22 opaklıkta koruma alanı rengi 55
    private val zoneFillColor = Color.argb(40, Color.red(brandColor), Color.green(brandColor), Color.blue(brandColor))

    var areCatsVisible: Boolean = true
        private set

    init {
        minClusterSize = 2
    }

    override fun shouldRenderAsCluster(cluster: Cluster<CatClusterItem>): Boolean {
        return cluster.size >= minClusterSize
    }

    override fun onBeforeClusterRendered(cluster: Cluster<CatClusterItem>, markerOptions: MarkerOptions) {
        markerOptions.icon(getClusterBadge(cluster.size))
        markerOptions.anchor(0.5f, 0.5f)
        markerOptions.zIndex(60.0f)
        markerOptions.visible(areCatsVisible)
    }

    override fun onClusterUpdated(cluster: Cluster<CatClusterItem>, marker: Marker) {
        marker.setIcon(getClusterBadge(cluster.size))
        marker.isVisible = areCatsVisible
    }

    override fun onBeforeClusterItemRendered(item: CatClusterItem, markerOptions: MarkerOptions) {
        markerOptions.position(item.position)
        markerOptions.icon(getClusterBadge(1))
        markerOptions.anchor(0.5f, 0.5f)
        markerOptions.title(item.name)
        markerOptions.zIndex(55.0f)
        markerOptions.visible(areCatsVisible)
    }

    override fun onClusterItemUpdated(item: CatClusterItem, marker: Marker) {
        marker.isVisible = areCatsVisible
    }



    fun setCatsVisibility(visible: Boolean, clusters: Set<Cluster<CatClusterItem>>) {
        if (areCatsVisible == visible) return
        areCatsVisible = visible

        clusterManager.markerCollection.markers.forEach { it.isVisible = visible }
        clusterManager.clusterMarkerCollection.markers.forEach { it.isVisible = visible }

        if (visible) {
            updateSafetyCircles(clusters)
        } else {
            clearSafetyCircles()
        }
    }

    /**
     * Kümelerin kapsadığı alanlara yarı saydam güvenlik çemberlerini çizer.
     */
    fun updateSafetyCircles(clusters: Set<Cluster<CatClusterItem>>) {
        if (!areCatsVisible) {
            clearSafetyCircles()
            return
        }
        val currentKeys = mutableSetOf<String>()

        for (cluster in clusters) {
            if (cluster.size < 2) continue
            val key = "${cluster.position.latitude}_${cluster.position.longitude}_${cluster.size}"
            currentKeys.add(key)

            if (!activeZoneCircles.containsKey(key)) {
                val points = cluster.items.map { it.position }
                val (center, radius) = CatPrivacyEngine.calculateZone(points)

                val circle = map.addCircle(
                    CircleOptions()
                        .center(center)
                        .radius(radius)
                        .fillColor(zoneFillColor)
                        .strokeColor(brandColor)
                        .strokeWidth(3f)
                        .zIndex(5.0f)
                )
                activeZoneCircles[key] = circle
            }
        }

        val iterator = activeZoneCircles.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!currentKeys.contains(entry.key)) {
                entry.value.remove()
                iterator.remove()
            }
        }
    }

    fun clearSafetyCircles() {
        activeZoneCircles.values.forEach { it.remove() }
        activeZoneCircles.clear()
    }

    private fun getClusterBadge(size: Int): BitmapDescriptor {
        val key = if (size > 99) 99 else size
        return iconCache.getOrPut(key) {
            generateClusterBadgeBitmap(key)
        }
    }

    private fun generateClusterBadgeBitmap(count: Int): BitmapDescriptor {
        val px = 120
        val bitmap = createBitmap(px, px)
        val canvas = Canvas(bitmap)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = brandColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(px / 2f, px / 2f, (px / 2f) - 6, fillPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(px / 2f, px / 2f, (px / 2f) - 6, strokePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val text = "$count Pati"
        val yOffset = (px / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(text, px / 2f, yOffset, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun clear() {
        iconCache.clear()
        activeZoneCircles.values.forEach { it.remove() }
        activeZoneCircles.clear()
    }
}