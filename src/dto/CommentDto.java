package dto;

public class CommentDto {

    // コメント番号
    private int commentNumber;

    // 名前
    private String name;

    // 時間
    private String time;

    // コメント本文
    private String comment;

    public CommentDto(int commentNumber, String name, String time, String comment) {
        this.commentNumber = commentNumber;
        this.time          = time;
        this.comment       = comment;
        this.name          = name;
    }

    public int getCommentNumber() {
        return this.commentNumber;
    }

    public String getName() {
        return this.name;
    }

    public String getTime() {
        return this.time;
    }

    public String getComment() {
        return this.comment;
    }
}
