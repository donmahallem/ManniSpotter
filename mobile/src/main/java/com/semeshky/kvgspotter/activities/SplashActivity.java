package com.semeshky.kvgspotter.activities;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.semeshky.kvgspotter.viewmodel.SplashActivityViewModel;

/**
 * Activity to be used as splashscreen on app start to display app logo
 */
public final class SplashActivity extends AppCompatActivity {
    private SplashActivityViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mViewModel = ViewModelProviders.of(this)
                .get(SplashActivityViewModel.class);
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
