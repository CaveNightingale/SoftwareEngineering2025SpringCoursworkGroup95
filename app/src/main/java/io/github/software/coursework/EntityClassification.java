package io.github.software.coursework;

import java.io.*;
import java.nio.file.*;

import java.util.*;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.distribution.BetaDistribution;

import java.util.stream.Collectors;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;

import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class EntityClassification {

    private static final Logger logger = Logger.getLogger("EntityClassification");

    private static final ClassPath classPath;
    private static final Set<ClassPath.ResourceInfo> resources;
    static {
        try {
            classPath = ClassPath.from(EntityClassification.class.getClassLoader());
            resources = classPath.getResources().stream().filter(r -> r.getResourceName().startsWith("io/github/software/coursework/")).collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Triple<String, Double, String>> entityClassification(String fileFolderName) {
        List<String> categories = getCategories(fileFolderName);

//        System.out.println(categories);

        Map<String, String> nGramToCategory = new HashMap<>();
        Map<String, Integer> nGramFrequency = new HashMap<>();
        Map<String, Integer> nGramAccuracy = new HashMap<>();

        for (String category : categories) {
//            System.out.println(category);

            if (category.equalsIgnoreCase("UNKNOWN") || category.equalsIgnoreCase("INDIVIDUAL")) {
                continue;
            }

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
            nGramProbability.put(nGrams, getProbability(accu, freq - accu, nGrams.length()));
        }

        List<Triple<String, Double, String>> ret = new ArrayList<>();

        for (Map.Entry<String, Double> entry : nGramProbability.entrySet()) {
            ret.add(Triple.of(entry.getKey(), entry.getValue(), nGramToCategory.get(entry.getKey())));
        }

        return ret;
    }

    public static List<String> getCategories(String fileFolderName) {
        List<String> categories = new ArrayList<>();

//        System.out.println(fileFolderName);

        URL url = Objects.requireNonNull(EntityClassification.class.getResource(fileFolderName));
//        System.out.println("URL: " + url);
        logger.info("URL: " + url);

        for (ClassPath.ResourceInfo resource : resources) {
            if (resource.getResourceName().startsWith("io/github/software/coursework/" + fileFolderName)) {
                categories.add(resource.getResourceName().substring(resource.getResourceName().lastIndexOf('/') + 1).replaceFirst("\\.txt$", ""));
            }
        }

//        System.out.println(categories);

        return categories;
    }

    public static Map<String, Integer> readText(String fileFolderName, String category) {
//        System.out.println("read Text " + category);
        try  {
            Path path = Paths.get(EntityClassification.class.getResource(fileFolderName).toURI());
            List<String> textByLines = Files.readAllLines(path.resolve(category + ".txt"), StandardCharsets.UTF_8);

            Map<String, Integer> nGrams = new HashMap<>();

            for (String line : textByLines) {
                line = line.replaceFirst("^[1-9]\\d*\\.\\s+", "");

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
                    if (!lst.isEmpty()) {
                        nGrams.compute(lst + part, (key, count) -> count == null ? 1 : count + 1);
                    }
                    nGrams.compute(part, (key, count) -> count == null ? 1 : count + 1);
                    lst = part;
                }
            }

            return nGrams;
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    public static double getProbability(int x, int y, int len) {
        double p = (len == 1) ? 0.98 : 0.8;

        BetaDistribution bd = new BetaDistribution(x + 1, y + 1);
        double l = 0.0, r = 1.0, mid;
        while (r - l > 0.00000001) {
            mid = (l + r) / 2;
            if (1 - bd.cumulativeProbability(mid) < p) {
                r = mid;
            } else {
                l = mid;
            }
        }
        return r;
    }
}
