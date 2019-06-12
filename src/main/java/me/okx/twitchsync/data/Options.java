package me.okx.twitchsync.data;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SerializableAs("options")
public class Options implements ConfigurationSerializable {

    private Boolean enabled;
    private String rank;
    private List<String> commands;
    private List<String> revokeCommands;

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

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialised = new HashMap<>();
        serialised.put("enabled", this.enabled);
        serialised.put("rank", this.rank);
        serialised.put("commands", this.commands);
        serialised.put("revoke-commands", this.revokeCommands);
        return serialised;
    }

    public static Options deserialize(Map<String, Object> map) {
        Options options = new Options();
        options.setEnabled((Boolean)map.get("enabled"));
        options.setRank((String)map.get("rank"));
        options.setCommands((List<String>)map.get("commands"));
        options.setRevokeCommands((List<String>)map.get("revoke-commands"));
        return options;
    }
}
