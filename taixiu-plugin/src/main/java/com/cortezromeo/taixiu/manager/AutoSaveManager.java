package com.cortezromeo.taixiu.manager;

import com.cortezromeo.taixiu.TaiXiu;
import com.cortezromeo.taixiu.task.AutoSaveTask;

public class AutoSaveManager {

    private static AutoSaveTask autoSaveTask;
    private final static TaiXiu plugin = TaiXiu.getPlugin();

    public static void startAutoSave(int time) {

        if (plugin.getConfig().getBoolean("database.auto-save.enable") && autoSaveStatus && autoSaveTask != null)
            return;

        autoSaveTask = new AutoSaveTask(time);
        autoSaveStatus = true;

    }

    public static void stopAutoSave() {

        if (!autoSaveStatus && autoSaveTask == null)
            return;

        autoSaveTask.cancel();
        autoSaveStatus = false;

    }

    public static void reloadTimeAutoSave() {

        if (!getAutoSaveStatus())
            return;

        stopAutoSave();
        startAutoSave(plugin.getConfig().getInt("database.auto-save.time"));

    }

    public static boolean autoSaveStatus = false;

    public static boolean getAutoSaveStatus() {
        return autoSaveStatus;
    }

    public static void setAutoSaveStatus(boolean b) {
        autoSaveStatus = b;
    }

}
