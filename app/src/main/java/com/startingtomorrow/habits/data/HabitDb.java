package com.startingtomorrow.habits.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class HabitDb extends SQLiteOpenHelper {
    public static class Habit {
        public long id;
        public String name;
        public int color;
        public String created;
        public String weekdays;
    }
    private static HabitDb instance;
    public static synchronized HabitDb get(Context c) {
        if (instance == null) instance = new HabitDb(c.getApplicationContext());
        return instance;
    }
    private HabitDb(Context c) { super(c, "starting_tomorrow.db", null, 2); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE habits(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, color INTEGER NOT NULL, created TEXT NOT NULL, weekdays TEXT NOT NULL DEFAULT '1234567')");
        db.execSQL("CREATE TABLE marks(habit_id INTEGER NOT NULL, day TEXT NOT NULL, state INTEGER NOT NULL, PRIMARY KEY(habit_id,day), FOREIGN KEY(habit_id) REFERENCES habits(id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE hidden_days(habit_id INTEGER NOT NULL, day TEXT NOT NULL, hidden INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(habit_id,day), FOREIGN KEY(habit_id) REFERENCES habits(id) ON DELETE CASCADE)");
        insertInitialHabit(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS hidden_days(habit_id INTEGER NOT NULL, day TEXT NOT NULL, hidden INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(habit_id,day), FOREIGN KEY(habit_id) REFERENCES habits(id) ON DELETE CASCADE)");
            db.execSQL("INSERT OR IGNORE INTO hidden_days(habit_id,day,hidden) SELECT habit_id,day,1 FROM marks WHERE state=2");
            db.execSQL("DELETE FROM marks WHERE state=2");
        }
    }
    private void insertInitialHabit(SQLiteDatabase db) {
        ContentValues v = new ContentValues(); v.put("name", "Зарядка"); v.put("color", 0xFF4F46E5);
        v.put("created", LocalDate.now().toString()); v.put("weekdays", "1234567"); db.insert("habits", null, v);
    }
    public List<Habit> all() {
        List<Habit> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM habits ORDER BY id", null)) {
            while (c.moveToNext()) { Habit h = new Habit(); h.id=c.getLong(0); h.name=c.getString(1); h.color=c.getInt(2); h.created=c.getString(3); h.weekdays=c.getString(4); out.add(h); }
        }
        return out;
    }
    public long save(Habit h) {
        ContentValues v = new ContentValues(); v.put("name", h.name); v.put("color", h.color);
        v.put("created", h.created == null ? LocalDate.now().toString() : h.created); v.put("weekdays", h.weekdays == null ? "1234567" : h.weekdays);
        if (h.id == 0) return getWritableDatabase().insert("habits", null, v);
        getWritableDatabase().update("habits", v, "id=?", new String[]{String.valueOf(h.id)}); return h.id;
    }
    public void delete(long id) { SQLiteDatabase db=getWritableDatabase(); db.delete("marks","habit_id=?",new String[]{String.valueOf(id)}); db.delete("hidden_days","habit_id=?",new String[]{String.valueOf(id)}); db.delete("habits","id=?",new String[]{String.valueOf(id)}); }

    public void toggleComplete(long id, LocalDate day) {
        SQLiteDatabase db=getWritableDatabase(); String date=day.toString();
        if (state(id,day)==1) db.delete("marks","habit_id=? AND day=?",new String[]{String.valueOf(id),date});
        else { ContentValues v=new ContentValues(); v.put("habit_id",id); v.put("day",date); v.put("state",1); db.insertWithOnConflict("marks",null,v,SQLiteDatabase.CONFLICT_REPLACE); }
        db.delete("hidden_days","habit_id=? AND day=?",new String[]{String.valueOf(id),date});
    }
    public void toggleHidden(long id, LocalDate day) {
        SQLiteDatabase db=getWritableDatabase(); String date=day.toString();
        if (isHidden(id,day)) db.delete("hidden_days","habit_id=? AND day=?",new String[]{String.valueOf(id),date});
        else { ContentValues v=new ContentValues();v.put("habit_id",id);v.put("day",date);v.put("hidden",1);db.insertWithOnConflict("hidden_days",null,v,SQLiteDatabase.CONFLICT_REPLACE); }
    }
    public int state(long id, LocalDate day) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT state FROM marks WHERE habit_id=? AND day=?",new String[]{String.valueOf(id),day.toString()})) { return c.moveToFirst()?c.getInt(0):0; }
    }
    public boolean isHidden(long id, LocalDate day) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM hidden_days WHERE habit_id=? AND day=?",new String[]{String.valueOf(id),day.toString()})) { return c.moveToFirst(); }
    }
    public LocalDate firstCompletedDate(long id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT MIN(day) FROM marks WHERE habit_id=? AND state=1",new String[]{String.valueOf(id)})) {
            if(c.moveToFirst() && c.getString(0)!=null) return LocalDate.parse(c.getString(0));
        } return null;
    }
    // Web-version rule: count calendar days from the first completed date; hidden days are removed from denominator.
    private boolean isScheduledDay(long id, LocalDate day) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT weekdays FROM habits WHERE id=?", new String[]{String.valueOf(id)})) {
            if (!c.moveToFirst()) return false;
            String weekdays = c.getString(0);
            return weekdays != null && weekdays.contains(String.valueOf(day.getDayOfWeek().getValue()));
        }
    }

    public int countEligible(long id, LocalDate from, LocalDate to) {
        LocalDate first=firstCompletedDate(id); if(first==null)return 0; if(from.isBefore(first))from=first;
        int n=0;
        for(LocalDate d=from;!d.isAfter(to);d=d.plusDays(1))
            if(isScheduledDay(id,d) && !isHidden(id,d)) n++;
        return n;
    }
    public int countState(long id, LocalDate from, LocalDate to, int wantedState) {
        LocalDate first=firstCompletedDate(id); if(first==null)return 0; if(from.isBefore(first))from=first;
        int n=0;
        for(LocalDate d=from;!d.isAfter(to);d=d.plusDays(1))
            if(isScheduledDay(id,d) && !isHidden(id,d) && state(id,d)==wantedState) n++;
        return n;
    }
    public int streak(long id) {
        LocalDate first=firstCompletedDate(id); if(first==null)return 0;
        LocalDate d=LocalDate.now(); int n=0;
        while(!d.isBefore(first)) {
            // Дни, в которые привычка не запланирована, не прерывают серию.
            if(!isScheduledDay(id,d) || isHidden(id,d)){d=d.minusDays(1);continue;}
            if(state(id,d)==1)n++;
            else if(d.equals(LocalDate.now())){d=d.minusDays(1);continue;}
            else break;
            d=d.minusDays(1);
        }
        return n;
    }
}
