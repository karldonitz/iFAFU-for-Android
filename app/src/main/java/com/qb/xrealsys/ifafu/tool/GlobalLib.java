package com.qb.xrealsys.ifafu.tool;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by sky on 11/02/2018.
 */

public class GlobalLib {

    public static String[] dateUnit = new String[] {"年", "月", "天", "小时", "分钟", "秒"};

    public static float GetRawSize(Context context, int unit, float value) {
        Resources res = context.getResources();
        return TypedValue.applyDimension(unit, value, res.getDisplayMetrics());
    }

    public static boolean CompareUtfWithGbk(String utf8, String gbk) throws IOException {
        return URLEncoder.encode(gbk, "gbk").contains(URLEncoder.encode(utf8, "gbk"));
    }

    public static BitmapDrawable BitmapToDrawable(Context context, Bitmap bitmap) {
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public static String[] CompareWithNowTime(List<Integer> compareTime) {
        String[] answer = new String[] {"-", "0", "分钟"};

        DateFormat    dateFormat = new SimpleDateFormat(
                "yyyy.MM.dd.HH.mm", Locale.getDefault());
        String        dateString = dateFormat.format(new Date(System.currentTimeMillis()));
        String[]      systemTime = dateString.split("\\.");
        for (int i = 0; i < compareTime.size(); i++) {
            int compare = compareTime.get(i);
            int system  = Integer.parseInt(systemTime[i]);
            if (compare < system) {
                answer[0] = "-";
                answer[1] = String.valueOf(system - compare);
                answer[2] = dateUnit[i];
                break;
            } else if (compare > system) {
                answer[0] = "+";
                answer[1] = String.valueOf(compare - system);
                answer[2] = dateUnit[i];
                break;
            }
        }

        return answer;
    }

    public static String GetLocalVersionName(Context ctx) {
        String localVersion = "";
        try {
            PackageInfo packageInfo = ctx.getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0);
            localVersion = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return localVersion;
    }

    public static void PutTextToClipboard(Context ctx, String label, String text) {
        ClipboardManager clipboardManager =
                (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(label, text);
        clipboardManager.setPrimaryClip(clipData);
    }
}
