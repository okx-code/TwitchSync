package me.okx.twitchsync;

import me.okx.twitchsync.data.Channel;
import me.okx.twitchsync.data.Options;
import me.okx.twitchsync.data.Upgrade;
import me.okx.twitchsync.events.PlayerListener;
import me.okx.twitchsync.util.SqlHelper;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Level;

public class TwitchSync extends JavaPlugin {
  private Permission perms = null;
  private Validator validator;
  private TwitchServer server;
  private SqlHelper sqlHelper;

  public Permission getPerms() {
    return this.perms;
  }

  @Override
  public void onLoad() {
    ConfigurationSerialization.registerClass(Options.class);
    ConfigurationSerialization.registerClass(Channel.class);
    ConfigurationSerialization.registerClass(Upgrade.class);
  }

  @Override
  public void onEnable() {
    debug("Logging works.", "Testing");
    getConfig().options().copyDefaults(true);
    saveDefaultConfig();
    initValidator();
    startServer();
    setupPermissions();
    sqlHelper = new SqlHelper(this);

    getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    getCommand("twitchsync").setExecutor(new TwitchSyncCommand(this));
    getCommand("revoke").setExecutor(new RevokeCommand(this));

    long time = 20*60*getConfig().getInt("revoke-interval-minutes");
    new Revoker(this).runTaskTimerAsynchronously(this, time, time);

    new Metrics(this);
  }

  @Override
  public void onDisable() {
    server.stop();
    sqlHelper.close();
  }

  private void setupPermissions() {
    if(!getServer().getPluginManager().isPluginEnabled("Vault")) {
      return;
    }

    RegisteredServiceProvider<Permission> rsp =
        getServer().getServicesManager().getRegistration(Permission.class);
    perms = rsp.getProvider();
    if(!perms.hasGroupSupport()) {
      perms = null;
    }
  }

  private void startServer() {
    try {
      server = new TwitchServer(this);
      server.start();
    } catch (IOException e) {
      getLogger().log(Level.SEVERE, "Error starting web server");
      e.printStackTrace();
    }
  }

  public SqlHelper getSqlHelper() {
    return sqlHelper;
  }

  private void initValidator() {
    this.validator = new Validator(this);
  }

  public Validator getValidator() {
    return validator;
  }

  public <T> T debug(T message) {
    if (getConfig().getBoolean("debug-mode")) {
      getLogger().log(Level.INFO, String.valueOf(message));
    }
    return message;
  }

  public <T> T debug(T message, String label) {
    if (getConfig().getBoolean("debug-mode")) {
      getLogger().log(Level.INFO, label + ": " + message);
    }
    return message;
  }

  public void debug(Throwable throwable) {
    if (getConfig().getBoolean("debug-mode")) {
      throwable.printStackTrace();
    }
  }
}
