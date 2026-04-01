package org.example.youtubescrapping;

import java.util.ArrayList;

public class Channel {


    String uploader;
    String epoch;
    String channel_follower_count;
    String playlist_count;
    ArrayList<Video> videos;

    public Channel(String uploader, String epoch, String channel_follower_count, String playlist_count, ArrayList<Video> listVideos) {
        this.uploader = uploader;
        this.epoch = epoch;
        this.channel_follower_count = channel_follower_count;
        this.videos = videos;
        this.playlist_count = playlist_count;
        this.videos = listVideos;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
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

    public ArrayList<Video> getVideos() {
        return videos;
    }

    public void setVideos(ArrayList<Video> videos) {
        this.videos = videos;
    }

    @Override
    public String toString() {
        return "Channel{" +
                "uploader='" + uploader + '\'' +
                ", epoch='" + epoch + '\'' +
                ", channel_follower_count='" + channel_follower_count + '\'' +
                ", playlist_count='" + playlist_count + '\'' +
                ", videos=" + videos +
                '}';
    }
}
