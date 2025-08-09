package top.mothership.cb3.pojo.osu.apiv2.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Data;

import java.net.URI;

public class ApiV2User {

    @Data
    public static class UserExtended extends User {
        @JsonProperty("discord")
        private String discord;

        @JsonProperty("has_supported")
        private boolean hasSupported;

        @JsonProperty("interests")
        private String interests;

        @JsonProperty("join_date")
        private String joinDate;

        @JsonProperty("kudosu")
        private ObjectNode kudosu;

        @JsonProperty("location")
        private String location;

        @JsonProperty("max_blocks")
        private long maxBlocks;

        @JsonProperty("max_friends")
        private long maxFriends;

        @JsonProperty("occupation")
        private String occupation;

        /**
         * 官网用户的默认游玩模式
         */
        @JsonProperty("playmode")
        private String mode;

        @JsonProperty("playstyle")
        private String[] playstyle;

        @JsonProperty("post_count")
        private long postCount;

        @JsonProperty("profile_hue")
        private Long profileHue;

        @JsonProperty("profile_order")
        private String[] profileOrder;

        @JsonProperty("title")
        private String title;

        @JsonProperty("title_url")
        private String titleUrl;

        @JsonProperty("twitter")
        private String twitter;

        @JsonProperty("website")
        private String website;

        @JsonProperty("comments_count")
        private long commentsCount;

        @JsonProperty("mapping_follower_count")
        private Long mappingFollowerCount;

        @Override
        @JsonIgnore
        public UserStatistics getStatistics() {
            if (getStatisticsCurrent() != null) {
                return getStatisticsCurrent();
            }

            switch (mode) {
                case "osu":
                    return getStatisticsModes().getOsu();
                case "taiko":
                    return getStatisticsModes().getTaiko();
                case "fruits":
                    return getStatisticsModes().getFruits();
                case "mania":
                    return getStatisticsModes().getMania();
                default:
                    throw new IllegalArgumentException("Invalid mode: " + mode);
            }
        }
    }

    @Data
    public static class User {
        @JsonProperty("avatar_url")
        private URI avatarUrl;

        @JsonProperty("country_code")
        private String countryCode;

        @JsonProperty("default_group")
        private String defaultGroup;

        @JsonProperty("id")
        private long id;

        @JsonProperty("is_active")
        private boolean isActive;

        @JsonProperty("is_bot")
        private boolean isBot;

        @JsonProperty("is_deleted")
        private boolean isDeleted;

        @JsonProperty("is_online")
        private boolean isOnline;

        @JsonProperty("is_supporter")
        private boolean isSupporter;

        @JsonProperty("last_visit")
        private String lastVisit;

        @JsonProperty("pm_friends_only")
        private boolean pmFriendsOnly;

        @JsonProperty("profile_colour")
        private String profileColor;

        @JsonProperty("username")
        private String username;

        // UserJsonAvailableIncludes
        @JsonProperty("account_history")
        private UserAccountHistory[] accountHistory;

        @JsonProperty("badges")
        private UserBadge[] badges;

        @JsonProperty("beatmap_playcounts_count")
        private Long beatmapPlaycountsCount;

        @JsonProperty("country")
        private Country country;

        @JsonProperty("cover")
        private UserCover cover;

        @JsonProperty("favourite_beatmapset_count")
        private Long favouriteBeatmapsetCount;

        @JsonProperty("follower_count")
        private Long followerCount;

        @JsonProperty("graveyard_beatmapset_count")
        private Long graveyardBeatmapsetCount;

        @JsonProperty("guest_beatmapset_count")
        private Long guestBeatmapsetCount;

        @JsonProperty("loved_beatmapset_count")
        private Long lovedBeatmapsetCount;

        @JsonProperty("pending_beatmapset_count")
        private Long pendingBeatmapsetCount;

        @JsonProperty("ranked_beatmapset_count")
        private Long rankedBeatmapsetCount;

        @JsonProperty("groups")
        private UserGroup[] groups;

        @JsonProperty("rank_highest")
        private UserHighestRank highestRank;

        @JsonProperty("is_admin")
        private Boolean isAdmin;

        @JsonProperty("is_bng")
        private Boolean isBng;

        @JsonProperty("is_full_bn")
        private Boolean isFullBn;

        @JsonProperty("is_gmt")
        private Boolean isGmt;

        @JsonProperty("is_limited_bn")
        private Boolean isLimitedBn;

        @JsonProperty("is_moderator")
        private Boolean isModerator;

        @JsonProperty("is_nat")
        private Boolean isNat;

        @JsonProperty("is_restricted")
        private Boolean isRestricted;

        @JsonProperty("is_silenced")
        private Boolean isSilenced;

        @JsonProperty("medals")
        private MedalCompact[] medals;

        @JsonProperty("monthly_playcounts")
        private MonthlyCount[] monthlyPlaycounts;

        @JsonProperty("page")
        private UserPage page;

        @JsonProperty("previous_usernames")
        private String[] previousUsernames;

        // 搞不懂为啥这里ppy要给两个rankhistory
        @JsonProperty("rank_history")
        private RankHistory rankHistory;

        @JsonProperty("replays_watched_counts")
        private MonthlyCount[] replaysWatchedCounts;

        @JsonProperty("scores_best_count")
        private Long scoresBestCount;

        @JsonProperty("scores_first_count")
        private Long scoresFirstCount;

        @JsonProperty("scores_recent_count")
        private Long scoresRecentCount;

        @JsonProperty("scores_pinned_count")
        private Long scoresPinnedCount;

        @JsonProperty("statistics")
        private UserStatistics statisticsCurrent;

        @JsonProperty("statistics_rulesets")
        private UserStatisticsModes statisticsModes;

        @JsonProperty("support_level")
        private Long supportLevel;

        @JsonProperty("active_tournament_banners")
        private JsonArray activeTournamentBanners;

        @JsonProperty("active_tournament_banner")
        private JsonObject activeTournamentBanner;
        @JsonIgnore
        public UserStatistics getStatistics() {
            return statisticsCurrent != null ? statisticsCurrent : statisticsModes.getOsu();
        }
    }

    @Data
    public static class Country {
        @JsonProperty("code")
        private String code;

        @JsonProperty("display")
        private String display;

        @JsonProperty("name")
        private String name;
    }

    @Data
    public static class UserCover {
        @JsonProperty("custom_url")
        private URI customUrl;

        @JsonProperty("url")
        private URI url;

        @JsonProperty("id")
        private String id;
    }

    @Data
    public static class UserPage {
        @JsonProperty("html")
        private String html;

        @JsonProperty("raw")
        private String raw;
    }

    @Data
    public static class UserHighestRank {
        @JsonProperty("rank")
        private long rank;

        @JsonProperty("updated_at")
        private String updatedAt;
    }

    @Data
    public static class RankHistory {
        @JsonProperty("mode")
        private String  mode;

        @JsonProperty("data")
        private long[] data;
    }

    @Data
    public static class UserStatisticsModes {
        @JsonProperty("osu")
        private UserStatistics osu;

        @JsonProperty("taiko")
        private UserStatistics taiko;

        @JsonProperty("fruits")
        private UserStatistics fruits;

        @JsonProperty("mania")
        private UserStatistics mania;
    }

    @Data
    public static class UserStatistics {
        @JsonProperty("level")
        private UserLevel level;

        @JsonProperty("global_rank")
        private long globalRank;

        @JsonProperty("pp")
        private double pp;

        @JsonProperty("ranked_score")
        private long rankedScore;

        @JsonProperty("hit_accuracy")
        private double hitAccuracy;

        @JsonProperty("play_count")
        private long playCount;

        @JsonProperty("play_time")
        private long playTime;

        @JsonProperty("total_score")
        private long totalScore;

        @JsonProperty("total_hits")
        private long totalHits;

        @JsonProperty("maximum_combo")
        private long maximumCombo;

        @JsonProperty("replays_watched_by_others")
        private long replaysWatchedByOthers;

        @JsonProperty("is_ranked")
        private boolean isRanked;

        @JsonProperty("grade_counts")
        private UserGradeCounts gradeCounts;

        @JsonProperty("country_rank")
        private long countryRank;

        @JsonProperty("rank")
        private UserRank rank;
    }

    @Data
    public static class UserGradeCounts {
        @JsonProperty("ss")
        private int ss;

        @JsonProperty("ssh")
        private int ssh;

        @JsonProperty("s")
        private int s;

        @JsonProperty("sh")
        private int sh;

        @JsonProperty("a")
        private int a;
    }

    @Data
    public static class UserGroup {
        @JsonProperty("colour")
        private String color;

        @JsonProperty("description")
        private String description;

        @JsonProperty("has_playmodes")
        private boolean hasModes;

        @JsonProperty("id")
        private long id;

        @JsonProperty("identifier")
        private String identifier;

        @JsonProperty("is_probationary")
        private boolean isProbationary;

        @JsonProperty("playmodes")
        private String[] modes;

        @JsonProperty("name")
        private String name;

        @JsonProperty("short_name")
        private String shortName;
    }

    @Data
    public static class UserLevel {
        @JsonProperty("current")
        private int current;

        @JsonProperty("progress")
        private int progress;
    }

    @Data
    public static class MonthlyCount {
        @JsonProperty("start_date")
        private String startDate;

        @JsonProperty("count")
        private int count;
    }

    @Data
    public static class UserRank {
        @JsonProperty("country")
        private int country;
    }


    @Data
    public static class UserBadge {
        @JsonProperty("awarded_at")
        private String awardedAt;

        @JsonProperty("description")
        private String description;

        @JsonProperty("image_url")
        private URI imageUrl;

        @JsonProperty("url")
        private URI url;
    }

    @Data
    public static class UserAccountHistory {
        @JsonProperty("id")
        private Long id;

        @JsonProperty("timestamp")
        private String time;

        @JsonProperty("description")
        private String description;

        @JsonProperty("type")
        private HistoryType historyType;

        @JsonProperty("length")
        private long seconds;

        @JsonProperty("permanent")
        private boolean permanent;
    }

    public enum HistoryType {
        NOTE("note"),
        RESTRICTION("restriction"),
        TOURNAMENT_BAN("tournament_ban"),
        SILENCE("silence");

        private final String description;

        HistoryType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static HistoryType fromDescription(String description) {
            for (HistoryType type : values()) {
                if (type.description.equals(description)) {
                    return type;
                }
            }
            return NOTE; // default
        }
    }

    // Placeholder class that would need to be implemented
    public static class MedalCompact { }
}