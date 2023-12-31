package pt.up.fe.els2022.instructions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.up.fe.els2022.model.ProgramState;
import pt.up.fe.els2022.model.Table;
import pt.up.fe.els2022.utils.FileUtils;
import pt.up.fe.specs.util.csv.CsvWriter;

public class SaveInstruction implements Instruction {
    private final String source;
    private final String path;
    private final List<String> columns;

    public SaveInstruction(String source, String path, List<String> columns) {
        this.source = source;
        this.path = path;

        if (columns == null) {
            columns = Collections.emptyList();
        }

        this.columns = columns;
    }

    public void execute(ProgramState state) {
        Table table = state.getTable(source);
        List<String> saveColumns = columns.isEmpty() ? List.copyOf(table.getColumnNames()) : columns;

        if (!table.getColumnNames().containsAll(saveColumns)) {
            throw new RuntimeException("Save instruction references columns that do not exist.");
        }

        File file = new File(path);
        if (!FileUtils.getExtension(file.getName()).equals("csv")) {
            throw new RuntimeException("Destination file must be a CSV file.");
        }

        CsvWriter csvWriter = new CsvWriter(saveColumns);
        csvWriter.setDelimiter(",");
        for (int i = 0; i < table.numRows(); i++) {
            Map<String, String> row = table.getRow(i);
            csvWriter.addLine(saveColumns.stream().map(row::get).collect(Collectors.toList()));
        }
        String csv = csvWriter.buildCsv();

        try {
            file.getParentFile().mkdirs();
            FileWriter writer = new FileWriter(file);
            writer.write(csv);
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
