package com.example.samplestickerapp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** The MP4 is only a player proxy; the GIF remains the export source. */
final class GifMedia {
    static final String SOURCE = "original_gif_path";
    static final String START_MS = "gif_start_ms";
    static final String END_MS = "gif_end_ms";
    static final String WIDTH = "gif_width";
    static final String HEIGHT = "gif_height";

    static boolean isGif(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".gif");
    }

    static String[] previewArguments(File source, File preview) {
        // Flatten alpha onto light gray for the video player only. Export keeps alpha.
        String filter = "[0:v]format=rgba,split[fg][base];"
                + "[base]lutrgb=r=238:g=238:b=238,format=rgb24[bg];"
                + "[bg][fg]overlay=shortest=1:format=auto,"
                + "scale=ceil(iw/2)*2:ceil(ih/2)*2,setsar=1[out]";
        return new String[]{"-y", "-ignore_loop", "1", "-i", source.getAbsolutePath(),
                "-filter_complex", filter, "-map", "[out]", "-c:v", "libx264",
                "-crf", "18", "-preset", "fast", "-pix_fmt", "yuv420p",
                "-vsync", "vfr", "-an", "-movflags", "+faststart", preview.getAbsolutePath()};
    }

    static String timelineFilter(long startMs, long endMs, double speed, int fps) {
        if (startMs < 0 || endMs <= startMs || !Double.isFinite(speed) || speed <= 0 || fps <= 0) {
            throw new IllegalArgumentException("Intervalo ou velocidade do GIF inválidos.");
        }
        // GIF delays have centisecond precision. Sample the held frames before trimming
        // so a cut inside a long frame doesn't jump forward to the next original frame.
        return String.format(Locale.US,
                "setpts=PTS-STARTPTS,fps=100,trim=start=%.3f:end=%.3f,"
                        + "setpts=(PTS-STARTPTS)/%.8f,fps=%d,",
                startMs / 1000.0, endMs / 1000.0, speed, fps);
    }

    static String[] webpArguments(File source, boolean gif, String filter,
                                  int quality, int compression, File output) {
        List<String> args = new ArrayList<>(Arrays.asList("-y"));
        if (gif) args.addAll(Arrays.asList("-ignore_loop", "1"));
        args.addAll(Arrays.asList("-i", source.getAbsolutePath(), "-vf", filter,
                "-c:v", "libwebp_anim", "-lossless", "0", "-quality", String.valueOf(quality),
                "-compression_level", String.valueOf(compression), "-preset", "none",
                "-loop", "0", "-an", "-vsync", "0", output.getAbsolutePath()));
        return args.toArray(new String[0]);
    }
}
