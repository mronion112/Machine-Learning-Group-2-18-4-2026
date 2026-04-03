package org.example.youtubescrapping;

import java.util.ArrayList;

public class Channel {

//    x1      Channel Follower Count
//    x2      Epoch
//    x3      Total Videos
//    x8      Avg view 10 videos of channel
//    x9      Avg view 10 videos of channel
//    x10      Avg like 10 videos of channel
//    x11     Avg comment 10 videos of channel
//    x12     Avg Duration 10 videos of Channel
//    x13     Frequency
//    x14     isChannelVerify



    String channel;
    String epoch;
    String channel_follower_count;
    String playlist_count;
    String avgLike10Videos;
    String avgView10Videos;
    String avgComment10Videos;
    String avgDuration10Videos;
    String freequency;
    String isChannelVerify;
    ArrayList<Video> videos;


    public Channel(String channel, String epoch, String channel_follower_count, String playlist_count, String avgView10Videos, String avgLike10Videos, String avgComment10Videos, String avgDuration10Videos, String freequency, String isChannelVerify, ArrayList<Video> videos) {
        this.epoch = epoch;
        this.channel = channel;
        this.channel_follower_count = channel_follower_count;
        this.playlist_count = playlist_count;
        this.avgView10Videos = avgView10Videos;
        this.avgLike10Videos = avgLike10Videos;
        this.avgComment10Videos = avgComment10Videos;
        this.avgDuration10Videos = avgDuration10Videos;
        this.freequency = freequency;
        this.isChannelVerify = isChannelVerify;
        this.videos = videos;
    }

    public String getchannel() {
        return channel;
    }

    public void setchannel(String channel) {
        this.channel = channel;
    }

    public String getEpoch() {
        return epoch;
    }

    public void setEpoch(String epoch) {
        this.epoch = epoch;
    }

    public String getChannel_follower_count() {
        return channel_follower_count;
    }

    public void setChannel_follower_count(String channel_follower_count) {
        this.channel_follower_count = channel_follower_count;
    }

    public String getPlaylist_count() {
        return playlist_count;
    }

    public void setPlaylist_count(String playlist_count) {
        this.playlist_count = playlist_count;
    }

    public String getAvgLike10Videos() {
        return avgLike10Videos;
    }

    public void setAvgLike10Videos(String avgLike10Videos) {
        this.avgLike10Videos = avgLike10Videos;
    }

    public String getAvgView10Videos() {
        return avgView10Videos;
    }

    public void setAvgView10Videos(String avgView10Videos) {
        this.avgView10Videos = avgView10Videos;
    }

    public String getAvgComment10Videos() {
        return avgComment10Videos;
    }

    public void setAvgComment10Videos(String avgComment10Videos) {
        this.avgComment10Videos = avgComment10Videos;
    }

    public String getAvgDuration10Videos() {
        return avgDuration10Videos;
    }

    public void setAvgDuration10Videos(String avgDuration10Videos) {
        this.avgDuration10Videos = avgDuration10Videos;
    }

    public String getFreequency() {
        return freequency;
    }

    public void setFreequency(String freequency) {
        this.freequency = freequency;
    }

    public ArrayList<Video> getVideos() {
        return videos;
    }

    public void setVideos(ArrayList<Video> videos) {
        this.videos = videos;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getIsChannelVerify() {
        return isChannelVerify;
    }

    public void setIsChannelVerify(String isChannelVerify) {
        this.isChannelVerify = isChannelVerify;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "channel='" + channel + '\'' +
                ", epoch='" + epoch + '\'' +
                ", channel_follower_count='" + channel_follower_count + '\'' +
                ", playlist_count='" + playlist_count + '\'' +
                ", avgLike10Videos='" + avgLike10Videos + '\'' +
                ", avgView10Videos='" + avgView10Videos + '\'' +
                ", avgComment10Videos='" + avgComment10Videos + '\'' +
                ", avgDuration10Videos='" + avgDuration10Videos + '\'' +
                ", freequency='" + freequency + '\'' +
                ", isChannelVerify='" + isChannelVerify + '\'' +
                ", videos=" + videos +
                '}';
    }
}
