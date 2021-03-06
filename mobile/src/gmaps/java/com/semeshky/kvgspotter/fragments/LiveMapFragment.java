package com.semeshky.kvgspotter.fragments;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.github.guennishueftgold.trapezeapi.VehicleLocation;
import com.github.guennishueftgold.trapezeapi.VehicleLocations;
import com.semeshky.kvgspotter.activities.TripPassagesActivity;
import com.semeshky.kvgspotter.database.Stop;
import com.semeshky.kvgspotter.location.LocationHelper;
import com.semeshky.kvgspotter.map.CoordinateUtil;
import com.semeshky.kvgspotter.map.DualOnMarkerClickListener;
import com.semeshky.kvgspotter.map.MapStyleGenerator;
import com.semeshky.kvgspotter.map.VehicleClusterItem;
import com.semeshky.kvgspotter.map.VehicleClusterRenderer;

import java.util.ArrayList;
import java.util.List;

public final class LiveMapFragment extends BaseLiveMapFragment {
    private final GoogleMap.OnMarkerClickListener mOnMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (marker.getTag() instanceof Stop) {
                /*
                startActivity(StationDetailActivity
                        .createIntent(getContext(),
                                (Stop) marker.getTag()));*/
                mViewModel.setSelectedStop((Stop) marker.getTag());
                return true;
            }
            return false;
        }
    };
    private final ClusterManager.OnClusterItemInfoWindowClickListener<VehicleClusterItem> mOnClusterItemClickListener
            = new ClusterManager.OnClusterItemInfoWindowClickListener<VehicleClusterItem>() {
        @Override
        public void onClusterItemInfoWindowClick(VehicleClusterItem clusterItem) {
            startActivity(TripPassagesActivity.createIntent(LiveMapFragment.this.getContext(),
                    clusterItem.getVehicleLocation().getTripId(),
                    clusterItem.getVehicleLocation().getName(),
                    null,
                    null,
                    "departure"));
        }
    };
    private ClusterManager<VehicleClusterItem> mClusterManager;
    private List<Marker> mVehicleMarker = new ArrayList<>();
    private List<Marker> mStopMarker = new ArrayList<>();

    @Override
    public void onMapReady(GoogleMap map) {
        super.onMapReady(map);
        //Disable map overlay toolbar
        map.getUiSettings()
                .setMapToolbarEnabled(false);
        //Clear lists just in case
        this.mVehicleMarker.clear();
        this.mStopMarker.clear();
        //Setup cluster manager for vehicle marker
        this.mClusterManager = new ClusterManager<>(this.getContext(), map);
        final VehicleClusterRenderer clusterRenderer = new VehicleClusterRenderer(this.getContext(), map, this.mClusterManager);
        this.mClusterManager.setRenderer(clusterRenderer);
        map.setOnCameraIdleListener(this.mClusterManager);
        final DualOnMarkerClickListener dualOnMarkerClickListener = new DualOnMarkerClickListener(this.mOnMarkerClickListener, this.mClusterManager);

        map.setOnMarkerClickListener(dualOnMarkerClickListener);
        map.setOnInfoWindowClickListener(this.mClusterManager);
        this.mClusterManager.setOnClusterItemInfoWindowClickListener(this.mOnClusterItemClickListener);
        //this.refreshData();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(54.3232941, 10.1381642), 12f));
        //If location permission is given show the current location
        this.maybeEnableMyLocation(map);
    }

    /**
     * Enables the MyLocation Option if LocationPermission is provided
     *
     * @param googleMap
     */
    @SuppressLint("MissingPermission")
    private void maybeEnableMyLocation(@NonNull final GoogleMap googleMap) {
        if (LocationHelper.hasLocationPermission(this.getContext())) {
            googleMap.setMyLocationEnabled(true);
        } else {
            googleMap.setMyLocationEnabled(false);
        }
    }

    @Override
    protected void updateVehicleLocations(VehicleLocations vehicleLocations) {
        if (this.getGoogleMap() == null)
            return;
        this.mClusterManager.clearItems();
        for (VehicleLocation vehicleLocation : vehicleLocations.getVehicles()) {
            if (vehicleLocation.isDeleted())
                continue;
            this.mClusterManager.addItem(new VehicleClusterItem(vehicleLocation));
        }
        this.mClusterManager.cluster();
        /*
        int i = 0;
        final int markers = this.mVehicleMarker.size();
        final List<VehicleLocation> vehicleLocationList = vehicleLocations.getVehicles();
        for (; i < vehicleLocationList.size(); i++) {
            final VehicleLocation vehicleLocation = vehicleLocationList.get(i);
            if (vehicleLocation.isDeleted())
                continue;
            Marker marker;
            if (i < markers) {
                marker = this.mVehicleMarker.get(i);
            } else {
                final MarkerOptions markerOptions= MapStyleGenerator.vehicleMarker(getResources());
                markerOptions.position(CoordinateUtil.convert(vehicleLocation));
                marker=this.getGoogleMap().addMarker(markerOptions);
                this.mClusterManager.addFavorite();
                this.mVehicleMarker.add(marker);
            }
            marker.setTag(vehicleLocation);
            marker.setTitle(vehicleLocation.getName());
            marker.setSnippet(vehicleLocation.getName());
            marker.setRotation(vehicleLocation.getHeading() - 90);
            marker.setPosition(CoordinateUtil.convert(vehicleLocation));
        }
        while(i<this.mVehicleMarker.size()) {
            this.mVehicleMarker.remove(i).remove();
        }*/
    }

    @Override
    protected void updateStops(List<Stop> stops) {
        if (this.getGoogleMap() == null)
            return;
        int i = 0;
        final int markers = this.mStopMarker.size();
        for (; i < stops.size(); i++) {
            final Stop stop = stops.get(i);
            Marker marker;
            if (i < markers) {
                marker = this.mStopMarker.get(i);
                marker.setPosition(CoordinateUtil.convert(stop));
            } else {
                final MarkerOptions markerOptions = MapStyleGenerator.stationMarker(getResources());
                markerOptions.position(CoordinateUtil.convert(stop));
                marker = this.getGoogleMap().addMarker(markerOptions);
                this.mVehicleMarker.add(marker);
            }
            marker.setTag(stop);
            marker.setTitle(stop.getName());
            marker.setSnippet(stop.getName());
        }
        while (i < this.mStopMarker.size()) {
            this.mStopMarker.remove(i).remove();
        }
    }

}
