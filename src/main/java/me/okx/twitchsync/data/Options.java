package me.okx.twitchsync.data;

import java.util.List;

public class Options {

    Boolean enabled;
    String rank;
    List<String> commands;
    List<String> revokeCommands;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public List<String> getRevokeCommands() {
        return revokeCommands;
    }

    public void setRevokeCommands(List<String> revokeCommands) {
        this.revokeCommands = revokeCommands;
    }
}
