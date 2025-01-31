package com.cortezromeo.taixiu.manager;

import com.cortezromeo.taixiu.TaiXiu;
import com.cortezromeo.taixiu.api.storage.ISession;
import com.cortezromeo.taixiu.storage.SessionDataStorage;
import com.cortezromeo.taixiu.storage.loadingtype.PluginDisablingType;
import com.cortezromeo.taixiu.storage.loadingtype.SessionEndingType;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.*;

import static com.cortezromeo.taixiu.manager.DebugManager.debug;
import static com.cortezromeo.taixiu.util.MessageUtil.log;
import static com.cortezromeo.taixiu.util.MessageUtil.thowErrorMessage;

public class DatabaseManager {
    public static List<String> togglePlayers = new ArrayList<>();
    public static Map<Long, ISession> taiXiuData = new TreeMap<>();
    public static long lastSession;

    public static PluginDisablingType pluginDisablingType;
    public static SessionEndingType sessionEndingType;

    private static DatabaseManager instance;

    private static TaiXiu plugin;

    public DatabaseManager(TaiXiu taiXiu) {
        plugin = taiXiu;
        instance = this;
    }

    public ISession getSessionData(long session) {
        if (!taiXiuData.containsKey(session)) {
            loadSessionData(session);
        }
        return taiXiuData.get(session);
    }

    public long getLastSession() {
        if (taiXiuData.isEmpty())
            lastSession = 0;
        else {
            lastSession = Collections.max(taiXiuData.keySet());
        }

        return lastSession;
    }

    public long getLastSessionFromFile() {

        File sessionFolder = new File(plugin.getDataFolder() + "/session");
        File[] listOfFilesSession = sessionFolder.listFiles();

        if (listOfFilesSession.length == 0) {
            return 0;
        }

        List<Long> sessions = new ArrayList<>();

        for (int i = 0; i < listOfFilesSession.length; i++) {
            if (listOfFilesSession[i].isFile()) {

                File sessionFile = listOfFilesSession[i];
                String session = FilenameUtils.removeExtension(sessionFile.getName());

                try {
                    Long.parseLong(session);
                } catch (Exception e) {
                    continue;
                }

                sessions.add(Long.valueOf(session));
            }
        }

        return Collections.max(sessions);
    }

    public void loadSessionData(long session) {

        debug("LOADING SESSION DATA", "Session number " + session);
        if (taiXiuData.containsKey(session))
            return;

        taiXiuData.put(session, SessionDataStorage.getSessionData(session));
        debug("SESSION DATA LOADED", "Session number " + session);
    }

    public void saveSessionData(long session) {

        debug("SAVING SESSION DATA", "Session number " + session);
        SessionDataStorage.saveTaiXiuData(session, taiXiuData.get(session));
        debug("SESSION DATA SAVED", "Session number " + session);
    }

    public void unloadSessionData(long session) {

        debug("UNLOADING SESSION DATA", "Session number " + session);
        SessionDataStorage.saveTaiXiuData(session, taiXiuData.get(session));
        taiXiuData.remove(session);
        debug("SESSION DATA UNLOADED", "Session number " + session);
    }

    public boolean checkExistsFileData(long session) {

        if (taiXiuData.containsKey(session))
            return true;

        File file = new File(plugin.getDataFolder() + "/session/" + session + ".yml");
        if (file.exists()) {
            try {
                return true;
            } catch (Exception e) {
                thowErrorMessage("" + e);
                return false;
            }
        }
        return false;
    }

    public void loadLoadingType() {

        FileConfiguration config = plugin.getConfig();

        sessionEndingType = SessionEndingType.valueOf(config.getString("database.while-ending-session.type").toUpperCase());
        pluginDisablingType = PluginDisablingType.valueOf(config.getString("database.while-disabling-plugin.type").toUpperCase());

    }

    public void saveDatabase() {

        File sessionFolder = new File(plugin.getDataFolder() + "/session");
        File[] listOfFilesSession = sessionFolder.listFiles();
        if (listOfFilesSession == null)
            return;

        if (DatabaseManager.pluginDisablingType == PluginDisablingType.SAVE_ALL) {
            Set<Long> sessionData = taiXiuData.keySet();
            log("&e[TAI XIU] Tiến hành save " + sessionData.size() + " dữ liệu...");
            for (long session : sessionData)
                saveSessionData(session);
            log("&e[TAI XIU] Save " + sessionData.size() + " dữ liệu thành công!");
        } else if (DatabaseManager.pluginDisablingType == PluginDisablingType.SAVE_LATEST) {
            long latestSession = getLastSession();

            log("&e[TAI XIU] Tiến hành save dữ liệu cuối cùng (Số " + latestSession + ")");
            saveSessionData(latestSession);
            log("&e[TAI XIU] Save dữ liệu số " + latestSession + " thành công!");
        } else {
            log("&e[TAI XIU] Tiến hành save dữ liệu số " + getLastSession() + " và xóa tất cả dữ liệu cũ...");

            int totalFiles = 0;
            for (File file : listOfFilesSession) {
                if (file.isFile()) {
                    totalFiles++;

                    long session = Long.parseLong(FilenameUtils.removeExtension(file.getName()));
                    if (session == getLastSessionFromFile()) {
                        totalFiles--;

                        if (taiXiuData.containsKey(session))
                            saveSessionData(session);
                        continue;
                    }

                    if (file.delete()) {
                        // stuffs
                    } else {
                        thowErrorMessage("KHÔNG THỂ XÓA FILE " + file.getName());
                    }
                }
            }
            log("&e[TAI XIU] Save thành công dữ liệu số " + getLastSession() + " và xóa " + totalFiles + " dữ liệu!");
        }
    }

    public static DatabaseManager getInstance() {
        return instance;
    }
}
