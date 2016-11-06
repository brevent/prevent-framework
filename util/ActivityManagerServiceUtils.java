package me.piebridge.prevent.framework.util;

import android.content.Context;
import android.os.Build;
import android.os.ServiceManager;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.piebridge.prevent.framework.PreventLog;

/**
 * Created by thom on 2016/10/18.
 */
public class ActivityManagerServiceUtils {

    /**
     * ID of stack where fullscreen activities are normally launched into.
     */
    private static final int FULLSCREEN_WORKSPACE_STACK_ID = 1;

    /**
     * ID of stack where freeform/resized activities are normally launched into.
     */
    public static final int FREEFORM_WORKSPACE_STACK_ID = FULLSCREEN_WORKSPACE_STACK_ID + 1;

    /**
     * ID of stack that occupies a dedicated region of the screen.
     */
    public static final int DOCKED_STACK_ID = FREEFORM_WORKSPACE_STACK_ID + 1;

    private ActivityManagerServiceUtils() {

    }

    public static Set<String> getCurrentPackages() {
        Object mAm = ServiceManager.getService(Context.ACTIVITY_SERVICE);
        if (mAm == null) {
            return Collections.emptySet();
        }
        Set<String> packageNames = new LinkedHashSet<String>();
        Object focusedStack = getFocusedStack(mAm);
        addPackage(packageNames, focusedStack, "focused", false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Object mStackSupervisor = ReflectUtils.get(mAm, "mStackSupervisor");
            Object fullscreenStack = getStack(mStackSupervisor, FULLSCREEN_WORKSPACE_STACK_ID);
            if (fullscreenStack != focusedStack) {
                addPackage(packageNames, fullscreenStack, "fullscreen workspace", true);
            }
            Object dockedStack = getStack(mStackSupervisor, DOCKED_STACK_ID);
            if (dockedStack != focusedStack) {
                addPackage(packageNames, dockedStack, "docked", true);
            }
        }
        PreventLog.d("current packages: " + packageNames);
        return Collections.unmodifiableSet(packageNames);
    }

    private static Object getStack(Object stackSupervisor, int stackId) {
        return ReflectUtils.invoke(stackSupervisor, "getStack", new Class<?>[]{int.class}, new Object[]{stackId});
    }

    private static Object getFocusedStack(Object mAm) {
        return ReflectUtils.invoke(mAm, "getFocusedStack");
    }

    private static void addPackage(Collection<String> packageNames, Object stack, String message, boolean checkVisible) {
        if (stack == null || (checkVisible && !isVisible(stack, message))) {
            return;
        }
        List mTaskHistory = (List) ReflectUtils.get(stack, "mTaskHistory");
        if (mTaskHistory != null) {
            for (Object taskRecord : mTaskHistory) {
                List mActivities = (List) ReflectUtils.get(taskRecord, "mActivities");
                if (mActivities != null) {
                    for (Object mActivity : mActivities) {
                        String packageName = ActivityRecordUtils.getPackageName(mActivity);
                        PreventLog.d("activityRecord: " + mActivity + ", packageName: " + packageName + ", message: " + message);
                        if (packageName != null) {
                            packageNames.add(packageName);
                        }
                    }
                }
            }
        }
    }

    private static Boolean isVisible(Object stack, String message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Integer visibility = (Integer) ReflectUtils.invoke(stack, "getStackVisibilityLocked",
                    new Class[]{null}, new Object[]{null});
            PreventLog.d("stack: " + stack + ", visibility: " + visibility + ", message: " + message);
            return visibility != null && visibility != 0;
        } else {
            return true;
        }
    }

}
