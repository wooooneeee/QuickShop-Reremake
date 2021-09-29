package org.maxgamer.quickshop.util.language.text;

import com.dumptruckman.bukkit.configuration.json.JsonConfiguration;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.util.MsgUtil;
import org.maxgamer.quickshop.util.Util;
import org.maxgamer.quickshop.util.language.text.distributions.Distribution;
import org.maxgamer.quickshop.util.language.text.distributions.crowdin.CrowdinOTA;
import org.maxgamer.quickshop.util.language.text.postprocessing.PostProcessor;
import org.maxgamer.quickshop.util.language.text.postprocessing.impl.ColorProcessor;
import org.maxgamer.quickshop.util.language.text.postprocessing.impl.FillerProcessor;
import org.maxgamer.quickshop.util.language.text.postprocessing.impl.PlaceHolderApiProcessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class TextManager {
    private final QuickShop plugin;
    private final Distribution distribution;
    // <File <Locale, Section>>
    private final Map<String, Map<String, JsonConfiguration>> locale2ContentMapping = new HashMap<>();
    private final static String languageFileCrowdin = "/master/src/main/resources/lang/%locale%/messages.json";
    public List<PostProcessor> postProcessors = new ArrayList<>();
    private JsonConfiguration bundledLang = new JsonConfiguration();

    public TextManager(QuickShop plugin) {
        this.plugin = plugin;
        this.distribution = new CrowdinOTA(plugin);
        load();
    }

    @NotNull
    private File getOverrideFilesFolder(@NotNull String crowdinPath) {
        File file = new File(crowdinPath);
        File folder = new File(new File(plugin.getDataFolder(),"overrides"), file.getName() + ".overrides");
        folder.mkdirs();
        return folder;
    }

    public void load() {
        plugin.getLogger().info("Checking for translation updates...");
        locale2ContentMapping.clear();
        postProcessors.clear();
        // Load mapping
        //for (String availableFile : distribution.getAvailableFiles()) {
        try {
            bundledLang.loadFromString(new String(IOUtils.toByteArray(new InputStreamReader(plugin.getResource("lang-original/messages.json")), StandardCharsets.UTF_8)));
        } catch (IOException | InvalidConfigurationException ex) {
            bundledLang = new JsonConfiguration();
            plugin.getLogger().log(Level.SEVERE, "Cannot load bundled language file from Jar, some strings may missing!", ex);
        }
        // init file mapping
        locale2ContentMapping.computeIfAbsent(languageFileCrowdin, e -> new HashMap<>());
        // Multi File and Multi-Language loader


        distribution.getAvailableLanguages().parallelStream().forEach(crowdinCode -> distribution.getAvailableFiles().parallelStream().forEach(crowdinFile -> {
            try {
                // load OTA text from Crowdin

                String minecraftCode = crowdinCode.toLowerCase(Locale.ROOT).replace("-", "_");

                Util.debugLog("Loading translation for locale: " + crowdinCode + " (" + minecraftCode + ")");
                JsonConfiguration configuration = new JsonConfiguration();
                try {
                    configuration.loadFromString(distribution.getFile(crowdinFile, crowdinCode));
                } catch (InvalidConfigurationException exception) {
                    configuration.loadFromString(distribution.getFile(crowdinFile, crowdinCode, true));
                }
                // load override text (allow user modification the translation)
                JsonConfiguration override = new JsonConfiguration();
                File localOverrideFile = new File(getOverrideFilesFolder(crowdinFile), minecraftCode + ".json");
                if (localOverrideFile.exists()) {
                    override.loadFromString(Util.readToString(localOverrideFile));
                    for (String key : override.getKeys(true)) {
                        if (key.equals("language-version") || key.equals("config-version") || key.equals("version"))
                            continue;
                        configuration.set(key, override.get(key));
                    }
                }
                locale2ContentMapping.get(languageFileCrowdin).computeIfAbsent(minecraftCode, e -> configuration);
                Util.debugLog("Locale " + crowdinFile);
                if (configuration.getInt("language-version") < bundledLang.getInt("language-version"))
                    Util.debugLog("Locale " + crowdinCode + " file version is outdated, some string will fallback to English.");
            } catch (CrowdinOTA.OTAException e) {
                plugin.getLogger().warning("Couldn't update the translation for locale " + crowdinCode + " because it not configured, please report to QuickShop");
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Couldn't update the translation for locale " + crowdinCode + " please check your network connection.", e);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Couldn't update the translation for locale " + crowdinCode + ".", e);
            }
        }));


//        for (String availableLanguage : distribution.getAvailableLanguages()) {
//
//        }
        postProcessors.add(new FillerProcessor());
        postProcessors.add(new ColorProcessor());
        postProcessors.add(new PlaceHolderApiProcessor());
    }

    public Text of(@NotNull String path, String... args) {
        return new Text(this, (CommandSender) null, locale2ContentMapping.get(languageFileCrowdin), path, args);
    }

    public Text of(@Nullable CommandSender sender, @NotNull String path, String... args) {
        return new Text(this, sender, locale2ContentMapping.get(languageFileCrowdin), path, args);
    }

    public Text of(@Nullable UUID sender, @NotNull String path, String... args) {
        return new Text(this, sender, locale2ContentMapping.get(languageFileCrowdin), path, args);
    }

    public TextList ofList(@NotNull String path, String... args) {
        return new TextList(this, (CommandSender) null, locale2ContentMapping.get(languageFileCrowdin), path, args);
    }

    public TextList ofList(@Nullable UUID sender, @NotNull String path, String... args) {
        return new TextList(this, sender, locale2ContentMapping.get(languageFileCrowdin), path, args);
    }

    public TextList ofList(@Nullable CommandSender sender, @NotNull String path, String... args) {
        return new TextList(this, sender, locale2ContentMapping.get(languageFileCrowdin), path, args);
    }

    public static class TextList {
        private final TextManager manager;
        private final String path;
        private final QuickShop plugin;
        private final Map<String, JsonConfiguration> mapping;
        private final CommandSender sender;
        private final String[] args;

        private TextList(TextManager manager, CommandSender sender, Map<String, JsonConfiguration> mapping, String path, String... args) {
            this.plugin = manager.plugin;
            this.manager = manager;
            this.sender = sender;
            this.mapping = mapping;
            this.path = path;
            this.args = args;
        }

        private TextList(TextManager manager, UUID sender, Map<String, JsonConfiguration> mapping, String path, String... args) {
            this.plugin = manager.plugin;
            this.manager = manager;
            if (sender != null)
                this.sender = Bukkit.getPlayer(sender);
            else
                this.sender = null;
            this.mapping = mapping;
            this.path = path;
            this.args = args;
        }

        private @NotNull List<String> fallbackLocal() {
            return manager.bundledLang.getStringList(path);
        }

        @NotNull
        private List<String> postProcess(@NotNull List<String> text) {
            List<String> texts = new ArrayList<>();
            for (PostProcessor postProcessor : this.manager.postProcessors)
                for (String s : text) {
                    texts.add(postProcessor.process(s, sender, args));
                }
            return texts;
        }

        @NotNull
        public List<String> forLocale(@NotNull String locale) {
            JsonConfiguration index = mapping.get(locale);
            if (index == null) {
                if (locale.equals("en_us")) {
                    List<String> str = fallbackLocal();
                    if (str.isEmpty())
                        return Collections.singletonList("Fallback Missing Language Key: " + path + ", report to QuickShop!");
                    return postProcess(str);
                } else {
                    return forLocale("en_us");
                }
            } else {
                List<String> str = index.getStringList(path);
                if (str.isEmpty()) {
                    return Collections.singletonList("Missing Language Key: " + path);
                } else {
                    return postProcess(str);
                }
            }

        }

        @NotNull
        public List<String> forLocale() {
            if (sender instanceof Player) {
                return forLocale(((Player) sender).getLocale());
            } else {
                return forLocale("en_us");
            }
        }

        public void send() {
            if (sender == null)
                return;
            for (String s : forLocale()) {
                MsgUtil.sendDirectMessage(sender, s);
            }
        }
    }

    public static class Text {
        private final TextManager manager;
        private final String path;
        private final QuickShop plugin;
        private final Map<String, JsonConfiguration> mapping;
        private final CommandSender sender;
        private final String[] args;

        private Text(TextManager manager, CommandSender sender, Map<String, JsonConfiguration> mapping, String path, String... args) {
            this.plugin = manager.plugin;
            this.manager = manager;
            this.sender = sender;
            this.mapping = mapping;
            this.path = path;
            this.args = args;
        }

        private Text(TextManager manager, UUID sender, Map<String, JsonConfiguration> mapping, String path, String... args) {
            this.plugin = manager.plugin;
            this.manager = manager;
            if (sender != null)
                this.sender = Bukkit.getPlayer(sender);
            else
                this.sender = null;
            this.mapping = mapping;
            this.path = path;
            this.args = args;
        }

        @Nullable
        private String fallbackLocal() {
            return manager.bundledLang.getString(path);
        }

        @NotNull
        private String postProcess(@NotNull String text) {
            for (PostProcessor postProcessor : this.manager.postProcessors)
                text = postProcessor.process(text, sender, args);
            return text;
        }

        @NotNull
        public String forLocale(@NotNull String locale) {
            JsonConfiguration index = mapping.get(locale);
            if (index == null) {
                if (locale.equals("en_us")) {
                    String str = fallbackLocal();
                    if (str == null)
                        return "Fallback Missing Language Key: " + path + ", report to QuickShop!";
                    return postProcess(str);
                } else {
                    return forLocale("en_us");
                }
            } else {
                String str = index.getString(path);
                if (str == null)
                    return "Missing Language Key: " + path;
                return postProcess(str);
            }
        }

        @NotNull
        public String forLocale() {
            if (sender instanceof Player) {
                return forLocale(((Player) sender).getLocale());
            } else {
                return forLocale("en_us");
            }
        }

        public void send() {
            if (sender == null)
                return;
            String lang = forLocale();
            MsgUtil.sendDirectMessage(sender, lang);
            // plugin.getQuickChat().send(sender, lang);
        }
    }
}
