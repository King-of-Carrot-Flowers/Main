package com.example.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
    private Button btnCollect, btnApply, btnDebugPrint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("活动详情");

        activityId = getIntent().getIntExtra("ACTIVITY_ID", -1);
        currentUserId = getIntent().getStringExtra("USER_ID");
        if (activityId == -1) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (currentUserId == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
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
        btnDebugPrint = findViewById(R.id.btn_debug_print);
    }

    // 【新增方法】判断活动是否已结束
    private boolean isActivityEnded(String activityTimeStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date activityDate = sdf.parse(activityTimeStr);
            Date currentDate = new Date();
            // 如果当前时间 晚于 活动时间，就是结束了
            return currentDate.after(activityDate);
        } catch (Exception e) {
            e.printStackTrace();
            return false; // 解析失败默认没结束
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

            // 【修改点 1】检测是否已结束
            boolean isEnded = isActivityEnded(activityBean.getTime());

            new Handler(Looper.getMainLooper()).post(() -> {
                tvTitle.setText(activityBean.getTitle());
                tvType.setText("活动类型：" + activityBean.getType());
                tvTime.setText("活动时间：" + activityBean.getTime());
                tvLocation.setText("活动地点：" + activityBean.getLocation());
                tvOrganizer.setText("主办方：" + activityBean.getOrganizer());
                tvApplyCount.setText("报名情况：" + currentPeople + "/" + activityBean.getMaxPeople());
                tvDescription.setText("活动详情：\n" + activityBean.getDescription());

                btnCollect.setText(isCollected ? "已收藏" : "收藏");
                btnCollect.setBackgroundTintList(getResources().getColorStateList(
                        isCollected ? android.R.color.darker_gray : R.color.orange));

                // 【修改点 2】传递 isEnded 参数
                updateApplyButtonStatus(isFull, isApplied, isEnded);

                btnCollect.setEnabled(true);
            });
        }).start();
    }

    // 【修改点 3】增加 isEnded 参数，并处理 UI
    private void updateApplyButtonStatus(boolean isFull, boolean isApplied, boolean isEnded) {
        if (isEnded) {
            // 如果活动结束，最高优先级，禁用按钮并变灰
            btnApply.setText("活动已结束");
            btnApply.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
            btnApply.setEnabled(false);
        } else if (isApplied) {
            // 已报名 -> 红色
            btnApply.setText("取消报名");
            btnApply.setBackgroundTintList(getResources().getColorStateList(R.color.red));
            btnApply.setEnabled(true);
        } else if (isFull) {
            // 人数已满 -> 灰色
            btnApply.setText("人数已满");
            btnApply.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
            btnApply.setEnabled(false);
        } else {
            // 可报名 -> 蓝色
            btnApply.setText("报名参加");
            btnApply.setBackgroundTintList(getResources().getColorStateList(R.color.blue));
            btnApply.setEnabled(true);
        }
    }

    private void setButtonListeners() {
        btnCollect.setOnClickListener(v -> new Thread(() -> {
            if (activityBean == null) return;
            // ... (收藏逻辑保持不变) ...
            // 省略这部分代码，和你原来的一样即可
            boolean isCollected = dbManager.isUserCollected(currentUserId, activityId);
            boolean dbSuccess;
            boolean reminderSetSuccess = false;

            if (isCollected) {
                dbSuccess = dbManager.uncollectActivity(currentUserId, activityId);
                if (dbSuccess) {
                    ReminderUtils.cancelReminder(DetailActivity.this, activityId);
                }
            } else {
                dbSuccess = dbManager.collectActivity(currentUserId, activityId);
                if (dbSuccess) {
                    reminderSetSuccess = ReminderUtils.setReminder(DetailActivity.this, currentUserId, activityBean);
                }
            }

            final boolean finalReminderSetSuccess = reminderSetSuccess;

            if (dbSuccess) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    btnCollect.setText(isCollected ? "收藏" : "已收藏");
                    btnCollect.setBackgroundTintList(getResources().getColorStateList(
                            isCollected ? R.color.orange : android.R.color.darker_gray));

                    Toast.makeText(DetailActivity.this,
                            isCollected ? "取消收藏成功" : "收藏成功",
                            Toast.LENGTH_SHORT).show();

                    if (!isCollected && finalReminderSetSuccess) {
                        Toast.makeText(DetailActivity.this, "已设置提醒", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start());

        btnApply.setOnClickListener(v -> new Thread(() -> {
            if (activityBean == null) return;

            // 【修改点 4】点击时的双重校验
            if (isActivityEnded(activityBean.getTime())) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(DetailActivity.this, "活动已结束，无法报名", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            boolean isApplied = dbManager.isUserApplied(currentUserId, activityId);
            int currentPeople = dbManager.getCurrentPeopleByActivityId(activityId);
            boolean isFull = currentPeople >= activityBean.getMaxPeople();

            if (isFull && !isApplied) return;

            boolean success;
            if (isApplied) {
                success = dbManager.cancelApplyActivity(currentUserId, activityId);
                currentPeople--;
            } else {
                success = dbManager.applyActivity(currentUserId, activityId);
                currentPeople++;
            }

            if (success) {
                final int finalCurrentPeople = currentPeople;
                // 注意这里也要重新获取一下 isEnded 状态（虽然极短时间内不会变，但为了逻辑严谨）
                final boolean finalIsEnded = isActivityEnded(activityBean.getTime());

                new Handler(Looper.getMainLooper()).post(() -> {
                    tvApplyCount.setText("报名情况：" + finalCurrentPeople + "/" + activityBean.getMaxPeople());
                    // 【修改点 5】更新按钮状态时传入 finalIsEnded
                    updateApplyButtonStatus(finalCurrentPeople >= activityBean.getMaxPeople(), !isApplied, finalIsEnded);

                    Toast.makeText(DetailActivity.this,
                            isApplied ? "取消报名成功" : "报名成功",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start());

        btnDebugPrint.setOnClickListener(v -> printAllTableData());
    }

    private void printAllTableData() {
        new Thread(() -> {
            dbManager.printUsersTable();
            dbManager.printActivitiesTable();
            dbManager.printRegistrationsTable();
            dbManager.printFavoritesTable();
            dbManager.printRemindersTable();
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(this, "已打印所有表数据到Logcat", Toast.LENGTH_SHORT).show()
            );
        }).start();
    }
}