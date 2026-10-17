package com.lion.memscan;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText packageInput, patternInput;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView label1 = new TextView(this);
        label1.setText("Package Name:");
        packageInput = new EditText(this);
        packageInput.setText("com.dts.freefiremax");

        TextView label2 = new TextView(this);
        label2.setText("Pattern (String):");
        patternInput = new EditText(this);
        patternInput.setHint("เช่น 1.104.1 หรือ 2019118045");

        Button scanBtn = new Button(this);
        scanBtn.setText("🔍 Scan Memory");
        scanBtn.setOnClickListener(v -> {
            String pkg = packageInput.getText().toString().trim();
            String pat = patternInput.getText().toString().trim();
            if (pkg.isEmpty() || pat.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกให้ครบ", Toast.LENGTH_SHORT).show();
                return;
            }
            resultText.setText("Scanning...");
            scanBtn.setEnabled(false);

            new Thread(() -> {
                final String result = scanMemory(pkg, pat);
                runOnUiThread(() -> {
                    resultText.setText(result);
                    scanBtn.setEnabled(true);
                });
            }).start();
        });

        resultText = new TextView(this);
        resultText.setTextSize(11);
        resultText.setTextIsSelectable(true);
        resultText.setPadding(0, 30, 0, 0);
        resultText.setText("Ready");

        ScrollView scroll = new ScrollView(this);
        scroll.addView(resultText);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.addView(label1);
        layout.addView(packageInput);
        layout.addView(label2);
        layout.addView(patternInput);
        layout.addView(scanBtn);
        layout.addView(scroll);

        setContentView(layout);
    }

    public native String scanMemory(String packageName, String pattern);

    static {
        System.loadLibrary("memscan");
    }
}
