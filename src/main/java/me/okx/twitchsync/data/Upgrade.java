package me.okx.twitchsync.data;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("upgrade")
public class Upgrade implements ConfigurationSerializable {

  private Integer threshold;
  private String rank;

  public Upgrade(Integer threshold, String rank) {
    this.threshold = threshold;
    this.rank = rank;
  }

  public Integer getThreshold() {
    return threshold;
  }

  public void setThreshold(Integer threshold) {
    this.threshold = threshold;
  }

  public String getRank() {
    return rank;
  }

  public void setRank(String rank) {
    this.rank = rank;
  }

  @Override
  public Map<String, Object> serialize() {
    Map<String, Object> serialised = new HashMap<>();
    serialised.put("rank", this.rank);
    serialised.put("threshold", this.threshold);
    return serialised;
  }

  public static Upgrade deserialize(Map<String, Object> map) {
    return new Upgrade((Integer)map.get("threshold"), (String)map.get("rank"));
  }
}
