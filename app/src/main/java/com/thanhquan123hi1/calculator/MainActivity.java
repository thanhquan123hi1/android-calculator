package com.thanhquan123hi1.calculator;

import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView equationText; // Input (raw)
    private TextView resultText;   // Preview / Final
    private MaterialSwitch themeSwitch;

    private String expression = "";
    private boolean isResultFinalized = false;

    private final DecimalFormat df = new DecimalFormat("#,###.########");

    // UI constants
    private static final float RESULT_PREVIEW_SIZE = 30f;
    private static final float RESULT_FINAL_SIZE = 48f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        equationText = findViewById(R.id.equationText);
        resultText = findViewById(R.id.resultText);
        themeSwitch = findViewById(R.id.themeSwitch);

        initTheme();
        bindButtons();
        resetResultStyle();
        render();
    }

    /* ================= THEME ================= */

    private void initTheme() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        themeSwitch.setChecked(mode == Configuration.UI_MODE_NIGHT_YES);

        themeSwitch.setOnCheckedChangeListener((v, checked) ->
                AppCompatDelegate.setDefaultNightMode(
                        checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                )
        );
    }

    /* ================= BUTTONS ================= */

    private void bindButtons() {

        int[] nums = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        for (int id : nums) {
            findViewById(id).setOnClickListener(v ->
                    appendDigit(((Button) v).getText().toString()));
        }

        findViewById(R.id.btnDot).setOnClickListener(v -> appendDot());

        int[] ops = {R.id.btnPlus, R.id.btnMinus, R.id.btnMultiply, R.id.btnDivide};
        for (int id : ops) {
            findViewById(id).setOnClickListener(v ->
                    appendOperator(((Button) v).getText().toString()));
        }

        findViewById(R.id.btnEquals).setOnClickListener(v -> onEquals());

        findViewById(R.id.btnBackspace).setOnClickListener(v -> {
            if (!expression.isEmpty()) {
                expression = expression.substring(0, expression.length() - 1);
                render();
            }
        });

        findViewById(R.id.btnAC).setOnClickListener(v -> resetAll());

        findViewById(R.id.btnPlusMinus).setOnClickListener(v -> {
            toggleSign();
            render();
        });

        findViewById(R.id.btnPercent).setOnClickListener(v -> {
            applyPercent();
            render();
        });
    }

    /* ================= INPUT ================= */

    private void appendDigit(String d) {

        if (isResultFinalized) {
            expression = "";
            isResultFinalized = false;
            resetResultStyle();
            equationText.setVisibility(View.VISIBLE);
        }

        String last = lastNumber();
        if ("0".equals(last) && !"0".equals(d)) {
            replaceLastNumber(d);
        } else {
            expression += d;
        }
        render();
    }

    private void appendDot() {
        if (isResultFinalized) return;

        String last = lastNumber();
        if (last.isEmpty()) expression += "0.";
        else if (!last.contains(".")) expression += ".";
        render();
    }

    private void appendOperator(String op) {
        if (isResultFinalized) {
            isResultFinalized = false;
            resetResultStyle();
            equationText.setVisibility(View.VISIBLE);
        }

        if (expression.isEmpty()) {
            if ("-".equals(op)) expression = "-";
            render();
            return;
        }

        char last = expression.charAt(expression.length() - 1);
        if (isOperator(last)) {
            expression = expression.substring(0, expression.length() - 1) + op;
        } else {
            expression += op;
        }
        render();
    }

    /* ================= EQUALS ================= */

    private void onEquals() {
        Double v = evaluate();
        if (v == null) return;

        // FINAL RESULT
        resultText.setText(df.format(v));
        resultText.setAlpha(1f);
        resultText.setTypeface(null, Typeface.BOLD);
        resultText.setTextSize(TypedValue.COMPLEX_UNIT_SP, RESULT_FINAL_SIZE);

        equationText.setText("");
        equationText.setVisibility(View.GONE);

        expression = removeDotZero(v);
        isResultFinalized = true;
    }

    /* ================= RENDER ================= */

    private void render() {
        equationText.setText(expression);

        Double v = evaluate();
        if (v != null && !isResultFinalized) {
            resultText.setText(df.format(v));
        } else if (!isResultFinalized) {
            String last = lastNumber();
            resultText.setText(last.isEmpty() || "-".equals(last) ? "0" : df.format(Double.parseDouble(last)));
        }
    }

    /* ================= EVALUATION ================= */

    private Double evaluate() {
        try {
            if (expression.isEmpty()) return null;
            char last = expression.charAt(expression.length() - 1);
            if (isOperator(last) || "-".equals(expression)) return null;

            List<String> tokens = tokenize(expression);
            if (tokens.size() < 3) return null;

            return eval(tokens);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        StringBuilder num = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                num.append(c);
            } else if (isOperator(c)) {
                if (num.length() > 0) {
                    tokens.add(num.toString());
                    num.setLength(0);
                }
                tokens.add(String.valueOf(c));
            }
        }
        if (num.length() > 0) tokens.add(num.toString());
        return tokens;
    }

    private double eval(List<String> tokens) {
        List<String> rpn = new ArrayList<>();
        Deque<String> ops = new ArrayDeque<>();

        for (String t : tokens) {
            if (!isOpToken(t)) rpn.add(t);
            else {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(t))
                    rpn.add(ops.pop());
                ops.push(t);
            }
        }
        while (!ops.isEmpty()) rpn.add(ops.pop());

        Deque<Double> st = new ArrayDeque<>();
        for (String t : rpn) {
            if (!isOpToken(t)) st.push(Double.parseDouble(t));
            else {
                double b = st.pop(), a = st.pop();
                st.push(
                        "+".equals(t) ? a + b :
                                "-".equals(t) ? a - b :
                                        "×".equals(t) ? a * b : a / b
                );
            }
        }
        return st.pop();
    }

    private int precedence(String op) {
        return ("×".equals(op) || "/".equals(op)) ? 2 : 1;
    }

    /* ================= UTIL ================= */

    private void resetAll() {
        expression = "";
        isResultFinalized = false;
        resetResultStyle();
        equationText.setVisibility(View.VISIBLE);
        render();
    }

    private void resetResultStyle() {
        resultText.setAlpha(0.5f);
        resultText.setTypeface(null, Typeface.NORMAL);
        resultText.setTextSize(TypedValue.COMPLEX_UNIT_SP, RESULT_PREVIEW_SIZE);
    }

    private boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '×' || c == '/';
    }

    private boolean isOpToken(String s) {
        return "+".equals(s) || "-".equals(s) || "×".equals(s) || "/".equals(s);
    }

    private String lastNumber() {
        int i = expression.length() - 1;
        while (i >= 0 && (Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '.')) i--;
        return expression.substring(i + 1);
    }

    private void replaceLastNumber(String s) {
        String last = lastNumber();
        expression = expression.substring(0, expression.length() - last.length()) + s;
    }

    private void toggleSign() {
        String last = lastNumber();
        if (last.isEmpty()) return;
        replaceLastNumber(last.startsWith("-") ? last.substring(1) : "-" + last);
    }

    private void applyPercent() {
        String last = lastNumber();
        if (last.isEmpty()) return;
        replaceLastNumber(removeDotZero(Double.parseDouble(last) / 100));
    }

    private String removeDotZero(double v) {
        String s = String.valueOf(v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
