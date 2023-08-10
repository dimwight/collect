/*
 * Copyright (C) 2018 Nafundi
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

package org.odk.collect.osmdroid;

import static androidx.core.graphics.drawable.DrawableKt.toBitmap;
import static org.odk.collect.maps.MapConsts.POLYGON_FILL_COLOR_OPACITY;
import static org.odk.collect.maps.MapConsts.POLYLINE_STROKE_WIDTH;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.LocationListener;

import org.odk.collect.androidshared.system.ContextUtils;
import org.odk.collect.location.LocationClient;
import org.odk.collect.maps.MapConfigurator;
import org.odk.collect.maps.MapFragment;
import org.odk.collect.maps.MapFragmentDelegate;
import org.odk.collect.maps.MapPoint;
import org.odk.collect.maps.layers.MapFragmentReferenceLayerUtils;
import org.odk.collect.maps.layers.ReferenceLayerRepository;
import org.odk.collect.maps.markers.MarkerDescription;
import org.odk.collect.maps.markers.MarkerIconCreator;
import org.odk.collect.maps.markers.MarkerIconDescription;
import org.odk.collect.settings.SettingsProvider;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.IMyLocationConsumer;
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * A MapFragment drawn by OSMDroid.
 */
public class OsmDroidMapFragment extends Fragment implements MapFragment,
        LocationListener, LocationClient.LocationClientListener {

    // Bundle keys understood by applyConfig().
    public static final String KEY_WEB_MAP_SERVICE = "WEB_MAP_SERVICE";

    @Inject
    ReferenceLayerRepository referenceLayerRepository;

    @Inject
    LocationClient locationClient;

    @Inject
    MapConfigurator mapConfigurator;

    @Inject
    SettingsProvider settingsProvider;

    private final MapFragmentDelegate mapFragmentDelegate = new MapFragmentDelegate(
            this,
            () -> mapConfigurator,
            () -> settingsProvider.getUnprotectedSettings(),
            this::onConfigChanged
    );

    private MapView map;
    private ReadyListener readyListener;
    private PointListener clickListener;
    private PointListener longPressListener;
    private PointListener gpsLocationListener;
    private FeatureListener featureClickListener;
    private FeatureListener dragEndListener;
    private MyLocationNewOverlay myLocationOverlay;
    private OsmLocationClientWrapper osmLocationClientWrapper;
    private int nextFeatureId = 1;
    private final Map<Integer, MapFeature> features = new HashMap<>();
    private boolean clientWantsLocationUpdates;
    private IGeoPoint lastMapCenter;
    private WebMapService webMapService;
    private File referenceLayerFile;
    private TilesOverlay referenceOverlay;
    private boolean hasCenter;

    @Override
    public void init(@Nullable ReadyListener readyListener, @Nullable ErrorListener errorListener) {
        Timber.d("5540: 138");
        this.readyListener = readyListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Timber.d("5540: 145");
        super.onCreate(savedInstanceState);
        mapFragmentDelegate.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        Timber.d("5540: 153");
        super.onAttach(context);
        OsmDroidDependencyComponent component = ((OsmDroidDependencyComponentProvider) context.getApplicationContext()).getOsmDroidDependencyComponent();
        component.inject(this);
    }

    @Override
    public void onStart() {
        Timber.d("5540: 160");
        super.onStart();
        mapFragmentDelegate.onStart();
    }

    @Override
    public void onResume() {
        Timber.d("5540: 167");
        super.onResume();
        enableLocationUpdates(clientWantsLocationUpdates);
    }

    @Override
    public void onPause() {
        Timber.d("5540: 174 onPause");
        super.onPause();
        enableLocationUpdates(false);
    }

    @Override
    public void onStop() {
        Timber.d("5540: 181");
        super.onStop();
        mapFragmentDelegate.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Timber.d("5540: 188");
        super.onSaveInstanceState(outState);
        mapFragmentDelegate.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        Timber.d("5540: 196");
        clearFeatures();  // prevent a memory leak due to refs held by markers
        MarkerIconCreator.clearCache();
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Timber.d("5540: 203");
        View view = inflater.inflate(R.layout.osm_map_layout, container, false);
        map = view.findViewById(R.id.osm_map_view);
        if (webMapService != null) {
            map.setTileSource(webMapService.asOnlineTileSource());
        }
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        map.setMinZoomLevel(2.0);
        map.setMaxZoomLevel(22.0);
        map.getController().setCenter(toGeoPoint(INITIAL_CENTER));
        map.getController().setZoom((int) INITIAL_ZOOM);
        map.setTilesScaledToDpi(true);
        map.setFlingEnabled(false);
        addAttributionAndMapEventsOverlays();
        loadReferenceOverlay();
        addMapLayoutChangeListener(map);

        osmLocationClientWrapper = new OsmLocationClientWrapper(locationClient);
        myLocationOverlay = new MyLocationNewOverlay(osmLocationClientWrapper, map);
        myLocationOverlay.setDrawAccuracyEnabled(true);
        Drawable drawable = ContextCompat.getDrawable(requireActivity(), org.odk.collect.maps.R.drawable.ic_crosshairs);
        Bitmap crosshairs = toBitmap(drawable, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), null);
        myLocationOverlay.setDirectionArrow(crosshairs, crosshairs);
        myLocationOverlay.setPersonHotspot(crosshairs.getWidth() / 2.0f, crosshairs.getHeight() / 2.0f);

        new Handler().postDelayed(() -> {
            // If the screen is rotated before the map is ready, this fragment
            // could already be detached, which makes it unsafe to use.  Only
            // call the ReadyListener if this fragment is still attached.
            if (readyListener != null && getActivity() != null) {
                mapFragmentDelegate.onReady();
                readyListener.onReady(this);
            }
        }, 100);
        return view;
    }

    @Override
    public @NonNull
    MapPoint getCenter() {
        Timber.d("5540: 243");
        return fromGeoPoint(map.getMapCenter());
    }

    @Override
    public void setCenter(@Nullable MapPoint center, boolean animate) {
        Timber.d("5540: 249");
        if (center != null) {
            if (animate) {
                map.getController().animateTo(toGeoPoint(center));
            } else {
                map.getController().setCenter(toGeoPoint(center));
            }
        }

        hasCenter = true;
    }

    @Override
    public double getZoom() {
        return map.getZoomLevel();
    }

    @Override
    public void zoomToPoint(@Nullable MapPoint center, boolean animate) {
        Timber.d("5540: 268");
        zoomToPoint(center, POINT_ZOOM, animate);
    }

    @Override
    public void zoomToPoint(@Nullable MapPoint center, double zoom, boolean animate) {
        Timber.d("5540: 274");
        // We're ignoring the 'animate' flag because OSMDroid doesn't provide
        // support for simultaneously animating the viewport center and zoom level.
        if (center != null) {
            // setCenter() must be done last; setZoom() does not preserve the center.
            map.getController().setZoom((int) Math.round(zoom));
            map.getController().setCenter(toGeoPoint(center));
        }

        hasCenter = true;
    }

    @Override
    public void zoomToBoundingBox(Iterable<MapPoint> points, double scaleFactor, boolean animate) {
        Timber.d("5540: 288");
        if (points != null) {
            int count = 0;
            List<GeoPoint> geoPoints = new ArrayList<>();
            MapPoint lastPoint = null;
            for (MapPoint point : points) {
                lastPoint = point;
                geoPoints.add(toGeoPoint(point));
                count++;
            }
            if (count == 1) {
                zoomToPoint(lastPoint, animate);
            } else if (count > 1) {
                // TODO(ping): Find a better solution.
                // zoomToBoundingBox sometimes fails to zoom correctly, either
                // zooming by the correct amount but leaving the bounding box
                // off-center, or centering correctly but not zooming in enough.
                // Adding a 100-ms delay avoids the problem most of the time, but
                // not always; it's here because the old GeoShapeOsmMapActivity
                // did it, not because it's known to be the best solution.
                final BoundingBox box = BoundingBox.fromGeoPoints(geoPoints)
                        .increaseByScale((float) (1 / scaleFactor));
                new Handler().postDelayed(() -> map.zoomToBoundingBox(box, animate), 100);
            }
        }

        hasCenter = true;
    }

    @Override
    public int addMarker(MarkerDescription markerDescription) {
        Timber.d("5540: 319");
        int featureId = nextFeatureId++;
        features.put(featureId, new MarkerFeature(map, markerDescription));
        return featureId;
    }

    @Override
    public List<Integer> addMarkers(List<MarkerDescription> markers) {
        Timber.d("5540: 327");
        List<Integer> featureIds = new ArrayList<>();
        for (MarkerDescription markerDescription : markers) {
            int featureId = addMarker(markerDescription);
            featureIds.add(featureId);
        }

        return featureIds;
    }

    @Override
    public void setMarkerIcon(int featureId, MarkerIconDescription markerIconDescription) {
        Timber.d("5540: 339");
        MapFeature feature = features.get(featureId);
        if (feature instanceof MarkerFeature) {
            ((MarkerFeature) feature).setIcon(markerIconDescription);
        }
    }

    @Override
    public @Nullable
    MapPoint getMarkerPoint(int featureId) {
        Timber.d("5540: 349");
        MapFeature feature = features.get(featureId);
        return feature instanceof MarkerFeature ? ((MarkerFeature) feature).getPoint() : null;
    }

    @Override
    public int addPolyLine(@NonNull Iterable<MapPoint> points, boolean closed, boolean draggable) {
        Timber.d("5540: 356 addPolyLine");
        int featureId = nextFeatureId++;
        if (draggable) {
            features.put(featureId, new DynamicPolyLineFeature(map, points, closed));
        } else {
            features.put(featureId, new StaticPolyLineFeature(map, points, closed));
        }
        return featureId;
    }

    @Override
    public int addPolygon(@NonNull Iterable<MapPoint> points) {
        Timber.d("5540: 368");
        int featureId = nextFeatureId++;
        features.put(featureId, new StaticPolygonFeature(map, points));
        return featureId;
    }

    @Override
    public void appendPointToPolyLine(int featureId, @NonNull MapPoint point) {
        Timber.d("5540: 376");
        MapFeature feature = features.get(featureId);
        if (feature instanceof DynamicPolyLineFeature) {
            ((DynamicPolyLineFeature) feature).addPoint(point);
        }
    }

    @Override
    public @NonNull
    List<MapPoint> getPolyLinePoints(int featureId) {
        Timber.d("5540: 386");
        MapFeature feature = features.get(featureId);
        if (feature instanceof DynamicPolyLineFeature) {
            return ((DynamicPolyLineFeature) feature).getPoints();
        }
        return new ArrayList<>();
    }

    @Override
    public void removePolyLineLastPoint(int featureId) {
        Timber.d("5540: 396");
        MapFeature feature = features.get(featureId);
        if (feature instanceof DynamicPolyLineFeature) {
            ((DynamicPolyLineFeature) feature).removeLastPoint();
        }
    }

    @Override
    public void clearFeatures() {
        Timber.d("5540: 405");
        for (MapFeature feature : features.values()) {
            feature.dispose();
        }
        map.invalidate();
        features.clear();
        nextFeatureId = 1;
    }

    @Override
    public void setClickListener(@Nullable PointListener listener) {
        Timber.d("5540: 416");
        clickListener = listener;
    }

    @Override
    public void setLongPressListener(@Nullable PointListener listener) {
        Timber.d("5540: 422");
        longPressListener = listener;
    }

    @Override
    public void setFeatureClickListener(@Nullable FeatureListener listener) {
        Timber.d("5540: 428");
        featureClickListener = listener;
    }

    @Override
    public void setDragEndListener(@Nullable FeatureListener listener) {
        Timber.d("5540: 434");
        dragEndListener = listener;
    }

    @Override
    public void setGpsLocationListener(@Nullable PointListener listener) {
        Timber.d("5540: 440");
        gpsLocationListener = listener;
    }

    @Override
    public void setRetainMockAccuracy(boolean retainMockAccuracy) {
        Timber.d("5540: 446");
        locationClient.setRetainMockAccuracy(retainMockAccuracy);
    }

    @Override
    public boolean hasCenter() {
        Timber.d("5540: 452");
        return hasCenter;
    }

    @Override
    public void runOnGpsLocationReady(@NonNull ReadyListener listener) {
        Timber.d("5540: 458");
        myLocationOverlay.runOnFirstFix(() -> getActivity().runOnUiThread(() -> listener.onReady(this)));
    }

    @Override
    public void setGpsLocationEnabled(boolean enable) {
        Timber.d("5540: 464");
        if (enable != clientWantsLocationUpdates) {
            clientWantsLocationUpdates = enable;
            enableLocationUpdates(clientWantsLocationUpdates);
        }
    }

    @Override
    public @Nullable
    MapPoint getGpsLocation() {
        Timber.d("5540: 474");
        return fromLocation(myLocationOverlay);
    }

    @Override
    public @Nullable
    String getLocationProvider() {
        Timber.d("5540: 481");
        Location fix = myLocationOverlay.getLastFix();
        return fix != null ? fix.getProvider() : null;
    }

    @Override
    public void onLocationChanged(Location location) {
        Timber.d("5540: 488");
        Timber.i("onLocationChanged: location = %s", location);
        if (gpsLocationListener != null) {
            MapPoint point = fromLocation(myLocationOverlay);
            if (point != null) {
                gpsLocationListener.onPoint(point);
            }
        }

        if (myLocationOverlay != null) {
            myLocationOverlay.onLocationChanged(location, osmLocationClientWrapper);
        }
    }

    @Override
    public void onClientStart() {
        Timber.d("5540: 504");
        map.getOverlays().add(myLocationOverlay);
        myLocationOverlay.setEnabled(true);
        myLocationOverlay.enableMyLocation();

        Timber.i("Requesting location updates (to %s)", this);
        locationClient.requestLocationUpdates(this);
    }

    @Override
    public void onClientStartFailure() {
        Timber.d("5540: 515");
    }

    @Override
    public void onClientStop() {
        Timber.d("5540: 520");
    }

    private void enableLocationUpdates(boolean enable) {
        Timber.d("5540: 524");
        if (enable) {
            Timber.i("Starting LocationClient %s (for MapFragment %s)", locationClient, this);
            locationClient.start(this);
        } else {
            Timber.i("Stopping LocationClient %s (for MapFragment %s)", locationClient, this);
            locationClient.stop();
            myLocationOverlay.setEnabled(false);
            safelyDisableOverlayLocationFollowing();
        }
    }

    /**
     * <a href="https://github.com/osmdroid/osmdroid/issues/1783">
     * https://github.com/osmdroid/osmdroid/issues/1783
     * </a>
     **/
    private void safelyDisableOverlayLocationFollowing() {
        Timber.d("5540: 543 safelyDisable");
        if (map.isAttachedToWindow()) {
            myLocationOverlay.disableFollowLocation();
            myLocationOverlay.disableMyLocation();
        }
    }

    private static @Nullable
    MapPoint fromLocation(@NonNull MyLocationNewOverlay overlay) {
        Timber.d("5540: 551");
        GeoPoint geoPoint = overlay.getMyLocation();
        if (geoPoint == null) {
            return null;
        }
        return new MapPoint(
                geoPoint.getLatitude(), geoPoint.getLongitude(),
                geoPoint.getAltitude(), overlay.getLastFix().getAccuracy()
        );
    }

    private static @NonNull
    MapPoint fromGeoPoint(@NonNull IGeoPoint geoPoint) {
        Timber.d("5540: 564");
        return new MapPoint(geoPoint.getLatitude(), geoPoint.getLongitude());
    }

    private static @NonNull
    MapPoint fromGeoPoint(@NonNull GeoPoint geoPoint) {
        Timber.d("5540: 570");
        return new MapPoint(geoPoint.getLatitude(), geoPoint.getLongitude(), geoPoint.getAltitude());
    }

    private static @NonNull
    MapPoint fromMarker(@NonNull Marker marker) {
        Timber.d("5540: 576");
        GeoPoint geoPoint = marker.getPosition();
        double sd = 0;
        try {
            sd = Double.parseDouble(marker.getSubDescription());
        } catch (NumberFormatException e) {
            Timber.w("Marker.getSubDescription() did not contain a number");
        }
        return new MapPoint(
                geoPoint.getLatitude(), geoPoint.getLongitude(), geoPoint.getAltitude(), sd
        );
    }

    private static @NonNull
    GeoPoint toGeoPoint(@NonNull MapPoint point) {
        Timber.d("5540: 591");
        return new GeoPoint(point.latitude, point.longitude, point.altitude);
    }

    /**
     * Updates the map to reflect the value of referenceLayerFile.
     */
    private void loadReferenceOverlay() {
        Timber.d("5540: 599");
        if (referenceOverlay != null) {
            map.getOverlays().remove(referenceOverlay);
            referenceOverlay = null;
        }
        if (referenceLayerFile != null) {
            OsmMBTileProvider mbprovider = new OsmMBTileProvider(new RegisterReceiver(requireActivity()), referenceLayerFile);
            referenceOverlay = new TilesOverlay(mbprovider, getContext());
            referenceOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
            map.getOverlays().add(0, referenceOverlay);
        }
        map.invalidate();
    }

    /**
     * Adds a listener that keeps track of the map center, and another
     * listener that restores the map center when the MapView's layout changes.
     * We have to do this because the MapView is buggy and fails to preserve its
     * view on a layout change, causing the map viewport to jump around when the
     * screen is resized or rotated in a way that doesn't restart the activity.
     */
    private void addMapLayoutChangeListener(MapView map) {
        Timber.d("5540: 621");
        lastMapCenter = map.getMapCenter();
        map.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                lastMapCenter = map.getMapCenter();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                lastMapCenter = map.getMapCenter();
                return false;
            }
        });
        map.addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        map.getController().setCenter(lastMapCenter));
    }

    private Marker createMarker(MapView map, MarkerDescription markerDescription) {
        Timber.d("5540: 642");
        // A Marker's position is a GeoPoint with latitude, longitude, and
        // altitude fields.  We need to store the standard deviation value
        // somewhere, so it goes in the marker's sub-description field.
        Marker marker = new Marker(map);
        marker.setPosition(toGeoPoint(markerDescription.getPoint()));
        marker.setSubDescription(Double.toString(markerDescription.getPoint().accuracy));
        marker.setDraggable(markerDescription.isDraggable());
        marker.setIcon(MarkerIconCreator.getMarkerIconDrawable(map.getContext(), markerDescription.getIconDescription()));
        marker.setAnchor(getIconAnchorValueX(markerDescription.getIconAnchor()), getIconAnchorValueY(markerDescription.getIconAnchor()));
        marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
            int featureId = findFeature(clickedMarker);
            if (featureClickListener != null && featureId != -1) {
                featureClickListener.onFeature(featureId);
                return true;  // consume the event
            }
            return false;
        });
        marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // When a marker is manually dragged, the position is no longer
                // obtained from a GPS reading, so the standard deviation field
                // is no longer meaningful; reset it to zero.
                marker.setSubDescription("0");
                updateFeature(findFeature(marker));
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                int featureId = findFeature(marker);
                updateFeature(featureId);
                if (dragEndListener != null && featureId != -1) {
                    dragEndListener.onFeature(featureId);
                }
            }
        });

        map.getOverlays().add(marker);
        return marker;
    }

    private float getIconAnchorValueX(@IconAnchor String iconAnchor) {
        Timber.d("5540: 689");
        switch (iconAnchor) {
            case BOTTOM:
            default:
                return Marker.ANCHOR_CENTER;
        }
    }

    private float getIconAnchorValueY(@IconAnchor String iconAnchor) {
        Timber.d("5540: 698");
        switch (iconAnchor) {
            case BOTTOM:
                return Marker.ANCHOR_BOTTOM;
            default:
                return Marker.ANCHOR_CENTER;
        }
    }

    /**
     * Finds the feature to which the given marker belongs.
     */
    private int findFeature(Marker marker) {
        Timber.d("5540: 711");
        for (int featureId : features.keySet()) {
            if (features.get(featureId).ownsMarker(marker)) {
                return featureId;
            }
        }
        return -1;  // not found
    }

    /**
     * Finds the feature to which the given polyline belongs.
     */
    private int findFeature(Polyline polyline) {
        Timber.d("5540: 724");
        for (int featureId : features.keySet()) {
            if (features.get(featureId).ownsPolyline(polyline)) {
                return featureId;
            }
        }
        return -1;  // not found
    }

    private int findFeature(Polygon polygon) {
        Timber.d("5540: 734");
        for (int featureId : features.keySet()) {
            if (features.get(featureId).ownsPolygon(polygon)) {
                return featureId;
            }
        }
        return -1;
    }

    private void updateFeature(int featureId) {
        Timber.d("5540: 744");
        MapFeature feature = features.get(featureId);
        if (feature != null) {
            feature.update();
        }
    }

    private void addAttributionAndMapEventsOverlays() {
        Timber.d("5540: 751");
        map.getOverlays().add(new AttributionOverlay(getContext()));
        map.getOverlays().add(
                new MapEventsOverlay(
                        new MapEventsReceiver(
                                point -> {
                                    if (clickListener != null) {
                                        clickListener.onPoint(point);
                                    }
                                },
                                point -> {
                                    if (longPressListener != null) {
                                        longPressListener.onPoint(point);
                                    }
                                }
                        )
                )
        );
    }

    private void onConfigChanged(Bundle config) {
        Timber.d("5540: 773");
        webMapService = (WebMapService) config.getSerializable(KEY_WEB_MAP_SERVICE);
        referenceLayerFile = MapFragmentReferenceLayerUtils.getReferenceLayerFile(config, referenceLayerRepository);
        if (map != null) {
            map.setTileSource(webMapService.asOnlineTileSource());
            loadReferenceOverlay();
        }
    }

    /**
     * A MapFeature is a physical feature on a map, such as a point, a road,
     * a building, a region, etc.  It is presented to the user as one editable
     * object, though its appearance may be constructed from multiple overlays
     * (e.g. geometric elements, handles for manipulation, etc.).
     */
    interface MapFeature {
        /**
         * Returns true if the given marker belongs to this feature.
         */
        boolean ownsMarker(Marker marker);

        /**
         * Returns true if the given polyline belongs to this feature.
         */
        boolean ownsPolyline(Polyline polyline);

        boolean ownsPolygon(Polygon polygon);

        /**
         * Updates the feature's geometry after any UI handles have moved.
         */
        void update();

        /**
         * Removes the feature from the map, leaving it no longer usable.
         */
        void dispose();
    }

    /**
     * A marker that can optionally be dragged by the user.
     */
    private class MarkerFeature implements MapFeature {
        final MapView map;
        Marker marker;

        MarkerFeature(MapView map, MarkerDescription markerDescription) {
            this.map = map;
            this.marker = createMarker(map, markerDescription);
        }

        public void setIcon(MarkerIconDescription markerIconDescription) {
            marker.setIcon(MarkerIconCreator.getMarkerIconDrawable(map.getContext(), markerIconDescription));
        }

        public MapPoint getPoint() {
            return fromMarker(marker);
        }

        public boolean ownsMarker(Marker givenMarker) {
            return marker.equals(givenMarker);
        }

        public boolean ownsPolyline(Polyline polyline) {
            return false;
        }

        @Override
        public boolean ownsPolygon(Polygon polygon) {
            return false;
        }

        public void update() {
        }

        public void dispose() {
            map.getOverlays().remove(marker);
            marker = null;
        }
    }

    /**
     * A polyline or polygon that can be manipulated by dragging markers at its vertices.
     */
    private class StaticPolyLineFeature implements MapFeature {
        final MapView map;
        final Polyline polyline;
        final boolean closedPolygon;

        StaticPolyLineFeature(MapView map, Iterable<MapPoint> points, boolean closedPolygon) {
            this.map = map;
            this.closedPolygon = closedPolygon;
            polyline = new Polyline();
            polyline.setColor(map.getContext().getResources().getColor(org.odk.collect.icons.R.color.mapLineColor));
            polyline.setOnClickListener((clickedPolyline, mapView, eventPos) -> {
                int featureId = findFeature(clickedPolyline);
                if (featureClickListener != null && featureId != -1) {
                    featureClickListener.onFeature(featureId);
                    return true;  // consume the event
                }
                return false;
            });
            Paint paint = polyline.getPaint();
            paint.setStrokeWidth(POLYLINE_STROKE_WIDTH);
            map.getOverlays().add(polyline);

            List<GeoPoint> geoPoints = StreamSupport.stream(points.spliterator(), false).map(mapPoint -> new GeoPoint(mapPoint.latitude, mapPoint.longitude, mapPoint.altitude)).collect(Collectors.toList());
            if (closedPolygon && !geoPoints.isEmpty()) {
                geoPoints.add(geoPoints.get(0));
            }
            polyline.setPoints(geoPoints);
            map.invalidate();
        }

        @Override
        public boolean ownsMarker(Marker givenMarker) {
            return false;
        }

        @Override
        public boolean ownsPolyline(Polyline givenPolyline) {
            return polyline.equals(givenPolyline);
        }

        @Override
        public boolean ownsPolygon(Polygon polygon) {
            return false;
        }

        @Override
        public void update() {
        }

        @Override
        public void dispose() {
            map.getOverlays().remove(polyline);
        }
    }

    /**
     * A polyline or polygon that can be manipulated by dragging markers at its vertices.
     */
    private class DynamicPolyLineFeature implements MapFeature {
        final MapView map;
        final List<Marker> markers = new ArrayList<>();
        final Polyline polyline;
        final boolean closedPolygon;

        DynamicPolyLineFeature(MapView map, Iterable<MapPoint> points, boolean closedPolygon) {
            this.map = map;
            this.closedPolygon = closedPolygon;
            polyline = new Polyline();
            polyline.setColor(map.getContext().getResources().getColor(org.odk.collect.icons.R.color.mapLineColor));
            polyline.setOnClickListener((clickedPolyline, mapView, eventPos) -> {
                int featureId = findFeature(clickedPolyline);
                if (featureClickListener != null && featureId != -1) {
                    featureClickListener.onFeature(featureId);
                    return true;  // consume the event
                }
                return false;
            });
            Paint paint = polyline.getPaint();
            paint.setStrokeWidth(POLYLINE_STROKE_WIDTH);
            map.getOverlays().add(polyline);
            for (MapPoint point : points) {
                markers.add(createMarker(map, new MarkerDescription(point, true, CENTER, new MarkerIconDescription(org.odk.collect.icons.R.drawable.ic_map_point))));
            }
            update();
        }

        @Override
        public boolean ownsMarker(Marker givenMarker) {
            return markers.contains(givenMarker);
        }

        @Override
        public boolean ownsPolyline(Polyline givenPolyline) {
            return polyline.equals(givenPolyline);
        }

        @Override
        public boolean ownsPolygon(Polygon polygon) {
            return false;
        }

        @Override
        public void update() {
            List<GeoPoint> geoPoints = new ArrayList<>();
            for (Marker marker : markers) {
                geoPoints.add(marker.getPosition());
            }
            if (closedPolygon && !geoPoints.isEmpty()) {
                geoPoints.add(geoPoints.get(0));
            }
            polyline.setPoints(geoPoints);
            map.invalidate();
        }

        @Override
        public void dispose() {
            for (Marker marker : markers) {
                map.getOverlays().remove(marker);
            }
            markers.clear();
            map.getOverlays().remove(polyline);
        }

        public List<MapPoint> getPoints() {
            List<MapPoint> points = new ArrayList<>();
            for (Marker marker : markers) {
                points.add(fromMarker(marker));
            }
            return points;
        }

        public void addPoint(MapPoint point) {
            markers.add(createMarker(map, new MarkerDescription(point, true, CENTER, new MarkerIconDescription(org.odk.collect.icons.R.drawable.ic_map_point))));
            update();
        }

        public void removeLastPoint() {
            if (!markers.isEmpty()) {
                int last = markers.size() - 1;
                map.getOverlays().remove(markers.get(last));
                markers.remove(last);
                update();
            }
        }
    }

    private class StaticPolygonFeature implements MapFeature {
        private final MapView map;
        private final Polygon polygon = new Polygon();

        StaticPolygonFeature(MapView map, Iterable<MapPoint> points) {
            this.map = map;

            map.getOverlays().add(polygon);
            int strokeColor = map.getContext().getResources().getColor(org.odk.collect.icons.R.color.mapLineColor);
            polygon.getOutlinePaint().setColor(strokeColor);
            polygon.setStrokeWidth(POLYLINE_STROKE_WIDTH);
            polygon.getFillPaint().setColor(ColorUtils.setAlphaComponent(strokeColor, POLYGON_FILL_COLOR_OPACITY));
            polygon.setPoints(StreamSupport.stream(points.spliterator(), false).map(point -> new GeoPoint(point.latitude, point.longitude)).collect(Collectors.toList()));
            polygon.setOnClickListener((polygon, mapView, eventPos) -> {
                int featureId = findFeature(polygon);
                if (featureClickListener != null && featureId != -1) {
                    featureClickListener.onFeature(featureId);
                    return true;  // consume the event
                }

                return false;
            });
        }

        @Override
        public boolean ownsMarker(Marker marker) {
            return false;
        }

        @Override
        public boolean ownsPolyline(Polyline polyline) {
            return false;
        }

        @Override
        public boolean ownsPolygon(Polygon polygon) {
            return polygon.equals(this.polygon);
        }

        @Override
        public void update() {
        }

        @Override
        public void dispose() {
            map.getOverlays().remove(polygon);
        }
    }

    /**
     * An overlay that draws an attribution message in the lower-right corner.
     */
    private static class AttributionOverlay extends Overlay {
        public static final int FONT_SIZE_DP = 12;
        public static final int MARGIN_DP = 10;

        private final Paint paint;

        AttributionOverlay(Context context) {
            super();

            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(ContextUtils.getThemeAttributeValue(context, com.google.android.material.R.attr.colorOnSurface));
            paint.setTextSize(FONT_SIZE_DP *
                    context.getResources().getDisplayMetrics().density);
            paint.setTextAlign(Paint.Align.RIGHT);
        }

        @Override
        public void draw(Canvas canvas, MapView map, boolean shadow) {
            String attribution = map.getTileProvider().getTileSource().getCopyrightNotice();
            if (!shadow && !map.isAnimating() && attribution != null && !attribution.isEmpty()) {
                String[] lines = attribution.split("\n");
                float lineHeight = paint.getFontSpacing();
                float x = canvas.getWidth() - MARGIN_DP;
                float y = canvas.getHeight() - MARGIN_DP - lineHeight * lines.length;

                canvas.save();
                canvas.concat(map.getProjection().getInvertedScaleRotateCanvasMatrix());
                for (String line : lines) {
                    y += lineHeight;
                    canvas.drawText(line, x, y, paint);
                }
                canvas.restore();
            }
        }
    }

    private static class OsmLocationClientWrapper implements IMyLocationProvider {
        private LocationClient locationClient;

        OsmLocationClientWrapper(LocationClient locationClient) {
            this.locationClient = locationClient;
        }

        @Override
        public boolean startLocationProvider(IMyLocationConsumer myLocationConsumer) {
            // locationClient.start launches async work and we need to be confident that
            // getLastKnownLocation is never called before onClientStart so we don't let the OSM
            // location overlay start the provider. We also ignore the location consumer passed in
            // and instead explicitly forward location updates to the overlay from onLocationChanged
            return true;
        }

        @Override
        public void stopLocationProvider() {
            locationClient.stop();
        }

        @Override
        public Location getLastKnownLocation() {
            return locationClient.getLastLocation();
        }

        @Override
        public void destroy() {
            locationClient.stop();
            locationClient = null;
        }
    }

    private static class RegisterReceiver implements IRegisterReceiver {

        private final Context context;

        RegisterReceiver(Context context) {
            this.context = context;
        }

        @Override
        public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
            return context != null ? context.registerReceiver(receiver, filter) : null;
        }

        @Override
        public void unregisterReceiver(BroadcastReceiver receiver) {
            if (context != null) {
                context.unregisterReceiver(receiver);
            }
        }

        @Override
        public void destroy() {
        }
    }

    private static class MapEventsReceiver implements org.osmdroid.events.MapEventsReceiver {

        private final PointListener clickListener;
        private final PointListener longPressListener;

        MapEventsReceiver(PointListener clickListener, PointListener longPressListener) {
            this.clickListener = clickListener;
            this.longPressListener = longPressListener;
        }

        @Override
        public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
            if (clickListener != null) {
                clickListener.onPoint(fromGeoPoint(geoPoint));
                return true;
            }
            return false;
        }

        @Override
        public boolean longPressHelper(GeoPoint geoPoint) {
            if (longPressListener != null) {
                longPressListener.onPoint(fromGeoPoint(geoPoint));
                return true;
            }
            return false;
        }
    }
}
