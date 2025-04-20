package io.github.software.coursework;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;

import java.util.List;

public class GMModelGeneratorTest {

    @Test
    public void testGMModelGenerationAndData() {
        GMModelGenerator generator = new GMModelGenerator();

        int componentCount = generator.getComponentCount();
        List<GMModelGenerator.GMMComponent> components = generator.getComponents();
        List<List<Double>> answerModel = generator.getAnswerGMModel();
        List<Pair<Double, Triple<Integer, Integer, Integer>>> data = generator.generateData(10000);

        // 输出结果用于人工检查
        System.out.println("Component count: " + componentCount);
        System.out.println("First component mu: " + components.get(0).mu);
        System.out.println("First component sigma: " + components.get(0).sigma);
        System.out.println("First component F size: " + components.get(0).F.size());
        System.out.println("First component G size: " + components.get(0).G.size());
        System.out.println("First component H size: " + components.get(0).H.size());

        System.out.println("Answer model size: " + answerModel.size());
        System.out.println("Sample data size: " + data.size());

        // 简单逻辑判断
        assert componentCount >= 10 && componentCount <= 30;
        assert components.size() == componentCount;
        assert answerModel.size() == componentCount;
        assert data.size() == 10000;
    }
}
