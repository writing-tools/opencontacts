package fontscaling;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import static android.content.Context.WINDOW_SERVICE;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.getTextSizeScaling;
import static opencontacts.open.com.opencontacts.utils.SharedPreferencesUtils.saveTextSizeScaling;

public class FontScalingUtil {
    public static boolean fontScalingSet = false; //don't tinker the value form outside. This will be handled for you by Utility. Unless you want to tinker around

    public static float getSystemScaledDensity(){
        return Resources.getSystem().getDisplayMetrics().scaledDensity;
    }

    public static void setCustomFontSizeOnViewCreated(Activity activity) {
        if(fontScalingSet) return;
        Configuration configuration = activity.getResources().getConfiguration();
        configuration.fontScale = configuration.fontScale * getTextSizeScaling(activity);
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) activity.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = metrics.density * configuration.fontScale;
        activity.getBaseContext().getResources().updateConfiguration(configuration, metrics);
        fontScalingSet = true;
    }

    public static void applyNewFontScaling(float newFontScale, Activity activity) {
        Configuration configuration = activity.getResources().getConfiguration();
        float oldScaling = getTextSizeScaling(activity);
        configuration.fontScale = (configuration.fontScale / oldScaling) * newFontScale;
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) activity.getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = (metrics.density / oldScaling) * newFontScale;
        activity.getBaseContext().getResources().updateConfiguration(configuration, metrics);
        saveTextSizeScaling(newFontScale, activity);
        activity.recreate();
    }

}
