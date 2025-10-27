package com.example.btl_mobileapp.utils;

import android.text.TextUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    /**
     * Tính tuổi từ ngày sinh (String)
     */
    public static int calculateAge(String dateOfBirth) {
        if (TextUtils.isEmpty(dateOfBirth)) {
            return 0;
        }

        try {
            SimpleDateFormat sdf;
            if (dateOfBirth.contains("/")) {
                sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }

            Date birthDate = sdf.parse(dateOfBirth);
            if (birthDate == null) return 0;

            Calendar birthCalendar = Calendar.getInstance();
            birthCalendar.setTime(birthDate);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (ParseException e) {
            return 0;
        }
    }

    /**
     * Lấy cung hoàng đạo từ ngày sinh (String)
     */
    public static String getZodiacSign(String dateOfBirth) {
        if (TextUtils.isEmpty(dateOfBirth)) {
            return "";
        }

        try {
            SimpleDateFormat sdf;
            if (dateOfBirth.contains("/")) {
                sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }

            Date birthDate = sdf.parse(dateOfBirth);
            if (birthDate == null) return "";

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(birthDate);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH) + 1;

            return getZodiacSignFromDayMonth(day, month);
        } catch (ParseException e) {
            return "";
        }
    }

    /**
     * Lấy cung hoàng đạo từ ngày và tháng
     */
    private static String getZodiacSignFromDayMonth(int day, int month) {
        if ((month == 3 && day >= 21) || (month == 4 && day <= 19)) return "Bạch Dương";
        if ((month == 4 && day >= 20) || (month == 5 && day <= 20)) return "Kim Ngưu";
        if ((month == 5 && day >= 21) || (month == 6 && day <= 20)) return "Song Tử";
        if ((month == 6 && day >= 21) || (month == 7 && day <= 22)) return "Cự Giải";
        if ((month == 7 && day >= 23) || (month == 8 && day <= 22)) return "Sư Tử";
        if ((month == 8 && day >= 23) || (month == 9 && day <= 22)) return "Xử Nữ";
        if ((month == 9 && day >= 23) || (month == 10 && day <= 22)) return "Thiên Bình";
        if ((month == 10 && day >= 23) || (month == 11 && day <= 21)) return "Bọ Cạp";
        if ((month == 11 && day >= 22) || (month == 12 && day <= 21)) return "Nhân Mã";
        if ((month == 12 && day >= 22) || (month == 1 && day <= 19)) return "Ma Kết";
        if ((month == 1 && day >= 20) || (month == 2 && day <= 18)) return "Bảo Bình";
        if ((month == 2 && day >= 19) || (month == 3 && day <= 20)) return "Song Ngư";

        return "";
    }

    /**
     * Format date string to dd/MM/yyyy
     */
    public static String formatDate(String dateString) {
        if (TextUtils.isEmpty(dateString)) {
            return "";
        }

        try {
            SimpleDateFormat inputFormat;
            if (dateString.contains("/")) {
                inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            } else {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }

            Date date = inputFormat.parse(dateString);
            if (date == null) return dateString;

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateString;
        }
    }

    /**
     * Convert String date to timestamp
     */
    public static long dateStringToTimestamp(String dateString) {
        if (TextUtils.isEmpty(dateString)) {
            return 0;
        }

        try {
            SimpleDateFormat sdf;
            if (dateString.contains("/")) {
                sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }

            Date date = sdf.parse(dateString);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            return 0;
        }
    }
}