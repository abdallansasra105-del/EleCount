package com.example.elecount.Hellper;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * مساعد لتوحيد لون شريط الحالة مع الهيدر
 *
 * الشرح: يمدّ الهيدر خلف شريط الحالة بلون واحد
 * حتى لا يظهر شريط أبيض منفصل عن الـ Toolbar
 */
public final class SystemBarsHelper {

    private SystemBarsHelper() {
    }

    /**
     * يجعل شريط الحالة شفافاً ويمدّ لون الهيدر خلفه
     *
     * @param activity النشاط الحالي
     * @param headerView عنصر الهيدر (Toolbar أو حاويته)
     */
    public static void applyColoredHeader(Activity activity, View headerView) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
        }

        ViewCompat.setOnApplyWindowInsetsListener(headerView, (view, windowInsets) -> {
            Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(
                    view.getPaddingLeft(),
                    statusBars.top,
                    view.getPaddingRight(),
                    view.getPaddingBottom()
            );
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(headerView);
    }
}
