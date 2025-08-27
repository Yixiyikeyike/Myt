package com.example.chapter03;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class CalculateActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tv_result;
    private String firstnum = "";
    private String oprator = "";
    private String secondnum = "";
    private String result = "";
    private String showText = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calculate);
        tv_result = findViewById(R.id.tv_result);

        // 设置所有按钮的点击监听
        int[] buttonIds = {
                R.id.btn_cancel, R.id.btn_chu, R.id.btn_chen, R.id.btn_guiling,
                R.id.btn_jia, R.id.btn_jian, R.id.btn_dian, R.id.btn_dengyu,
                R.id.btn_fenshu, R.id.btn_zero, R.id.btn_one, R.id.btn_two,
                R.id.btn_three, R.id.btn_four, R.id.btn_five, R.id.btn_six,
                R.id.btn_seven, R.id.btn_eight, R.id.btn_nine, R.id.btn_sqr
        };

        for (int id : buttonIds) {
            findViewById(id).setOnClickListener(this);
        }

        // 初始化显示为0
        refreshText("0");
    }

    @Override
    public void onClick(View v) {
        String inputtext;

        if (v.getId() == R.id.btn_sqr) {
            inputtext = "√";
        } else if (v instanceof TextView) {
            inputtext = ((TextView) v).getText().toString();
        } else {
            inputtext = "";
        }

        int viewId = v.getId();

        // 处理清除按钮
        if (viewId == R.id.btn_cancel) {
            clearCalculator();
            return;
        }

        // 处理归零按钮
        if (viewId == R.id.btn_guiling) {
            clearCalculator();
            return;
        }

        // 处理等号按钮
        if (viewId == R.id.btn_dengyu) {
            calculateResult();
            return;
        }

        // 处理运算符
        if (viewId == R.id.btn_chu || viewId == R.id.btn_chen ||
                viewId == R.id.btn_jia || viewId == R.id.btn_jian) {

            // 如果已经有第一个数字，设置运算符
            if (!firstnum.isEmpty()) {
                oprator = inputtext;
                refreshText(showText + " " + oprator + " ");
            }
            return;
        }

        // 处理小数点
        if (viewId == R.id.btn_dian) {
            handleDecimalPoint();
            return;
        }

        // 处理分数按钮（这里简化为除法）
        if (viewId == R.id.btn_fenshu) {
            if (!firstnum.isEmpty() && oprator.isEmpty()) {
                oprator = "/";
                refreshText(showText + " / ");
            }
            return;
        }

        // 处理平方根按钮
        if (viewId == R.id.btn_sqr) {
            calculateSquareRoot();
            return;
        }

        // 处理数字输入
        handleNumberInput(inputtext);
    }

    private void handleNumberInput(String input) {
        if (oprator.isEmpty()) {
            // 输入第一个数字
            if (firstnum.equals("0") && !input.equals(".")) {
                firstnum = input;
            } else {
                firstnum += input;
            }
            refreshText(firstnum);
        } else {
            // 输入第二个数字
            if (secondnum.equals("0") && !input.equals(".")) {
                secondnum = input;
            } else {
                secondnum += input;
            }
            refreshText(firstnum + " " + oprator + " " + secondnum);
        }
    }

    private void handleDecimalPoint() {
        if (oprator.isEmpty()) {
            if (!firstnum.contains(".")) {
                firstnum = firstnum.isEmpty() ? "0." : firstnum + ".";
                refreshText(firstnum);
            }
        } else {
            if (!secondnum.contains(".")) {
                secondnum = secondnum.isEmpty() ? "0." : secondnum + ".";
                refreshText(firstnum + " " + oprator + " " + secondnum);
            }
        }
    }

    private void calculateResult() {
        if (firstnum.isEmpty() || oprator.isEmpty() || secondnum.isEmpty()) {
            refreshText("输入不完整");
            return;
        }

        try {
            double num1 = Double.parseDouble(firstnum);
            double num2 = Double.parseDouble(secondnum);
            double resultValue = 0;

            switch (oprator) {
                case "+":
                    resultValue = num1 + num2;
                    break;
                case "-":
                    resultValue = num1 - num2;
                    break;
                case "×": // 注意：这里应该是乘号，检查你的字符串资源
                case "*": // 备用
                    resultValue = num1 * num2;
                    break;
                case "÷": // 注意：这里应该是除号，检查你的字符串资源
                case "/":
                    if (num2 == 0) {
                        refreshText("不能除以零");
                        return;
                    }
                    resultValue = num1 / num2;
                    break;
            }

            // 格式化结果：如果是整数就不显示小数点
            String resultStr;
            if (resultValue == (int) resultValue) {
                resultStr = String.valueOf((int) resultValue);
            } else {
                resultStr = String.valueOf(resultValue);
                // 限制小数位数
                if (resultStr.length() > 10) {
                    resultStr = String.format("%.6f", resultValue);
                }
            }

            // 保存结果，可以继续计算
            firstnum = resultStr;
            oprator = "";
            secondnum = "";
            refreshText(resultStr);

        } catch (NumberFormatException e) {
            refreshText("数字格式错误");
        }
    }

    private void calculateSquareRoot() {
        if (oprator.isEmpty() && !firstnum.isEmpty()) {
            try {
                double num = Double.parseDouble(firstnum);
                if (num < 0) {
                    refreshText("负数不能开平方");
                    return;
                }
                double sqrtResult = Math.sqrt(num);

                String resultStr;
                if (sqrtResult == (int) sqrtResult) {
                    resultStr = String.valueOf((int) sqrtResult);
                } else {
                    resultStr = String.format("%.6f", sqrtResult);
                }

                refreshText("√(" + firstnum + ") = " + resultStr);
                firstnum = resultStr; // 结果可以作为下一次计算的第一个数

            } catch (NumberFormatException e) {
                refreshText("数字格式错误");
            }
        } else {
            refreshText("请在输入数字后使用√");
        }
    }

    private void clearCalculator() {
        firstnum = "";
        oprator = "";
        secondnum = "";
        result = "";
        refreshText("0");
    }

    private void refreshText(String text) {
        showText = text;
        tv_result.setText(showText);
    }
}