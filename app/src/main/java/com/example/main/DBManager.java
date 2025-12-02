package com.example.main;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private DBHelper dbHelper;

    public DBManager(Context context) {
        dbHelper = new DBHelper(context);
    }

    // 注册用户
    public boolean registerUser(String studentId, String password) {
        // 先检查学号是否存在
        if (isStudentIdExists(studentId)) {
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // 直接写SQL，表名users，列名student_id和password
            String sql = "INSERT INTO users (student_id, password) VALUES (?, ?)";
            db.execSQL(sql, new Object[]{studentId, password});
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.close();
        }
    }

    // 登录验证
    public boolean loginUser(String studentId, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT * FROM users WHERE student_id = ? AND password = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{studentId, password});

        boolean loginSuccess = cursor.moveToFirst();

        cursor.close();
        db.close();
        return loginSuccess;
    }


    // 检查学号是否存在
    public boolean isStudentIdExists(String studentId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT * FROM users WHERE student_id = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{studentId});

        boolean exists = cursor.moveToFirst();

        cursor.close();
        db.close();
        return exists;
    }


    // 打印数据库里所有用户（修复getColumnIndex可能返回-1的问题）
    public void printAllUsers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 查询所有用户
        String sql = "SELECT * FROM users";
        Cursor cursor = db.rawQuery(sql, null);

        Log.d("数据库内容", "===== 当前所有用户 =====");

        // 先检查cursor是否有效
        if (cursor != null && cursor.moveToFirst()) {
            // 获取列索引并检查是否有效
            int idIndex = cursor.getColumnIndex("student_id");
            int pwdIndex = cursor.getColumnIndex("password");

            // 遍历结果
            do {
                // 检查列索引是否有效
                String studentId = (idIndex != -1) ? cursor.getString(idIndex) : "未知列";
                String password = (pwdIndex != -1) ? cursor.getString(pwdIndex) : "未知列";

                // 打印到Logcat
                Log.d("数据库内容", "学号：" + studentId + " | 密码：" + password);
            } while (cursor.moveToNext());
        } else {
            Log.d("数据库内容", "数据库里还没有用户！");
        }

        Log.d("数据库内容", "=======================");

        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }

    // 打印指定用户信息（修复getColumnIndex可能返回-1的问题）
    public void printUserInfo(String studentId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT * FROM users WHERE student_id = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{studentId});

        Log.d("用户信息", "===== 查询学号：" + studentId + " =====");

        if (cursor != null && cursor.moveToFirst()) {
            // 获取列索引并检查
            int idIndex = cursor.getColumnIndex("student_id");
            int pwdIndex = cursor.getColumnIndex("password");

            String id = (idIndex != -1) ? cursor.getString(idIndex) : "未知列";
            String password = (pwdIndex != -1) ? cursor.getString(pwdIndex) : "未知列";

            Log.d("用户信息", "学号：" + id + " | 密码：" + password);
        } else {
            Log.d("用户信息", "该用户不存在！");
        }

        Log.d("用户信息", "=======================");

        if (cursor != null) {
            cursor.close();
        }
        db.close();
    }

    // ----------------------------------------------------------------------------
    private int id;
    private String title;
    private String type;
    private String time;
    private String location;
    private String description;
    private String organizer;
    private int maxPeople;
    private int currentPeople;


    // 按日期查询活动（修复getColumnIndex=-1问题）
    public List<ActivityBean> getActivityByDate(String targetDate) {
        List<ActivityBean> list = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT * FROM activities WHERE SUBSTR(time, 1, 10) = ? ORDER BY time ASC";
        Cursor cursor = db.rawQuery(sql, new String[]{targetDate});

        // 先检查cursor是否有数据
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // 直接用列的索引（按表创建顺序），不会返回-1
                int id = cursor.getInt(0);                // 第0列：id
                String title = cursor.getString(1);       // 第1列：title
                String type = cursor.getString(2);        // 第2列：type
                String time = cursor.getString(3);        // 第3列：time
                String location = cursor.getString(4);    // 第4列：location
                String description = cursor.getString(5); // 第5列：description
                String organizer = cursor.getString(6);   // 第6列：organizer
                int maxPeople = cursor.getInt(7);         // 第7列：maxPeople
                String tag = cursor.getString(8);         // 第 8 列是 tag
                list.add(new ActivityBean(id, title, type, time, location, description, organizer, maxPeople,tag));
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return list;
    }

    public List<ActivityBean> getActivityByDateAndTag(String targetDate, String tag) {

        List<ActivityBean> list = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT * FROM activities WHERE SUBSTR(time, 1, 10) = ?";
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(targetDate);

        // 2. 增加 TAG 筛选条件
        if (tag != null && !tag.isEmpty()) {
            sql += " AND tag = ?";
            selectionArgs.add(tag);
        }

        // 3. 排序
        sql += " ORDER BY time ASC";

        Cursor cursor = db.rawQuery(sql, selectionArgs.toArray(new String[0]));

        // 先检查cursor是否有数据
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // 直接用列的索引（按表创建顺序），不会返回-1
                int id = cursor.getInt(0);                // 第0列：id
                String title = cursor.getString(1);       // 第1列：title
                String type = cursor.getString(2);        // 第2列：type
                String time = cursor.getString(3);        // 第3列：time
                String location = cursor.getString(4);    // 第4列：location
                String description = cursor.getString(5); // 第5列：description
                String organizer = cursor.getString(6);   // 第6列：organizer
                int maxPeople = cursor.getInt(7);         // 第7列：maxPeople
                String activityTag = cursor.getString(8);
                list.add(new ActivityBean(id, title, type, time, location, description, organizer, maxPeople,activityTag));
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return list;
    }
    // 同理修复getAllActivityList方法
    public ActivityBean getActivityById(int activityId) {
        ActivityBean activityBean = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 查询指定ID的活动（用int参数更合理）
        Cursor cursor = db.rawQuery("SELECT * FROM activities WHERE id = ?",
                new String[]{String.valueOf(activityId)});

        if (cursor != null && cursor.moveToFirst()) {
            // 只取第一条数据（ID是主键，唯一）
            int id = cursor.getInt(0);
            String title = cursor.getString(1);
            String type = cursor.getString(2);
            String time = cursor.getString(3);
            String location = cursor.getString(4);
            String description = cursor.getString(5);
            String organizer = cursor.getString(6);
            int maxPeople = cursor.getInt(7);
            String tag = cursor.getString(8);

            // 创建ActivityBean对象
            activityBean = new ActivityBean(id, title, type, time, location,
                    description, organizer, maxPeople,tag);
        }

        // 关闭资源
        if (cursor != null) {
            cursor.close();
        }
        db.close();

        return activityBean; // 返回单个对象（没找到则返回null）
    }

    public List<ActivityBean> getAllActivityList() {
        List<ActivityBean> list = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM activities ORDER BY time ASC", null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // 直接用列索引
                int id = cursor.getInt(0);
                String title = cursor.getString(1);
                String type = cursor.getString(2);
                String time = cursor.getString(3);
                String location = cursor.getString(4);
                String description = cursor.getString(5);
                String organizer = cursor.getString(6);
                int maxPeople = cursor.getInt(7);
                String tag = cursor.getString(8);
                list.add(new ActivityBean(id, title, type, time, location, description, organizer, maxPeople,tag));
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return list;
    }

    // 统计活动当前报名人数（从报名表查）
    public int getCurrentPeopleByActivityId(int activityId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 查报名表中该活动的记录数
        String sql = "SELECT COUNT(*) FROM registrations WHERE activity_id = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(activityId)});

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0); // 获取统计结果
        }

        cursor.close();
        db.close();
        return count;
    }

    // ---------------------- 收藏相关方法 ----------------------
    // 收藏活动
    public boolean collectActivity(String userId, int activityId) {
        if (isUserCollected(userId, activityId)) {
            return false; // 已收藏
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            String sql = "INSERT INTO favorites (user_id, activity_id) VALUES (?, ?)";
            db.execSQL(sql, new Object[]{userId, activityId});
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.close();
        }
    }

    // 取消收藏
    public boolean uncollectActivity(String userId, int activityId) {
        if (!isUserCollected(userId, activityId)) {
            return false; // 未收藏
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            String sql = "DELETE FROM favorites WHERE user_id = ? AND activity_id = ?";
            db.execSQL(sql, new Object[]{userId, activityId});
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.close();
        }
    }

    // 检查用户是否收藏了活动
    public boolean isUserCollected(String userId, int activityId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT * FROM favorites WHERE user_id = ? AND activity_id = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{userId, String.valueOf(activityId)});

        boolean isCollected = cursor.moveToFirst();
        cursor.close();
        db.close();
        return isCollected;
    }

    // ---------------------- 报名相关方法 ----------------------
    // 报名活动
    public boolean applyActivity(String userId, int activityId) {
        if (isUserApplied(userId, activityId)) {
            return false; // 已报名
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            String sql = "INSERT INTO registrations (user_id, activity_id, register_time) VALUES (?, ?, datetime('now'))";
            db.execSQL(sql, new Object[]{userId, activityId});
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.close();
        }
    }

    // 取消报名
    public boolean cancelApplyActivity(String userId, int activityId) {
        if (!isUserApplied(userId, activityId)) {
            return false; // 未报名
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            String sql = "DELETE FROM registrations WHERE user_id = ? AND activity_id = ?";
            db.execSQL(sql, new Object[]{userId, activityId});
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.close();
        }
    }

    // 检查用户是否报名了活动
    public boolean isUserApplied(String userId, int activityId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT * FROM registrations WHERE user_id = ? AND activity_id = ?";
        Cursor cursor = db.rawQuery(sql, new String[]{userId, String.valueOf(activityId)});

        boolean isApplied = cursor.moveToFirst();
        cursor.close();
        db.close();
        return isApplied;
    }

    // 打印用户表
    public void printUsersTable() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users", null);

        Log.d("调试-用户表", "===== 用户表数据 =====");
        if (cursor.moveToFirst()) {
            do {
                String studentId = cursor.getString(0);
                String password = cursor.getString(1);
                Log.d("调试-用户表", "学号：" + studentId + " | 密码：" + password);
            } while (cursor.moveToNext());
        } else {
            Log.d("调试-用户表", "用户表为空");
        }
        Log.d("调试-用户表", "====================");

        cursor.close();
        db.close();
    }

    // 打印活动表
    public void printActivitiesTable() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM activities", null);

        Log.d("调试-活动表", "===== 活动表数据 =====");
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String title = cursor.getString(1);
                String type = cursor.getString(2);
                String time = cursor.getString(3);
                String location = cursor.getString(4);
                String description = cursor.getString(5);
                String organizer = cursor.getString(6);
                int maxPeople = cursor.getInt(7);

                Log.d("调试-活动表",
                        "ID：" + id +
                                " | 标题：" + title +
                                " | 类型：" + type +
                                " | 时间：" + time +
                                " | 地点：" + location +
                                " | 主办方：" + organizer +
                                " | 最大人数：" + maxPeople
                );
            } while (cursor.moveToNext());
        } else {
            Log.d("调试-活动表", "活动表为空");
        }
        Log.d("调试-活动表", "====================");

        cursor.close();
        db.close();
    }

    // 打印报名表
    public void printRegistrationsTable() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM registrations", null);

        Log.d("调试-报名表", "===== 报名表数据 =====");
        if (cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(0);
                int activityId = cursor.getInt(1);
                String registerTime = cursor.getString(2);

                Log.d("调试-报名表",
                        "用户ID：" + userId +
                                " | 活动ID：" + activityId +
                                " | 报名时间：" + registerTime
                );
            } while (cursor.moveToNext());
        } else {
            Log.d("调试-报名表", "报名表为空");
        }
        Log.d("调试-报名表", "====================");

        cursor.close();
        db.close();
    }

    // 打印收藏表
    public void printFavoritesTable() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM favorites", null);

        Log.d("调试-收藏表", "===== 收藏表数据 =====");
        if (cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(0);
                int activityId = cursor.getInt(1);

                Log.d("调试-收藏表",
                        "用户ID：" + userId +
                                " | 活动ID：" + activityId
                );
            } while (cursor.moveToNext());
        } else {
            Log.d("调试-收藏表", "收藏表为空");
        }
        Log.d("调试-收藏表", "====================");

        cursor.close();
        db.close();
    }

    // 打印提醒表
    public void printRemindersTable() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM reminders", null);

        Log.d("调试-提醒表", "===== 提醒表数据 =====");
        if (cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(0);
                int activityId = cursor.getInt(1);
                String remindTime = cursor.getString(2);

                Log.d("调试-提醒表",
                        "用户ID：" + userId +
                                " | 活动ID：" + activityId +
                                " | 提醒时间：" + remindTime
                );
            } while (cursor.moveToNext());
        } else {
            Log.d("调试-提醒表", "提醒表为空");
        }
        Log.d("调试-提醒表", "====================");

        cursor.close();
        db.close();
    }

    // ---------------------- 密码修改 ----------------------
    public boolean updatePassword(String userId, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            String sql = "UPDATE users SET password = ? WHERE student_id = ?";
            db.execSQL(sql, new Object[]{newPassword, userId});
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.close();
        }
    }

    // ---------------------- 已收藏活动查询 ----------------------
    public List<ActivityBean> getFavoriteActivities(String userId) {
        List<ActivityBean> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 关联查询收藏表和活动表
        String sql = "SELECT a.* FROM activities a " +
                "JOIN favorites f ON a.id = f.activity_id " +
                "WHERE f.user_id = ? " +
                "ORDER BY a.time DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{userId});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String title = cursor.getString(1);
                String type = cursor.getString(2);
                String time = cursor.getString(3);
                String location = cursor.getString(4);
                String description = cursor.getString(5);
                String organizer = cursor.getString(6);
                int maxPeople = cursor.getInt(7);
                String tag = cursor.getString(8);
                list.add(new ActivityBean(id, title, type, time, location, description, organizer, maxPeople,tag));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    // ---------------------- 已报名活动查询 ----------------------
    public List<ActivityBean> getRegisteredActivities(String userId) {
        List<ActivityBean> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 关联查询报名表和活动表
        String sql = "SELECT a.* FROM activities a " +
                "JOIN registrations r ON a.id = r.activity_id " +
                "WHERE r.user_id = ? " +
                "ORDER BY a.time DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{userId});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String title = cursor.getString(1);
                String type = cursor.getString(2);
                String time = cursor.getString(3);
                String location = cursor.getString(4);
                String description = cursor.getString(5);
                String organizer = cursor.getString(6);
                int maxPeople = cursor.getInt(7);
                String tag = cursor.getString(8);

                list.add(new ActivityBean(id, title, type, time, location, description, organizer, maxPeople,tag));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    // ----------------- AI ----------------
    // DBManager.java
// 获取用户已收藏/报名的活动详情（兴趣画像）
    public List<ActivityBean> getUserInteractedActivities(String userId) {
        List<ActivityBean> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT a.* FROM activities a " +
                "JOIN (SELECT activity_id FROM favorites WHERE user_id=? " +
                "UNION SELECT activity_id FROM registrations WHERE user_id=?) f " +
                "ON a.id = f.activity_id";

        Cursor cursor = db.rawQuery(sql, new String[]{userId, userId});
        if (cursor.moveToFirst()) {
            do {
                ActivityBean bean = new ActivityBean(
                        cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5),
                        cursor.getString(6), cursor.getInt(7)
                );
                list.add(bean);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    // 获取用户未收藏/报名的所有活动（候选池）
    public List<ActivityBean> getUninteractedActivities(String userId) {
        List<ActivityBean> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT a.* FROM activities a " +
                "LEFT JOIN favorites f ON a.id=f.activity_id AND f.user_id=? " +
                "LEFT JOIN registrations r ON a.id=r.activity_id AND r.user_id=? " +
                "WHERE f.activity_id IS NULL AND r.activity_id IS NULL";

        Cursor cursor = db.rawQuery(sql, new String[]{userId, userId});
        if (cursor.moveToFirst()) {
            do {
                ActivityBean bean = new ActivityBean(
                        cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                        cursor.getString(3), cursor.getString(4), cursor.getString(5),
                        cursor.getString(6), cursor.getInt(7)
                );
                list.add(bean);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}
