package io.github.software.coursework;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TagPredictionTest {

    @Test
    public void test() {
        TagPrediction tagPrediction = new TagPrediction("Tags.txt");

        assertTrue(tagPrediction.checkTag("Valentine's Day", 2, 15),
                "Tag check failed for Valentine's Day at 2/15");

    }
}
