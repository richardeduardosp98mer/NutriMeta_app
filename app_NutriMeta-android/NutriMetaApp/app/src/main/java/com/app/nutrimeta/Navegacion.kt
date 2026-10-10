
package com.app.nutrimeta

import android.app.Activity
import android.content.Intent
import com.app.nutrimeta.dashboard.DashboardActivity

object Navegacion {

    fun abrirDashboard(activity: Activity) {

        val intent = Intent(
            activity,
            DashboardActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        activity.startActivity(intent)
        activity.finish()
    }
}
