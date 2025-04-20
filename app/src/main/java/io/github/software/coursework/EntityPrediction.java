package io.github.software.coursework;

import org.apache.commons.lang3.tuple.Triple;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityPrediction {

    public static Map<String, String> listedNames;
    public static Map<String, String> nGramsClassification;
    public static Map<String, Double> nGramsScore;
    public static List<String> categories;
    public static String target;
    public static int x;
    public static URL urlDataset;

    public EntityPrediction(String target) {
        this.target = target;
        categories = new ArrayList<>();
        x = 0;

        listedNames = new HashMap<>();
        nGramsClassification = new HashMap<>();
        nGramsScore = new HashMap<>();
        urlDataset = EntityClassification.class.getResource(target);
    }

    public static boolean hasCategory(String category) {
        return categories.contains(category);
    }

    public static void loadNGram() {
        System.out.println("Loading N-Grams");

        EntityClassification entityClassification = new EntityClassification();
        entityClassification.entityClassification(target);

        try {
            System.out.println("Loading Names");

            File folder = Paths.get(urlDataset.toURI()).toFile();

            if (!folder.exists() || !folder.isDirectory()) {
                throw new RuntimeException("Folder does not exist or is not a directory");
            }

            File[] files = folder.listFiles();

            for (File file : files) {
                String currentName = file.getName().replaceFirst("\\.txt$", "");
                categories.add(currentName);

                System.out.println("currentName: " + file.getName());

                byte[] allTexts = Files.readAllBytes(file.toPath());

                String reencodedText = new String(allTexts, StandardCharsets.UTF_8);
                List<String> textByLines = reencodedText.lines().collect(Collectors.toList());

                for (String line : textByLines) {
                    line = line.replaceFirst("^[1-9]\\d*\\.\\s+", "");

                    listedNames.put(line, currentName);

                    System.out.println(line + " " + currentName);
                }

//                FileWriter writer = new FileWriter(file.toPath().toString(), false);
//                writer.write("");
            }
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("listedNames read");

        String projectRoot = System.getProperty("user.dir");
        Path pathBayesian = Paths.get(projectRoot, "build", "Bayesian", target + ".txt");

        try {
            byte[] allTexts = Files.readAllBytes(pathBayesian);
            String bayesianText = new String(allTexts, StandardCharsets.UTF_8);
            List<String> textByLines = bayesianText.lines().collect(Collectors.toList());

            for (String line : textByLines) {
                String[] parts = line.split("\t");
                String nGram = parts[0];
                Double probability = Double.parseDouble(parts[1]);
                String classification = parts[2];

                nGramsClassification.put(nGram, classification);
                nGramsScore.put(nGram, probability);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Triple<String, String, Double> predict(String classificationTarget) {
        System.out.println("classificationTarget: " + classificationTarget);

        if (listedNames.containsKey(classificationTarget)) {
            System.out.println("目标已存在表中，类型为：" + target + " : " + listedNames.get(classificationTarget));

            return Triple.of(listedNames.get(classificationTarget), classificationTarget, 1.0);
        } else {
            System.out.println("目标不存在表中。");
        }

        Double probability = 0.0;

        Random random = new Random();
        String classification = categories.get(random.nextInt(categories.size()));
        String tmp = "";

        List<String> parts = new ArrayList<>();
        Pattern pattern = Pattern.compile("([a-zA-Z0-9]+)|([\\u4e00-\\u9fa5])");
        Matcher matcher = pattern.matcher(classificationTarget);

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
                if (nGramsScore.containsKey(lst + part)) {
                    if (probability < nGramsScore.get(lst + part)) {
                        probability = nGramsScore.get(lst + part);
                        classification = nGramsClassification.get(lst + part);
                        tmp = lst + part;
                    }
                }
            }
            if (nGramsScore.containsKey(part)) {
                if (probability < nGramsScore.get(part)) {
                    probability = nGramsScore.get(part);
                    classification = nGramsClassification.get(part);
                    tmp = part;
                }
            }
            lst = part;
        }

        if (probability == 0.0) {
            System.out.println("ngram 未匹配到。");
        } else {
            System.out.println("匹配成功： 最大概率nGram：" + tmp + "，概率：" + probability + ",匹配结果：" + classification);
        }

        return Triple.of(classification, tmp, probability);
    }

    public static void saveParams() {
        try {
            Map<String, BufferedWriter> Writers = new HashMap<>();

            for (String category : categories) {
                Path path = Paths.get(urlDataset.toURI()).resolve(category + ".txt");

                if (Files.exists(path)) {
                    FileWriter writer = new FileWriter(path.toString(), false);
                    writer.write("");
                } else {
                    File file = new File(path.toString());
                    file.createNewFile();
                }

                Writers.put(category, new BufferedWriter(new FileWriter(path.toString())));
            }

            for (Map.Entry<String, String> entry : listedNames.entrySet()) {
                Writers.get(entry.getValue()).write(entry.getKey() + "\n");
            }

            for (String category : categories) {
                Writers.get(category).close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (java.net.URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void saveAndReload() {
        saveParams();

        listedNames.clear();
        nGramsScore.clear();
        nGramsClassification.clear();
        categories.clear();

        loadNGram();
    }

    public void setCategory(String name, String category) {
        x++;

        listedNames.put(name, category);
        if (!categories.contains(category)) {
            categories.add(category);
        }

        if (x == 1) {
            x = 0;
            saveAndReload();
        }
    }
}
