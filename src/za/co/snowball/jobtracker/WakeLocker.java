/*
 * TODO Difficulty replacing deprecated methods and problem with wakeLock.release never reached
 *  The solution for fixing the deprecated method is to replace with FLAG_KEEP_SCREEN_ON and rather using WindowManager
 *  e.g. getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
 */

package za.co.snowball.jobtracker;

import android.content.Context;
import android.os.PowerManager;

public abstract class WakeLocker {
    private static PowerManager.WakeLock wakeLock;

    public static void acquire(Context context) {
        if (wakeLock != null) wakeLock.release();
 
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE, "WakeLock");
        wakeLock.acquire();
    }
 
    public static void release() {
        if (wakeLock != null) wakeLock.release(); wakeLock = null;
    }
}

