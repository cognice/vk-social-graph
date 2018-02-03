package us.cognice.vk.model;

public class JsonEdge {

    private String from, to;

    public JsonEdge() {
    }

    public JsonEdge(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {

        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
