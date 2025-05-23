package io.github.software.coursework.algo;

import com.google.common.reflect.ClassPath;
import org.apache.commons.lang3.tuple.Triple;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EntityPrediction {

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

    public Map<String, String> listedNames;
    public Map<String, String> nGramsClassification;
    public Map<String, Double> nGramsScore;
    public List<String> categories;
    public String target;
    public int x;
    public URL urlDataset;

    public EntityPrediction(String target) {
        this.target = target;
        categories = new ArrayList<>();
        x = 0;

        listedNames = new HashMap<>();
        nGramsClassification = new HashMap<>();
        nGramsScore = new HashMap<>();
        urlDataset = EntityClassification.class.getResource("/io/github/software/coursework/" + target);
    }

    public boolean hasCategory(String category) {
        return categories.contains(category);
    }

    public void loadNGram() {
//        System.out.println("Loading N-Grams");

        List<Triple<String, Double, String>> nGramMap = EntityClassification.entityClassification(target);
//        System.out.println(nGramMap);

        for (ClassPath.ResourceInfo resource : resources) {
            if (resource.url().toString().startsWith(urlDataset.toString())) {

                String fileName = resource.getResourceName();
                String simpleName = fileName.substring(fileName.lastIndexOf("/") + 1);
                String currentName = simpleName.replaceFirst("\\.txt$", "");
                categories.add(currentName);
                try (InputStream inputStream = resource.url().openStream()) {
                    byte[] bytes = inputStream.readAllBytes();
                    for (String line : new String(bytes, StandardCharsets.UTF_8).split("\n")) {
                        line = line.replaceFirst("^[1-9]\\d*\\.\\s+", "").replaceAll("\r", "");
                        listedNames.put(line, currentName);
//                        System.out.println(line + " " + simpleName);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

//        System.out.println("listedNames read");

        for (Triple<String, Double, String> triple : nGramMap) {
            nGramsScore.put(triple.getLeft(), triple.getMiddle());
            nGramsClassification.put(triple.getLeft(), triple.getRight());
        }

    }

    public Triple<String, String, Double> predict(String classificationTarget) {
//        System.out.println("classificationTarget: " + classificationTarget);

        if (listedNames.containsKey(classificationTarget)) {
//            System.out.println("目标已存在表中，类型为：" + target + " : " + listedNames.get(classificationTarget));

            return Triple.of(listedNames.get(classificationTarget), classificationTarget, 1.0);
        } else {
//            System.out.println("目标不存在表中。");
        }

        double probability = 0.0;

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
//            System.out.println("ngram 未匹配到。");
        } else {
//            System.out.println("匹配成功： 最大概率nGram：" + tmp + "，概率：" + probability + ",匹配结果：" + classification);
        }

        return Triple.of(classification, tmp, probability);
    }

    public void saveParams() {
        try {
            HashMap<String, StringBuilder> textByCategory = new HashMap<>();
            for (Map.Entry<String, String> entry : listedNames.entrySet()) {
                textByCategory.computeIfAbsent(entry.getValue(), k -> new StringBuilder()).append(entry.getKey()).append("\n");
            }
            Map<String, BufferedWriter> writers = new HashMap<>();
            // TODO: Rewrite this so that it works with compiled jar files
            for (Map.Entry<String, StringBuilder> entry : textByCategory.entrySet()) {
                Files.writeString(
                        Paths.get(urlDataset.toURI()).resolve(entry.getKey() + ".txt"),
                        entry.getValue(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
            }
        } catch (IOException | java.net.URISyntaxException e) {
            e.printStackTrace();
        }

    }

    public void saveAndReload() {
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

        if (x >= 1) {
            x = 0;
            saveAndReload();
        }
    }
}
