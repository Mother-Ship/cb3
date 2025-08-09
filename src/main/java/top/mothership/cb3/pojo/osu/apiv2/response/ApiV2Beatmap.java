package top.mothership.cb3.pojo.osu.apiv2.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;

public class ApiV2Beatmap {

    @Data
    public static class Beatmap {
        @JsonProperty("beatmapset_id")
        private long beatmapsetId;

        @JsonProperty("difficulty_rating")
        private double difficultyRating;

        @JsonProperty("id")
        private long beatmapId;

        @JsonProperty("mode")
        private String mode;

        @JsonProperty("status")
        private Status status;

        @JsonProperty("total_length")
        private long totalLength;

        @JsonProperty("user_id")
        private long userId;

        @JsonProperty("version")
        private String version;

        // Accuracy = OD
        @JsonProperty("accuracy")
        private double od;

        @JsonProperty("ar")
        private double ar;

        @JsonProperty("bpm")
        private Double bpm;

        @JsonProperty("convert")
        private boolean convert;

        @JsonProperty("count_circles")
        private long countCircles;

        @JsonProperty("count_sliders")
        private long countSliders;

        @JsonProperty("count_spinners")
        private long countSpinners;

        @JsonProperty("cs")
        private double cs;

        @JsonProperty("deleted_at")
        private String deletedAt;

        @JsonProperty("drain")
        private double hpDrain;

        @JsonProperty("hit_length")
        private long hitLength;

        @JsonProperty("is_scoreable")
        private boolean isScoreable;

        @JsonProperty("last_updated")
        private String lastUpdated;

        @JsonProperty("mode_int")
        private int modeInt;

        @JsonProperty("passcount")
        private long passcount;

        @JsonProperty("playcount")
        private long playcount;

        @JsonProperty("ranked")
        private long ranked;

        @JsonProperty("url")
        private URI url;

        @JsonProperty("checksum")
        private String checksum;

        @JsonProperty("beatmapset")
        private ApiV2BeatmapSet.Beatmapset beatmapset;

        @JsonProperty("failtimes")
        private BeatmapFailtimes failtimes;

        @JsonProperty("max_combo")
        private Long maxCombo;

        @JsonProperty("user")
        private ApiV2User.User user;
    }

    @Data
    public static class BeatmapFailtimes {
        @JsonProperty("fail")
        private int[] fail;

        @JsonProperty("exit")
        private int[] exit;
    }


    public enum Status {
        graveyard,
        wip,
        pending,
        ranked,
        approved,
        qualified,
        loved
    }

}