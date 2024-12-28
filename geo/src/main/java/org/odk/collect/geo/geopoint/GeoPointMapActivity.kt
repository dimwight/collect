/*
* Copyright (C) 2011 University of Washington
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License
* is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing permissions and limitations under
* the License.
*/
package org.odk.collect.geo.geopoint

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.odk.collect.androidshared.ui.DialogFragmentUtils
import org.odk.collect.androidshared.ui.FragmentFactoryBuilder
import org.odk.collect.androidshared.ui.ToastUtils
import org.odk.collect.async.Scheduler
import org.odk.collect.externalapp.ExternalAppUtils
import org.odk.collect.geo.Constants.EXTRA_DRAGGABLE_ONLY
import org.odk.collect.geo.Constants.EXTRA_READ_ONLY
import org.odk.collect.geo.Constants.EXTRA_RETAIN_MOCK_ACCURACY
import org.odk.collect.geo.GeoActivityUtils.requireLocationPermissions
import org.odk.collect.geo.GeoDependencyComponentProvider
import org.odk.collect.geo.GeoUtils.capitalizeGps
import org.odk.collect.geo.R
import org.odk.collect.maps.MapFragment
import org.odk.collect.maps.MapFragmentFactory
import org.odk.collect.maps.MapPoint
import org.odk.collect.maps.layers.OfflineMapLayersPickerBottomSheetDialogFragment
import org.odk.collect.maps.layers.ReferenceLayerRepository
import org.odk.collect.maps.markers.MarkerDescription
import org.odk.collect.maps.markers.MarkerIconDescription
import org.odk.collect.settings.SettingsProvider
import org.odk.collect.strings.R.string
import org.odk.collect.strings.localization.LocalizedActivity
import org.odk.collect.webpage.ExternalWebPageHelper
import timber.log.Timber
import javax.inject.Inject

/**
 * Allow the user to indicate a location by placing a marker on a map, either
 * by touching a point on the map or by tapping a button to place the marker
 * at the current location (obtained from GPS or other location sensors).
 */
class GeoPointMapActivity : LocalizedActivity() {

    companion object {
        const val POINT_KEY = "point"
        const val IS_DRAGGED_KEY = "is_dragged"
        const val CAPTURE_LOCATION_KEY = "capture_location"
        const val FOUND_FIRST_LOCATION_KEY = "found_first_location"
        const val SET_CLEAR_KEY = "set_clear"
        const val POINT_FROM_INTENT_KEY = "point_from_intent"
        const val INTENT_READ_ONLY_KEY = "intent_read_only"
        const val INTENT_DRAGGABLE_KEY = "intent_draggable"
        const val IS_POINT_LOCKED_KEY = "is_point_locked"
        const val PLACE_MARKER_BUTTON_ENABLED_KEY = "place_marker_button_enabled"
        const val ZOOM_BUTTON_ENABLED_KEY = "zoom_button_enabled"
        const val CLEAR_BUTTON_ENABLED_KEY = "clear_button_enabled"
        const val LOCATION_STATUS_VISIBILITY_KEY = "location_status_visibility"
        const val LOCATION_INFO_VISIBILITY_KEY = "location_info_visibility"
        const val EXTRA_LOCATION = "gp"
    }

    protected var previousState: Bundle? = null

    @Inject
    lateinit var mapFragmentFactory: MapFragmentFactory

    @Inject
    lateinit var referenceLayerRepository: ReferenceLayerRepository

    @Inject
    lateinit var scheduler: Scheduler

    @Inject
    lateinit var settingsProvider: SettingsProvider

    @Inject
    lateinit var externalWebPageHelper: ExternalWebPageHelper

    private var map: MapFragment? = null
    private var featureId: Int? = -1 // will be a positive featureId once map is ready

    private lateinit var locationStatus: TextView
    private lateinit var locationInfo: TextView

    private var location: MapPoint? = null
    private lateinit var placeMarkerButton: ImageButton

    private var isDragged: Boolean = false

    private lateinit var zoomButton: ImageButton
    private lateinit var clearButton: ImageButton

    private var captureLocation: Boolean = false
    private var foundFirstLocation: Boolean = false

    /**
     * True if a tap on the clear button removed an existing marker and
     * no new marker has been placed.
     */
    private var setClear: Boolean = false

    /** True if the current point came from the intent.  */
    private var pointFromIntent: Boolean = false

    /** True if the intent requested for the point to be read-only.  */
    private var intentReadOnly: Boolean = false

    /** True if the intent requested for the marker to be draggable.  */
    private var intentDraggable: Boolean = false

    /** While true, the point cannot be moved by dragging or long-pressing.  */
    private var isPointLocked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as GeoDependencyComponentProvider).geoDependencyComponent.inject(
            this
        )
        supportFragmentManager.fragmentFactory = FragmentFactoryBuilder()
            .forClass(MapFragment::class.java) {
                mapFragmentFactory.createMapFragment() as Fragment
            }
            .forClass(OfflineMapLayersPickerBottomSheetDialogFragment::class.java) {
                OfflineMapLayersPickerBottomSheetDialogFragment(
                    activityResultRegistry,
                    referenceLayerRepository,
                    scheduler,
                    settingsProvider,
                    externalWebPageHelper
                )
            }
            .build()
        System.out.println("6136: this = " + this)
        super.onCreate(savedInstanceState)

        requireLocationPermissions(this)

        previousState = savedInstanceState

        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        try {
            setContentView(R.layout.geopoint_layout)
        } catch (e: NoClassDefFoundError) {
            Timber.e(e, "Google maps not accessible due to: %s ", e.message)
            ToastUtils.showShortToast(
                this,
                string.google_play_services_error_occured
            )
            finish()
            return
        }

        locationStatus = findViewById<TextView?>(R.id.location_status)
        locationInfo = findViewById<TextView?>(R.id.location_info)
        placeMarkerButton = findViewById<ImageButton?>(R.id.place_marker)
        zoomButton = findViewById<ImageButton?>(R.id.zoom)

        val mapFragment: MapFragment =
            (findViewById<android.view.View?>(R.id.map_container) as androidx.fragment.app.FragmentContainerView).getFragment()
        mapFragment.init({ newMapFragment: MapFragment ->
            this.initMap(
                newMapFragment
            )
        }, { this.finish() })
    }

    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        if (map == null) {
            // initMap() is called asynchronously, so map can be null if the activity
            // is stopped (e.g. by screen rotation) before initMap() gets to run.
            // In this case, preserve any provided instance state.
            state.putAll(previousState)
            return
        }

        state.putParcelable(POINT_KEY, map?.getMarkerPoint(featureId!!))

        // Flags
        state.putBoolean(IS_DRAGGED_KEY, isDragged)
        state.putBoolean(CAPTURE_LOCATION_KEY, captureLocation)
        state.putBoolean(FOUND_FIRST_LOCATION_KEY, foundFirstLocation)
        state.putBoolean(SET_CLEAR_KEY, setClear)
        state.putBoolean(POINT_FROM_INTENT_KEY, pointFromIntent)
        state.putBoolean(INTENT_READ_ONLY_KEY, intentReadOnly)
        state.putBoolean(INTENT_DRAGGABLE_KEY, intentDraggable)
        state.putBoolean(IS_POINT_LOCKED_KEY, isPointLocked)

        // UI state
        state.putBoolean(PLACE_MARKER_BUTTON_ENABLED_KEY, placeMarkerButton.isEnabled)
        state.putBoolean(ZOOM_BUTTON_ENABLED_KEY, zoomButton.isEnabled)
        state.putBoolean(CLEAR_BUTTON_ENABLED_KEY, clearButton.isEnabled)
        state.putInt(LOCATION_STATUS_VISIBILITY_KEY, locationStatus.visibility)
        state.putInt(LOCATION_INFO_VISIBILITY_KEY, locationInfo.visibility)
    }

    fun returnLocation() {
        var result: String? = null

        if (setClear || (intentReadOnly && featureId == -1)) {
            result = ""
        } else if (isDragged || intentReadOnly || pointFromIntent) {
            result = formatResult(map!!.getMarkerPoint(featureId!!))
        } else if (location != null) {
            result = formatResult(location!!)
        }

        if (result != null) {
            ExternalAppUtils.returnSingleValue(this, result)
        } else {
            finish()
        }
    }

    @android.annotation.SuppressLint("MissingPermission") // Permission handled in Constructor
    fun initMap(newMapFragment: MapFragment) {
        map = newMapFragment
        map?.setDragEndListener { draggedFeatureId: Int ->
            this.onDragEnd(
                draggedFeatureId
            )
        }
        map?.setLongPressListener { point: MapPoint? ->
            this.onLongPress(point!!)
        }

        val acceptLocation: ImageButton? = findViewById<ImageButton?>(R.id.accept_location)
        acceptLocation!!.setOnClickListener { v: android.view.View? -> returnLocation() }

        placeMarkerButton.isEnabled = false
        placeMarkerButton.setOnClickListener { v: android.view.View? ->
            val mapPoint: MapPoint? = map?.getGpsLocation()
            if (mapPoint != null) {
                placeMarker(mapPoint)
                zoomToMarker(true)
            }
        }

        // Focuses on marked location
        zoomButton.isEnabled = false
        zoomButton.setOnClickListener { v: android.view.View? ->
            map?.zoomToPoint(
                map?.getGpsLocation(),
                true
            )
        }

        // Menu Layer Toggle
        findViewById<android.view.View?>(R.id.layer_menu).setOnClickListener { v: android.view.View? ->
            DialogFragmentUtils.showIfNotShowing<OfflineMapLayersPickerBottomSheetDialogFragment>(
                OfflineMapLayersPickerBottomSheetDialogFragment::class.java,
                supportFragmentManager
            )
        }

        clearButton = findViewById<ImageButton?>(R.id.clear)
        clearButton.isEnabled = false
        clearButton.setOnClickListener { v: android.view.View? ->
            clear()
            if (map?.getGpsLocation() != null) {
                placeMarkerButton.isEnabled = true
                // locationStatus.setVisibility(View.VISIBLE);
            }
            // placeMarkerButton.setEnabled(true);
            locationInfo.visibility = android.view.View.VISIBLE
            locationStatus.visibility = android.view.View.VISIBLE
            pointFromIntent = false
        }

        val intent = intent
        if (intent != null && intent.extras != null) {
            intentDraggable = intent.getBooleanExtra(EXTRA_DRAGGABLE_ONLY, false)
            if (!intentDraggable) {
                // Not Draggable, set text for Map else leave as placement-map text
                locationInfo.text = getString(string.geopoint_no_draggable_instruction)
            }

            intentReadOnly = intent.getBooleanExtra(EXTRA_READ_ONLY, false)
            if (intentReadOnly) {
                captureLocation = true
                clearButton.isEnabled = false
            }

            if (intent.hasExtra(EXTRA_LOCATION)) {
                val point = intent.getParcelableExtra<MapPoint?>(
                    EXTRA_LOCATION
                )

                // If the point is initially set from the intent, the "place marker"
                // button, dragging, and long-pressing are all initially disabled.
                // To enable them, the user must clear the marker and add a new one.
                isPointLocked = true
                placeMarker(point!!)
                placeMarkerButton.isEnabled = false

                captureLocation = true
                pointFromIntent = true
                locationInfo.visibility = android.view.View.GONE
                locationStatus.visibility = android.view.View.GONE
                zoomButton.isEnabled = true
                foundFirstLocation = true
                zoomToMarker(false)
            }
        }

        map?.setRetainMockAccuracy(intent!!.getBooleanExtra(EXTRA_RETAIN_MOCK_ACCURACY, false))
        map?.setGpsLocationListener { point: MapPoint? ->
            this.onLocationChanged(
                point
            )
        }
        map?.setGpsLocationEnabled(true)

        previousState?.let { restoreFromInstanceState(it) }
    }

    protected fun restoreFromInstanceState(state: Bundle) {
        isDragged = state.getBoolean(IS_DRAGGED_KEY, false)
        captureLocation = state.getBoolean(CAPTURE_LOCATION_KEY, false)
        foundFirstLocation = state.getBoolean(FOUND_FIRST_LOCATION_KEY, false)
        setClear = state.getBoolean(SET_CLEAR_KEY, false)
        pointFromIntent = state.getBoolean(POINT_FROM_INTENT_KEY, false)
        intentReadOnly = state.getBoolean(INTENT_READ_ONLY_KEY, false)
        intentDraggable = state.getBoolean(INTENT_DRAGGABLE_KEY, false)
        isPointLocked = state.getBoolean(IS_POINT_LOCKED_KEY, false)

        // Restore the marker and dialog after the flags, because they use some of them.
        val point = state.getParcelable<MapPoint?>(POINT_KEY)
        if (point != null) {
            placeMarker(point)
        } else {
            clear()
        }

        // Restore the flags again, because placeMarker() and clear() modify some of them.
        isDragged = state.getBoolean(IS_DRAGGED_KEY, false)
        captureLocation = state.getBoolean(CAPTURE_LOCATION_KEY, false)
        foundFirstLocation = state.getBoolean(FOUND_FIRST_LOCATION_KEY, false)
        setClear = state.getBoolean(SET_CLEAR_KEY, false)
        pointFromIntent = state.getBoolean(POINT_FROM_INTENT_KEY, false)
        intentReadOnly = state.getBoolean(INTENT_READ_ONLY_KEY, false)
        intentDraggable = state.getBoolean(INTENT_DRAGGABLE_KEY, false)
        isPointLocked = state.getBoolean(IS_POINT_LOCKED_KEY, false)

        placeMarkerButton.isEnabled = state.getBoolean(PLACE_MARKER_BUTTON_ENABLED_KEY, false)
        zoomButton.isEnabled = state.getBoolean(ZOOM_BUTTON_ENABLED_KEY, false)
        clearButton.isEnabled = state.getBoolean(CLEAR_BUTTON_ENABLED_KEY, false)

        locationInfo.visibility = state.getInt(
            LOCATION_INFO_VISIBILITY_KEY,
            android.view.View.GONE
        )
        locationStatus.visibility = state.getInt(
            LOCATION_STATUS_VISIBILITY_KEY,
            android.view.View.GONE
        )
    }

    fun onLocationChanged(point: MapPoint?) {
        if (setClear) {
            placeMarkerButton.isEnabled = true
        }

        this.location = point

        if (point != null) {
            enableZoomButton()

            if (!captureLocation && !setClear) {
                placeMarker(point)
                placeMarkerButton.isEnabled = true
            }

            if (!foundFirstLocation) {
                map?.zoomToPoint(map?.getGpsLocation(), true)
                foundFirstLocation = true
            }

            locationStatus.text = formatLocationStatus(map?.getLocationProvider(), point.accuracy)
        }
    }

    fun formatResult(point: MapPoint): String {
        return String.format(
            "%s %s %s %s",
            point.latitude,
            point.longitude,
            point.altitude,
            point.accuracy
        )
    }

    fun formatLocationStatus(provider: String?, accuracyRadius: Double): String {
        return (getString(
            string.location_accuracy,
            java.text.DecimalFormat("#.##").format(accuracyRadius)
        ) + " " + getString(
            string.location_provider, capitalizeGps(provider)
        ))
    }

    fun onDragEnd(draggedFeatureId: Int) {
        if (draggedFeatureId == featureId) {
            isDragged = true
            captureLocation = true
            setClear = false
            map?.setCenter(map?.getMarkerPoint(featureId!!), true)
        }
    }

    fun onLongPress(point: MapPoint) {
        if (intentDraggable && !intentReadOnly && !isPointLocked) {
            placeMarker(point)
            enableZoomButton()
            isDragged = true
        }
    }

    private fun enableZoomButton() {
        zoomButton.isEnabled = true
    }

    fun zoomToMarker(animate: Boolean) {
        map?.zoomToPoint(map?.getMarkerPoint(featureId!!), animate)
    }

    private fun clear() {
        map?.clearFeatures()
        featureId = -1
        clearButton.isEnabled = false

        isPointLocked = false
        isDragged = false
        captureLocation = false
        setClear = true
    }

    /** Places the marker and enables the button to remove it.  */
    private fun placeMarker(point: MapPoint) {
        map?.clearFeatures()
        featureId = map?.addMarker(
            MarkerDescription(
                point,
                intentDraggable && !intentReadOnly && !isPointLocked,
                MapFragment.CENTER,
                MarkerIconDescription(org.odk.collect.icons.R.drawable.ic_map_point)
            )
        )
        if (!intentReadOnly) {
            clearButton.isEnabled = true
        }
        captureLocation = true
        setClear = false
    }

    @androidx.annotation.VisibleForTesting
    fun getLocationStatus(): String {
        return locationStatus.text.toString()
    }
}
