package io.github.software.coursework;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.FileOutputStream;
import java.io.PrintStream;

public class MathTest {

    @Test
    public void main1() {
        System.out.println("中文测试：你好，世界！");
        System.out.println("JVM 默认编码：" + System.getProperty("file.encoding"));

        // 观测数据
        int x = 10; // 类别a的样本数
        int y = 5;  // 非a的样本数

        // 先验参数（假设为Beta(1,1)）
        double alphaPrior = 1.0;
        double betaPrior = 1.0;

        // 后验参数
        double alphaPost = alphaPrior + x;
        double betaPost = betaPrior + y;

        // 创建Beta分布对象
        BetaDistribution betaDist = new BetaDistribution(alphaPost, betaPost);

        // 计算 P(a > p)
        double p = 0.6;
        double cdf = betaDist.cumulativeProbability(p); // P(a <= p)
        double posteriorProb = 1 - cdf; // P(a > p)

        System.out.printf("后验概率 P(a > %.2f) = %.4f", p, posteriorProb);

    }

    public static void main2() {
        String filePath = "app/src/test/java/io/github/software/coursework/Categories1/";
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }

            System.out.println("文件内容：\n" + content);
        } catch (IOException e) {
            System.err.println("错误: " + e.getMessage());
        }
    }

    public static void main3() {
        try (BufferedReader br = new BufferedReader(new FileReader("对象表/交通大类付款对象表.txt"))) {

            FileOutputStream fileStream = new FileOutputStream("output.txt", true);
            PrintStream printStream = new PrintStream(fileStream);

            System.setOut(printStream);

            String line;
            while ((line = br.readLine()) != null) {
                // 1. 去除行首的数字标号（如"1."）
                String processedLine = line.replaceFirst("^\\d+\\.", "");

                System.out.println(processedLine.replaceFirst("^\\s+", ""));

                // 2. 删除中文括号（全角括号）
                processedLine = processedLine.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");

                // 3. 分割字符串：字母数字串或单个中文字符
                List<String> parts = new ArrayList<>();
                Pattern pattern = Pattern.compile("([a-zA-Z0-9]+)|([\\u4e00-\\u9fa5])");
                Matcher matcher = pattern.matcher(processedLine);
                while (matcher.find()) {
                    if (matcher.group(1) != null) {
                        parts.add(matcher.group(1)); // 字母数字子串
                    } else {
                        parts.add(matcher.group(2)); // 单个中文字符
                    }
                }

                // 输出结果（示例）
//                System.out.println("原始行: " + line);
//                System.out.println("处理后: " + processedLine);
//                System.out.println("分割结果: " + parts);
//                System.out.println("----------------------");
            }

            printStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
