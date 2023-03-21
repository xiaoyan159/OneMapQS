package com.navinfo.collect.library.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.Objects;
import java.util.UUID;

/**
 * @author qj
 * @version V1.0
 * @ClassName: TileElement
 * @Date 2022/4/14
 * @Description: ${TileElement}(关联表)
 */
@Entity(tableName = "tileElement",indices = {@Index("tilex"),@Index("tiley"),@Index("element_uuid")})
public class TileElement extends Feature{

    @ColumnInfo(name = "tilex")
    private int tilex;
    @ColumnInfo(name = "tiley")
    private int tiley;
    @ColumnInfo(name = "element_uuid")
    private String elementId;

    public int getTilex() {
        return tilex;
    }

    public void setTilex(int tilex) {
        this.tilex = tilex;
    }

    public int getTiley() {
        return tiley;
    }

    public void setTiley(int tiley) {
        this.tiley = tiley;
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TileElement that = (TileElement) o;
        return tilex == that.tilex && tiley == that.tiley && Objects.equals(elementId, that.elementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tilex, tiley, elementId);
    }

    @Override
    public String toString() {
        return "TileElement{" +
                "tilex=" + tilex +
                ", tiley=" + tiley +
                ", elementId='" + elementId + '\'' +
                '}';
    }
}
