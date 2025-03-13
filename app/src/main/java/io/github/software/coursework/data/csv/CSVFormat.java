package io.github.software.coursework.data.csv;

import com.google.common.io.Files;
import io.github.software.coursework.data.Document;
import io.github.software.coursework.data.Reference;
import io.github.software.coursework.data.memory.MemoryStorage;
import io.github.software.coursework.data.schema.Entity;
import io.github.software.coursework.data.schema.Transaction;
import io.github.software.coursework.data.text.TextDocument;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CSVFormat {
    private static ArrayList<ImmutablePair<String, String>> flatten(HashMap<Reference<?>, CSVDocument> documents, HashSet<Reference<?>> exclude, HashMap<Reference<?>, ArrayList<ImmutablePair<String, String>>> cache, CSVDocument current) {
        ArrayList<ImmutablePair<String, String>> result = new ArrayList<>(current.data);
        for (ImmutablePair<String, Reference<?>> entry : current.refs) {
            if (exclude.contains(entry.getValue())) {
                continue;
            }
            ArrayList<ImmutablePair<String, String>> map = cache.get(entry.getValue());
            if (map == null) {
                CSVDocument document = documents.get(entry.getValue());
                if (document == null) {
                    continue;
                }
                exclude.add(entry.getValue());
                map = flatten(documents, exclude, cache, document);
                exclude.remove(entry.getValue());
                cache.put(entry.getValue(), map);
            }
            for (ImmutablePair<String, String> pair : map) {
                result.add(ImmutablePair.of(entry.getKey() + pair.getKey(), pair.getValue()));
            }
        }
        return result;
    }

    public static void exportTo(MemoryStorage storage, File file) throws IOException {
        HashMap<Reference<?>, CSVDocument> documents = new HashMap<>();
        for (Reference<Entity> reference : storage.getEntities()) {
            Entity entity = storage.getEntity(reference);
            CSVDocument document = new CSVDocument();
            Document.Writer writer = document.writer();
            writer.writeReference("id", reference);
            entity.serialize(writer);
            documents.put(reference, document);
        }
        HashMap<Reference<?>, CSVDocument> transactions = new HashMap<>();
        for (Reference<Transaction> reference : storage.getTransactions()) {
            Transaction transaction = storage.getTransaction(reference);
            CSVDocument document = new CSVDocument();
            Document.Writer writer = document.writer();
            writer.writeReference("id", reference);
            transaction.serialize(writer);
            transactions.put(reference, document);
            documents.put(reference, document);
        }
        List<ArrayList<ImmutablePair<String, String>>> result = transactions.values().stream().map(d -> flatten(documents, new HashSet<>(transactions.keySet()), new HashMap<>(), d)).toList();
        Set<String> keySet = result.stream().flatMap(List::stream).map(ImmutablePair::getKey).collect(HashSet::new, HashSet::add, HashSet::addAll);
        String[] keys = keySet.toArray(new String[0]);
        Arrays.sort(keys);
        HashSet<Order> keyOrderRequired = new HashSet<>();
        for (ArrayList<ImmutablePair<String, String>> map : result) {
            for (int i = 1, prev = Arrays.binarySearch(keys, map.getFirst().getLeft()), curr; i < map.size(); i++, prev = curr) {
                curr = Arrays.binarySearch(keys, map.get(i).getLeft());
                keyOrderRequired.add(new Order(prev, curr));
            }
        }
        Order[] keyOrder = keyOrderRequired.toArray(new Order[0]);
        Arrays.sort(keyOrder, Comparator.comparingInt(o -> o.l));
        int[] rightDegree = new int[keys.length];
        int[] leftEnd = new int[keys.length];
        int[] sorted = new int[keys.length];
        Arrays.fill(sorted, -1);
        for (int i = 0; i < keyOrder.length; i++) {
            leftEnd[keyOrder[i].l] = i;
            rightDegree[keyOrder[i].r]++;
        }
        int sortedCount = 0;
        for (int i = 0; i < keys.length; i++) {
            if (rightDegree[i] == 0) {
                sorted[sortedCount++] = i;
            }
        }
        for (int i = 0; i < keys.length; i++) {
            if (sorted[i] == -1) {
                throw new RuntimeException("Keys appear in different order in different documents. This may indicate a non-deterministic serialize() implementation in one of the classes.");
            }
            for (int j = leftEnd[sorted[i]]; j >= 0 && keyOrder[j].l == sorted[i]; j--) {
                if (--rightDegree[keyOrder[j].r] == 0) {
                    sorted[sortedCount++] = keyOrder[j].r;
                }
            }
        }
        String[] headers = new String[keys.length];
        for (int i = sorted.length - 1; i >= 0; i--) {
            headers[i] = keys[sorted[i]];
        }
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            CSVPrinter printer = new CSVPrinter(writer, org.apache.commons.csv.CSVFormat.DEFAULT);
            printer.printRecord(Arrays.stream(headers).map(s -> s.substring(1)));
            for (ArrayList<ImmutablePair<String, String>> map : result) {
                String[] row = new String[map.size()];
                for (int i = 0, j = 0; i < sorted.length; i++) {
                    if (j < map.size() && map.get(j).getLeft().equals(keys[sorted[i]])) {
                        row[i] = map.get(j++).getRight();
                    }
                }
                printer.printRecord((Object[]) row);
            }
        }
    }

    private record Order(int l, int r) {
    }
}
