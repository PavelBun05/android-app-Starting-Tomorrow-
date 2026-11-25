package com.startingtomorrow.habits;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridLayout;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.*;

import com.startingtomorrow.habits.data.HabitDb;
import com.startingtomorrow.habits.data.HabitDb.Habit;

import java.time.*;
import java.time.format.*;
import java.util.*;

public class MainActivity extends Activity {
    private HabitDb db;
    private LinearLayout root, content;
    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNav;
    private int tab = 0, monthOffset = 0;
    private boolean dark;
    private int bg, surface, text, muted, line, accent;
    private final int[] palette = {0xFF4F46E5, 0xFF0D9488, 0xFFF97316, 0xFFDB2777, 0xFF7C3AED,
            0xFF2563EB, 0xFF65A30D};
    private final String[] months = {"января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"};

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        db = HabitDb.get(this);
        dark = prefs().getBoolean("dark", false);
        colors();
        show();
    }

    private android.content.SharedPreferences prefs() {
        return getSharedPreferences("app_settings", MODE_PRIVATE);
    }

    private void colors() {
        bg = Color.parseColor(dark ? "#171923" : "#F0F2F5");
        surface = Color.parseColor(dark ? "#222633" : "#FFFFFF");
        text = Color.parseColor(dark ? "#F4F5F8" : "#171923");
        muted = Color.parseColor(dark ? "#A5ADBD" : "#737987");
        line = Color.parseColor(dark ? "#343B4A" : "#E8EAF0");
        accent = Color.parseColor("#4F46E5");
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        if (Build.VERSION.SDK_INT >= 23)
            getWindow().getDecorView().setSystemUiVisibility(dark ? 0 :
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private TextView label(String s, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(text);
        if (bold)
            t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    private TextView small(String s) {
        TextView t = label(s, 12, false);
        t.setTextColor(muted);
        return t;
    }

    private void pad(View v, int a, int b, int c, int d) {
        v.setPadding(dp(a), dp(b), dp(c), dp(d));
    }

    private void space(LinearLayout l, int h) {
        View v = new View(this);
        l.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
    }

    private void show() {
        colors();
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.rootLayout);
        content = findViewById(R.id.contentContainer);
        bottomNav = findViewById(R.id.bottomNavigation);
        root.setBackgroundColor(bg);
        TextView appTitle = findViewById(R.id.appTitle);
        appTitle.setTextColor(text);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        int selected = tab == 0 ? R.id.nav_today : tab == 1 ? R.id.nav_calendar : tab == 2 ?
                R.id.nav_stats : R.id.nav_settings;
        if (bottomNav.getSelectedItemId() != selected)
            bottomNav.setSelectedItemId(selected);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            tab = id == R.id.nav_today ? 0 : id == R.id.nav_calendar ? 1 : id == R.id.nav_stats ?
                    2 : 3;
            show();
            return true;
        });
        if (tab == 0)
            dashboard();
        else if (tab == 1)
            calendar();
        else if (tab == 2)
            statistics();
        else
            settings();
        bottomNav.setBackgroundColor(surface);
    }

    private LinearLayout card() {
        LinearLayout c = column();
        pad(c, 16, 15, 16, 15);
        c.setBackground(shape(surface, 18));
        return c;
    }

    private void dashboard() {
        LocalDate today = LocalDate.now();
        List<Habit> habits = db.all();
        int done = 0, eligible = 0;
        for (Habit h : habits) {
            if (h.weekdays.contains("" + today.getDayOfWeek().getValue()) && !db.isHidden(h.id,
                    today)) {
                eligible++;
                if (db.state(h.id, today) == 1)
                    done++;
            }
        }
        TextView date = label(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM",
                new Locale("ru"))), 14, true);
        content.addView(date);
        space(content, 12);
        LinearLayout summary = card();
        TextView p = label((eligible == 0 ? 0 : done * 100 / eligible) + "%", 34, true);
        p.setTextColor(accent);
        summary.addView(p);
        summary.addView(small("Выполнено привычек сегодня: " + done + " из " + eligible));
        space(summary, 10);
        ProgressBar progress = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progress.setMax(Math.max(1, eligible));
        progress.setProgress(done);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        summary.addView(progress, new LinearLayout.LayoutParams(-1, dp(7)));
        content.addView(summary);
        space(content, 20);
        LinearLayout head = row();
        head.addView(label("Мои привычки", 19, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView add = label("+ Добавить", 14, true);
        add.setTextColor(accent);
        add.setOnClickListener(v -> editHabit(null));
        head.addView(add);
        content.addView(head);
        space(content, 10);
        if (habits.isEmpty()) {
            empty("Пока нет привычек", "Добавь первую привычку, с которой начнёшь завтра.");
            return;
        }
        for (Habit h : habits) {
            LinearLayout c = card();
            LinearLayout r = row();
            View dot = new View(this);
            dot.setBackground(shape(h.color, 8));
            r.addView(dot, new LinearLayout.LayoutParams(dp(10), dp(34)));
            LinearLayout info = column();
            pad(info, 12, 0, 0, 0);
            info.addView(label(h.name, 16, true));
            info.addView(small("Серия: " + db.streak(h.id) + " дн.  ·  " + habitPercent(h) + "%"));
            r.addView(info, new LinearLayout.LayoutParams(0, -2, 1));
            int state = db.state(h.id, today);
            boolean scheduledToday =
                    h.weekdays != null && h.weekdays.contains(String.valueOf(today.getDayOfWeek().getValue()));
            boolean doneToday = scheduledToday && state == 1;
            TextView check = label(doneToday ? "✓" : (scheduledToday ? "○" : "—"), 23, true);
            check.setTextColor(doneToday ? Color.WHITE : (scheduledToday ? h.color : muted));
            check.setGravity(Gravity.CENTER);
            check.setBackground(shape(doneToday ? h.color : (scheduledToday ? bg : line), 30));
            r.addView(check, new LinearLayout.LayoutParams(dp(44), dp(44)));
            check.setEnabled(scheduledToday);
            check.setAlpha(scheduledToday ? 1f : 0.55f);
            if (scheduledToday)
                check.setOnClickListener(v -> {
                    if (!today.isAfter(LocalDate.now())) {
                        db.toggleComplete(h.id, today);
                        show();
                    }
                });
            c.addView(r);
            c.setOnClickListener(v -> habitDetails(h));
            c.setOnLongClickListener(v -> {
                editHabit(h);
                return true;
            });
            space(content, 9);
            content.addView(c);
        }
    }

    private int habitPercent(Habit h) {
        LocalDate first = db.firstCompletedDate(h.id);
        if (first == null)
            return 0;
        int total = db.countEligible(h.id, first, LocalDate.now());
        int done = db.countState(h.id, first, LocalDate.now(), 1);
        return total == 0 ? 0 : Math.min(100, Math.round(done * 100f / total));
    }

    private void calendar() {
        YearMonth ym = YearMonth.now().plusMonths(monthOffset);
        LinearLayout nav = row();
        TextView prev = label("‹", 30, true), next = label("›", 30, true);
        prev.setTextColor(accent);
        next.setTextColor(accent);
        prev.setGravity(Gravity.CENTER);
        next.setGravity(Gravity.CENTER);
        nav.addView(prev, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = label(ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL,
                new Locale("ru")) + " " + ym.getYear(), 18, true);
        title.setGravity(Gravity.CENTER);
        nav.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48)));
        prev.setOnClickListener(v -> {
            monthOffset--;
            show();
        });
        next.setOnClickListener(v -> {
            monthOffset++;
            show();
        });
        content.addView(nav);
        space(content, 8);
        content.addView(small("Нажатие — отметить выполнение. Долгое нажатие — исключить день из " +
                "расчёта."));
        space(content, 8);
        content.addView(small("● выполнено     ● пропущено     ● исключено"));
        space(content, 12);
        String[] weekNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (Habit h : db.all()) {
            LinearLayout c = card();
            LinearLayout top = row();
            View dot = new View(this);
            dot.setBackground(shape(h.color, 5));
            top.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(26)));
            TextView name = label(h.name, 15, true);
            pad(name, 9, 0, 0, 0);
            top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            top.addView(small(habitPercent(h) + "%"));
            c.addView(top);
            space(c, 10);
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(7);
            grid.setUseDefaultMargins(false);
            for (String dayName : weekNames) {
                TextView w = small(dayName);
                w.setGravity(Gravity.CENTER);
                GridLayout.LayoutParams wp = new GridLayout.LayoutParams();
                wp.width = 0;
                wp.height = dp(28);
                wp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                grid.addView(w, wp);
            }
            int first = ym.atDay(1).getDayOfWeek().getValue();
            for (int i = 1; i < first; i++) {
                View blank = new View(this);
                GridLayout.LayoutParams bp = new GridLayout.LayoutParams();
                bp.width = 0;
                bp.height = dp(46);
                bp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                grid.addView(blank, bp);
            }
            LocalDate firstCompleted = db.firstCompletedDate(h.id);
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                final LocalDate date = ym.atDay(day);
                final int state = db.state(h.id, date);
                final boolean hidden = db.isHidden(h.id, date);
                TextView cell = label(String.valueOf(day), 14, true);
                cell.setGravity(Gravity.CENTER);
                boolean scheduled =
                        h.weekdays != null && h.weekdays.contains(String.valueOf(date.getDayOfWeek().getValue()));
                boolean future = date.isAfter(LocalDate.now());
                boolean missed =
                        scheduled && firstCompleted != null && date.isAfter(firstCompleted) && date.isBefore(LocalDate.now()) && state == 0 && !hidden;
                boolean inactive = !scheduled;
                int cellColor = inactive ? Color.parseColor(dark ? "#303541" : "#E5E7EB") :
                        (hidden ? Color.parseColor(dark ? "#505866" : "#D1D5DB") :
                                (state == 1 ? h.color : (missed ? Color.parseColor(dark ?
                                        "#593B42" : "#F8CACA") : surface)));
                int cellTextColor = inactive ? Color.parseColor(dark ? "#727B8D" : "#A1A1AA") :
                        (state == 1 ? Color.WHITE : (missed ? Color.parseColor(dark ? "#FFD5D5" :
                                "#8A2525") : text));
                if (future)
                    cellTextColor = muted;
                cell.setTextColor(cellTextColor);
                cell.setBackground(shape(cellColor, 12));
                if (!scheduled) {
                    cell.setAlpha(0.72f);
                    cell.setContentDescription(date + " — привычка не запланирована");
                }
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = 0;
                lp.height = dp(46);
                lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                grid.addView(cell, lp);
                if (scheduled) {
                    cell.setOnClickListener(v -> {
                        if (!date.isAfter(LocalDate.now())) {
                            db.toggleComplete(h.id, date);
                            show();
                        }
                    });
                    cell.setOnLongClickListener(v -> {
                        if (!date.isAfter(LocalDate.now())) {
                            db.toggleHidden(h.id, date);
                            show();
                        }
                        return true;
                    });
                } else {
                    cell.setClickable(false);
                    cell.setLongClickable(false);
                }
            }
            c.addView(grid);
            space(c, 5);
            c.addView(small("Нажми: выполнить / отменить · удерживай: исключить / вернуть"));
            space(content, 10);
            content.addView(c);
        }
    }

    private void statistics() {
        List<Habit> habits = db.all();
        LocalDate now = LocalDate.now(), week = now.minusDays(6), month = now.minusDays(29);
        TextView h = label("Общий прогресс", 19, true);
        content.addView(h);
        space(content, 10);
        int totalPercent = 0;
        for (Habit x : habits)
            totalPercent += habitPercent(x);
        int overall = habits.isEmpty() ? 0 : Math.min(100,
                Math.round(totalPercent / (float) habits.size()));
        LinearLayout c = card();
        TextView pct = label(overall + "%", 36, true);
        pct.setTextColor(accent);
        c.addView(pct);
        c.addView(small("Средний прогресс по привычкам — как в веб-версии"));
        space(c, 12);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(overall);
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        c.addView(bar, new LinearLayout.LayoutParams(-1, dp(8)));
        content.addView(c);
        space(content, 18);
        content.addView(label("По привычкам", 18, true));
        space(content, 8);
        for (Habit x : habits) {
            LinearLayout item = card();
            item.addView(label(x.name, 16, true));
            space(item, 5);
            int w = db.countState(x.id, week, now, 1), we = db.countEligible(x.id, week, now), m
                    = db.countState(x.id, month, now, 1), me = db.countEligible(x.id, month, now);
            item.addView(small("7 дней: " + w + "/" + we + "  ·  30 дней: " + m + "/" + me));
            space(item, 5);
            item.addView(small("Текущая серия: " + db.streak(x.id) + " дней"));
            space(content, 8);
            content.addView(item);
        }
        if (habits.isEmpty())
            empty("Нет данных", "Добавь привычку, чтобы появилась статистика.");
    }

    private void settings() {
        content.addView(label("Настройки", 21, true));
        space(content, 12);
        LinearLayout theme = card();
        theme.addView(label("Оформление", 16, true));
        space(theme, 6);
        theme.addView(small("Выбери светлую или тёмную тему приложения."));
        space(theme, 10);
        Switch sw = new Switch(this);
        sw.setText("Тёмная тема");
        sw.setTextColor(text);
        sw.setChecked(dark);
        sw.setOnCheckedChangeListener((b, v) -> {
            dark = v;
            prefs().edit().putBoolean("dark", v).apply();
            show();
        });
        theme.addView(sw);
        content.addView(theme);
        space(content, 10);
        LinearLayout rem = card();
        rem.addView(label("Напоминания", 16, true));
        space(rem, 6);
        rem.addView(small("Ежедневное уведомление о привычках."));
        Switch rs = new Switch(this);
        rs.setText("Ежедневное напоминание");
        rs.setTextColor(text);
        rs.setChecked(prefs().getBoolean("reminders", false));
        rem.addView(rs);
        TextView reminderStatus = small(reminderStatusText());
        rem.addView(reminderStatus);
        TextView time = label(String.format(Locale.getDefault(), "%02d:%02d", prefs().getInt(
                "hour", 20), prefs().getInt("minute", 0)), 18, true);
        time.setTextColor(accent);
        pad(time, 0, 12, 0, 0);
        rem.addView(time);
        Button testReminder = new Button(this);
        testReminder.setText("Проверить уведомление через минуту");
        rem.addView(testReminder);
        testReminder.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 44);
                Toast.makeText(this, "Разреши уведомления, затем нажми проверку ещё раз",
                        Toast.LENGTH_LONG).show();
            } else {
                scheduleTestReminder(this);
                Toast.makeText(this, "Тестовое уведомление запланировано на 1 минуту",
                        Toast.LENGTH_LONG).show();
            }
        });
        time.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, min) -> {
                prefs().edit().putInt("hour", hour).putInt("minute", min).apply();
                time.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, min));
                if (rs.isChecked())
                    enableReminders();
                reminderStatus.setText(reminderStatusText());
            }, prefs().getInt("hour", 20), prefs().getInt("minute", 0), true).show();
        });
        rs.setOnCheckedChangeListener((b, v) -> {
            prefs().edit().putBoolean("reminders", v).apply();
            if (v) {
                enableReminders();
            } else
                cancelReminder(this);
            reminderStatus.setText(reminderStatusText());
        });
        content.addView(rem);
        space(content, 10);
        LinearLayout about = card();
        about.addView(label("О приложении", 16, true));
        space(about, 6);
        about.addView(small("Starting tomorrow · версия 1.0\nДанные хранятся локально на этом " +
                "устройстве. Интернет и регистрация не требуются."));
        content.addView(about);
    }

    private String reminderStatusText() {
        boolean notifications =
                Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean exact =
                Build.VERSION.SDK_INT < 31 || ((AlarmManager) getSystemService(ALARM_SERVICE)).canScheduleExactAlarms();
        if (!notifications)
            return "Нужно разрешить уведомления в Android.";
        if (!exact)
            return "Точные будильники не разрешены. Напоминание будет запланировано с возможной " +
                    "задержкой.";
        return "Уведомления разрешены. Напоминание будет приходить ежедневно.";
    }

    private void enableReminders() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 44);
            return;
        }
        if (Build.VERSION.SDK_INT >= 31 && !((AlarmManager) getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()) {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            } catch (Exception e) {
                Toast.makeText(this, "Разреши точные будильники в настройках Android",
                        Toast.LENGTH_LONG).show();
            }
            // Даже без специального разрешения ставим неточное напоминание, чтобы оно не
            // пропадало совсем.
        }
        scheduleReminder(this, prefs().getInt("hour", 20), prefs().getInt("minute", 0));
        Toast.makeText(this, "Напоминание запланировано", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 44) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
                enableReminders();
            else
                Toast.makeText(this, "Без разрешения уведомления не будут показываться",
                        Toast.LENGTH_LONG).show();
            show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (prefs().getBoolean("reminders", false))
            scheduleReminder(this, prefs().getInt("hour", 20), prefs().getInt("minute", 0));
        if (tab == 3)
            show();
    }

    public static void scheduleReminder(Context c, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis())
            cal.add(Calendar.DAY_OF_YEAR, 1);
        Intent i = new Intent(c, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 101, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            // Fallback: Android всё равно доставит напоминание, но может немного задержать его.
            if (Build.VERSION.SDK_INT >= 23)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            else
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else
            am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
    }

    public static void scheduleTestReminder(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.putExtra("test_notification", true);
        PendingIntent pi = PendingIntent.getBroadcast(c, 102, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(ALARM_SERVICE);
        long trigger = System.currentTimeMillis() + 60_000L;
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            if (Build.VERSION.SDK_INT >= 23)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
            else
                am.set(AlarmManager.RTC_WAKEUP, trigger, pi);
        } else if (Build.VERSION.SDK_INT >= 23)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        else
            am.setExact(AlarmManager.RTC_WAKEUP, trigger, pi);
    }

    public static void cancelReminder(Context c) {
        PendingIntent pi = PendingIntent.getBroadcast(c, 101, new Intent(c,
                ReminderReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        ((AlarmManager) c.getSystemService(ALARM_SERVICE)).cancel(pi);
    }

    private void empty(String title, String sub) {
        LinearLayout c = card();
        c.setGravity(Gravity.CENTER);
        c.addView(label(title, 17, true));
        space(c, 5);
        c.addView(small(sub));
        space(content, 12);
        content.addView(c);
    }

    private void editHabit(Habit old) {
        LinearLayout box = column();
        pad(box, 4, 4, 4, 4);
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        name.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        name.setHint("Например, Читать 20 минут");
        name.setTextColor(text);
        name.setHintTextColor(muted);
        if (old != null)
            name.setText(old.name);
        box.addView(name);
        box.addView(small("Цвет привычки"));
        LinearLayout colors = row();
        final int[] selected = {old == null ? palette[0] : old.color};
        for (int col : palette) {
            TextView swatch = label("●", 28, true);
            swatch.setTextColor(col);
            swatch.setGravity(Gravity.CENTER);
            swatch.setOnClickListener(v -> {
                selected[0] = col;
            });
            colors.addView(swatch, new LinearLayout.LayoutParams(0, dp(45), 1));
        }
        box.addView(colors);
        box.addView(small("Дни недели (для расписания)"));
        String[] days = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        CheckBox[] checks = new CheckBox[7];
        LinearLayout dayrow = row();
        for (int i = 0; i < 7; i++) {
            checks[i] = new CheckBox(this);
            checks[i].setText(days[i]);
            checks[i].setTextSize(11);
            checks[i].setTextColor(text);
            checks[i].setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
            checks[i].setChecked(old == null || old.weekdays.contains("" + (i + 1)));
            dayrow.addView(checks[i], new LinearLayout.LayoutParams(0, -2, 1));
        }
        box.addView(dayrow);
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(old == null ? "Новая" +
                        " привычка" : "Изменить привычку").setView(box)
                .setNegativeButton("Отмена", null).setPositiveButton("Сохранить", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) {
                        Toast.makeText(this, "Введите название привычки", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    StringBuilder wd = new StringBuilder();
                    for (int i = 0; i < 7; i++)
                        if (checks[i].isChecked())
                            wd.append(i + 1);
                    if (wd.length() == 0) {
                        Toast.makeText(this, "Выберите хотя бы один день", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Habit h = old == null ? new Habit() : old;
                    h.name = n;
                    h.color = selected[0];
                    h.weekdays = wd.toString();
                    if (h.created == null)
                        h.created = LocalDate.now().toString();
                    db.save(h);
                    show();
                });
        if (old != null)
            builder.setNeutralButton("Удалить", (d, w) -> new AlertDialog.Builder(this).setTitle(
                    "Удалить привычку?").setMessage("История выполнения этой привычки также будет" +
                    " удалена.").setNegativeButton("Отмена", null).setPositiveButton("Удалить",
                    (x, y) -> {
                db.delete(old.id);
                show();
            }).show());
        AlertDialog dialog = builder.show();
        name.requestFocus();
        name.post(() -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(name, InputMethodManager.SHOW_IMPLICIT));
    }

    private void habitDetails(Habit h) {
        LinearLayout box = column();
        pad(box, 8, 4, 8, 4);
        box.addView(label(h.name, 19, true));
        space(box, 8);
        box.addView(small("Текущая серия: " + db.streak(h.id) + " дней"));
        box.addView(small("Общий прогресс: " + habitPercent(h) + "%"));
        space(box, 12);
        new AlertDialog.Builder(this).setTitle("Привычка").setView(box).setPositiveButton(
                "Изменить", (d, w) -> editHabit(h)).setNeutralButton("Закрыть", null).show();
    }
}
