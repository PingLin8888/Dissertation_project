package core;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AudioManager {
    private static AudioManager instance;
    private final Map<String, Clip> soundCache;
    private final Set<String> activeSounds; // Track currently active sounds

    // Sound type constants
    private static final String SOUND_EERIE = "eerie";
    private static final String SOUND_CHASER = "chaser";
    private static final String SOUND_INVISIBILITY = "invisibility";
    private static final String SOUND_MENU = "menu";
    private static final String SOUND_CONSUME = "consume";
    private static final String SOUND_GAMEOVER = "gameover";
    private static final String SOUND_GAMEPASS = "gamePass";
    private static final String SOUND_GAMESTART = "gamestart";
    private static final String SOUND_WALK = "walk";
    private static final String SOUND_TELEPORT = "teleport";
    private static final String SOUND_SLIDE = "slide";
    private static final String SOUND_DAMAGE = "damage";

    // Different channels for different types of sounds
    private static final float EFFECTS_VOLUME = 0.7f;
    private static final float WALK_VOLUME = 0.3f;

    private float masterVolume = 1.0f;
    private float musicVolume = 0.7f;
    private float sfxVolume = 0.5f;

    private AudioManager() {
        soundCache = new HashMap<>();
        activeSounds = new HashSet<>();
        initializeSounds();
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    private void initializeSounds() {
        // Load all sounds at startup using constants
        loadSound(SOUND_MENU, "/sounds/bookOpen_was_cilck.wav");
        loadSound(SOUND_CONSUME, "/sounds/handleCoins.wav");
        loadSound(SOUND_GAMEOVER, "/sounds/404743__owlstorm__retro-video-game-sfx-fail.wav");
        loadSound(SOUND_GAMEPASS, "/sounds/397355__plasterbrain__tada-fanfare-a.wav");
        loadSound(SOUND_GAMESTART, "/sounds/243020__plasterbrain__game-start.wav");
        loadSound(SOUND_WALK, "/sounds/329601__inspectorj__footsteps-dry-leaves-c.wav");
        loadSound(SOUND_TELEPORT, "/sounds/laser5.wav");
        loadSound(SOUND_SLIDE, "/sounds/lowThreeTone.wav");
        loadSound(SOUND_DAMAGE, "/sounds/laser8.wav");
        loadSound(SOUND_EERIE, "/sounds/near_dark.wav");
        loadSound(SOUND_CHASER, "/sounds/chaser.wav");
        loadSound(SOUND_INVISIBILITY, "/sounds/invisibility_loop.wav");
    }

    private void loadSound(String soundId, String resourcePath) {
        try {

            var resource = getClass().getResource(resourcePath);
            if (resource == null) {
                System.err.println("Could not find sound resource: " + resourcePath);
                return;
            }

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(resource);

            // For footstep sounds, trim the duration
            if (soundId.equals(SOUND_WALK)) {
                // Convert to PCM format if needed
                AudioFormat format = audioInputStream.getFormat();
                if (!format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    audioInputStream = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED,
                            audioInputStream);
                }

                // Get the first 200ms of audio
                int bytesPerFrame = format.getFrameSize();
                long framesForDuration = (long) (0.2 * format.getFrameRate()); // 200ms
                byte[] audioData = new byte[(int) (framesForDuration * bytesPerFrame)];

                audioInputStream.read(audioData);
                AudioInputStream shortenedStream = new AudioInputStream(
                        new ByteArrayInputStream(audioData),
                        format,
                        audioData.length / bytesPerFrame);
                audioInputStream = shortenedStream;
            }

            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            // Add listener to reset clip position
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.setFramePosition(0);
                }
            });

            soundCache.put(soundId, clip);
            audioInputStream.close();
        } catch (Exception e) {
            System.err.println("Error loading sound " + soundId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to check if a sound should loop
    private boolean isLoopingSound(String soundId) {
        return soundId.equals(SOUND_EERIE) ||
                soundId.equals(SOUND_CHASER) ||
                soundId.equals(SOUND_INVISIBILITY);
    }

    public void playSound(String soundId) {
        Clip clip = soundCache.get(soundId);
        if (clip != null && !clip.isRunning()) { // Only play if not already playing
            try {
                clip.setFramePosition(0);

                // Apply volume settings before playing
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float effectiveVolume = masterVolume * (isMusic(soundId) ? musicVolume : sfxVolume);

                    // Handle 0 volume correctly by setting to minimum gain (silent)
                    if (effectiveVolume <= 0.001f) {
                        gainControl.setValue(gainControl.getMinimum());
                    } else {
                        float dB = (float) (Math.log10(Math.max(0.01f, effectiveVolume)) * 20.0f);
                        gainControl
                                .setValue(Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB)));
                    }
                }

                clip.start();
                // Only add to activeSounds if it's a looping sound
                if (isLoopingSound(soundId)) {
                    activeSounds.add(soundId);
                }
            } catch (Exception e) {
                System.err.println("Error playing sound " + soundId + ": " + e.getMessage());
            }
        }
    }

    public void stopSound(String soundId) {
        Clip clip = soundCache.get(soundId);
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.setFramePosition(0);
            activeSounds.remove(soundId);
        }
    }

    // New method to fade out a sound gradually over fadeDuration milliseconds
    public void fadeOutSound(String soundId, int fadeDuration) {
        Clip clip = soundCache.get(soundId);
        if (clip != null && clip.isRunning()) {
            try {
                // First check if the clip supports volume control
                if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    System.out.println("Volume control not supported for " + soundId + ", using fallback stop");
                    stopSound(soundId);
                    return;
                }

                // If we get here, volume control is supported
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float currentGain = gainControl.getValue();
                float minGain = gainControl.getMinimum();
                int steps = 20;
                long sleepInterval = fadeDuration / steps;

                new Thread(() -> {
                    try {
                        for (int i = 0; i < steps && clip.isRunning(); i++) {
                            float newGain = currentGain + (minGain - currentGain) * (i + 1) / steps;
                            gainControl.setValue(newGain);
                            Thread.sleep(sleepInterval);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        stopSound(soundId);
                        // Reset gain to original value
                        try {
                            gainControl.setValue(currentGain);
                        } catch (IllegalArgumentException ex) {
                            System.err.println("Error resetting gain for " + soundId);
                        }
                    }
                }).start();
            } catch (Exception e) {
                System.err.println("Error during fade out for " + soundId + ": " + e.getMessage());
                stopSound(soundId); // Fallback to immediate stop
            }
        }
    }

    public void stopAllSoundsExcept(String exceptSoundId) {
        Set<String> soundsToStop = new HashSet<>(activeSounds);
        soundsToStop.remove(exceptSoundId);

        for (String soundId : soundsToStop) {
            stopSound(soundId);
        }
    }

    public void cleanup() {
        for (Clip clip : soundCache.values()) {
            clip.close();
        }
        soundCache.clear();
    }

    public void setWalkVolume(float volume) {
        Clip clip = soundCache.get(SOUND_WALK);
        if (clip != null) {
            try {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (20 * Math.log10(volume));
                gainControl.setValue(dB);
            } catch (Exception e) {
                System.err.println("Error setting walk volume: " + e.getMessage());
            }
        }
    }

    public void playLoopingSound(String soundId) {
        if (!isLoopingSound(soundId)) {
            System.err.println("Warning: Attempting to loop non-looping sound: " + soundId);
            return;
        }
        Clip clip = soundCache.get(soundId);
        if (clip != null) {
            clip.setFramePosition(0);

            // Apply volume settings for all looping sounds
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float effectiveVolume = masterVolume * (isMusic(soundId) ? musicVolume : sfxVolume);

                // Handle 0 volume correctly by setting to minimum gain (silent)
                float dB;
                if (effectiveVolume <= 0.001f) {
                    dB = gainControl.getMinimum();
                } else {
                    dB = (float) (Math.log10(Math.max(0.01f, effectiveVolume)) * 20.0f);
                    dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                }
                gainControl.setValue(dB);
                System.out.println("Starting looping sound: " + soundId + " with volume: " + effectiveVolume + " (dB: "
                        + dB + ")");
            }

            clip.loop(Clip.LOOP_CONTINUOUSLY);
            activeSounds.add(soundId);
        }
    }

    public void stopLoopingSound(String soundId) {
        Clip clip = soundCache.get(soundId);
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            boolean wasRemoved = activeSounds.remove(soundId); // Remove from active sounds
            System.out.println("Stopped looping sound: " + soundId + ", was in active sounds: " + wasRemoved);
        } else {
            System.out.println("Cannot stop looping sound: " + soundId + " - not found in cache");
        }
    }

    // New method to pause all active sounds
    public void pauseAllSounds() {
        for (String soundId : activeSounds) {
            Clip clip = soundCache.get(soundId);
            if (clip != null && clip.isRunning()) {
                clip.stop();
            }
        }
    }

    // New method to resume all active sounds
    public void resumeAllSounds() {
        for (String soundId : activeSounds) {
            Clip clip = soundCache.get(soundId);
            if (clip != null && isLoopingSound(soundId)) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        }
    }

    // Add a method to stop all currently playing sounds
    public void stopAllSounds() {
        // Create a copy of the activeSounds set to avoid
        // ConcurrentModificationException
        Set<String> soundsToStop = new HashSet<>(activeSounds);
        for (String soundId : soundsToStop) {
            stopSound(soundId);
        }
        activeSounds.clear();
    }

    public void setMasterVolume(float volume) {
        this.masterVolume = volume;
        updateAllVolumes();

        // Ensure the invisibility sound is properly updated
        Clip invisibilityClip = soundCache.get(SOUND_INVISIBILITY);
        if (invisibilityClip != null && invisibilityClip.isRunning()) {
            if (!activeSounds.contains(SOUND_INVISIBILITY)) {
                System.out.println("Re-adding invisibility sound to active sounds list");
                activeSounds.add(SOUND_INVISIBILITY);
            }
            updateInvisibilitySoundVolume();
        }
    }

    public void setMusicVolume(float volume) {
        this.musicVolume = volume;
        updateAllVolumes();
    }

    public void setSFXVolume(float volume) {
        this.sfxVolume = volume;
        updateAllVolumes();
    }

    private void updateAllVolumes() {
        System.out.println("Updating all volumes. Master volume: " + masterVolume);
        for (Map.Entry<String, Clip> entry : soundCache.entrySet()) {
            String soundId = entry.getKey();
            Clip clip = entry.getValue();

            try {
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float effectiveVolume = masterVolume * (isMusic(soundId) ? musicVolume : sfxVolume);

                    // Handle 0 volume correctly by setting to minimum gain (silent)
                    float dB;
                    if (effectiveVolume <= 0.001f) {
                        dB = gainControl.getMinimum();
                        System.out.println("Setting " + soundId + " to minimum volume (silent)");
                    } else {
                        dB = (float) (Math.log10(Math.max(0.01f, effectiveVolume)) * 20.0f);
                        dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                        System.out.println(
                                "Setting volume for " + soundId + " to " + effectiveVolume + " (dB: " + dB + ")");
                    }
                    gainControl.setValue(dB);
                }
            } catch (Exception e) {
                System.err.println("Error setting volume for " + soundId + ": " + e.getMessage());
            }
        }

        // Make sure all active sounds respect the new volume settings
        updateActiveLoopingSounds();
    }

    /**
     * Updates the volume for all currently active looping sounds
     */
    private void updateActiveLoopingSounds() {
        System.out.println("Updating active looping sounds. Active sounds: " + activeSounds);
        for (String soundId : activeSounds) {
            if (isLoopingSound(soundId)) {
                Clip clip = soundCache.get(soundId);
                if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    try {
                        FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        float effectiveVolume = masterVolume * (isMusic(soundId) ? musicVolume : sfxVolume);

                        // Handle 0 volume correctly by setting to minimum gain (silent)
                        float dB;
                        if (effectiveVolume <= 0.001f) {
                            dB = gainControl.getMinimum();
                        } else {
                            dB = (float) (Math.log10(Math.max(0.01f, effectiveVolume)) * 20.0f);
                            dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                        }
                        gainControl.setValue(dB);
                        System.out.println(
                                "Updating active looping sound: " + soundId + " to volume: " + effectiveVolume
                                        + " (dB: " + dB + ")");
                    } catch (Exception e) {
                        System.err.println("Error updating volume for active sound " + soundId + ": " + e.getMessage());
                    }
                } else {
                    System.out.println("Warning: Cannot update volume for " + soundId +
                            (clip == null ? " - clip is null" : " - no gain control support"));
                }
            }
        }
    }

    /**
     * Specifically updates the volume for the invisibility sound effect
     * to ensure it respects the master volume setting even while looping.
     */
    public void updateInvisibilitySoundVolume() {
        Clip clip = soundCache.get(SOUND_INVISIBILITY);
        if (clip != null) {
            // Add to active sounds if it's running but not in the list
            if (clip.isRunning() && !activeSounds.contains(SOUND_INVISIBILITY)) {
                System.out.println("Adding running invisibility sound to active sounds list");
                activeSounds.add(SOUND_INVISIBILITY);
            }

            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                try {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float effectiveVolume = masterVolume * sfxVolume;

                    // Handle 0 volume correctly by setting to minimum gain (silent)
                    float dB;
                    if (effectiveVolume <= 0.001f) {
                        dB = gainControl.getMinimum();
                    } else {
                        dB = (float) (Math.log10(Math.max(0.01f, effectiveVolume)) * 20.0f);
                        dB = Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB));
                    }
                    gainControl.setValue(dB);
                    System.out.println("Updated invisibility sound volume to: " + effectiveVolume +
                            " (dB: " + dB + "), isRunning: " + clip.isRunning());
                } catch (Exception e) {
                    System.err.println("Error updating invisibility volume: " + e.getMessage());
                }
            } else {
                System.out.println("Invisibility sound does not support volume control");
            }
        } else {
            System.out.println("Invisibility sound clip not found in cache");
        }
    }

    private boolean isMusic(String soundId) {
        // Add any background music sound IDs here
        return soundId.equals(SOUND_EERIE) ||
                soundId.equals(SOUND_CHASER) ||
                soundId.equals(SOUND_INVISIBILITY);
    }

    /**
     * Public method to update volumes of all currently playing sounds.
     * This is useful when volume settings change while sounds are playing.
     */
    public void updateAllPlayingSoundVolumes() {
        updateActiveLoopingSounds();
    }
}