package com.m3sv.plainupnp.di

import android.content.Intent
import com.m3sv.plainupnp.common.preferences.PreferencesRepository
import com.m3sv.plainupnp.presentation.onboarding.OnboardingManager
import com.m3sv.selectcontentdirectory.SelectContentDirectoryActivity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOnboardingManager(preferencesRepository: PreferencesRepository): OnboardingManager =
        OnboardingManager(preferencesRepository = preferencesRepository) { activity ->
            activity.startActivity(Intent(activity, SelectContentDirectoryActivity::class.java))
        }
}
