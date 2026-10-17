package com.lion.memscan;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    // ───── Color palette (Cyberpunk Dark) ─────
    private static final int COLOR_BG          = Color.parseColor("#0A0E1A");
    private static final int COLOR_CARD        = Color.parseColor("#131A2B");
    private static final int COLOR_CARD_ALT    = Color.parseColor("#1A2338");
    private static final int COLOR_ACCENT      = Color.parseColor("#00D9FF");
    private static final int COLOR_ACCENT_DARK = Color.parseColor("#0099CC");
    private static final int COLOR_TEXT        = Color.parseColor("#E8F0FF");
    private static final int COLOR_TEXT_DIM    = Color.parseColor("#7A8599");
    private static final int COLOR_SUCCESS     = Color.parseColor("#00FF88");
    private static final int COLOR_ERROR       = Color.parseColor("#FF3366");

    private Spinner appSpinner;
    private EditText patternInput;
    private TextView resultText;
    private TextView statusBadge;
    private Button scanBtn;
    private Button refreshBtn;

    private final List<String> appPackages = new ArrayList<>();
    private final List<String> appLabels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ───── Root container ─────
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(20), dp(30), dp(20), dp(20));

        // ───── Header ─────
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = new TextView(this);
        logo.setText("LION");
        logo.setTextSize(28);
        logo.setTextColor(COLOR_ACCENT);
        logo.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView logoSub = new TextView(this);
        logoSub.setText(" SCAN");
        logoSub.setTextSize(28);
        logoSub.setTextColor(COLOR_TEXT);
        logoSub.setTypeface(null, android.graphics.Typeface.BOLD);

        // Status badge
        statusBadge = new TextView(this);
        statusBadge.setText("● READY");
        statusBadge.setTextSize(11);
        statusBadge.setTextColor(COLOR_SUCCESS);
        statusBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        statusBadge.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.parseColor("#002233"));
        badgeBg.setCornerRadius(dp(20));
        badgeBg.setStroke(dp(1), COLOR_ACCENT);
        statusBadge.setBackground(badgeBg);

        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeParams.setMargins(dp(10), 0, 0, 0);
        statusBadge.setLayoutParams(badgeParams);

        header.addView(logo);
        header.addView(logoSub);
        header.addView(statusBadge);

        // ───── Subtitle ─────
        TextView subtitle = new TextView(this);
        subtitle.setText("Memory Scanner • External Process");
        subtitle.setTextSize(12);
        subtitle.setTextColor(COLOR_TEXT_DIM);
        subtitle.setPadding(0, dp(4), 0, dp(24));

        // ───── Card 1: Target App ─────
        LinearLayout card1 = createCard();
        TextView label1 = createLabel("TARGET APPLICATION");
        card1.addView(label1);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        appSpinner = new Spinner(this);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                0, dp(48), 1f);
        appSpinner.setLayoutParams(spinnerParams);
        appSpinner.setBackground(createRoundedBg(COLOR_CARD_ALT, COLOR_ACCENT_DARK, 1, 10));
        appSpinner.setPadding(dp(12), 0, dp(12), 0);

        refreshBtn = new Button(this);
        refreshBtn.setText("⟳");
        refreshBtn.setTextSize(20);
        refreshBtn.setTextColor(COLOR_ACCENT);
        refreshBtn.setBackground(createRoundedBg(COLOR_CARD_ALT, COLOR_ACCENT_DARK, 1, 10));
        LinearLayout.LayoutParams refreshParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        refreshParams.setMargins(dp(8), 0, 0, 0);
        refreshBtn.setLayoutParams(refreshParams);
        refreshBtn.setOnClickListener(v -> loadRunningApps());

        row1.addView(appSpinner);
        row1.addView(refreshBtn);
        card1.addView(row1);

        // ───── Card 2: Pattern ─────
        LinearLayout card2 = createCard();
        TextView label2 = createLabel("PATTERN SEARCH");
        card2.addView(label2);

        patternInput = new EditText(this);
        patternInput.setHint("เช่น libil2cpp.so หรือ 1.104.1");
        patternInput.setHintTextColor(COLOR_TEXT_DIM);
        patternInput.setTextColor(COLOR_TEXT);
        patternInput.setTextSize(14);
        patternInput.setBackground(createRoundedBg(COLOR_CARD_ALT, COLOR_ACCENT_DARK, 1, 10));
        patternInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        patternInput.setSingleLine(true);
        card2.addView(patternInput);

        // Quick chips
        LinearLayout chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(0, dp(12), 0, 0);

        String[] chips = {"libil2cpp.so", "libunity.so", "FreeFire", "1.104.1"};
        for (String chip : chips) {
            TextView chipView = new TextView(this);
            chipView.setText(chip);
            chipView.setTextSize(11);
            chipView.setTextColor(COLOR_ACCENT);
            chipView.setPadding(dp(10), dp(6), dp(10), dp(6));
            chipView.setBackground(createRoundedBg(Color.parseColor("#001A2E"), COLOR_ACCENT_DARK, 1, 20));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            chipParams.setMargins(0, 0, dp(6), 0);
            chipView.setLayoutParams(chipParams);
            chipView.setOnClickListener(v -> patternInput.setText(chip));
            chipsRow.addView(chipView);
        }
        card2.addView(chipsRow);

        // ───── Scan Button ─────
        scanBtn = new Button(this);
        scanBtn.setText("🔍  START SCAN");
        scanBtn.setTextSize(16);
        scanBtn.setTextColor(Color.WHITE);
        scanBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        GradientDrawable scanBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{COLOR_ACCENT_DARK, COLOR_ACCENT});
        scanBg.setCornerRadius(dp(12));
        scanBtn.setBackground(scanBg);
        scanBtn.setPadding(0, dp(16), 0, dp(16));
        LinearLayout.LayoutParams scanParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        scanParams.setMargins(0, dp(20), 0, dp(20));
        scanBtn.setLayoutParams(scanParams);
        scanBtn.setOnClickListener(v -> startScan());

        // ───── Result Card ─────
        LinearLayout card3 = createCard();
        TextView label3 = createLabel("RESULTS");
        card3.addView(label3);

        resultText = new TextView(this);
        resultText.setTextSize(11);
        resultText.setTextColor(COLOR_TEXT);
        resultText.setTextIsSelectable(true);
        resultText.setTypeface(android.graphics.Typeface.MONOSPACE);
        resultText.setPadding(dp(4), dp(12), dp(4), dp(12));
        resultText.setText("> Ready to scan\n> Select target app and enter pattern");

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scrollParams);
        scroll.addView(resultText);
        card3.addView(scroll);

        // ───── Add all to root ─────
        root.addView(header);
        root.addView(subtitle);
        root.addView(card1);
        root.addView(card2);
        root.addView(scanBtn);
        root.addView(card3);

        setContentView(root);
        loadRunningApps();
    }

    // ───── Helpers: UI builders ─────

    private int dp(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(createRoundedBg(COLOR_CARD, COLOR_CARD_ALT, 1, 16));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(params);
        return card;
    }

    private TextView createLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(10);
        label.setTextColor(COLOR_TEXT_DIM);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setLetterSpacing(0.15f);
        label.setPadding(0, 0, 0, dp(8));
        return label;
    }

    private GradientDrawable createRoundedBg(int fill, int stroke, int strokeWidth, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (strokeWidth > 0) d.setStroke(dp(strokeWidth), stroke);
        return d;
    }

    // ───── Load running apps ─────

    private void loadRunningApps() {
        appPackages.clear();
        appLabels.clear();

        setStatus("● LOADING", COLOR_TEXT_DIM);

        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            PackageManager pm = getPackageManager();

            List<ActivityManager.RunningAppProcessInfo> processes =
                    am.getRunningAppProcesses();

            if (processes == null) {
                Toast.makeText(this, "Cannot access running apps", Toast.LENGTH_SHORT).show();
                setStatus("● ERROR", COLOR_ERROR);
                return;
            }

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
                    label = pm.getApplicationLabel(info).toString() + "  ·  " + pkg;
                } catch (Exception ignored) {}

                appPackages.add(pkg);
                appLabels.add(label);
            }

            if (appPackages.isEmpty()) {
                appPackages.add("com.dts.freefireth");
                appLabels.add("Free Fire  ·  com.dts.freefireth");
                appPackages.add("com.dts.freefiremax");
                appLabels.add("Free Fire MAX  ·  com.dts.freefiremax");
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    this, android.R.layout.simple_spinner_item, appLabels) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView tv = (TextView) super.getView(position, convertView, parent);
                    tv.setTextColor(COLOR_TEXT);
                    tv.setTextSize(13);
                    tv.setPadding(dp(4), 0, dp(4), 0);
                    return tv;
                }
                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                    tv.setTextColor(COLOR_TEXT);
                    tv.setBackgroundColor(COLOR_CARD_ALT);
                    tv.setPadding(dp(14), dp(14), dp(14), dp(14));
                    return tv;
                }
            };
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            appSpinner.setAdapter(adapter);

            setStatus("● READY", COLOR_SUCCESS);
            Toast.makeText(this, "โหลด " + appPackages.size() + " แอป", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            setStatus("● ERROR", COLOR_ERROR);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setStatus(String text, int color) {
        statusBadge.setText(text);
        statusBadge.setTextColor(color);
    }

    private void startScan() {
        if (appPackages.isEmpty()) {
            Toast.makeText(this, "ไม่มีแอปในรายการ กด ⟳ ก่อน", Toast.LENGTH_SHORT).show();
            return;
        }

        String pattern = patternInput.getText().toString().trim();
        if (pattern.isEmpty()) {
            Toast.makeText(this, "กรุณากรอก Pattern", Toast.LENGTH_SHORT).show();
            return;
        }

        int idx = appSpinner.getSelectedItemPosition();
        String pkg = appPackages.get(idx);

        setStatus("● SCANNING", COLOR_ACCENT);
        resultText.setText("> Scanning " + pkg + " ...\n> Pattern: " + pattern + "\n\n");
        scanBtn.setEnabled(false);

        new Thread(() -> {
            final String result = scanMemory(pkg, pattern);
            runOnUiThread(() -> {
                resultText.setText("> " + result.replace("\n", "\n> "));
                scanBtn.setEnabled(true);
                if (result.contains("✅")) setStatus("● DONE", COLOR_SUCCESS);
                else setStatus("● FAILED", COLOR_ERROR);
            });
        }).start();
    }

    public native String scanMemory(String packageName, String pattern);

    static {
        System.loadLibrary("memscan");
    }
}
