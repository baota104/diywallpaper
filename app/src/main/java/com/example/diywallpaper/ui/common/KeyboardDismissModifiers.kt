package com.example.diywallpaper.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@Composable
fun Modifier.hideKeyboardOnTapOutside(): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    return pointerInput(focusManager, keyboardController) {
        detectTapGestures(
            onTap = {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        )
    }
}

fun Modifier.consumeTapForKeyboardDismiss(): Modifier {
    return pointerInput(Unit) {
        detectTapGestures(onTap = { })
    }
}
