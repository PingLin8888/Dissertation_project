package core;

import javafx.application.Application;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import java.net.URL;

public class SoundPlayer extends Application {
    private MediaPlayer mediaPlayer;

    public void playSound(String filePath) {
        URL resource = getClass().getResource(filePath);
        if (resource == null) {
            System.out.println("Resource not found: " + filePath);
        } else {
            System.out.println("Resource found: " + resource.toString());
        }
        Media sound = new Media(getClass().getResource(filePath).toString());
        mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.play();
    }

    @Override
    public void start(Stage primaryStage) {
        // Example usage
        playSound("/assets/sounds/tone1.ogg");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
