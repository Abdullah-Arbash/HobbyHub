package de.hobbyhub.hobbyhub.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import de.hobbyhub.hobbyhub.dao.UserDao;
import de.hobbyhub.hobbyhub.model.User;

@Database(entities = {User.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract UserDao userDao();
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "hobbyhub-db")
                    .createFromAsset("database/hobbyhub.db").build();
        }
        return INSTANCE;
    }
}
