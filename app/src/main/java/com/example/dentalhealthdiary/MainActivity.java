package com.example.dentalhealthdiary;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    public static final String CHANNEL_ID = "dental_health_channel";
    public static final String PREFS_NAME = "DentalHealthPrefs";

    private static final String KEY_DATE = "date";
    private static final String KEY_MORNING = "morning";
    private static final String KEY_EVENING = "evening";
    private static final String KEY_FLOSS = "floss";
    private static final String KEY_PAIN = "pain";
    private static final String KEY_NOTE = "note";
    private static final String KEY_HISTORY = "history";
    private static final String KEY_DENTIST = "dentist";

    private TextView todayStatusText;
    private TextView recommendationText;
    private TextView historyText;
    private TextView nextDentistText;
    private EditText painLevelEdit;
    private EditText noteEdit;
    private EditText dentistDateEdit;
    private EditText dentistTimeEdit;

    private SharedPreferences prefs;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        createNotificationChannel();
        requestNotificationPermission();
        initViews();
        resetDayIfNeeded();
        updateUi();
    }

    private void initViews() {
        todayStatusText = findViewById(R.id.todayStatusText);
        recommendationText = findViewById(R.id.recommendationText);
        historyText = findViewById(R.id.historyText);
        nextDentistText = findViewById(R.id.nextDentistText);
        painLevelEdit = findViewById(R.id.painLevelEdit);
        noteEdit = findViewById(R.id.noteEdit);
        dentistDateEdit = findViewById(R.id.dentistDateEdit);
        dentistTimeEdit = findViewById(R.id.dentistTimeEdit);

        Button morningButton = findViewById(R.id.morningButton);
        Button eveningButton = findViewById(R.id.eveningButton);
        Button flossButton = findViewById(R.id.flossButton);
        Button saveFeelingButton = findViewById(R.id.saveFeelingButton);
        Button saveDentistButton = findViewById(R.id.saveDentistButton);
        Button remindersButton = findViewById(R.id.remindersButton);
        Button resetTodayButton = findViewById(R.id.resetTodayButton);
        Button clearHistoryButton = findViewById(R.id.clearHistoryButton);

        morningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markDailyAction(KEY_MORNING, "Утренняя чистка зубов");
            }
        });

        eveningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markDailyAction(KEY_EVENING, "Вечерняя чистка зубов");
            }
        });

        flossButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markDailyAction(KEY_FLOSS, "Зубная нить / ирригатор");
            }
        });

        saveFeelingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFeeling();
            }
        });

        saveDentistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDentistVisit();
            }
        });

        remindersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scheduleDailyReminders();
            }
        });

        resetTodayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetToday();
            }
        });

        clearHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHistory();
            }
        });
    }

    private void markDailyAction(String key, String title) {
        prefs.edit().putBoolean(key, true).apply();
        addHistory(title, "Отметка выполнена");
        vibrate(90);
        Toast.makeText(this, "Сохранено: " + title, Toast.LENGTH_SHORT).show();
        updateUi();
    }

    private void saveFeeling() {
        String painText = painLevelEdit.getText().toString().trim();
        String note = noteEdit.getText().toString().trim();

        int pain = 0;
        if (!painText.isEmpty()) {
            try {
                pain = Integer.parseInt(painText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Введите боль числом от 0 до 10", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (pain < 0 || pain > 10) {
            Toast.makeText(this, "Боль должна быть от 0 до 10", Toast.LENGTH_SHORT).show();
            return;
        }

        prefs.edit()
                .putInt(KEY_PAIN, pain)
                .putString(KEY_NOTE, note)
                .apply();

        String recordNote = "Боль: " + pain + "/10";
        if (!note.isEmpty()) {
            recordNote += ". Заметка: " + note;
        }
        addHistory("Самочувствие", recordNote);
        vibrate(70);
        Toast.makeText(this, "Самочувствие сохранено", Toast.LENGTH_SHORT).show();
        updateUi();
    }

    private void saveDentistVisit() {
        String date = dentistDateEdit.getText().toString().trim();
        String time = dentistTimeEdit.getText().toString().trim();

        if (date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "Введите дату и время визита", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Date visitDate = fullFormat.parse(date + " " + time);
            if (visitDate == null) {
                Toast.makeText(this, "Неверный формат даты", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit().putString(KEY_DENTIST, date + " " + time).apply();
            addHistory("Визит к стоматологу", "Запланирован на " + displayFormat.format(visitDate));
            scheduleDentistReminder(visitDate.getTime());
            vibrate(100);
            Toast.makeText(this, "Визит сохранен", Toast.LENGTH_SHORT).show();
            updateUi();
        } catch (ParseException e) {
            Toast.makeText(this, "Формат: дата 2026-06-20, время 14:30", Toast.LENGTH_LONG).show();
        }
    }

    private void scheduleDailyReminders() {
        scheduleRepeatingReminder(8, 0, 101,
                ReminderReceiver.ACTION_BRUSH_MORNING,
                "Утренняя чистка зубов",
                "Пора почистить зубы утром и отметить это в дневнике.");

        scheduleRepeatingReminder(21, 0, 102,
                ReminderReceiver.ACTION_BRUSH_EVENING,
                "Вечерняя чистка зубов",
                "Пора почистить зубы перед сном и отметить это в дневнике.");

        addHistory("Напоминания", "Ежедневные уведомления включены: 08:00 и 21:00");
        Toast.makeText(this, "Ежедневные напоминания включены", Toast.LENGTH_LONG).show();
        updateUi();
    }

    private void scheduleRepeatingReminder(int hour, int minute, int requestCode,
                                           String action, String title, String message) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.setAction(action);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, title);
        intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent
        );
    }

    private void scheduleDentistReminder(long visitTimeMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager == null) return;

        long reminderTime = visitTimeMillis - 24L * 60L * 60L * 1000L;
        if (reminderTime <= System.currentTimeMillis()) {
            reminderTime = visitTimeMillis;
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_DENTIST);
        intent.putExtra(ReminderReceiver.EXTRA_TITLE, "Визит к стоматологу");
        intent.putExtra(ReminderReceiver.EXTRA_MESSAGE, "Напоминание о запланированном визите к стоматологу.");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                201,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.set(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
    }

    private void resetDayIfNeeded() {
        String today = dateFormat.format(new Date());
        String savedDate = prefs.getString(KEY_DATE, "");
        if (!today.equals(savedDate)) {
            prefs.edit()
                    .putString(KEY_DATE, today)
                    .putBoolean(KEY_MORNING, false)
                    .putBoolean(KEY_EVENING, false)
                    .putBoolean(KEY_FLOSS, false)
                    .putInt(KEY_PAIN, 0)
                    .putString(KEY_NOTE, "")
                    .apply();
        }
    }

    private void resetToday() {
        prefs.edit()
                .putString(KEY_DATE, dateFormat.format(new Date()))
                .putBoolean(KEY_MORNING, false)
                .putBoolean(KEY_EVENING, false)
                .putBoolean(KEY_FLOSS, false)
                .putInt(KEY_PAIN, 0)
                .putString(KEY_NOTE, "")
                .apply();
        addHistory("Сброс дня", "Отметки за сегодня очищены");
        Toast.makeText(this, "День сброшен", Toast.LENGTH_SHORT).show();
        updateUi();
    }

    private void clearHistory() {
        prefs.edit().putString(KEY_HISTORY, "[]").apply();
        Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
        updateUi();
    }

    private void updateUi() {
        resetDayIfNeeded();

        boolean morning = prefs.getBoolean(KEY_MORNING, false);
        boolean evening = prefs.getBoolean(KEY_EVENING, false);
        boolean floss = prefs.getBoolean(KEY_FLOSS, false);
        int pain = prefs.getInt(KEY_PAIN, 0);
        String note = prefs.getString(KEY_NOTE, "");

        int score = calculateScore(morning, evening, floss, pain);

        String status = "Сегодня: " + prefs.getString(KEY_DATE, dateFormat.format(new Date())) + "\n"
                + "Утренняя чистка: " + yesNo(morning) + "\n"
                + "Вечерняя чистка: " + yesNo(evening) + "\n"
                + "Нить / ирригатор: " + yesNo(floss) + "\n"
                + "Боль: " + pain + "/10\n"
                + "Индекс ухода: " + score + "%";
        todayStatusText.setText(status);

        painLevelEdit.setText(pain == 0 ? "" : String.valueOf(pain));
        noteEdit.setText(note);
        recommendationText.setText(makeRecommendation(morning, evening, floss, pain, note));
        nextDentistText.setText(getDentistText());
        historyText.setText(getHistoryText());
    }

    private int calculateScore(boolean morning, boolean evening, boolean floss, int pain) {
        int score = 0;
        if (morning) score += 35;
        if (evening) score += 35;
        if (floss) score += 20;
        if (pain == 0) score += 10;
        if (pain >= 5) score -= 15;
        if (score < 0) score = 0;
        if (score > 100) score = 100;
        return score;
    }

    private String makeRecommendation(boolean morning, boolean evening, boolean floss, int pain, String note) {
        StringBuilder builder = new StringBuilder();

        if (!morning && !evening) {
            builder.append("Сегодня еще нет отметок чистки. Для хорошей гигиены отметь чистку утром и вечером.\n");
        } else if (!morning) {
            builder.append("Не забудь утреннюю чистку зубов.\n");
        } else if (!evening) {
            builder.append("Не забудь вечернюю чистку зубов перед сном.\n");
        } else {
            builder.append("Отлично: две чистки за день отмечены.\n");
        }

        if (!floss) {
            builder.append("Добавь зубную нить или ирригатор для ухода между зубами.\n");
        } else {
            builder.append("Нить / ирригатор отмечены — это плюс к профилактике.\n");
        }

        if (pain >= 7) {
            builder.append("Высокая боль или чувствительность. Лучше не откладывать визит к стоматологу.");
        } else if (pain >= 4) {
            builder.append("Есть заметная чувствительность. Наблюдай динамику и запланируй консультацию при повторении.");
        } else if (pain > 0) {
            builder.append("Легкий дискомфорт записан. Следи, повторяется ли он в истории.");
        } else {
            builder.append("Боль не отмечена. Продолжай регулярный уход.");
        }

        if (!note.isEmpty()) {
            builder.append("\nПоследняя заметка: ").append(note);
        }

        return builder.toString();
    }

    private String getDentistText() {
        String dentist = prefs.getString(KEY_DENTIST, "");
        if (dentist.isEmpty()) {
            return "Визит к стоматологу не запланирован.";
        }
        try {
            Date date = fullFormat.parse(dentist);
            if (date != null) {
                long diff = date.getTime() - System.currentTimeMillis();
                long days = diff / (24L * 60L * 60L * 1000L);
                String status = diff >= 0 ? "Осталось примерно " + days + " дн." : "Дата уже прошла.";
                return "Ближайший визит: " + displayFormat.format(date) + "\n" + status;
            }
        } catch (ParseException ignored) {
        }
        return "Ближайший визит: " + dentist;
    }

    private void addHistory(String title, String text) {
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            JSONObject item = new JSONObject();
            item.put("time", new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date()));
            item.put("title", title);
            item.put("text", text);
            array.put(item);

            while (array.length() > 50) {
                JSONArray trimmed = new JSONArray();
                for (int i = 1; i < array.length(); i++) {
                    trimmed.put(array.getJSONObject(i));
                }
                array = trimmed;
            }

            prefs.edit().putString(KEY_HISTORY, array.toString()).apply();
        } catch (Exception e) {
            prefs.edit().putString(KEY_HISTORY, "[]").apply();
        }
    }

    private String getHistoryText() {
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_HISTORY, "[]"));
            if (array.length() == 0) {
                return "История пока пустая.";
            }

            StringBuilder builder = new StringBuilder();
            int start = Math.max(0, array.length() - 12);
            for (int i = array.length() - 1; i >= start; i--) {
                JSONObject item = array.getJSONObject(i);
                builder.append(item.optString("time"))
                        .append(" — ")
                        .append(item.optString("title"))
                        .append("\n")
                        .append(item.optString("text"))
                        .append("\n\n");
            }
            return builder.toString().trim();
        } catch (Exception e) {
            return "История повреждена или пуста.";
        }
    }

    private String yesNo(boolean value) {
        return value ? "да" : "нет";
    }

    private void vibrate(long millis) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(millis);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания о здоровье зубов",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Уведомления о чистке зубов и визите к стоматологу");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 55);
        }
    }
}
