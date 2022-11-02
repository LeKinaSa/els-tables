package pt.up.fe.els2022.internal;

import pt.up.fe.els2022.model.MetadataType;

import java.util.List;
import java.util.Map;

public abstract class LoadBuilder<T extends LoadBuilder<T>> extends InstructionBuilder {
    protected String target;
    protected List<String> filePaths;
    protected Map<String, MetadataType> metadataColumns;

    public T withTarget(String target) {
        this.target = target;
        return thisSubclass();
    }

    public T withFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
        return thisSubclass();
    }

    public T withMetadataColumns(Map<String, MetadataType> metadataColumns) {
        this.metadataColumns = metadataColumns;
        return thisSubclass();
    }

    @SuppressWarnings("unchecked")
    private T thisSubclass() {
        return (T) this;
    }

    @Override
    protected void validate() {
        if (target == null || filePaths == null) {
            throw new RuntimeException("Missing arguments for load instruction.");
        }
    }
}
