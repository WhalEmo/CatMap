package com.beem.catmap.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ReplyModel {
    private String replyId;
    private String commentId;
    private String name;
    private String replyContent;
    private Date date;
    private String replyUserId;
    private int likeCountReply = 0;
    private boolean isLiked = false;
    private boolean isSending = false;
    private boolean localOnly = false;

    public ReplyModel() {
        this.replyId = "";
    }

    public ReplyModel(String replyId, String name, String replyContent, Date date, String replyUserId, int likeCountReply, boolean isSending) {
        this.replyId = replyId != null ? replyId : "";
        this.name = name;
        this.replyContent = replyContent;
        this.date = date;
        this.replyUserId = replyUserId;
        this.likeCountReply = likeCountReply;
        this.isSending = isSending;
    }

    public ReplyModel copy() {
        ReplyModel kopya = new ReplyModel(this.replyId, this.name, this.replyContent, this.date, this.replyUserId, this.likeCountReply, this.isSending);
        kopya.setCommentId(this.commentId);
        kopya.setLiked(this.isLiked);
        kopya.setLocalOnly(this.localOnly);
        return kopya;
    }

    public String getReplyId() { return replyId; }
    public void setReplyId(String replyId) { this.replyId = replyId; }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReplyContent() { return replyContent; }
    public void setReplyContent(String replyContent) { this.replyContent = replyContent; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getReplyUserId() { return replyUserId; }
    public void setReplyUserId(String replyUserId) { this.replyUserId = replyUserId; }

    public int getLikeCountReply() { return likeCountReply; }
    public void setLikeCountReply(int likeCountReply) { this.likeCountReply = likeCountReply; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { this.isLiked = liked; }

    public boolean isSending() { return isSending; }
    public void setSending(boolean sending) { isSending = sending; }

    public boolean isLocalOnly() { return localOnly; }
    public void setLocalOnly(boolean localOnly) { this.localOnly = localOnly; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReplyModel that = (ReplyModel) o;
        return likeCountReply == that.likeCountReply &&
                isLiked == that.isLiked &&
                isSending == that.isSending &&
                localOnly == that.localOnly &&
                Objects.equals(replyId, that.replyId) &&
                Objects.equals(commentId, that.commentId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(replyContent, that.replyContent) &&
                Objects.equals(date, that.date) &&
                Objects.equals(replyUserId, that.replyUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(replyId, commentId, name, replyContent, date, replyUserId, likeCountReply, isLiked, isSending, localOnly);
    }

}