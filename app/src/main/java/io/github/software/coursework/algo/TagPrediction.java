package io.github.software.coursework.algo;

import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.abs;

public class TagPrediction {

    private static final int SUFFIX_LENGTH_DELETE = 8;
    private static final int SUFFIX_LENGTH_DATE = 5;

    public Map<String, Pair<Integer, Integer>> tagList;
    public String fileName;
    private static final Logger logger = Logger.getLogger(TagPrediction.class.getName());

    public TagPrediction(String fileName) {
        tagList = new HashMap<>();
        this.fileName = fileName;

        readTags();
    }

    public void readTags() {
        tagList.clear();

        URL url = TagPrediction.class.getResource("/io/github/software/coursework/" + fileName);
        Pattern pattern = Pattern.compile("^(\\d{2})/(\\d{2})$");

        if (url == null) {
            logger.warning("Could not find file " + fileName);
            return;
        }

        String filePath = url.getPath().toString().replaceFirst("^/[A-Z]:", "");

        try {
            List<String> lines = Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8);

            for (String line : lines) {

                int firstSpace = line.indexOf(' ');
                if (firstSpace == -1) {
                    logger.warning("Error: No space found");
                }

                String trimmed = line.substring(firstSpace + 1);

                if (trimmed.length() < SUFFIX_LENGTH_DELETE) {
                    logger.warning("Error: Length less than 8");
                }

                String name = trimmed.substring(0, trimmed.length() - SUFFIX_LENGTH_DELETE);
                String date = trimmed.substring(trimmed.length() - SUFFIX_LENGTH_DATE);

                Matcher matcher = pattern.matcher(date);

                if (matcher.matches()) {
                    String strNum1 = matcher.group(1);
                    String strNum2 = matcher.group(2);

                    int intNum1 = Integer.parseInt(strNum1);
                    int intNum2 = Integer.parseInt(strNum2);

                    tagList.put(name, Pair.of(intNum1, intNum2));
//                    System.out.println("Festival name : " + name + ", Date : " + date + ", Num1 :" + intNum1 + ", Num2 :" + intNum2);
                } else {
                    logger.warning("Error: Invalid format");
                }
            }

        } catch (IOException e) {
            logger.warning("Error: " + e.getMessage());
        }
    }

    public boolean checkTag(String tag, int month, int day) {
        if (tagList.containsKey(tag)) {
//            if (abs(month * 30 + day - (tagList.get(tag).getLeft() * 30 + tagList.get(tag).getRight())) <= 15)
//                System.out.println(month + " " + day + " " + tagList.get(tag).getLeft() + " " + tagList.get(tag).getRight());
            return abs(month * 30 + day - (tagList.get(tag).getLeft() * 30 + tagList.get(tag).getRight())) <= 15;
        } else {
            return false;
        }
    }

}
