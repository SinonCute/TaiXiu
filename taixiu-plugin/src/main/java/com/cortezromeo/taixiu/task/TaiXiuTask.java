package com.cortezromeo.taixiu.task;

import com.cortezromeo.taixiu.TaiXiu;
import com.cortezromeo.taixiu.api.TaiXiuResult;
import com.cortezromeo.taixiu.api.TaiXiuState;
import com.cortezromeo.taixiu.api.event.SessionSwapEvent;
import com.cortezromeo.taixiu.api.storage.ISession;
import com.cortezromeo.taixiu.file.MessageFile;
import com.cortezromeo.taixiu.manager.DatabaseManager;
import com.cortezromeo.taixiu.manager.TaiXiuManager;
import com.cortezromeo.taixiu.storage.loadingtype.SessionEndingType;
import com.cortezromeo.taixiu.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import static com.cortezromeo.taixiu.manager.DebugManager.debug;

public class TaiXiuTask implements Runnable {
    private BukkitTask task;
    private int time;
    private TaiXiuState state;
    private ISession data;
    private final TaiXiu plugin = TaiXiu.getPlugin();
    private final TaiXiuManager manager = TaiXiu.getTaiXiuManager();
    private final DatabaseManager databaseManager = TaiXiu.getDatabaseManager();

    public TaiXiuTask(int time) {
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 20L);
        data = databaseManager.getSessionData(databaseManager.getLastSessionFromFile());
        this.time = time;
        this.state = TaiXiuState.PLAYING;

        debug("TAIXIU TASK", "RUNNING TASK ID: " + getTaskID() + " | SESSION NUMBER: " + data.getSession());
    }

    public BukkitTask getBukkitTask() {
        return task;
    }

    public int getTaskID() {
        return task.getTaskId();
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public TaiXiuState getState() {
        return state;
    }

    public void setState(TaiXiuState state) {
        this.state = state;
    }

    public ISession getSession() {
        return data;
    }

    public void setSession(long session) {
        this.data = databaseManager.getSessionData(session);
    }

    public void setSession(ISession session) {
        this.data = session;
    }

    @Override
    public void run() {
        if (state == TaiXiuState.PLAYING) {
            try {
                time--;

                if (getSession().getResult() != TaiXiuResult.NONE)
                    time = 0;

                if (time == 0) {
                    time = plugin.getConfig().getInt("task.taiXiuTask.time-per-session");

                    if (getSession().getTaiPlayers().isEmpty() && getSession().getXiuPlayers().isEmpty()) {
                        MessageUtil.sendBoardCast(MessageFile.get().getString("session-result-not-enough-player").replace("%session%", String.valueOf(getSession().getSession())));
                    } else {
                        manager.resultSeason(getSession(), 0, 0, 0);

                        ISession oldSessionData = getSession();
                        long newSession = databaseManager.getLastSession() + 1;
                        setSession(newSession);

                        SessionSwapEvent event = new SessionSwapEvent(oldSessionData, getSession());

                        debug("SESSION SWAPPED", "Old session: " + oldSessionData.getSession() + " " +
                                "| New session: " + newSession);

                        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getServer().getPluginManager().callEvent(event));

                        if (DatabaseManager.sessionEndingType == SessionEndingType.SAVE) {
                            databaseManager.saveSessionData(oldSessionData.getSession());
                        } else {
                            databaseManager.unloadSessionData(oldSessionData.getSession());
                        }
                    }
                }
            } catch (Exception e) {
                cancel();
                MessageUtil.thowErrorMessage("" + e);
                setSession(databaseManager.getLastSession() + 1);
                manager.startTask(plugin.getConfig().getInt("task.taiXiuTask.time-per-session"));
            }
        }
    }

    public void cancel() {
        task.cancel();
    }

}
