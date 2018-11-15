package xyz.gnas.elif.core.models.explorer;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.io.File;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class ExplorerModel {
    private ExplorerModel otherModel;

    private File path;

    public ExplorerModel getOtherModel() {
        return otherModel;
    }

    public void setOtherModel(ExplorerModel otherModel) {
        this.otherModel = otherModel;
    }

    public File getPath() {
        return path;
    }

    public void setPath(File path) {
        this.path = path;
    }
}
