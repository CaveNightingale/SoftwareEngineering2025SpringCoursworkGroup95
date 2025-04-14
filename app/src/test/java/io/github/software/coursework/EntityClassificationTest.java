package io.github.software.coursework;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityClassificationTest {

    @Test
    public void aaa() throws IOException, URISyntaxException {
        EntityClassification classification = new EntityClassification();
        classification.entityClassification("Dataset");

//        try (BufferedReader dirReader = new BufferedReader(new InputStreamReader(
//                Objects.requireNonNull(EntityClassification.class.getResourceAsStream("Dataset"))))) {
//            while (true) {
//                String line = dirReader.readLine();
//                if (line == null) {
//                    break;
//                }
//                try (InputStream reader = Objects.requireNonNull(EntityClassification.class.getResourceAsStream("Dataset/" + line))) {
//                    System.out.println("===" + line + "===");
//                    System.out.println(new String(reader.readAllBytes()));
//                }
//            }
//        }
//        URL url = EntityClassification.class.getResource("Dataset");
//        Path url1 = Path.of(url.toURI()).resolve("Diet.txt");
//        System.out.println(Files.readAllBytes(url1));
//
//        Files.writeString(Path.of("build/output.txt"), "62343");
    }

    @Test
    public void bbb() throws IOException, URISyntaxException {
        String target = "Dataset";
        String classificationTarget = "万顺叫车";

        System.out.println("通过数据集 " + target + " 预测目标： " + classificationTarget);

        EntityClassification entityClassification = new EntityClassification();
//        entityClassification.entityClassification("Dataset");

        Map<String, String> listedNames = new HashMap<>(); /// 在 Datasets 文件夹中出现的所有列表中的名字。
        Map<String, String> nGramsClassification = new HashMap<>(); /// nGrams 对应的类型。
        Map<String, Double> nGramsScore = new HashMap<>(); /// nGrams 对应的概率。

        URL urlDataset = EntityClassification.class.getResource("Dataset");

        File folder = Paths.get(urlDataset.toURI()).toFile();

        if (!folder.exists() || !folder.isDirectory()) {
            throw new RuntimeException("Folder does not exist or is not a directory");
        }

        File[] files = folder.listFiles();
        List<String> catagories = new ArrayList<>();

        for (File file : files) {
            String currentName = file.getName().replace(".txt", "");
            catagories.add(currentName);

            System.out.println("currentName: " + file.getName());

            byte[] allTexts = Files.readAllBytes(file.toPath());
            String reencodedText = new String(allTexts, StandardCharsets.UTF_8);
            List<String> textByLines = reencodedText.lines().collect(Collectors.toList());

//            System.out.println("reencoded text: " + reencodedText + "all text: " + allTexts);

            for (String line : textByLines) {

                listedNames.put(line, currentName);
            }
        }

        System.out.println("listedNames read");

        String projectRoot = System.getProperty("user.dir");
        Path pathBayesian = Paths.get(projectRoot, "build", "Bayesian", target + ".txt");

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

        if (listedNames.containsKey(classificationTarget)) {
            System.out.println("目标已存在表中，类型为：" + target + " : " + listedNames.get(classificationTarget));
        } else {
            System.out.println("目标不存在表中。");
        }

        Double probability = 0.0;

        Random random = new Random();
        String classification = catagories.get(random.nextInt(catagories.size()));
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

    }
}
