package com.semeshky.kvgspotter.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.guennishueftgold.trapezeapi.FulltextSearchResult;
import com.github.guennishueftgold.trapezeapi.TripPassageStop;
import com.semeshky.kvgspotter.BR;
import com.semeshky.kvgspotter.R;
import com.semeshky.kvgspotter.database.Stop;
import com.semeshky.kvgspotter.databinding.ActivityDetailStationBinding;
import com.semeshky.kvgspotter.viewmodel.StationDetailActivityViewModel;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableSingleObserver;
import timber.log.Timber;

public final class StationDetailActivity extends AppCompatActivity {
    public final static String EXTRA_STATION_SHORT_NAME = StationDetailActivity.class.getName() + ".stop_short_name";
    public static final String EXTRA_STATION_NAME = StationDetailActivity.class.getName() + ".stop_name";
    public static final String SCENE_TRANSITION_TITLE = "transitionTitle";
    protected ActivityDetailStationBinding mBinding;
    protected StationDetailActivityViewModel mViewModel;
    protected MenuItem mFavoriteMenuItem;
    protected Disposable mErrorDisposable;

    public final static Intent createIntent(@NonNull Context context, @NonNull FulltextSearchResult fulltextSearchResult) {
        return StationDetailActivity.createIntent(context,
                fulltextSearchResult.getShortName(),
                fulltextSearchResult.getName());
    }

    public final static Intent createIntent(@NonNull Context context,
                                            @Nullable String shortName,
                                            @Nullable String name) {
        final Intent intent = new Intent(context, StationDetailActivity.class);
        intent.putExtra(EXTRA_STATION_SHORT_NAME, shortName);
        intent.putExtra(EXTRA_STATION_NAME, name);
        return intent;
    }

    public final static Intent createIntent(@NonNull Context context, @NonNull String shortName) {
        return StationDetailActivity.createIntent(context, shortName, null);
    }

    public final static Intent createIntent(@NonNull Context context) {
        return StationDetailActivity.createIntent(context, null, null);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull Stop stop) {
        return StationDetailActivity.createIntent(context, stop.getShortName(), stop.getName());
    }

    public static Intent createIntent(@NonNull Context context, @NonNull TripPassageStop station) {
        return StationDetailActivity.createIntent(context,
                station.getShortName(),
                station.getName());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail_station);
        this.mViewModel = ViewModelProviders.of(this)
                .get(StationDetailActivityViewModel.class);
        final Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            this.mViewModel.stationName.set(extras.getString(EXTRA_STATION_NAME, getString(R.string.station_name)));
            this.mViewModel.stationShortName.set(extras.getString(EXTRA_STATION_SHORT_NAME, ""));
        }
        this.setSupportActionBar(this.mBinding.toolbar);
        this.mBinding.setVariable(BR.viewModel, this.mViewModel);
        if (this.mBinding.viewPager != null) {
            final StationDetailPagerAdapter detailPagerAdapter = new StationDetailPagerAdapter(getSupportFragmentManager(), this);
            this.mBinding.viewPager.setAdapter(detailPagerAdapter);
            this.mBinding.tabLayout.setupWithViewPager(this.mBinding.viewPager);
        }
        this.mViewModel.isStationFavorited()
                .observe(this,
                        new Observer<Boolean>() {
                            @Override
                            public void onChanged(@Nullable Boolean favorited) {
                                if (favorited == null)
                                    return;
                                StationDetailActivity.this
                                        .setFavoriteDrawable(favorited);
                            }
                        });
    }


    @Override
    public void onResume() {
        super.onResume();
        this.mErrorDisposable = this.mViewModel
                .getLoadErrorFlowable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        StationDetailActivity
                                .this
                                .showNetworkErrorSnackbar();
                    }
                });
        this.mViewModel.startSyncService();
    }

    @Override
    public void onPause() {
        if (this.mErrorDisposable != null &&
                !this.mErrorDisposable.isDisposed()) {
            this.mErrorDisposable.dispose();
        }
        this.mViewModel.stopSyncService();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.station_detail, menu);
        this.mFavoriteMenuItem = menu.findItem(R.id.action_favorize);
        if (this.mViewModel.isStationFavorited().getValue() != null) {
            /**
             * Using the viewpager reinflates the menu so it needs to be set every time
             */
            this.mFavoriteMenuItem.setIcon(this.mViewModel.isStationFavorited().getValue() ?
                    R.drawable.ic_favorite_white_24dp :
                    R.drawable.ic_favorite_border_white_24dp);
        }
        return true;
    }

    private AnimatedVectorDrawableCompat getAnimatedVectorDrawable(@DrawableRes int id) {
        return AnimatedVectorDrawableCompat.create(this, id);
    }

    protected void setFavoriteDrawable(final boolean isFavorited) {
        /**
         * Menus can be sometimes not yet being inflated when this method will be called
         */
        if (this.mFavoriteMenuItem == null)
            return;
        if (isFavorited) {
            AnimatedVectorDrawableCompat drawableCompat = getAnimatedVectorDrawable(R.drawable.ic_favorite_animated_24dp);
            this.mFavoriteMenuItem.setIcon(drawableCompat);
            drawableCompat.start();
        } else {
            AnimatedVectorDrawableCompat drawableCompat = getAnimatedVectorDrawable(R.drawable.ic_unfavorite_animated_24dp);
            this.mFavoriteMenuItem.setIcon(drawableCompat);
            drawableCompat.start();
        }
    }

    private void switchFavorite() {
        this.mViewModel.toggleFavorite()
                .subscribe(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        showFavoriteStatusSnackar(aBoolean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }
                });
    }

    /**
     * @param favorited
     */
    protected void showFavoriteStatusSnackar(boolean favorited) {
        final Snackbar snackbar = Snackbar.make(this.mBinding.coordinatorLayout,
                favorited ? R.string.stop_favorited : R.string.stop_removed_from_favorites,
                Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_favorize:
                this.switchFavorite();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void showNetworkErrorSnackbar() {
        final Snackbar snackbar = Snackbar.make(this.mBinding.coordinatorLayout,
                R.string.error_loading_departures,
                Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StationDetailActivity
                        .this
                        .mViewModel
                        .refresh();
            }
        });
        snackbar.show();
    }
}
