package top.mothership.cb3.pojo.osu.apiv2.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.mothership.cb3.manager.constant.ApiV2ModeHolder;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import static top.mothership.cb3.manager.constant.ApiV2ModeHolder.*;


/**
 * 用户成绩响应模型
 */

@Data
@NoArgsConstructor
public class ApiV2Score {

    @Data
    public static class BeatmapScoreLazer {
        @JsonProperty("position")
        private int position;

        @JsonProperty("score")
        private ScoreLazer score;
    }

    @Data
    public static class ScoreLazer {
        @JsonProperty("accuracy")
        private double accuracy;

        @JsonProperty("beatmap_id")
        private long beatmapId;

        @JsonProperty("best_id")
        private Long bestId;

        @JsonProperty("build_id")
        private Long buildId;

        @JsonProperty("classic_total_score")
        private long classicTotalScore;

        @JsonProperty("ended_at")
        private String endedAt;

        @JsonProperty("has_replay")
        private boolean hasReplay;

        @JsonProperty("id")
        private long id;

        @JsonProperty("is_perfect_combo")
        private boolean isPerfectCombo;

        @JsonProperty("legacy_perfect")
        private boolean legacyPerfect;

        @JsonProperty("legacy_score_id")
        private Long legacyScoreId;

        @JsonProperty("legacy_total_score")
        private long legacyTotalScore;

        @JsonProperty("max_combo")
        private long maxCombo;

        @JsonProperty("maximum_statistics")
        private ScoreStatisticsLazer maximumStatistics;

        @JsonProperty("mods")
        private Mod[] mods;

        @JsonProperty("passed")
        private boolean passed;

        @JsonProperty("pp")
        private Double pp;

        @JsonProperty("preserve")
        private boolean preserve;

        @JsonProperty("processed")
        private boolean processed;

        @JsonProperty("rank")
        private String rank;

        @JsonProperty("ranked")
        private boolean ranked;

        @JsonProperty("ruleset_id")
        private int modeInt;

        @JsonProperty("started_at")
        private String startedAt;

        @JsonProperty("statistics")
        private ScoreStatisticsLazer statistics;

        @JsonProperty("total_score")
        private long score;

        @JsonProperty("type")
        private String kind;

        @JsonProperty("user_id")
        private long userId;

        // SoloScoreJsonAttributesMultiplayer
        @JsonProperty("playlist_item_id")
        private Long playlistItemId;

        @JsonProperty("room_id")
        private Long roomId;

        @JsonProperty("solo_score_id")
        private Long soloScoreId;

        // ScoreJsonAvailableIncludes
        @JsonProperty("beatmap")
        private ApiV2Beatmap.Beatmap beatmap;

        @JsonProperty("beatmapset")
        private ApiV2BeatmapSet.Beatmapset beatmapset;

        @JsonProperty("user")
        private ApiV2User.User user;

        @JsonProperty("weight")
        private ScoreWeight weight;

        @JsonProperty("match")
        private Match match;

        @JsonProperty("rank_country")
        private Long rankCountry;

        @JsonProperty("rank_global")
        private Long rankGlobal;

        // ScoreJsonDefaultIncludes
        @JsonProperty("current_user_attributes")
        private CurrentUserAttributes currentUserAttributes;

        // Tool properties (transient - not serialized)
        private boolean convertFromOld = false;

        // Cached values
        private String cachedJsonMods;
        private Double cachedLegacyAcc;
        private String cachedLegacyRank;
        private ScoreStatisticsLazer cachedConvertStatistics;

        // Computed properties
        public String getMode() {
            return ApiV2ModeHolder.fromInt(modeInt);
        }

        public boolean isLazer() {
            return startedAt != null;
        }

        public boolean isClassic() {
            return startedAt == null;
        }

        public long getScoreAuto() {
            return isClassic() ? legacyTotalScore : score;
        }

        public String getRankAuto() {
            return isClassic() ? getLegacyRank() : rank;
        }

        public double getAccAuto() {
            return isClassic() ? getLegacyAcc() : accuracy;
        }

        public ScoreStatisticsLazer getConvertStatistics() {
            if (cachedConvertStatistics != null) {
                return cachedConvertStatistics;
            }

            if (convertFromOld) {
                cachedConvertStatistics = statistics;
                return statistics;
            }

            if (Objects.equals(getMode(), FRUITS)) {
                cachedConvertStatistics = new ScoreStatisticsLazer();
                cachedConvertStatistics.setCountGreat(statistics.getCountGreat());
                cachedConvertStatistics.setCountOk(statistics.getLargeTickHit());
                cachedConvertStatistics.setCountMeh(statistics.getSmallTickHit());
                cachedConvertStatistics.setCountKatu(statistics.getSmallTickMiss());
                cachedConvertStatistics.setCountGeki(statistics.getCountGeki());
                cachedConvertStatistics.setCountMiss(statistics.getCountMiss() + statistics.getLargeTickMiss());
            } else {
                cachedConvertStatistics = statistics;
            }

            return cachedConvertStatistics;
        }

        public String getLegacyRank() {
            if (cachedLegacyRank != null) {
                return cachedLegacyRank;
            }

            if ("F".equals(this.rank)) {
                cachedLegacyRank = "F";
                return cachedLegacyRank;
            }

            switch (getMode()) {
                case OSU: {
                    long totalHits = statistics.totalHits(getMode());
                    double greatRate = totalHits > 0 ? (double) statistics.getCountGreat() / totalHits : 1.0;
                    double mehRate = totalHits > 0 ? (double) statistics.getCountMeh() / totalHits : 1.0;

                    if (greatRate == 1.0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "XH" : "X";
                    } else if (greatRate > 0.9 && mehRate <= 0.01 && statistics.getCountMiss() == 0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "SH" : "S";
                    } else if ((greatRate > 0.8 && statistics.getCountMiss() == 0) || greatRate > 0.9) {
                        cachedLegacyRank = "A";
                    } else if ((greatRate > 0.7 && statistics.getCountMiss() == 0) || greatRate > 0.8) {
                        cachedLegacyRank = "B";
                    } else if (greatRate > 0.6) {
                        cachedLegacyRank = "C";
                    } else {
                        cachedLegacyRank = "D";
                    }
                    break;
                }
                case TAIKO: {
                    long totalHits = statistics.totalHits(getMode());
                    double greatRate = totalHits > 0 ? (double) statistics.getCountGreat() / totalHits : 1.0;

                    if (greatRate == 1.0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "XH" : "X";
                    } else if (greatRate > 0.9 && statistics.getCountMiss() == 0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "SH" : "S";
                    } else if ((greatRate > 0.8 && statistics.getCountMiss() == 0) || greatRate > 0.9) {
                        cachedLegacyRank = "A";
                    } else if ((greatRate > 0.7 && statistics.getCountMiss() == 0) || greatRate > 0.8) {
                        cachedLegacyRank = "B";
                    } else if (greatRate > 0.6) {
                        cachedLegacyRank = "C";
                    } else {
                        cachedLegacyRank = "D";
                    }
                    break;
                }
                case FRUITS: {
                    double acc = statistics.accuracy(getMode());

                    if (acc == 1.0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "XH" : "X";
                    } else if (acc > 0.98) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "SH" : "S";
                    } else if (acc > 0.94) {
                        cachedLegacyRank = "A";
                    } else if (acc > 0.9) {
                        cachedLegacyRank = "B";
                    } else if (acc > 0.85) {
                        cachedLegacyRank = "C";
                    } else {
                        cachedLegacyRank = "D";
                    }
                    break;
                }
                case MANIA: {
                    double acc = statistics.accuracy(getMode());

                    if (acc == 1.0) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "XH" : "X";
                    } else if (acc > 0.95) {
                        cachedLegacyRank = Arrays.stream(mods).anyMatch(Mod::isVisualMod) ? "SH" : "S";
                    } else if (acc > 0.9) {
                        cachedLegacyRank = "A";
                    } else if (acc > 0.8) {
                        cachedLegacyRank = "B";
                    } else if (acc > 0.7) {
                        cachedLegacyRank = "C";
                    } else {
                        cachedLegacyRank = "D";
                    }
                    break;
                }
                default: {
                    cachedLegacyRank = rank;
                    break;
                }
            }

            return cachedLegacyRank;
        }

        public double getLegacyAcc() {
            if (cachedLegacyAcc != null) {
                return cachedLegacyAcc;
            }

            if (convertFromOld) {
                cachedLegacyAcc = accuracy;
                return accuracy;
            }

            cachedLegacyAcc = statistics.accuracy(getMode());
            return cachedLegacyAcc;
        }
    }

    @Data
    public static class Match implements Serializable {
        @JsonProperty("pass")
        private boolean pass;

        @JsonProperty("slot")
        private long slot;

        @JsonProperty("team")
        private long team;
    }

    @Data
    public static class CurrentUserAttributes implements Serializable {
        @JsonProperty("pin")
        private CurrentUserPin pin;
    }

    @Data
    public static class CurrentUserPin implements Serializable {
        @JsonProperty("is_pinned")
        private boolean isPinned;

        @JsonProperty("score_id")
        private long scoreId;
    }

    @Data
    public static class ScoreWeight implements Serializable {
        @JsonProperty("percentage")
        public double Percentage;

        @JsonProperty("pp")
        public double PP;
    }

    @Data
    public static class ScoreStatisticsLazer implements Serializable {
        @JsonProperty("ok")
        private long countOk;

        @JsonProperty("great")
        private long countGreat;

        @JsonProperty("meh")
        private long countMeh;

        @JsonProperty("perfect")
        private long countGeki;

        @JsonProperty("good")
        private long countKatu;

        @JsonProperty("miss")
        private long countMiss;

        @JsonProperty("large_tick_hit")
        private long largeTickHit;

        @JsonProperty("large_tick_miss")
        private long largeTickMiss;

        @JsonProperty("small_tick_hit")
        private long smallTickHit;

        @JsonProperty("small_tick_miss")
        private long smallTickMiss;

        @JsonProperty("ignore_hit")
        private long ignoreHit;

        @JsonProperty("ignore_miss")
        private long ignoreMiss;

        @JsonProperty("large_bonus")
        private long largeBonus;

        @JsonProperty("small_bonus")
        private long smallBonus;

        @JsonProperty("slider_tail_hit")
        private long sliderTailHit;

        @JsonProperty("combo_break")
        private long comboBreak;

        @JsonProperty("legacy_combo_increase")
        private long legacyComboIncrease;

        public long passedObjects(String mode) {
            switch (mode) {
                case OSU:
                    return countGreat + countOk + countMeh + countMiss;
                case TAIKO:
                    return countGreat + countOk + countMiss;
                case FRUITS:
                    return countGreat + countOk + countMiss;
                case MANIA:
                    return countGeki + countKatu + countGreat + countOk + countMeh + countMiss;
                default:
                    return 0;
            }
        }

        public long totalHits(String mode) {
            switch (mode) {
                case OSU:
                    return countGreat + countOk + countMeh + countMiss;
                case TAIKO:
                    return countGreat + countOk + countMiss;
                case FRUITS:
                    return smallTickHit + largeTickHit + countGreat + countMiss + smallTickMiss + largeTickMiss;
                case MANIA:
                    return countGeki + countKatu + countGreat + countOk + countMeh + countMiss;
                default:
                    return 0;
            }
        }

        public double accuracy(String mode) {
            long totalHits = totalHits(mode);

            if (totalHits == 0) {
                return 0.0;
            }

            switch (mode) {
                case OSU:
                    return (double) ((6 * countGreat) + (2 * countOk) + countMeh) / (double) (6 * totalHits);
                case TAIKO:
                    return (double) ((2 * countGreat) + countOk) / (double) (2 * totalHits);
                case FRUITS:
                    return (double) (smallTickHit + largeTickHit + countGreat) / (double) totalHits;
                case MANIA:
                    return (double) (6 * (countGeki + countGreat) + 4 * countKatu + 2 * countOk + countMeh) / (double) (6 * totalHits);
                default:
                    return 0;
            }
        }
    }


    @Data
    public static class Mod {
        @JsonProperty("acronym")
        private String acronym;

        @JsonProperty("settings")
        private ObjectNode settings;

        public static Mod fromString(String mod) {
            Mod modObj = new Mod();
            modObj.setAcronym(mod);
            return modObj;
        }

        public boolean isClassic() {
            return "CL".equals(acronym);
        }

        public boolean isVisualMod() {
            return "HD".equals(acronym) || "FL".equals(acronym);
        }

        public boolean isSpeedChangeMod() {
            return "DT".equals(acronym) || "NC".equals(acronym) ||
                    "HT".equals(acronym) || "DC".equals(acronym);
        }
    }
}