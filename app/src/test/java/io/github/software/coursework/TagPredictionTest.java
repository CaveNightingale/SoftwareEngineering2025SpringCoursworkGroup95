package io.github.software.coursework;

import org.junit.jupiter.api.Test;

public class TagPredictionTest {

    @Test
    public void test() {
        TagPrediction tagPrediction = new TagPrediction("Tags.txt");

        assert tagPrediction.checkTag("Valentine's Day", 2, 15);

    }
}
