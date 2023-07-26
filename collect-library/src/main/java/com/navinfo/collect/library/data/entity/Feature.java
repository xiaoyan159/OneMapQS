package com.navinfo.collect.library.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author qj
 * @version V1.0
 * @ClassName: Feature
 * @Date 2022/4/14
 * @Description: ${TODO}(数据基类)
 */
public class Feature extends Object implements Serializable, Cloneable {
//    //主键
//    @PrimaryKey(autoGenerate = true)
//    public int rowId;

    @PrimaryKey()
    @ColumnInfo(name = "uuid")
    @NonNull
    private String id = UUID.randomUUID().toString();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
