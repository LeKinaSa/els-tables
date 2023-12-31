package pt.up.fe.els2022.adapters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.map.ListOrderedMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.up.fe.els2022.model.Table;

public class JsonAdapter extends StructuredAdapter {
    public JsonAdapter(List<String> paths, List<String> columns) {
        super(paths, columns);
    }

    private JsonElement findByPath(JsonElement current, List<PathFragment> fragments, int fragmentIdx) {
        PathFragment fragment = fragments.get(fragmentIdx);

        if (current.isJsonObject()) {
            JsonObject currentObj = current.getAsJsonObject();

            JsonElement element = currentObj.get(fragment.getKey());
            if (element != null) {
                if (fragmentIdx == fragments.size() - 1) {
                    return element;
                }
                element = findByPath(element, fragments, fragmentIdx + 1);
                if (element != null) return element;
            }

            if (!fragment.isDirectChild()) {
                for (Entry<String, JsonElement> entry : currentObj.entrySet()) {
                    element = findByPath(entry.getValue(), fragments, fragmentIdx);
                    if (element != null) return element;
                }
            }
        }

        return null;
    }

    private List<PathFragment> splitPath(String path) {
        List<PathFragment> fragments = new ArrayList<>();

        String[] doubleSlash = path.split("//");
        List<List<String>> result = Stream.of(doubleSlash).map(s -> List.of(s.split("/"))).collect(Collectors.toList());

        for (List<String> part : result) {
            String firstKey = part.get(0);

            if (!firstKey.isBlank()) {
                fragments.add(new PathFragment(firstKey, false));
            }

            part.stream().skip(1).forEach(key -> fragments.add(new PathFragment(key, true)));
        }

        return fragments;
    }

    @Override
    public Table extractTable(List<File> files) {
        Table table = new Table();
        Map<String, List<String>> rows = new ListOrderedMap<>();

        try {
            for (File file : files) {
                for (String path : paths) {
                    String contents = Files.readString(file.toPath());
                    JsonElement root = JsonParser.parseString(contents);
    
                    JsonElement element = root;
                    if (!path.equals("/")) {
                        List<PathFragment> fragments = splitPath(path);
                        element = findByPath(root, fragments, 0);
                    }
    
                    if (element == null) {
                        throw new RuntimeException("Could not find an element that matches the specified path: " + path +
                                "in file " + file.getName() + ".");
                    }
    
                    JsonObject obj = element.getAsJsonObject();
    
                    if (columns.isEmpty()) {
                        // Get all key-value pairs (with primitive values) in object
                        for (Entry<String, JsonElement> entry : obj.entrySet()) {
                            String colName = entry.getKey();
                            JsonElement colValue = entry.getValue();
    
                            if (colValue.isJsonPrimitive() || colValue.isJsonNull()) {
                                rows.putIfAbsent(colName, new ArrayList<>());
                                rows.get(colName).add(colValue.isJsonPrimitive() ? colValue.getAsString() : "null");
                            }
                        }
                    }
                    else {
                        // Get specific key-value pairs in a particular order
                        for (String colName : columns) {
                            JsonElement colValue = obj.get(colName);
                            if (colValue == null || !colValue.isJsonPrimitive() || !colValue.isJsonNull()) {
                                throw new RuntimeException("Column " + colName + " does not exist or does not correspond to a primitive value.");
                            }
                            rows.putIfAbsent(colName, new ArrayList<>());
                            rows.get(colName).add(colValue.isJsonPrimitive() ? colValue.getAsString() : "null");
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        table.addRows(rows);
        return table;
    }
}
