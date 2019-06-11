package me.okx.twitchsync.data;

public class Channel {

    String  name;
    Options subscribeOptions;
    Options followOptions;
    String rank;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Options getSubscribeOptions() {
        return subscribeOptions;
    }

    public void setSubscribeOptions(Options subscribeOptions) {
        this.subscribeOptions = subscribeOptions;
    }

    public Options getFollowOptions() {
        return followOptions;
    }

    public void setFollowOptions(Options followOptions) {
        this.followOptions = followOptions;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }
}
