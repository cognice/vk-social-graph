package us.cognice.vk.model;

import java.util.ArrayList;
import java.util.List;

public class JsonGraph {

    private List<JsonUser> users = new ArrayList<>();
    private List<JsonEdge> links = new ArrayList<>();

    public List<JsonUser> getUsers() {
        return users;
    }

    public void setUsers(List<JsonUser> users) {
        this.users = users;
    }

    public List<JsonEdge> getLinks() {
        return links;
    }

    public void setLinks(List<JsonEdge> links) {
        this.links = links;
    }
}
