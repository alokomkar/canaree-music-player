package dev.olog.msc.presentation.sleeptimer.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import dev.olog.msc.presentation.sleeptimer.SleepTimerPickerDialog

@Module
abstract class SleepTimerInjector {
    @ContributesAndroidInjector
    abstract fun provideSleepTimerDialog() : SleepTimerPickerDialog
}