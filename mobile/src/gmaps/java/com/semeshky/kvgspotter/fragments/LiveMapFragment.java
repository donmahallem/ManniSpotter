package com.semeshky.kvgspotter.fragments;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.semeshky.kvg.kvgapi.VehicleLocation;
import com.semeshky.kvg.kvgapi.VehicleLocations;
import com.semeshky.kvgspotter.R;
import com.semeshky.kvgspotter.activities.StationDetailActivity;
import com.semeshky.kvgspotter.activities.TripPassagesActivity;
import com.semeshky.kvgspotter.database.Stop;
import com.semeshky.kvgspotter.map.CoordinateUtil;
import com.semeshky.kvgspotter.map.DualOnMarkerClickListener;
import com.semeshky.kvgspotter.map.MapStyleGenerator;
import com.semeshky.kvgspotter.map.VehicleClusterItem;
import com.semeshky.kvgspotter.map.VehicleClusterRenderer;

import java.util.ArrayList;
import java.util.List;

public final class LiveMapFragment extends BaseLiveMapFragment {
    private ClusterManager<VehicleClusterItem> mClusterManager;
    private final GoogleMap.OnMarkerClickListener mOnMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            if (marker.getTag() instanceof Stop) {
                startActivity(StationDetailActivity
                        .createIntent(getContext(),
                                (Stop) marker.getTag()));
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

    private List<Marker> mVehicleMarker=new ArrayList<>();
    private List<Marker> mStopMarker=new ArrayList<>();
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
        //this.mClusterManager.setOnClusterItemClickListener(this.mOnClusterItemClickListener);
        this.mClusterManager.setOnClusterItemInfoWindowClickListener(this.mOnClusterItemClickListener);
        //this.refreshData();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(54.3232941, 10.1381642), 12f));
    }

    @Override
    protected void updateVehicleLocations(VehicleLocations vehicleLocations) {
        this.mClusterManager.clearItems();
        for(VehicleLocation vehicleLocation:vehicleLocations.getVehicles()){
            if(vehicleLocation.isDeleted())
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
                this.mClusterManager.addItem();
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
        int i = 0;
        final int markers = this.mStopMarker.size();
        for (; i < stops.size(); i++) {
            final Stop stop = stops.get(i);
            Marker marker;
            if (i < markers) {
                marker = this.mStopMarker.get(i);
                marker.setPosition(CoordinateUtil.convert(stop));
            } else {
                final MarkerOptions markerOptions= MapStyleGenerator.stationMarker(getResources());
                markerOptions.position(CoordinateUtil.convert(stop));
                marker=this.getGoogleMap().addMarker(markerOptions);
                this.mVehicleMarker.add(marker);
            }
            marker.setTag(stop);
            marker.setTitle(stop.getName());
            marker.setSnippet(stop.getName());
        }
        while(i<this.mStopMarker.size()) {
            this.mStopMarker.remove(i).remove();
        }
    }

}
