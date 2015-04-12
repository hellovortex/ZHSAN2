package com.zhsan.gameobject;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.zhsan.common.Point;
import com.zhsan.common.exception.FileReadException;
import com.zhsan.common.exception.FileWriteException;
import com.zhsan.resources.GlobalStrings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by Peter on 7/4/2015.
 */
public class Architecture extends GameObject {

    public static final String SAVE_FILE = "Architecture.csv";

    private String nameImageName;

    private ArchitectureKind kind;

    private List<Point> location;

    private Architecture(int id) {
        super(id);
    }

    public List<Point> getLocation() {
        return location;
    }

    public ArchitectureKind getKind() {
        return kind;
    }

    public static final GameObjectList<Architecture> fromCSV(FileHandle root, @NotNull GameScenario scen) {
        int version = scen.getGameSurvey().getVersion();

        GameObjectList<Architecture> result = new GameObjectList<>();

        FileHandle f = root.child(SAVE_FILE);
        try (CSVReader reader = new CSVReader(new InputStreamReader(f.read()))) {
            String[] line;
            int index = 0;
            while ((line = reader.readNext()) != null) {
                index++;
                if (index == 1) continue; // skip first line.

                Architecture data = new Architecture(Integer.parseInt(line[0]));
                if (version == 1) {
                    data.nameImageName = line[1];
                    data.setName(line[2]);
                    data.kind = scen.getArchitectureKinds().get(Integer.parseInt(line[3]));
                    data.location = Point.fromCSVList(line[7]);
                } else {
                    data.nameImageName = line[1];
                    data.setName(line[2]);
                    data.kind = scen.getArchitectureKinds().get(Integer.parseInt(line[3]));
                    data.location = Point.fromCSVList(line[4]);
                }

                result.add(data);
            }
        } catch (IOException e) {
            throw new FileReadException(f.path(), e);
        }

        return result;
    }

    public static final void toCSV(FileHandle root, GameObjectList<Architecture> data) {
        FileHandle f = root.child(SAVE_FILE);
        try (CSVWriter writer = new CSVWriter(f.writer(false))) {
            writer.writeNext(GlobalStrings.getString(GlobalStrings.Keys.ARCHITECTURE_SAVE_HEADER).split(","));
            for (Architecture d : data) {
                writer.writeNext(new String[]{
                        String.valueOf(d.getId()),
                        d.nameImageName,
                        d.getName(),
                        String.valueOf(d.kind.getId()),
                        Point.toCSVList(d.location)
                });
            }
        } catch (IOException e) {
            throw new FileWriteException(f.path(), e);
        }

    }

    public String getNameImageName() {
        return nameImageName;
    }
}
