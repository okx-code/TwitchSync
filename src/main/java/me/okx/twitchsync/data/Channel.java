package me.okx.twitchsync.data;

import com.google.common.eventbus.Subscribe;
import org.bukkit.ChatColor;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("channel")
public class Channel implements ConfigurationSerializable {

    private String  name;
    private Options subscribe;
    private Options follow;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Options getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Options subscribe) {
        this.subscribe = subscribe;
    }

    public Options getFollow() {
        return follow;
    }

    public void setFollow(Options follow) {
        this.follow = follow;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialised = new HashMap<>();
        serialised.put("name", this.name);
        serialised.put("subscribe", this.subscribe);
        serialised.put("follow", this.follow);
        return serialised;
    }

    public static Channel deserialize(Map<String, Object> map) {
        Channel channel = new Channel();
        channel.setName((String)map.get("name"));
        channel.setSubscribe((Options)map.get("subscribe"));
        channel.setFollow((Options)map.get("follow"));
        return channel;
    }
}
