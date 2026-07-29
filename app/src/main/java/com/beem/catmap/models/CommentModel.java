package com.beem.catmap.models;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class CommentModel {
    private String commentId;
    private String username;
    private String commentContent;
    private Date date;
    private String loadId;

    private ArrayList<ReplyModel> replies = new ArrayList<>();
    private DocumentSnapshot lastReply;
    private boolean areRepliesVisible = false;
    private boolean areRepliesLoaded = false;
    private boolean hasNoReplies = false;
    private boolean hasMoreReplies = true;
    private boolean isLiked = false;
    private int likeCount = 0;

    private int sumRepliesCount = 0;

    private boolean isSending = false;

    public CommentModel() {}

    public CommentModel(String commentId, String username, String commentContent, Date date, ArrayList<ReplyModel> replies, String loadId, boolean isSending) {
        this.commentId = commentId;
        this.username = username;
        this.commentContent = commentContent;
        this.date = date;
        this.replies = replies != null ? replies : new ArrayList<>();
        this.loadId = loadId;
        this.isSending = isSending;
    }
    public CommentModel copy() {
        ArrayList<ReplyModel> yeniListe =
                this.replies == null
                        ? new ArrayList<>()
                        : new ArrayList<>(this.replies);

        CommentModel newModel = new CommentModel(
                this.commentId,
                this.username,
                this.commentContent,
                this.date,
                yeniListe,
                this.loadId,
                this.isSending
        );

        SharedCommentData(newModel);
        return newModel;
    }
    private void SharedCommentData(CommentModel target) {
        target.lastReply = this.lastReply;
        target.areRepliesVisible = this.areRepliesVisible;
        target.areRepliesLoaded = this.areRepliesLoaded;
        target.hasNoReplies = this.hasNoReplies;
        target.hasMoreReplies = this.hasMoreReplies;
        target.isLiked = this.isLiked;
        target.likeCount = this.likeCount;
        target.isSending = this.isSending;
        target.sumRepliesCount = this.sumRepliesCount;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCommentContent() { return commentContent; }
    public void setCommentContent(String commentContent) { this.commentContent = commentContent; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public String getLoadId() { return loadId; }
    public void setLoadId(String loadId) { this.loadId = loadId; }

    public ArrayList<ReplyModel> getReplies() { return replies; }
    public void setReplies(ArrayList<ReplyModel> replies) { this.replies = replies; }

    public DocumentSnapshot getLastReply() { return lastReply; }
    public void setLastReply(DocumentSnapshot lastReply) { this.lastReply = lastReply; }

    public boolean isAreRepliesVisible() { return areRepliesVisible; }
    public void setAreRepliesVisible(boolean areRepliesVisible) { this.areRepliesVisible = areRepliesVisible; }

    public boolean isAreRepliesLoaded() { return areRepliesLoaded; }
    public void setAreRepliesLoaded(boolean areRepliesLoaded) { this.areRepliesLoaded = areRepliesLoaded; }

    public boolean isHasNoReplies() { return hasNoReplies; }
    public void setHasNoReplies(boolean hasNoReplies) { this.hasNoReplies = hasNoReplies; }

    public boolean isHasMoreReplies() { return hasMoreReplies; }
    public void setHasMoreReplies(boolean hasMoreReplies) { this.hasMoreReplies = hasMoreReplies; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { this.isLiked = liked; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public Boolean getSending() { return isSending; }
    public void setSending(Boolean sending) { isSending = sending; }

    public int getSumRepliesCount() {
        return sumRepliesCount;
    }

    public void setSumRepliesCount(int sumRepliesCount) {
        this.sumRepliesCount = sumRepliesCount;
    }

    public boolean isSending() {
        return isSending;
    }

    public void setSending(boolean sending) {
        isSending = sending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentModel that = (CommentModel) o;

        return areRepliesVisible == that.areRepliesVisible &&
                areRepliesLoaded == that.areRepliesLoaded &&
                hasNoReplies == that.hasNoReplies &&
                hasMoreReplies == that.hasMoreReplies &&
                isLiked == that.isLiked &&
                likeCount == that.likeCount &&
                isSending == that.isSending &&
                sumRepliesCount == that.sumRepliesCount &&
                Objects.equals(commentId, that.commentId) &&
                Objects.equals(username, that.username) &&
                Objects.equals(commentContent, that.commentContent) &&
                Objects.equals(date, that.date) &&
                Objects.equals(loadId, that.loadId) &&
                Objects.equals(replies, that.replies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commentId, username, commentContent, date, loadId, replies,
                areRepliesVisible, areRepliesLoaded, hasNoReplies,
                hasMoreReplies, isLiked, likeCount, isSending, sumRepliesCount);
    }
}