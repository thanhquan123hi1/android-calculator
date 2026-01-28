package com.thanhquan123hi1.calculator;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private TextView equationText;
    private TextView resultText;
    private MaterialSwitch themeSwitch;

    private String currentNumber = "";
    private String operand1 = "";
    private String operator = "";
    private boolean isResultDisplayed = false;

    private final DecimalFormat decimalFormat = new DecimalFormat("#,###.########");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        equationText = findViewById(R.id.equationText);
        resultText = findViewById(R.id.resultText);
        themeSwitch = findViewById(R.id.themeSwitch);

        // Initialize theme switch state
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        themeSwitch.setChecked(currentNightMode == Configuration.UI_MODE_NIGHT_YES);

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        setNumberListeners();
        setOperationListeners();
    }

    private void setNumberListeners() {
        int[] numberIds = {
                R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
                R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9, R.id.btnDot
        };

        View.OnClickListener listener = v -> {
            Button b = (Button) v;
            String text = b.getText().toString();

            if (isResultDisplayed) {
                currentNumber = "";
                isResultDisplayed = false;
                equationText.setText("");
                operand1 = "";
                operator = "";
            }

            if (text.equals(".")) {
                if (!currentNumber.contains(".")) {
                    if (currentNumber.isEmpty())
                        currentNumber = "0";
                    currentNumber += ".";
                }
            } else {
                if (currentNumber.equals("0"))
                    currentNumber = text;
                else
                    currentNumber += text;
            }
            updateResultText(currentNumber);
        };

        for (int id : numberIds) {
            findViewById(id).setOnClickListener(listener);
        }
    }

    private void setOperationListeners() {
        findViewById(R.id.btnAC).setOnClickListener(v -> {
            currentNumber = "";
            operand1 = "";
            operator = "";
            equationText.setText("");
            resultText.setText("0");
        });

        findViewById(R.id.btnBackspace).setOnClickListener(v -> {
            if (!currentNumber.isEmpty()) {
                currentNumber = currentNumber.substring(0, currentNumber.length() - 1);
                if (currentNumber.isEmpty())
                    updateResultText("0");
                else
                    updateResultText(currentNumber);
            }
        });

        findViewById(R.id.btnPlusMinus).setOnClickListener(v -> {
            if (!currentNumber.isEmpty() && !currentNumber.equals("0")) {
                if (currentNumber.startsWith("-")) {
                    currentNumber = currentNumber.substring(1);
                } else {
                    currentNumber = "-" + currentNumber;
                }
                updateResultText(currentNumber);
            }
        });

        findViewById(R.id.btnPercent).setOnClickListener(v -> {
            if (!currentNumber.isEmpty()) {
                try {
                    double val = Double.parseDouble(currentNumber);
                    val = val / 100;
                    currentNumber = String.valueOf(val);
                    if (currentNumber.endsWith(".0")) {
                        currentNumber = currentNumber.substring(0, currentNumber.length() - 2);
                    }
                    updateResultText(currentNumber);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        });

        View.OnClickListener opListener = v -> {
            Button b = (Button) v;
            String op = b.getText().toString();

            if (!currentNumber.isEmpty()) {
                if (!operand1.isEmpty()) {
                    // Chained operation? For now just handle one step or update operand1
                    // Let's behave like standard: calculate current pair, then set result as
                    // operand1
                    // operand1
                    calculate();
                    if (operand1.equals("Error")) {
                        currentNumber = "";
                        operator = "";
                        isResultDisplayed = true;
                        return;
                    }
                } else {
                    operand1 = currentNumber;
                }
                currentNumber = "";
                operator = op;
                // e.g. "123 +"
                equationText.setText(formatNumber(operand1) + " " + operator);
                // Result text can stay showing operand1 or clear. Standard usually clears or
                // keeps showing until new number.
                // We'll keep showing it but next number input will clear/append to new
                // currentNumber.
            } else if (!operand1.isEmpty()) {
                // Change operator
                operator = op;
                equationText.setText(formatNumber(operand1) + " " + operator);
            }
        };

        findViewById(R.id.btnDivide).setOnClickListener(opListener);
        findViewById(R.id.btnMultiply).setOnClickListener(opListener);
        findViewById(R.id.btnMinus).setOnClickListener(opListener);
        findViewById(R.id.btnPlus).setOnClickListener(opListener);

        findViewById(R.id.btnEquals).setOnClickListener(v -> {
            if (!currentNumber.isEmpty() && !operand1.isEmpty() && !operator.isEmpty()) {
                String op2 = currentNumber;
                calculate();
                if (operand1.equals("Error")) {
                    currentNumber = "";
                    operator = "";
                    isResultDisplayed = true;
                    return;
                }
                // After calculate, operand1 has the result, currentNumber is empty (or result?)
                // Standard behavior:
                // Equation: "Op1 op Op2"
                // Result: "Result"
                // And state is reset so next number writes new, or next op uses result.
                equationText.setText(formatNumber(operand1) + " " + operator + " " + formatNumber(op2));
                currentNumber = operand1; // Keep result as current number for chaining?
                operand1 = "";
                operator = "";
                // But wait, if I type number now, it should clear.
                isResultDisplayed = true;
            }
        });
    }

    private void calculate() {
        if (operand1.isEmpty() || currentNumber.isEmpty() || operand1.equals("Error"))
            return;

        double n1 = 0;
        double n2 = 0;
        try {
            n1 = Double.parseDouble(operand1);
            n2 = Double.parseDouble(currentNumber);
        } catch (NumberFormatException e) {
            operand1 = "Error";
            updateResultText("Error");
            return;
        }

        double res = 0;

        switch (operator) {
            case "/":
                if (n2 != 0)
                    res = n1 / n2;
                else {
                    operand1 = "Error";
                    updateResultText("Error");
                    return;
                }
                break;
            case "×":
                res = n1 * n2;
                break;
            case "-":
                res = n1 - n2;
                break;
            case "+":
                res = n1 + n2;
                break;
        }

        operand1 = String.valueOf(res);
        // Remove trailing .0
        if (operand1.endsWith(".0")) {
            operand1 = operand1.substring(0, operand1.length() - 2);
        }
        updateResultText(operand1);
    }

    private void updateResultText(String val) {
        if (val.isEmpty()) {
            resultText.setText("0");
            return;
        }
        resultText.setText(formatNumber(val));
    }

    private String formatNumber(String val) {
        if (val.equals("Error"))
            return "Error";
        try {
            double d = Double.parseDouble(val);
            return decimalFormat.format(d);
        } catch (Exception e) {
            return val;
        }
    }
}