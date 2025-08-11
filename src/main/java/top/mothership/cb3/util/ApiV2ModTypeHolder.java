package top.mothership.cb3.util;

import java.util.*;

public class ApiV2ModTypeHolder {


    /**
     * osu!lazer Mod类型计算器
     * 根据mod缩写计算对应的mod类型
     * 基于 osu!lazer wiki 的mod分类系统
     */


    // Mod映射表
    private static final Map<String, ModInfo> MOD_MAP = new HashMap<>();

    static {
        // Difficulty Reduction mods
        addMod("EZ", "Easy", ModType.DIFFICULTY_REDUCTION);
        addMod("NF", "No Fail", ModType.DIFFICULTY_REDUCTION);
        addMod("HT", "Half Time", ModType.DIFFICULTY_REDUCTION);
        addMod("DC", "Daycore", ModType.DIFFICULTY_REDUCTION);
        addMod("NR", "No Release", ModType.DIFFICULTY_REDUCTION);

        // Difficulty Increase mods
        addMod("HR", "Hard Rock", ModType.DIFFICULTY_INCREASE);
        addMod("SD", "Sudden Death", ModType.DIFFICULTY_INCREASE);
        addMod("PF", "Perfect", ModType.DIFFICULTY_INCREASE);
        addMod("DT", "Double Time", ModType.DIFFICULTY_INCREASE);
        addMod("NC", "Nightcore", ModType.DIFFICULTY_INCREASE);

        addMod("FI", "Fade In", ModType.DIFFICULTY_INCREASE);
        addMod("HD", "Hidden", ModType.DIFFICULTY_INCREASE);
        addMod("CO", "Cover", ModType.DIFFICULTY_INCREASE);
        addMod("FL", "Flashlight", ModType.DIFFICULTY_INCREASE);
        addMod("BL", "Blinds", ModType.DIFFICULTY_INCREASE);
        addMod("ST", "Strict Tracking", ModType.DIFFICULTY_INCREASE);
        addMod("AC", "Accuracy Challenge", ModType.DIFFICULTY_INCREASE);




        // Automation mods
        addMod("AT", "Autoplay", ModType.AUTOMATION);
        addMod("CN", "Cinema", ModType.AUTOMATION);
        addMod("RX", "Relax", ModType.AUTOMATION);
        addMod("AP", "Autopilot", ModType.AUTOMATION);
        addMod("SO", "Spun Out", ModType.AUTOMATION);


        // Conversion mods
        addMod("TP", "Target Practice", ModType.CONVERSION);
        addMod("DA", "Difficulty Adjust", ModType.CONVERSION);
        addMod("CL", "Classic", ModType.CONVERSION);
        addMod("RD", "Random", ModType.CONVERSION);
        addMod("MR", "Mirror", ModType.CONVERSION);
        addMod("AL", "Alternate", ModType.CONVERSION);
        addMod("SW", "Swap", ModType.CONVERSION);
        addMod("SG", "Single Tap", ModType.CONVERSION);
        addMod("IN", "Invert", ModType.CONVERSION);
        addMod("CS", "Constant Speed", ModType.CONVERSION);
        addMod("HO", "Hold Off", ModType.CONVERSION);

        // Key mods (osu!mania)
        addMod("1K", "1K", ModType.CONVERSION);
        addMod("2K", "2K", ModType.CONVERSION);
        addMod("3K", "3K", ModType.CONVERSION);
        addMod("4K", "4K", ModType.CONVERSION);
        addMod("5K", "5K", ModType.CONVERSION);
        addMod("6K", "6K", ModType.CONVERSION);
        addMod("7K", "7K", ModType.CONVERSION);
        addMod("8K", "8K", ModType.CONVERSION);
        addMod("9K", "9K", ModType.CONVERSION);
        addMod("10K", "10K", ModType.CONVERSION);

        // Fun mods
        addMod("TR", "Transform", ModType.FUN);
        addMod("WG", "Wiggle", ModType.FUN);
        addMod("SI", "Spin In", ModType.FUN);

        addMod("GR", "Grow", ModType.FUN);
        addMod("DF", "Deflate", ModType.FUN);

        addMod("WU", "Wind Up", ModType.FUN);
        addMod("WD", "Wind Down", ModType.FUN);

        addMod("TC", "Traceable", ModType.FUN);
        addMod("BR", "Barrel Roll", ModType.FUN);

        addMod("AD", "Approach Different", ModType.FUN);
        addMod("FF", "Floating Fruits", ModType.FUN);

        addMod("MU", "Muted", ModType.FUN);
        addMod("NS", "No Scope", ModType.FUN);
        addMod("MG", "Magnetised", ModType.FUN);
        addMod("RP", "Repel", ModType.FUN);



        addMod("AS", "Adaptive Speed", ModType.CONVERSION);
        addMod("FR", "Freeze Frame", ModType.CONVERSION);
        addMod("BU", "Bubbles", ModType.CONVERSION);
        addMod("SY", "Synesthesia", ModType.CONVERSION);
        addMod("DP", "Depth", ModType.CONVERSION);
        addMod("BM", "Bloom", ModType.CONVERSION);


        // System mods
        addMod("TD", "Touch Device", ModType.SYSTEM);
        addMod("SV2", "Score V2", ModType.SYSTEM);
    }

    /**
     * 添加mod到映射表
     */
    private static void addMod(String abbreviation, String fullName, ModType type) {
        MOD_MAP.put(abbreviation.toUpperCase(), new ModInfo(abbreviation, fullName, type));
    }

    /**
     * 根据mod缩写获取mod类型
     *
     * @param modAbbreviation mod缩写（如: HD, HR, EZ等）
     * @return ModType 对应的mod类型，如果找不到返回null
     */
    public static ModType getModType(String modAbbreviation) {
        if (modAbbreviation == null || modAbbreviation.trim().isEmpty()) {
            return null;
        }

        ModInfo modInfo = MOD_MAP.get(modAbbreviation.toUpperCase().trim());
        return modInfo != null ? modInfo.getType() : null;
    }

    /**
     * 根据mod缩写获取完整的mod信息
     *
     * @param modAbbreviation mod缩写
     * @return ModInfo mod信息对象，如果找不到返回null
     */
    public static ModInfo getModInfo(String modAbbreviation) {
        if (modAbbreviation == null || modAbbreviation.trim().isEmpty()) {
            return null;
        }

        return MOD_MAP.get(modAbbreviation.toUpperCase().trim());
    }

    /**
     * 根据mod缩写获取CSS类名
     *
     * @param modAbbreviation mod缩写
     * @return CSS类名，如果找不到返回空字符串
     */
    public static String getCssClass(String modAbbreviation) {
        ModType type = getModType(modAbbreviation);
        return type != null ? type.getCssClass() : "";
    }

    /**
     * 获取所有支持的mod缩写列表
     *
     * @return 所有mod缩写的集合
     */
    public static Set<String> getAllSupportedMods() {
        return new HashSet<>(MOD_MAP.keySet());
    }

    /**
     * 获取指定类型的所有mod
     *
     * @param type mod类型
     * @return 该类型下所有mod的信息列表
     */
    public static List<ModInfo> getModsByType(ModType type) {
        List<ModInfo> result = new ArrayList<>();
        for (ModInfo modInfo : MOD_MAP.values()) {
            if (modInfo.getType() == type) {
                result.add(modInfo);
            }
        }
        return result;
    }

    /**
     * 批量获取多个mod的类型
     *
     * @param modAbbreviations mod缩写列表
     * @return mod缩写到类型的映射
     */
    public static Map<String, ModType> getModTypes(List<String> modAbbreviations) {
        Map<String, ModType> result = new HashMap<>();
        for (String mod : modAbbreviations) {
            ModType type = getModType(mod);
            if (type != null) {
                result.put(mod, type);
            }
        }
        return result;
    }

    /**
     * 检查mod是否存在
     *
     * @param modAbbreviation mod缩写
     * @return 如果mod存在返回true，否则返回false
     */
    public static boolean isValidMod(String modAbbreviation) {
        return getModType(modAbbreviation) != null;
    }

    // 使用示例和测试方法
    public static void main(String[] args) {
        // 测试单个mod
        System.out.println("HD mod type: " + getModType("HD"));
        System.out.println("EZ mod type: " + getModType("EZ"));
        System.out.println("AT mod type: " + getModType("AT"));

        // 测试CSS类名
        System.out.println("HD CSS class: " + getCssClass("HD"));
        System.out.println("CL CSS class: " + getCssClass("CL"));

        // 测试mod信息
        ModInfo hdInfo = getModInfo("HD");
        if (hdInfo != null) {
            System.out.printf("HD: %s (%s) - %s%n",
                    hdInfo.getFullName(),
                    hdInfo.getAbbreviation(),
                    hdInfo.getType().getDisplayName());
        }

        // 测试批量获取
        List<String> testMods = Arrays.asList("HD", "HR", "EZ", "AT", "CL", "UNKNOWN");
        Map<String, ModType> types = getModTypes(testMods);
        System.out.println("Batch mod types: " + types);

        // 打印某类型的所有mod
        System.out.println("\nDifficulty Increase mods:");
        getModsByType(ModType.DIFFICULTY_INCREASE).forEach(mod ->
                System.out.println("  " + mod.getAbbreviation() + " - " + mod.getFullName())
        );
    }

    /**
     * Mod类型枚举
     */
    public enum ModType {
        DIFFICULTY_REDUCTION("Difficulty Reduction", "diff-reduct"),
        DIFFICULTY_INCREASE("Difficulty Increase", "diff-inc"),
        AUTOMATION("Automation", "automation"),
        CONVERSION("Conversion", "conversion"),
        FUN("Fun", "fun"),
        SYSTEM("System", "sys");

        private final String displayName;
        private final String cssClass;

        ModType(String displayName, String cssClass) {
            this.displayName = displayName;
            this.cssClass = cssClass;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    /**
     * Mod信息类
     */
    public static class ModInfo {
        private final String abbreviation;
        private final String fullName;
        private final ModType type;

        public ModInfo(String abbreviation, String fullName, ModType type) {
            this.abbreviation = abbreviation;
            this.fullName = fullName;
            this.type = type;
        }

        public String getAbbreviation() {
            return abbreviation;
        }

        public String getFullName() {
            return fullName;
        }

        public ModType getType() {
            return type;
        }
    }
}
