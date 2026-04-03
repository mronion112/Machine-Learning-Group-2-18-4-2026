package org.example.youtubescrapping;


//    x4      Avg view
//    x5      Avg Comment
//    x6      Avg like
//    x7      Avg Duration

//    x12     timestamp

public class Video {
    private String title;
    private String view_count;
    private String comment_count;
    private String like_count;
    private String duration;
    private String timestamp;

    public Video(String title, String view_count, String comment_count, String like_count, String duration, String timestamp) {
        this.title = title;
        this.view_count = view_count;
        this.comment_count = comment_count;
        this.like_count = like_count;
        this.duration = duration;
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getView_count() {
        return view_count;
    }

    public void setView_count(String view_count) {
        this.view_count = view_count;
    }

    public String getComment_count() {
        return comment_count;
    }

    public void setComment_count(String comment_count) {
        this.comment_count = comment_count;
    }

    public String getLike_count() {
        return like_count;
    }

    public void setLike_count(String like_count) {
        this.like_count = like_count;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String gettimestamp() {
        return timestamp;
    }

    public void settimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Video{" +
                "title='" + title + '\'' +
                ", view_count='" + view_count + '\'' +
                ", comment_count='" + comment_count + '\'' +
                ", like_count='" + like_count + '\'' +
                ", duration='" + duration + '\'' +
                ", timestamp='" +  + '\'' +
                '}';
    }
}
