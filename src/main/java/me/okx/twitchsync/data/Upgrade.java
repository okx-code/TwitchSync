package me.okx.twitchsync.data;

public class Upgrade {

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
}
