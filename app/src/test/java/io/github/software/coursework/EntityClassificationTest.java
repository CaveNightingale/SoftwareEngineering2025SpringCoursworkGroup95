package io.github.software.coursework;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
}
