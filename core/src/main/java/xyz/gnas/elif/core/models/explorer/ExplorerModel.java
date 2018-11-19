package xyz.gnas.elif.core.models.explorer;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import java.io.File;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@id")
public class ExplorerModel {
    private ExplorerModel otherModel;

    private File folder;

    public ExplorerModel getOtherModel() {
        return otherModel;
    }

    public void setOtherModel(ExplorerModel otherModel) {
        this.otherModel = otherModel;
    }

    public File getFolder() {
        return folder;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }
}
