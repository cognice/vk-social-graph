package us.cognice.vk.model;

public class JsonUser {

    private String id;
    private String screenName;
    private String firstName;
    private String lastName;
    private String logo;

    public JsonUser() {
    }

    public JsonUser(String id, String screenName, String firstName, String lastName, String logo) {
        this.id = id;
        this.screenName = screenName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.logo = logo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}
