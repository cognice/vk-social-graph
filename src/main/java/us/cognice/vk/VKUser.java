package us.cognice.vk;

import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import us.cognice.graph.layout.Cell;
import us.cognice.vk.model.ImageConverter;
import us.cognice.vk.model.JsonUser;

import java.io.IOException;

/**
 * Created by Kirill Simonov on 19.06.2017.
 */
public class VKUser extends Cell {

    private String screenName;
    private String firstName;
    private String lastName;
    private String url;
    private Image icon;

    public VKUser(String id, String screenName, String firstName, String lastName, String url) {
        super(id, firstName + " " + lastName, 7, url);
        this.screenName = screenName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.url = url;
    }

    public VKUser(String id, String screenName, String firstName, String lastName, Image image) {
        super(id, firstName + " " + lastName, 7, image);
        this.screenName = screenName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.icon = image;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Image getIcon() {
        return icon;
    }

    public void setIcon(Image icon) {
        this.icon = icon;
        super.setFill(new ImagePattern(icon));
    }

    public boolean iconSet() {
        return icon != null;
    }

    public JsonUser toJsonUser() throws IOException {
        return new JsonUser(getId(), screenName, firstName, lastName, ImageConverter.encode(icon));
    }
}
