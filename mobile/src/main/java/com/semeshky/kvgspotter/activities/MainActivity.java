package com.semeshky.kvgspotter.activities;

import android.Manifest;
import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.semeshky.kvgspotter.R;
import com.semeshky.kvgspotter.adapter.HomeAdapter;
import com.semeshky.kvgspotter.databinding.ActivityMainBinding;
import com.semeshky.kvgspotter.fragments.RequestLocationPermissionDialogFragment;
import com.semeshky.kvgspotter.location.LocationHelper;
import com.semeshky.kvgspotter.presenter.MainActivityPresenter;
import com.semeshky.kvgspotter.viewmodel.MainActivityViewModel;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ACCESS_LOCATION = 2928;
    private final HomeAdapter.HomeAdapterEventListener mFavoriteSelectedListener = new HomeAdapter.HomeAdapterEventListener() {
        @Override
        public void onFavoriteSelected(@NonNull String shortName, @Nullable String name) {
            final Intent intent = StationDetailActivity.createIntent(MainActivity.this,
                    shortName,
                    name);
            startActivity(intent);
        }

        @Override
        public void onRequestPermission() {
            MainActivity
                    .this
                    .requestLocationPermission();
        }
    };
    private final Consumer<Throwable> LOCATION_THROWABLE_CONSUMER = new Consumer<Throwable>() {
        @Override
        public void accept(Throwable throwable) throws Exception {
            Timber.e(throwable);
        }
    };
    private ActivityMainBinding mBinding;
    private MainActivityPresenter mMainActivityPresenter;
    private MainActivityViewModel mViewModel;
    private HomeAdapter mHomeAdapter;
    private Disposable mFavoriteDisposable;
    private LocationHelper mLocationHelper;
    private Disposable mNearbyDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        this.mViewModel = ViewModelProviders.of(this)
                .get(MainActivityViewModel.class);
        this.setSupportActionBar((Toolbar) this.mBinding.toolbar.getRoot());
        this.mMainActivityPresenter = new MainActivityPresenter(this);
        this.mHomeAdapter = new HomeAdapter(this.mFavoriteSelectedListener);
        this.mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.mBinding.recyclerView.setAdapter(this.mHomeAdapter);
        this.mBinding.setPresenter(this.mMainActivityPresenter);
        this.mBinding.executePendingBindings();
        this.mLocationHelper = new LocationHelper(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, StationSearchActivity.class)));
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_live_map:
                startActivity(new Intent(this, LiveMapActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, PreferencesActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Timber.d("approved perms");
                } else {
                    Timber.d("Location permission denied");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    private void requestLocationPermission() {
        this.requestLocationPermission(true);
    }

    private void requestLocationPermission(boolean showDialog) {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) && showDialog) {
                showRequestPermissionDialog();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_LOCATION);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Flowable<List<HomeAdapter.DistanceStop>> favoriteFlowable;
        if (LocationHelper.hasLocationPermission(this)) {
            this.mHomeAdapter.setHasLocationPermission(true);
            this.mNearbyDisposable = this.mViewModel.createNearbyFlowable(this.mLocationHelper.getLocationFlowable())
                    .subscribe(new Consumer<List<HomeAdapter.DistanceStop>>() {
                        @Override
                        public void accept(List<HomeAdapter.DistanceStop> distanceStops) throws Exception {
                            mHomeAdapter.setNearby(distanceStops);
                        }
                    }, LOCATION_THROWABLE_CONSUMER);
            favoriteFlowable = this.mViewModel.getFavoriteFlowable(this.mLocationHelper.getLocationFlowable());
        } else {
            this.mHomeAdapter.setHasLocationPermission(false);
            favoriteFlowable = this.mViewModel.getFavoriteFlowable(null);
        }
        this.mFavoriteDisposable = favoriteFlowable
                .subscribe(new Consumer<List<HomeAdapter.DistanceStop>>() {
                    @Override
                    public void accept(List<HomeAdapter.DistanceStop> distanceStops) throws Exception {
                        mHomeAdapter.setFavorites(distanceStops);
                    }
                }, LOCATION_THROWABLE_CONSUMER);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mFavoriteDisposable != null && !this.mFavoriteDisposable.isDisposed())
            this.mFavoriteDisposable.dispose();
        if (this.mNearbyDisposable != null && !this.mNearbyDisposable.isDisposed())
            this.mNearbyDisposable.dispose();
    }

    private void showRequestPermissionDialog() {
        RequestLocationPermissionDialogFragment dialog = new RequestLocationPermissionDialogFragment();
        dialog.setOnLocationRequestDialogListener(new RequestLocationPermissionDialogFragment.OnLocationRequestDialogListener() {
            @Override
            public void onApproveRequest(boolean approved) {
                if (!approved)
                    return;
                MainActivity
                        .this
                        .requestLocationPermission(false);
            }
        });
        dialog.show(this.getSupportFragmentManager(), "ask_for_location");
    }
}
