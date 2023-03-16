package de.hobbyhub.hobbyhub.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "tbl_users")
public class User implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int uid;
    @ColumnInfo(name = "display_name")
    private String displayName;
    private String email;
    @ColumnInfo(name = "original_id")
    private String originalId;

    public User(String displayName, String email, String originalId) {
        this.displayName = displayName;
        this.email = email;
        this.originalId = originalId;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOriginalId() {
        return originalId;
    }

    public void setOriginalId(String originalId) {
        this.originalId = originalId;
    }
}
