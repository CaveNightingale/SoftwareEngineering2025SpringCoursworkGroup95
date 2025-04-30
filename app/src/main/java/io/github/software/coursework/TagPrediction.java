package io.github.software.coursework;

import com.google.common.reflect.ClassPath;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public class TagPrediction {

    public Map<String, Pair<Integer, Integer>> tagList;
    public String fileName;

    public TagPrediction(String fileName) {
        tagList = new HashMap<>();
        this.fileName = fileName;

        ReadTags();
    }

    public void ReadTags() {
        tagList.clear();

        URL url = TagPrediction.class.getResource(fileName);
        Pattern pattern = Pattern.compile("^(\\d{2})/(\\d{2})$");

        System.out.println(url.getPath());

        try (BufferedReader br = new BufferedReader(new FileReader(url.getPath()))) {

            String line;
            while ((line = br.readLine()) != null) {

//                System.out.println("Printing Lines");
//                System.out.println(line);

                int firstSpace = line.indexOf(' ');
                if (firstSpace == -1) {
                    System.out.println("Error: No space found");
                }

                String trimmed = line.substring(firstSpace + 1);
                if (trimmed.length() < 8) {
                    System.out.println("Error: Length less than 8");
                }

                String name = trimmed.substring(0, trimmed.length() - 8);
                String date = trimmed.substring(trimmed.length() - 5);

                Matcher matcher = pattern.matcher(date);

                if (matcher.matches()) {
                    String strNum1 = matcher.group(1);
                    String strNum2 = matcher.group(2);

                    int intNum1 = Integer.parseInt(strNum1);
                    int intNum2 = Integer.parseInt(strNum2);

                    tagList.put(name, Pair.of(intNum1, intNum2));
//                    System.out.println("Festival name : " + name + ", Date : " + date + ", Num1 :" + intNum1 + ", Num2 :" + intNum2);
                } else {
                    System.out.println("Error: Invalid format");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkTag(String tag, int month, int day) {
        if (tagList.containsKey(tag)) {
            if (abs(month * 30 + day - (tagList.get(tag).getLeft() * 30 + tagList.get(tag).getRight())) <= 15)
                System.out.println(month + " " + day + " " + tagList.get(tag).getLeft() + " " + tagList.get(tag).getRight());
            return abs(month * 30 + day - (tagList.get(tag).getLeft() * 30 + tagList.get(tag).getRight())) <= 15;
        } else {
            return false;
        }
    }

}
