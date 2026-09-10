package com.vakit.widget.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vakit.widget.VakitApplication

/**
 * Small helper that builds a [ViewModelProvider.Factory] resolving the
 * [VakitApplication] from the CreationExtras, so screens can construct their
 * view models with the app container.
 */
inline fun <reified VM : ViewModel> appViewModelFactory(
    crossinline create: (VakitApplication) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VakitApplication
        create(app)
    }
}