//package core;
//
//import javafx.application.Application;
//import javafx.scene.media.MediaPlayer;
//import javafx.stage.Stage;
//import javax.sound.sampled.AudioInputStream;
//import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.Clip;
//import javax.sound.sampled.LineUnavailableException;
//import javax.sound.sampled.UnsupportedAudioFileException;
//import java.io.File;
//import java.io.IOException;
//
//public class SoundPlayer extends Application {
//    private MediaPlayer mediaPlayer;
//
//    public void playSound(String filePath) {
//        try {
//            // Create an AudioInputStream from the WAV file
//            File soundFile = new File(filePath);
//            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
//
//            // Get a clip resource
//            Clip clip = AudioSystem.getClip();
//            clip.open(audioInputStream);
//
//            // Start playing the audio
//            clip.start();
//
//            // Optional: Loop the audio if needed
//            // clip.loop(Clip.LOOP_CONTINUOUSLY);
//
//            // Optional: Wait for the audio to finish playing
//            // clip.drain();
//
//        } catch (UnsupportedAudioFileException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (LineUnavailableException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void start(Stage primaryStage) {
//        // Example usage
//        playSound("/assets/sounds/tone1.ogg");
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}
