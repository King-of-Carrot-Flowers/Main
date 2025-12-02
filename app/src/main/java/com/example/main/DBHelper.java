package com.example.main;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "bh_activity.db";
    private static final int DB_VERSION = 3;  // 升级版本号

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 用户表（之前的登录注册表）
        db.execSQL("CREATE TABLE users (student_id TEXT PRIMARY KEY, password TEXT NOT NULL)");

        // 活动表建表SQL（去掉currentPeople）
        db.execSQL("CREATE TABLE activities (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "time TEXT NOT NULL, " +
                "location TEXT NOT NULL, " +
                "description TEXT, " +
                "organizer TEXT, " +
                "maxPeople INTEGER," +
                "tag TEXT)");

        // 收藏表
        db.execSQL("CREATE TABLE favorites (" +
                "user_id TEXT, " +
                "activity_id INTEGER, " +
                "PRIMARY KEY (user_id, activity_id))");

        // 报名表
        db.execSQL("CREATE TABLE registrations (" +
                "user_id TEXT, " +
                "activity_id INTEGER, " +
                "register_time TEXT, " +
                "PRIMARY KEY (user_id, activity_id))");

        // 提醒表
        db.execSQL("CREATE TABLE reminders (" +
                "user_id TEXT, " +
                "activity_id INTEGER, " +
                "remind_time TEXT, " +
                "PRIMARY KEY (user_id, activity_id))");

        // 初始化测试数据（11.28-12.03，包含今天11.30）
        db.execSQL("INSERT INTO activities (title, type, time, location, description, organizer, maxPeople, tag) VALUES " +
                // 11.28（周五）
                "('北航青年学者论坛：量子计算', '讲座', '2025-11-28 10:00', '学术会堂201', '邀请清华、北大青年学者分享量子计算前沿', '科研处', 300, '其他'), " +
                "('羽毛球社团友谊赛', '社团', '2025-11-28 16:00', '沙河体育馆', '与北理工羽毛球社团交流比赛', '羽毛球社团', 40, '美育'), " +
                // 11.29（周六）
                "('创新创业大赛宣讲会', '比赛', '2025-11-29 14:00', '新主楼会议中心', '讲解大赛规则、报名方式及奖项设置', '创新创业中心', 200, '其他'), " +
                "('摄影技巧分享会', '社团', '2025-11-29 19:00', '沙河校区二教105', '专业摄影师分享人像/风光摄影技巧', '摄影社团', 80, '美育'), " +
                // 11.30（周日，今天）
                "('“AI+教育”主题沙龙', '讲座', '2025-11-30 15:00', '图书馆五层研讨室', '探讨人工智能在教育领域的应用与挑战', '教育学院', 50, '其他'), " +
                "('校合唱团排练', '社团', '2025-11-30 18:00', '学生活动中心音乐厅', '备战北京市大学生合唱比赛', '校合唱团', 60, '美育'), " +
                "('篮球校队选拔赛', '比赛', '2025-11-30 14:00', '学院路校区篮球场', '选拔新一届校篮球队成员', '体育部', 50, '劳育'), " +
                // 12.01（周一）
                "('“航天精神”主题讲座', '讲座', '2025-12-01 09:00', '晨兴音乐厅', '邀请航天英雄分享飞天故事', '宇航学院', 500, '德育'), " +
                "('编程马拉松赛前培训', '比赛', '2025-12-01 18:00', '计算机学院实验室', '讲解马拉松赛题类型及解题技巧', 'ACM社团', 100, '其他'), " +
                // 12.02（周二）
                "('环保公益活动：校园植树', '社团', '2025-12-02 13:00', '沙河校区南门绿地', '种植树苗，共建绿色校园', '环保社团', 120, '劳育'), " +
                "('金融科技前沿讲座', '讲座', '2025-12-02 15:00', '经管学院报告厅', '银行高管分享金融科技发展趋势', '经管学院', 150, '其他'), " +
                // 12.03（周三）
                "('机器人竞赛校内选拔赛', '比赛', '2025-12-03 09:00', '机器人实验室', '选拔队伍参加全国大学生机器人竞赛', '自动化学院', 40, '其他'), " +
                "('吉他社露天演出', '社团', '2025-12-03 19:00', '学院路校区银杏大道', '吉他弹唱，欢迎点歌', '吉他社团', 200, '美育')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级时删除旧表重建
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS activities");
        db.execSQL("DROP TABLE IF EXISTS favorites");
        db.execSQL("DROP TABLE IF EXISTS registrations");
        db.execSQL("DROP TABLE IF EXISTS reminders");
        onCreate(db);
    }
}