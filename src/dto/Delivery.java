package dto;

import java.text.ParseException;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

public class Delivery {

    private String   id;
    private String   link;
    private String   author;
    private String   title;
    private int      viewing;
    private int      view;
    private int      comments;
    private String   created;
    private String[] tags;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public int getViewing() {
        return viewing;
    }
    public void setViewing(int viewing) {
        this.viewing = viewing;
    }
    public int getView() {
        return view;
    }
    public void setView(int view) {
        this.view = view;
    }
    public int getComments() {
        return comments;
    }
    public void setComments(int comments) {
        this.comments = comments;
    }
    public String getCreated() {
        return formatForView(created);
    }
    public void setCreated(String created) {
        this.created = created;
    }
    public String[] getTags() {
        return tags;
    }
    public void setTags(String[] tags) {
        this.tags = tags;
    }

    private String formatForView(String created) {
        try {
            Date date = DateUtils.parseDate(created, "EEE, dd MMM yyyy HH:mm:ss 'GMT'");
            date = DateUtils.addHours(date, 9);
            return DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
        } catch (ParseException e) {
            return created;
        }
    }
}
