package it.ksuploader.client.utils;

import java.io.IOException;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Represents one of the application's sounds.
 */
public class Sound extends Thread {

    /**
     * A URL to the success sound inside the JAR file.
     */
    public static final URL URL_TO_SUCCESS_SOUND = Sound.class.getResource("/complete.wav");

    private final Clip clip;

    /**
     * Loads the sound data into a new Sound object.
     *
     * @param toSoundFile A URL to the file containing the audio data.
     * @throws UnsupportedAudioFileException If the URL does not point to valid
     * audio file data recognized by the system.
     * @throws IOException If an I/O error occurs while reading the file.
     * @throws LineUnavailableException If the audio file cannot be read due to
     * resource restrictions.
     */
    public Sound(URL toSoundFile) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(toSoundFile)) {
            AudioFormat format = audioInputStream.getFormat();
            byte[] audioData = new byte[audioInputStream.available()];
            audioInputStream.read(audioData);

            DataLine.Info info = new DataLine.Info(Clip.class, format);
            clip = (Clip) AudioSystem.getLine(info);
            clip.open(format, audioData, 0, audioData.length);
            clip.addLineListener((LineEvent event) -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
        }
    }

    @Override
    public void run() {
        if (clip.isRunning()) {
            clip.close();
        }
        clip.start();
    }
}
