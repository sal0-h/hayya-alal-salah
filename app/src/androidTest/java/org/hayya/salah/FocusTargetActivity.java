// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah;
/** Separate test-APK package/window. It contains no user data and ships in no app build. */
public class FocusTargetActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle saved) {
        super.onCreate(saved);
        android.widget.TextView label = new android.widget.TextView(this);
        label.setText("Synthetic Focus target");
        label.setTextSize(24);
        setContentView(label);
    }
}
