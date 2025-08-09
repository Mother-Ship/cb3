package top.mothership.cb3.pojo.osu.apiv2.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;

public class ApiV2BeatmapSet {

    @Data
    public static class Beatmapset {
        @JsonProperty("artist")
        private String artist;

        @JsonProperty("artist_unicode")
        private String artistUnicode;

        @JsonProperty("covers")
        private BeatmapCovers covers;

        @JsonProperty("creator")
        private String creator;

        @JsonProperty("favourite_count")
        private long favouriteCount;

        @JsonProperty("hype")
        private BeatmapHype hype;

        @JsonProperty("id")
        private long id;

        @JsonProperty("nsfw")
        private boolean isNsfw;

        @JsonProperty("offset")
        private long offset;

        @JsonProperty("play_count")
        private long playCount;

        @JsonProperty("preview_url")
        private String previewUrl;

        @JsonProperty("source")
        private String source;

        @JsonProperty("spotlight")
        private boolean spotlight;

        @JsonProperty("status")
        private String status;

        @JsonProperty("title")
        private String title;

        @JsonProperty("title_unicode")
        private String titleUnicode;

        @JsonProperty("user_id")
        private long userId;

        @JsonProperty("video")
        private boolean video;

        @JsonProperty("availability")
        private BeatmapAvailability availability;

        @JsonProperty("bpm")
        private long bpm;

        @JsonProperty("can_be_hyped")
        private boolean canBeHyped;

        @JsonProperty("discussion_enabled")
        private boolean discussionEnabled;

        @JsonProperty("discussion_locked")
        private boolean discussionLocked;

        @JsonProperty("is_scoreable")
        private boolean isScoreable;

        @JsonProperty("last_updated")
        private String lastUpdated;

        @JsonProperty("legacy_thread_url")
        private URI legacyThreadUrl;

        @JsonProperty("nominations_summary")
        private NominationsSummary nominationsSummary;

        @JsonProperty("ranked")
        private long ranked;

        @JsonProperty("ranked_date")
        private String rankedDate;

        @JsonProperty("storyboard")
        private boolean storyboard;

        @JsonProperty("submitted_date")
        private String submittedDate;

        @JsonProperty("tags")
        private String tags;

        @JsonProperty("ratings")
        private long[] ratings;

        @JsonProperty("beatmaps")
        private ApiV2Beatmap[] beatmaps;

    }

    @Data
    public static class BeatmapAvailability {
        @JsonProperty("download_disabled")
        private boolean downloadDisabled;

        @JsonProperty("more_information")
        private String moreInformation;
    }

    @Data
    public static class BeatmapCovers {
        @JsonProperty("cover")
        private String cover;

        @JsonProperty("cover@2x")
        private String cover2x;

        @JsonProperty("card")
        private String card;

        @JsonProperty("card@2x")
        private String card2x;

        @JsonProperty("list")
        private String list;

        @JsonProperty("list@2x")
        private String list2x;

        @JsonProperty("slimcover")
        private String slimCover;

        @JsonProperty("slimcover@2x")
        private String slimCover2x;
    }

    @Data
    public static class BeatmapHype {
        @JsonProperty("current")
        private int current;

        @JsonProperty("required")
        private int required;
    }

    @Data
    public static class NominationsSummary {
        @JsonProperty("current")
        private int current;

        @JsonProperty("required")
        private int required;
    }


}