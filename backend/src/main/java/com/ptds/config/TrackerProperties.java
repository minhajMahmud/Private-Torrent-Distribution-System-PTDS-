package com.ptds.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.tracker")
@Getter @Setter
public class TrackerProperties {
    /** Announce URLs advertised in generated .torrent files and magnet links. */
    private List<String> announceUrls = List.of("udp://localhost:6969/announce");
    private int pieceLengthBytes = 262144; // 256 KiB
}
