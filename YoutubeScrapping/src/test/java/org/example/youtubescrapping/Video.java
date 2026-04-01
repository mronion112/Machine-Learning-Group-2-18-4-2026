package org.example.youtubescrapping;

public class Video {
    private String view_count;
    private String comment_count;
    private String like_count;

    public Video() {
    }

    public Video(String view_count, String comment_count, String like_count) {
        this.view_count = view_count;
        this.comment_count = comment_count;
        this.like_count = like_count;
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

    @Override
    public String toString() {
        return "Video{" +
                "view_count='" + view_count + '\'' +
                ", comment_count='" + comment_count + '\'' +
                ", like_count='" + like_count + '\'' +
                '}';
    }
}
