package com.example.reddit

import android.app.Activity
import android.app.AlertDialog
import com.example.reddit.R // Replace with your actual package name

class LoadingDialog(val activity: Activity) {
    private lateinit var dialog: AlertDialog

    fun startLoading() {
        val inflater = activity.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_loading, null)

        val builder = AlertDialog.Builder(activity)
        builder.setView(dialogView)
        // CRITICAL: Set to false so the user cannot tap outside to dismiss it and click buttons twice
        builder.setCancelable(false)

        dialog = builder.create()
        // Makes the white square background of the dialog disappear
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    fun dismissDialog() {
        if (this::dialog.isInitialized && dialog.isShowing) {
            dialog.dismiss()
        }
    }
}