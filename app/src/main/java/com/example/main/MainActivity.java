package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private DBManager dbManager;
    private CalendarView calendarView;
    private TextView tvCurrentDate;
    private ListView listView;
    private String currentDate;
    private String currentUserId;
    private List<ActivityBean> activityBeanList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 【新增】Android 13+ 动态申请通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        ImageView ivUserCenter = findViewById(R.id.iv_user_center);
        ivUserCenter.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, UserCenterActivity.class)
                    .putExtra("USER_ID", currentUserId));
        });

        listView = findViewById(R.id.listView);
        calendarView = findViewById(R.id.calendarView);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);

        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(new Date());
        tvCurrentDate.setText("当前日期：" + currentDate);

        currentUserId = getIntent().getStringExtra("USER_ID");

        dbManager = new DBManager(this);
        new Thread(() -> loadActivitiesByDate(currentDate)).start();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String monthStr = String.format(Locale.CHINA, "%02d", month + 1);
            String dayStr = String.format(Locale.CHINA, "%02d", dayOfMonth);
            currentDate = year + "-" + monthStr + "-" + dayStr;
            tvCurrentDate.setText("当前日期：" + currentDate);
            loadActivitiesByDate(currentDate);
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            ActivityBean bean = activityBeanList.get(position);
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);
            intent.putExtra("ACTIVITY_ID", bean.getId());
            intent.putExtra("USER_ID", currentUserId);
            startActivity(intent);
        });
    }

    private void loadActivitiesByDate(String date) {
        new Thread(() -> {
            activityBeanList = dbManager.getActivityByDate(date);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (activityBeanList.isEmpty()) {
                    String[] emptyTip = new String[]{"当前日期暂无活动"};
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this, android.R.layout.simple_list_item_1, emptyTip);
                    listView.setAdapter(adapter);
                } else {
                    String[] activityNames = new String[activityBeanList.size()];
                    for (int i = 0; i < activityBeanList.size(); i++) {
                        ActivityBean bean = activityBeanList.get(i);
                        activityNames[i] = bean.getTitle() + " | " + bean.getTime().split(" ")[1];
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            MainActivity.this, android.R.layout.simple_list_item_1, activityNames);
                    listView.setAdapter(adapter);
                }
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadActivitiesByDate(currentDate);
    }
}