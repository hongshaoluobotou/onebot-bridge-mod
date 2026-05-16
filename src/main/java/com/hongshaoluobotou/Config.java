package com.hongshaoluobotou;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
    private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);
    private static final String CONFIG_FILE = "onebot-bridge-mod.toml";
    private static final String DEFAULT_CONFIG_RESOURCE = "/assets/onebot-bridge-mod/onebot-bridge-mod.toml";
    private static final Pattern TOML_PATTERN = Pattern.compile(
            "^\\s*([A-Za-z0-9_]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|(\\d+))\\s*(?:#.*)?$"
    ); // 匹配 key = "value" 或 key = 'value' 或 key = 123

    public String url = "127.0.0.1:3001";
    public String token = "";
    public long groupId = 123456789;

    // volatile保证多线程可见性
    private static volatile Config INSTANCE;

    public static Config get() {
        if (INSTANCE == null) {
            synchronized (Config.class) {
                if (INSTANCE == null) {
                    INSTANCE = load();
                }
            }
        }
        return INSTANCE;
    }

    private static Config load() {
        Path configDir = Path.of("config", "onebot-bridge-mod");
        Path configPath = configDir.resolve(CONFIG_FILE);

        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                LOGGER.error("无法创建配置目录", e);
            }
        }

        if (!Files.exists(configPath)) {
            try (InputStream templateStream = Config.class.getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
                if (templateStream != null) {
                    Files.copy(templateStream, configPath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("已从模板生成默认配置文件: {}", configPath);
                } else {
                    LOGGER.warn("未找到模板文件: {}", DEFAULT_CONFIG_RESOURCE);
                }
            } catch (IOException e) {
                LOGGER.error("复制模板配置文件失败", e);
            }
        }

        if (Files.exists(configPath)) {
            try {
                Config config = parseToml(configPath);
                LOGGER.info("已加载配置文件: {}", configPath);
                return config;
            } catch (Exception e) {
                LOGGER.error("读取配置文件失败，使用默认配置", e);
            }
        }

        return new Config();
    }

    private static Config parseToml(Path path) throws IOException {
        Config config = new Config();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                Matcher matcher = TOML_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2) != null ? matcher.group(2) :
                            matcher.group(3) != null ? matcher.group(3) :
                                    matcher.group(4);

                    switch (key) {
                        case "url" -> config.url = value;
                        case "token" -> config.token = value;
                        case "groupId" -> config.groupId = Long.parseLong(value);
                    }
                }
            }
        }
        return config;
    }

    public void reload() {
        INSTANCE = load();
    }
}