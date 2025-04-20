package io.github.software.coursework;

import org.apache.commons.lang3.tuple.Triple;
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
    public void aaa() {
        EntityClassification.entityClassification("Categories1");
    }

    @Test
    public void bbb() throws IOException, URISyntaxException {
        String target = "Categories1";
        String classificationTarget = "万顺叫车";

        System.out.println("通过数据集 " + target + " 预测目标： " + classificationTarget);

        List<Triple<String, Double, String>> nGramMap = EntityClassification.entityClassification("Categories1");

        Map<String, String> listedNames = new HashMap<>(); /// 在 Datasets 文件夹中出现的所有列表中的名字。
        Map<String, String> nGramsClassification = new HashMap<>(); /// nGrams 对应的类型。
        Map<String, Double> nGramsScore = new HashMap<>(); /// nGrams 对应的概率。

        URL urlDataset = EntityClassification.class.getResource("Categories1");

        File folder = Paths.get(urlDataset.toURI()).toFile();

        if (!folder.exists() || !folder.isDirectory()) {
            throw new RuntimeException("Folder does not exist or is not a directory");
        }

        File[] files = folder.listFiles();
        List<String> catagories = new ArrayList<>();

        for (File file : files) {
            String currentName = file.getName().replaceFirst("\\.txt$", "");
            catagories.add(currentName);

            System.out.println("currentName: " + file.getName());

            byte[] allTexts = Files.readAllBytes(file.toPath());
            String reencodedText = new String(allTexts, StandardCharsets.UTF_8);
            List<String> textByLines = reencodedText.lines().toList();

//            System.out.println("reencoded text: " + reencodedText + "all text: " + allTexts);

            for (String line : textByLines) {

                listedNames.put(line, currentName);
            }
        }

        System.out.println("listedNames read");

        for (Triple<String, Double, String> triple : nGramMap) {
            nGramsScore.put(triple.getLeft(), triple.getMiddle());
            nGramsClassification.put(triple.getLeft(), triple.getRight());
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

    @Test
    public void ccc() {

        EntityPrediction entityPrediction = new EntityPrediction("Categories1");
        entityPrediction.loadNGram();

        System.out.println(entityPrediction.predict("黄映杰"));
        System.out.println(entityPrediction.predict("好好吃饭"));

        entityPrediction.setCategory("瓦洛兰特", "aaa");
        entityPrediction.setCategory("无印良品", "bbb");
        entityPrediction.setCategory("无垠星环", "bbb");
        entityPrediction.saveAndReload();


        System.out.println(entityPrediction.predict("美团"));
        System.out.println(entityPrediction.predict("无畏契约"));
        System.out.println(entityPrediction.predict("掌上瓦洛兰特"));

        entityPrediction.saveParams();
    }

    @Test
    public void ddd() {
        EntityPrediction entityPrediction1 = new EntityPrediction("Categories1");
        entityPrediction1.loadNGram();
        entityPrediction1.saveParams();
        EntityPrediction entityPrediction2 = new EntityPrediction("Categories2");
        entityPrediction2.loadNGram();
        entityPrediction2.saveParams();
    }
}
