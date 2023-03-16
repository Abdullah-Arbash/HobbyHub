package de.hobbyhub.hobbyhub.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import de.hobbyhub.hobbyhub.model.User;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface UserDao {

    @Insert
    public Single<Long> insertUser(User user);

    @Update
    public Completable updateUser(User user);

    @Delete
    public Completable deleteUser(User user);

    @Query("SELECT * FROM tbl_users WHERE uid = :id")
    public Single<User> getUserById(int id);

    @Query("SELECT * FROM tbl_users WHERE original_id = :oid")
    public Single<User> getUserByOriginId(String oid);

}
