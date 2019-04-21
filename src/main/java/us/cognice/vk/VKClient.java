package us.cognice.vk;

import com.google.gson.Gson;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.friends.UserXtrLists;
import com.vk.api.sdk.objects.friends.responses.GetFieldsResponse;
import com.vk.api.sdk.objects.users.UserXtrCounters;
import com.vk.api.sdk.queries.users.UserField;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import us.cognice.graph.layout.Edge;
import us.cognice.graph.layout.Graph;
import us.cognice.graph.layout.Layout;
import us.cognice.graph.layout.MouseGestures;
import us.cognice.graph.layout.forced.ForcedLayout;
import us.cognice.vk.model.ImageConverter;
import us.cognice.vk.model.JsonEdge;
import us.cognice.vk.model.JsonGraph;
import us.cognice.vk.model.JsonUser;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

/**
 * Created by Kirill Simonov on 13.06.2017.
 */
public class VKClient extends Application {

    private final String API_VERSION = "5.21";
    private final String AUTH_URL = "https://oauth.vk.com/authorize"
            + "?client_id={APP_ID}"
            + "&scope={PERMISSIONS}"
            + "&redirect_uri={REDIRECT_URI}"
            + "&display={DISPLAY}"
            + "&v={API_VERSION}"
            + "&response_type=token";
    private static final String REDIRECT = "https://oauth.vk.com/blank.html";
    private final String TOKEN_KEY = "access_token";
    private final String USER_KEY = "user_id";
    private final String APP_ID = "4185274";
    private final int WIDTH = 1100, HEIGHT = 820;
    private Map<String, String> params;
    private ExecutorService executor = ForkJoinPool.commonPool();
    private MouseGestures mouseGestures;
    private BorderPane container;
    private Scene scene;
    private Gson gson = new Gson();
    private Graph<VKUser> graph;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            String reqUrl = AUTH_URL
                    .replace("{APP_ID}", APP_ID)
                    .replace("{PERMISSIONS}", "friends,photo")
                    .replace("{REDIRECT_URI}", REDIRECT)
                    .replace("{DISPLAY}", "page")
                    .replace("{API_VERSION}", API_VERSION);
            URL url = new URL(reqUrl);
            final WebView browser = new WebView();
            primaryStage.setTitle("VK Social Graph");
            container = new BorderPane(createLoader("Loading login page...", browser.getEngine().getLoadWorker().progressProperty()));
            BorderPane controls = new BorderPane();
            Button loadButton = new Button("Load existing graph");
            loadButton.getStyleClass().add("button-with-margin");
            Button saveButton = new Button("Save graph");
            saveButton.getStyleClass().add("button-with-margin");
            saveButton.setDisable(true);
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
            fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            loadButton.setOnMouseClicked(event -> {
                fileChooser.setTitle("Open Saved Graph");
                File file = fileChooser.showOpenDialog(primaryStage);
                if (file != null) {
                    loadGraph(file);
                }
            });
            saveButton.setOnMouseClicked(event -> {
                fileChooser.setTitle("Save Graph");
                File file = fileChooser.showSaveDialog(primaryStage);
                if (file != null) {
                    saveGraph(file);
                }
            });
            controls.setLeft(loadButton);
            controls.setRight(saveButton);
            container.setBottom(controls);
            BorderPane.setAlignment(loadButton, Pos.BOTTOM_CENTER);
            scene = new Scene(container, WIDTH, HEIGHT);
            URL styleURL = getClass().getClassLoader().getResource("application.css");
            if (styleURL != null) {
                scene.getStylesheets().add(styleURL.toExternalForm());
            }
            primaryStage.setScene(scene);
            primaryStage.show();
            browser.getEngine().load(url.toString());
            browser.getEngine().getLoadWorker().stateProperty().addListener(
                (ObservableValue<? extends Worker.State> observable,
                 Worker.State oldValue,
                 Worker.State newValue) -> {
                    if (newValue != Worker.State.SUCCEEDED) {
                        return;
                    }
                    String location = browser.getEngine().getLocation();
                    if (location.startsWith(REDIRECT)) {
                        params = Arrays.stream(location.substring(location.lastIndexOf('#') + 1).split("&")).collect(
                                HashMap::new,
                                (map, s) -> {
                                    String[] pv = s.split("=");
                                    map.put(pv[0], pv[1]);
                                }, HashMap::putAll);
                        retrieveFriends();
                    } else {
                        container.setCenter(browser);
                    }
                }
            );
        } catch (MalformedURLException e) {
            showMessage(Alert.AlertType.ERROR, "Error", "Error while establishing connection:", e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveGraph(File file) {
        Node controls = container.getBottom();
        controls.setDisable(true);
        FileSavingTask task = new FileSavingTask(file);
        task.setOnSucceeded(event -> {
            container.setCenter(graph.getCanvas());
            controls.setDisable(false);
            showMessage(Alert.AlertType.INFORMATION, "Success", "Graph was successfully saved", "");
        });
        task.setOnFailed(event -> {
            container.setCenter(graph.getCanvas());
            controls.setDisable(false);
            showMessage(Alert.AlertType.ERROR, "Error", "Error while building graph:", event.getSource().getException().getMessage());
            event.getSource().getException().printStackTrace();
        });
        executor.submit(task);
        container.setCenter(createLoader("Saving graph...", task.progressProperty()));
    }

    @SuppressWarnings("unchecked")
    private void loadGraph(File file) {
        Node controls = container.getBottom();
        controls.setDisable(true);
        FileLoadingTask task = new FileLoadingTask(file);
        task.setOnSucceeded(event -> {
            displayGraph((Graph<VKUser>) event.getSource().getValue());
            controls.setDisable(false);
        });
        task.setOnFailed(event -> {
            controls.setDisable(false);
            showMessage(Alert.AlertType.ERROR, "Error", "Error while building graph:", event.getSource().getException().getMessage());
            event.getSource().getException().printStackTrace();
        });
        executor.submit(task);
        container.setCenter(createLoader("Loading graph...", task.progressProperty()));
    }

    private StackPane createLoader(String label, ReadOnlyDoubleProperty progress) {
        ProgressBar bar = new ProgressBar();
        Text text = new Text(label);
        bar.progressProperty().bind(progress);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setMinHeight(text.getBoundsInLocal().getHeight() + 10);
        bar.setMinWidth(text.getBoundsInLocal().getWidth() + 10);
        bar.setMaxWidth(200);
        StackPane barBox = new StackPane();
        barBox.getChildren().addAll(bar, text);
        return barBox;
    }

    @SuppressWarnings("unchecked")
    private void retrieveFriends() {
        Node loadButton = ((BorderPane) container.getBottom()).getRight();
        loadButton.setDisable(true);
        FriendsRetrievingTask task = new FriendsRetrievingTask();
        task.setOnSucceeded(event -> {
            displayGraph((Graph<VKUser>) event.getSource().getValue());
            loadButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            loadButton.setDisable(false);
            showMessage(Alert.AlertType.ERROR, "Error", "Error while building graph:", event.getSource().getException().getMessage());
            event.getSource().getException().printStackTrace();
        });
        executor.submit(task);
        container.setCenter(createLoader("Loading friends list...", task.progressProperty()));
    }

    private void displayGraph(Graph<VKUser> graph) {
        this.graph = graph;
        Layout layout = new ForcedLayout(container, graph, WIDTH, HEIGHT, 0.7);
        mouseGestures = new MouseGestures(graph, layout);
        graph.getCells().values().forEach(mouseGestures::apply);
        layout.start();
        ((BorderPane) container.getBottom()).getRight().setDisable(false);
    }

    private void showMessage(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.show();
    }

    private class FileSavingTask extends Task<Void> {
        private File target;
        FileSavingTask(File target) {
            this.target = target;
        }
        @Override
        protected Void call() throws Exception {
            try (Writer writer = new BufferedWriter(new FileWriter(target))) {
                int progress = 0;
                int totalProgress = graph.getCells().size() + graph.getEdges().size() / 100;
                updateProgress(progress, totalProgress);
                JsonGraph jsonGraph = new JsonGraph();
                for (VKUser u: graph.getCells().values()) {
                    jsonGraph.getUsers().add(u.toJsonUser());
                    updateProgress(progress++, totalProgress);
                }
                for (Edge<VKUser> e: graph.getEdges()) {
                    jsonGraph.getLinks().add(new JsonEdge(e.getSource().getId(), e.getTarget().getId()));
                    updateProgress(progress++, totalProgress);
                }
                gson.toJson(jsonGraph, writer);
            }
            return null;
        }
    }

    private class FileLoadingTask extends Task<Graph> {
        private File json;
        FileLoadingTask(File json) {
            this.json = json;
        }
        @Override
        protected Graph call() throws Exception {
            try (BufferedReader br = new BufferedReader(new FileReader(json))) {
                JsonGraph jsonGraph = gson.fromJson(br, JsonGraph.class);
                int progress = 0;
                int totalProgress = jsonGraph.getUsers().size() + jsonGraph.getLinks().size() / 100;
                updateProgress(progress, totalProgress);
                Graph<VKUser> graph = new Graph<>();
                for (JsonUser u: jsonGraph.getUsers()) {
                    VKUser friend = new VKUser(u.getId(), u.getScreenName(), u.getFirstName(), u.getLastName(), ImageConverter.decode(u.getLogo()));
                    graph.addCell(friend);
                    updateProgress(progress++, totalProgress);
                }
                for (JsonEdge e: jsonGraph.getLinks()) {
                    graph.addEdge(e.getFrom(), e.getTo());
                    updateProgress(progress++, totalProgress);
                }
                return graph;
            }
        }
    }

    private class FriendsRetrievingTask extends Task<Graph> {
        @Override
        protected Graph call() throws Exception {
            TransportClient transportClient = HttpTransportClient.getInstance();
            VkApiClient vk = new VkApiClient(transportClient);
            UserActor actor = new UserActor(Integer.parseInt(params.get(USER_KEY)), params.get(TOKEN_KEY));
            UserXtrCounters profile = vk.users().get(actor).fields(UserField.NICKNAME, UserField.SCREEN_NAME, UserField.PHOTO_50).execute().get(0);
            GetFieldsResponse friends = vk.friends().get(actor, UserField.NICKNAME, UserField.SCREEN_NAME, UserField.PHOTO_50).execute();
            long lastCall = System.currentTimeMillis();
            Graph<VKUser> graph = new Graph<>();
            VKUser me = new VKUser(String.valueOf(actor.getId()), profile.getScreenName(), profile.getFirstName(), profile.getLastName(), new Image(profile.getPhoto50()));
            graph.addCell(me);
            Map<Integer, VKUser> idMap = new HashMap<>();
            for (UserXtrLists u : friends.getItems()) {
                VKUser friend = new VKUser(String.valueOf(u.getId()), u.getScreenName(), u.getFirstName(), u.getLastName(), u.getPhoto50());
                graph.addCell(friend);
                graph.addEdge(me.getId(), friend.getId());
                idMap.put(u.getId(), friend);
            }
            int progress = 0;
            updateProgress(progress, friends.getCount() + 10);
            Thread.sleep(Math.max(0, 334 - System.currentTimeMillis() + lastCall));
            for (UserXtrLists u : friends.getItems()) {
                try {
                    List<Integer> mutual = vk.friends().getMutual(actor).sourceUid(actor.getId()).targetUid(u.getId()).execute();
                    lastCall = System.currentTimeMillis();
                    VKUser user = idMap.get(u.getId());
                    if (!user.iconSet()) {
                        user.setIcon(new Image(user.getUrl()));
                    }
                    mutual.forEach(id -> graph.addEdge(idMap.get(id).getId(), user.getId()));
                    Thread.sleep(Math.max(0, 334 - System.currentTimeMillis() + lastCall));
                } catch (ApiException | ClientException e) {
                    e.printStackTrace();
                }
                updateProgress(progress++, friends.getCount() + 10);
            }
            idMap.values().stream().filter(vkUser -> !vkUser.iconSet()).forEach(vkUser -> vkUser.setIcon(new Image(vkUser.getUrl())));
            updateProgress(friends.getCount() + 10, friends.getCount() + 10);
            return graph;
        }
    }
}
