package com.navinfo.collect.library.data.dao.impl;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.navinfo.collect.library.data.entity.CheckManager;
import com.navinfo.collect.library.data.entity.Element;
import com.navinfo.collect.library.data.entity.LayerManager;
import com.navinfo.collect.library.data.entity.Project;
import com.navinfo.collect.library.data.entity.TileElement;
import com.tencent.wcdb.database.SQLiteCipherSpec;
import com.tencent.wcdb.database.SQLiteDatabase;

import com.tencent.wcdb.room.db.WCDBOpenHelperFactory;
import android.os.AsyncTask;
import android.util.Log;
import com.tencent.wcdb.repair.BackupKit;
import com.tencent.wcdb.repair.RecoverKit;
import com.tencent.wcdb.room.db.WCDBDatabase;
import java.util.ArrayList;
import java.util.List;

@Database(entities = {Element.class, TileElement.class, LayerManager.class, Project.class, CheckManager.class},version = 1, exportSchema = false)
public abstract class MapLifeDataBase extends RoomDatabase {
    // marking the instance as volatile to ensure atomic access to the variable
    /**
     * 数据库单例对象
     */
    private static volatile MapLifeDataBase INSTANCE;

    /**
     * 要素数据库类
     */
    public abstract IElementDao getElementDao();

    /**
     * 图层要素数据库类
     */
    public abstract ITileElementDao getTileElementDao();

    /**
     * 图层数据库类
     */

    public abstract ILayerManagerDao getLayerManagerDao();

    /**
     * 项目管理类
     *
     * @return
     */
    public abstract IProjectManagerDao getProjectManagerDao();

    /**
     * 字段检查项
     */
    public abstract ICheckManagerDao getCheckManagerDao();

    /**
     * 数据库秘钥
     */
    private final static String DB_PASSWORD = "123456";

    public static MapLifeDataBase getDatabase(final Context context, final String name) {
        if (INSTANCE == null) {
            synchronized (MapLifeDataBase.class) {
                if (INSTANCE == null) {
                    // [WCDB] To use Room library with WCDB, pass a WCDBOpenHelper factory object
                    // to the database builder with .openHelperFactory(...). In the factory object,
                    // you can specify passphrase and cipher options to open or create encrypted
                    // database, as well as optimization options like asynchronous checkpoint.
                    SQLiteCipherSpec cipherSpec = new SQLiteCipherSpec()
                            .setPageSize(1024)
                            .setSQLCipherVersion(3);
                    WCDBOpenHelperFactory factory = new WCDBOpenHelperFactory()
                            .passphrase(DB_PASSWORD.getBytes())  // passphrase to the database, remove this line for plain-text
                            .cipherSpec(cipherSpec)               // cipher to use, remove for default settings
                            .writeAheadLoggingEnabled(true)       // enable WAL mode, remove if not needed
                            .asyncCheckpointEnabled(true);        // enable asynchronous checkpoint, remove if not needed

                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            MapLifeDataBase.class, name)

                            // [WCDB] Specify open helper to use WCDB database implementation instead
                            // of the Android framework.
                            .openHelperFactory(factory)

                            // Wipes and rebuilds instead of migrating if no Migration object.
                            // Migration is not part of this codelab.
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Override the onOpen method to populate the database.
     * For this sample, we clear the database every time it is created or opened.
     * <p>
     * If you want to populate the database only when the database is created for the 1st time,
     * override RoomDatabase.Callback()#onCreate
     */
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // If you want to keep the data through app restarts,
            // comment out the following line.
            new PopulateDbAsync(INSTANCE).execute();
        }
    };

    /**
     * Populate the database in the background.
     * If you want to start with more words, just add them.
     */
    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final ILayerManagerDao mLayerDao;

        PopulateDbAsync(MapLifeDataBase db) {
            mLayerDao = db.getLayerManagerDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            // Start the app with a clean database every time.
            // Not needed if you only populate on creation.
            //mDao.deleteAll();
            Log.e("qj", "doInBackground");
            return null;
        }
    }

    /**
     * 数据恢复
     */
    protected boolean recoverData(){
        if(INSTANCE!=null){
            SQLiteDatabase sqlite = ((WCDBDatabase) INSTANCE.getOpenHelper().getWritableDatabase()).getInnerDatabase();
            RecoverKit recover = new RecoverKit(
                    sqlite,                         // 要恢复到的目标 DB
                    sqlite.getPath() + "-backup",   // 备份文件
                    DB_PASSWORD.getBytes()           // 加密备份文件的密钥，非 DB 密钥
            );
            int result = recover.run(false);    // fatal 参数传 false 表示遇到错误忽略并继续，
            // 若传 true 遇到错误则中止并返回 FAILED
            switch (result) {
                case RecoverKit.RESULT_OK:
                    /* 成功 */
                    Log.e("qj","sRoomDatabaseCallback==RecoverKit成功");
                    return true;
                case RecoverKit.RESULT_CANCELED: /* 取消操作 */
                    Log.e("qj","sRoomDatabaseCallback==RecoverKit取消操作");
                    break;
                case RecoverKit.RESULT_FAILED: /* 失败 */
                    Log.e("qj","sRoomDatabaseCallback==RecoverKit失败");
                    break;

            }

            recover.release();
        }

        return false;
    }

    /**
     * 备份数据
     */
    protected boolean backup() {
        Log.e("qj", "sRoomDatabaseCallback===backup==start");
        if (INSTANCE != null) {
            //备份文件
            SQLiteDatabase sqlite = ((WCDBDatabase) INSTANCE.getOpenHelper().getWritableDatabase()).getInnerDatabase();
            BackupKit backup = new BackupKit(
                    sqlite,                         // 要备份的 DB
                    sqlite.getPath() + "-backup",   // 备份文件
                    "123456".getBytes(),          // 加密备份文件的密钥，非 DB 密钥
                    0, null);
            int result = backup.run();
            switch (result) {
                case BackupKit.RESULT_OK:
                    /* 成功 */
                    Log.e("qj", "sRoomDatabaseCallback==成功");
                    return true;
                case BackupKit.RESULT_CANCELED:
                    /* 取消操作 */
                    Log.e("qj", "sRoomDatabaseCallback==取消操作");
                    break;
                case BackupKit.RESULT_FAILED:
                    /* 失败 */
                    Log.e("qj", "sRoomDatabaseCallback==失败");
                    break;
            }

            backup.release();
        }
        Log.e("qj", "sRoomDatabaseCallback===backup==end");
        return false;
    }

    protected void release() {
        INSTANCE = null;
    }
}
