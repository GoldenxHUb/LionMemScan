package com.lion.memscan;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int COLOR_BG       = 0xFF0A0E1A;
    private static final int COLOR_CARD     = 0xFF131A2B;
    private static final int COLOR_CARD_ALT = 0xFF1A2338;
    private static final int COLOR_ACCENT   = 0xFF00D9FF;
    private static final int COLOR_ACCENT_D = 0xFF0099CC;
    private static final int COLOR_TEXT     = 0xFFE8F0FF;
    private static final int COLOR_TEXT_DIM = 0xFF7A8599;
    private static final int COLOR_SUCCESS  = 0xFF00FF88;

    private Spinner appSpinner;
    private TextView resultText;
    private TextView statusBadge;
    private Button scanBtn;

    private final List<String> appPackages = new ArrayList<>();
    private final List<String> appLabels = new ArrayList<>();

    private int dp(int v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable roundedBg(int fill, int stroke, int sw, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (sw > 0) d.setStroke(dp(sw), stroke);
        return d;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(20), dp(30), dp(20), dp(20));

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = new TextView(this);
        logo.setText("LION");
        logo.setTextSize(28);
        logo.setTextColor(COLOR_ACCENT);
        logo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView logoSub = new TextView(this);
        logoSub.setText(" SPY");
        logoSub.setTextSize(28);
        logoSub.setTextColor(COLOR_TEXT);
        logoSub.setTypeface(null, android.graphics.Typeface.BOLD);

        statusBadge = new TextView(this);
        statusBadge.setText("READY");
        statusBadge.setTextSize(11);
        statusBadge.setTextColor(COLOR_SUCCESS);
        statusBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        statusBadge.setBackground(roundedBg(0xFF002233, COLOR_ACCENT, 1, 20));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(dp(10), 0, 0, 0);
        statusBadge.setLayoutParams(bp);

        header.addView(logo);
        header.addView(logoSub);
        header.addView(statusBadge);

        TextView subtitle = new TextView(this);
        subtitle.setText("External Memory Spy - อ่านค่าจริงจากเกม");
        subtitle.setTextSize(12);
        subtitle.setTextColor(COLOR_TEXT_DIM);
        subtitle.setPadding(0, dp(4), 0, dp(24));

        // Target
        LinearLayout card1 = new LinearLayout(this);
        card1.setOrientation(LinearLayout.VERTICAL);
        card1.setBackground(roundedBg(COLOR_CARD, COLOR_CARD_ALT, 1, 16));
        card1.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p1.setMargins(0, 0, 0, dp(16));
        card1.setLayoutParams(p1);

        TextView label1 = new TextView(this);
        label1.setText("TARGET GAME");
        label1.setTextSize(10);
        label1.setTextColor(COLOR_TEXT_DIM);
        label1.setTypeface(null, android.graphics.Typeface.BOLD);
        card1.addView(label1);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        appSpinner = new Spinner(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(48), 1f);
        appSpinner.setLayoutParams(sp);

        Button refreshBtn = new Button(this);
        refreshBtn.setText("R");
        refreshBtn.setTextColor(COLOR_ACCENT);
        refreshBtn.setBackground(roundedBg(COLOR_CARD_ALT, COLOR_ACCENT_D, 1, 10));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(48), dp(48));
        rp.setMargins(dp(8), 0, 0, 0);
        refreshBtn.setLayoutParams(rp);
        refreshBtn.setOnClickListener(v -> loadRunningApps());

        row1.addView(appSpinner);
        row1.addView(refreshBtn);
        card1.addView(row1);

        // Scan button
        scanBtn = new Button(this);
        scanBtn.setText("🕵️ START SPY");
        scanBtn.setTextSize(16);
        scanBtn.setTextColor(Color.WHITE);
        scanBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable scanBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{COLOR_ACCENT_D, COLOR_ACCENT});
        scanBg.setCornerRadius(dp(12));
        scanBtn.setBackground(scanBg);
        LinearLayout.LayoutParams scanP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        scanP.setMargins(0, 0, 0, dp(20));
        scanBtn.setLayoutParams(scanP);
        scanBtn.setOnClickListener(v -> startScan());

        // Results
        LinearLayout card3 = new LinearLayout(this);
        card3.setOrientation(LinearLayout.VERTICAL);
        card3.setBackground(roundedBg(COLOR_CARD, COLOR_CARD_ALT, 1, 16));
        card3.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        card3.setLayoutParams(p3);

        TextView label3 = new TextView(this);
        label3.setText("LIVE SPY DATA");
        label3.setTextSize(10);
        label3.setTextColor(COLOR_TEXT_DIM);
        label3.setTypeface(null, android.graphics.Typeface.BOLD);
        card3.addView(label3);

        resultText = new TextView(this);
        resultText.setTextSize(11);
        resultText.setTextColor(COLOR_TEXT);
        resultText.setTextIsSelectable(true);
        resultText.setTypeface(android.graphics.Typeface.MONOSPACE);
        resultText.setText("Ready to spy");

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scl = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scl);
        scroll.addView(resultText);
        card3.addView(scroll);

        root.addView(header);
        root.addView(subtitle);
        root.addView(card1);
        root.addView(scanBtn);
        root.addView(card3);

        setContentView(root);
        loadRunningApps();
    }

    private void loadRunningApps() {
        appPackages.clear();
        appLabels.clear();
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            PackageManager pm = getPackageManager();
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes == null) return;

            for (ActivityManager.RunningAppProcessInfo proc : processes) {
                if (proc.pkgList.length == 0) continue;
                String pkg = proc.pkgList[0];
                if (appPackages.contains(pkg)) continue;
                if (pkg.equals(getPackageName())) continue;
                if (pkg.startsWith("android") || pkg.startsWith("com.android")
                        || pkg.startsWith("com.google.android")
                        || pkg.startsWith("com.oplus")) continue;

                String label = pkg;
                try {
                    ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                    label = pm.getApplicationLabel(info).toString() + " (" + pkg + ")";
                } catch (Exception ignored) {}

                appPackages.add(pkg);
                appLabels.add(label);
            }

            if (appPackages.isEmpty()) {
                appPackages.add("com.dts.freefireth");
                appLabels.add("Free Fire (com.dts.freefireth)");
                appPackages.add("com.dts.freefiremax");
                appLabels.add("Free Fire MAX (com.dts.freefiremax)");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, appLabels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            appSpinner.setAdapter(adapter);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startScan() {
        if (appPackages.isEmpty()) return;
        int idx = appSpinner.getSelectedItemPosition();
        String pkg = appPackages.get(idx);

        statusBadge.setText("SPYING");
        statusBadge.setTextColor(COLOR_ACCENT);
        resultText.setText("Spying " + pkg + "...\n");
        scanBtn.setEnabled(false);

        new Thread(() -> {
            final String result = scanMemory(pkg, "spy");
            runOnUiThread(() -> {
                resultText.setText(result);
                scanBtn.setEnabled(true);
                statusBadge.setText("DONE");
                statusBadge.setTextColor(COLOR_SUCCESS);
            });
        }).start();
    }

    public native String scanMemory(String packageName, String pattern);

    static {
        System.loadLibrary("memscan");
    }
}
