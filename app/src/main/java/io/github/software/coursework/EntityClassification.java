package io.github.software.coursework;

import java.io.*;
import java.nio.file.*;

import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;

import org.apache.commons.math3.distribution.BetaDistribution;

import java.util.stream.Collectors;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class EntityClassification {

    public static void entityClassification(String fileFolderName) {
        List<String> categories = getCategories(fileFolderName);

        System.out.println(categories);

        Map<String, String> nGramToCategory = new HashMap<>();
        Map<String, Integer> nGramFrequency = new HashMap<>();
        Map<String, Integer> nGramAccuracy = new HashMap<>();

        for (String category : categories) {
            System.out.println(category);

            Map<String, Integer> nGramMap = readText(fileFolderName, category);

            for (Map.Entry<String, Integer> entry : nGramMap.entrySet()) {
                String nGram = entry.getKey();
                Integer freq = entry.getValue();
                if (!nGramToCategory.containsKey(nGram)) {
                    nGramToCategory.put(nGram, category);
                    nGramFrequency.put(nGram, freq);
                    nGramAccuracy.put(nGram, freq);
                } else {
                    if (freq > nGramAccuracy.get(nGram)) {
                        nGramAccuracy.put(nGram, freq);
                        nGramToCategory.put(nGram, category);
                    }
                    nGramFrequency.put(nGram, freq + nGramFrequency.get(nGram));
                }
            }
        }

        Map<String, Double> nGramProbability = new HashMap<>();
        for (String nGrams : nGramToCategory.keySet()) {
            int accu = nGramFrequency.get(nGrams), freq = nGramFrequency.get(nGrams);
            nGramProbability.put(nGrams, getProbability(accu, freq - accu));
        }

        if (!Files.exists(Paths.get("build/Bayesian"))) {
            try {
                Files.createDirectories(Paths.get("build/Bayesian"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String modelParametersDirectory = "build/Bayesian/" + fileFolderName + ".txt";

        try (FileWriter writer = new FileWriter(modelParametersDirectory, false)) {
            writer.write(""); // 写入空内容
            System.out.println("Writing " + modelParametersDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileOutputStream fileStream = new FileOutputStream(modelParametersDirectory, false)) {

            PrintStream printStream = new PrintStream(fileStream);
            System.setOut(printStream);

            for (Map.Entry<String, Double> entry : nGramProbability.entrySet()) {
                printStream.println(entry.getKey() + "\t" + entry.getValue() + "\t" + nGramToCategory.get(entry.getKey()));
            }

            printStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getCategories(String fileFolderName) {
        List<String> categories = new ArrayList<>();

        System.out.println(fileFolderName);

        try (BufferedReader dirReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(EntityClassification.class.getResourceAsStream(fileFolderName))))) {

            while (true) {
                String line = dirReader.readLine();

                if (line == null) {
                    break;
                }

                categories.add(line.replaceFirst(".txt", ""));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(categories);

        return categories;
    }

    public static Map<String, Integer> readText(String fileFolderName, String category) {
        try (InputStream reader = Objects.requireNonNull(EntityClassification.class.getResourceAsStream(fileFolderName + "/" + category + ".txt"))) {
            byte[] allText = reader.readAllBytes();
            System.out.println(allText);

            String content = new String(allText, StandardCharsets.UTF_8);

            List<String> textByLines = content.lines().collect(Collectors.toList());
            Map<String, Integer> nGrams = new HashMap<>();

            for (String line : textByLines) {
//                line = line.replaceFirst("^\\d+\\.", "");

                line = line.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");

                List<String> parts = new ArrayList<>();
                Pattern pattern = Pattern.compile("([a-zA-Z0-9]+)|([\\u4e00-\\u9fa5])");
                Matcher matcher = pattern.matcher(line);

                while (matcher.find()) {
                    if (matcher.group(1) != null) {
                        parts.add(matcher.group(1)); // 字母数字子串
                    } else {
                        parts.add(matcher.group(2)); // 单个中文字符
                    }
                }

                String lst = "";
                for (String part : parts) {
                    if (lst != "") {
                        if (nGrams.containsKey(lst + part)) {
                            nGrams.put(lst + part, nGrams.get(lst + part) + 1);
                        } else {
                            nGrams.put(lst + part, 1);
                        }
                    }
                    if (nGrams.containsKey(part)) {
                        nGrams.put(part, nGrams.get(part) + 1);
                    } else {
                        nGrams.put(part, 1);
                    }
                    lst = part;
                }
            }

            return nGrams;
        } catch (IOException e) {
            e.printStackTrace();

            return new HashMap<>();
        }
    }

    public static Double getProbability(Integer x, Integer y) {
        BetaDistribution bd = new BetaDistribution(x + 1, y + 1);
        double l = 0.0, r = 1.0, mid;
        while (r - l > 0.00000001) {
            mid = (l + r) / 2;
            if (1 - bd.cumulativeProbability(mid) < 0.95) {
                r = mid;
            } else {
                l = mid;
            }
        }
        return r;
    }
}
