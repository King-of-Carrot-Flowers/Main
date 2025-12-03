package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {
    private DBManager dbManager;
    private int activityId;
    private ActivityBean activityBean;
    private String currentUserId;
    private TextView tvTitle, tvType, tvTime, tvLocation, tvOrganizer, tvApplyCount, tvDescription;

    // 【修改点1】移除 btnDebugPrint 的声明
    private Button btnCollect, btnApply;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("活动详情");
        }

        activityId = getIntent().getIntExtra("ACTIVITY_ID", -1);
        currentUserId = getIntent().getStringExtra("USER_ID");
        if (activityId == -1 || currentUserId == null) {
            Toast.makeText(this, "参数错误或用户未登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        dbManager = new DBManager(this);
        loadActivityDetail();
        setButtonListeners();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tv_title);
        tvType = findViewById(R.id.tv_type);
        tvTime = findViewById(R.id.tv_time);
        tvLocation = findViewById(R.id.tv_location);
        tvOrganizer = findViewById(R.id.tv_organizer);
        tvApplyCount = findViewById(R.id.tv_apply_count);
        tvDescription = findViewById(R.id.tv_description);
        btnCollect = findViewById(R.id.btn_collect);
        btnApply = findViewById(R.id.btn_apply);

        // 【修改点2】移除对 btn_debug_print 的 findViewById 调用
        // btnDebugPrint = findViewById(R.id.btn_debug_print);
    }

    private boolean isActivityEnded(String activityTimeStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date activityDate = sdf.parse(activityTimeStr);
            return new Date().after(activityDate);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadActivityDetail() {
        btnCollect.setEnabled(false);
        btnApply.setEnabled(false);

        new Thread(() -> {
            activityBean = dbManager.getActivityById(activityId);
            if (activityBean == null) {
                new Handler(Looper.getMainLooper()).post(this::finish);
                return;
            }
            int currentPeople = dbManager.getCurrentPeopleByActivityId(activityId);
            boolean isApplied = dbManager.isUserApplied(currentUserId, activityId);
            boolean isCollected = dbManager.isUserCollected(currentUserId, activityId);
            boolean isFull = currentPeople >= activityBean.getMaxPeople();
            boolean isEnded = isActivityEnded(activityBean.getTime());

            new Handler(Looper.getMainLooper()).post(() -> {
                tvTitle.setText(activityBean.getTitle());
                tvType.setText("活动类型：" + activityBean.getType());
                tvTime.setText("活动时间：" + activityBean.getTime());
                tvLocation.setText("活动地点：" + activityBean.getLocation());
                tvOrganizer.setText("主办方：" + activityBean.getOrganizer());
                tvApplyCount.setText("报名情况：" + currentPeople + "/" + activityBean.getMaxPeople());
                tvDescription.setText("活动详情：\n" + activityBean.getDescription());

                updateCollectButtonStatus(isCollected);
                updateApplyButtonStatus(isFull, isApplied, isEnded);

                btnCollect.setEnabled(true);
            });
        }).start();
    }

    private void updateCollectButtonStatus(boolean isCollected) {
        if (isCollected) {
            btnCollect.setText("已收藏");
            // 假设您在 colors.xml 中定义了灰色
            btnCollect.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));
        } else {
            btnCollect.setText("收藏");
            // 假设您在 colors.xml 中定义了橙色
            btnCollect.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.orange)));
        }
    }

    private void updateApplyButtonStatus(boolean isFull, boolean isApplied, boolean isEnded) {
        if (isEnded) {
            btnApply.setText("活动已结束");
            btnApply.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));
            btnApply.setEnabled(false);
        } else if (isApplied) {
            btnApply.setText("取消报名");
            btnApply.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red)));
            btnApply.setEnabled(true);
        } else if (isFull) {
            btnApply.setText("人数已满");
            btnApply.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray)));
            btnApply.setEnabled(false);
        } else {
            btnApply.setText("报名参加");
            btnApply.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue)));
            btnApply.setEnabled(true);
        }
    }

    private void setButtonListeners() {
        btnCollect.setOnClickListener(v -> new Thread(() -> {
            if (activityBean == null) return;

            boolean isCollected = dbManager.isUserCollected(currentUserId, activityId);
            boolean dbSuccess;

            if (isCollected) {
                dbSuccess = dbManager.uncollectActivity(currentUserId, activityId);
                if (dbSuccess) {
                    ReminderUtils.cancelReminder(DetailActivity.this, activityId);
                }
            } else {
                dbSuccess = dbManager.collectActivity(currentUserId, activityId);
                if (dbSuccess) {
                    ReminderUtils.setReminder(DetailActivity.this, currentUserId, activityBean);
                }
            }

            if (dbSuccess) {
                // 回到主线程更新UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    updateCollectButtonStatus(!isCollected);
                    Toast.makeText(DetailActivity.this,
                            isCollected ? "取消收藏成功" : "收藏成功",
                            Toast.LENGTH_SHORT).show();
                    if (!isCollected) {
                        Toast.makeText(DetailActivity.this, "已为您设置活动提醒", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start());

        btnApply.setOnClickListener(v -> new Thread(() -> {
            if (activityBean == null) return;

            if (isActivityEnded(activityBean.getTime())) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(DetailActivity.this, "活动已结束，无法操作", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            boolean isApplied = dbManager.isUserApplied(currentUserId, activityId);
            int currentPeople = dbManager.getCurrentPeopleByActivityId(activityId);
            boolean isFull = currentPeople >= activityBean.getMaxPeople();

            if (!isApplied && isFull) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(DetailActivity.this, "人数已满，无法报名", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            boolean dbSuccess;
            if (isApplied) {
                dbSuccess = dbManager.cancelApplyActivity(currentUserId, activityId);
            } else {
                dbSuccess = dbManager.applyActivity(currentUserId, activityId);
            }

            if (dbSuccess) {
                // 重新获取最新报名人数
                int newCurrentPeople = dbManager.getCurrentPeopleByActivityId(activityId);
                boolean newIsApplied = !isApplied;

                new Handler(Looper.getMainLooper()).post(() -> {
                    tvApplyCount.setText("报名情况：" + newCurrentPeople + "/" + activityBean.getMaxPeople());
                    updateApplyButtonStatus(newCurrentPeople >= activityBean.getMaxPeople(), newIsApplied, false);
                    Toast.makeText(DetailActivity.this,
                            isApplied ? "取消报名成功" : "报名成功",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start());

        // 【修改点3】移除对 btnDebugPrint 的监听器设置
    }

    // 【修改点4】移除 printAllTableData() 这个方法
}