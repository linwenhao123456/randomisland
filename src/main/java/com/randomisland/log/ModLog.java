package com.randomisland.log;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * mod 专用运行日志：同时输出到 Minecraft 控制台和独立文件。
 *
 * <p>文件位置：{@code .minecraft/config/random_island/mod_running_log.txt}
 * （每次启动追加，并写入会话分隔头，便于区分多次运行）
 */
public final class ModLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("random_island");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FILE_NAME = "mod_running_log.txt";

    private static Path logFile;
    private static boolean ready;

    private ModLog() {
    }

    /** 初始化日志文件并写入会话头，返回日志文件路径（失败时返回 null）。 */
    public static synchronized Path init() {
        if (ready) {
            return logFile;
        }

        ready = true;
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("random_island");
            Files.createDirectories(dir);
            logFile = dir.resolve(FILE_NAME);

            StringBuilder sb = new StringBuilder();
            sb.append(System.lineSeparator());
            sb.append("==================== 随机空岛 mod 运行日志 ====================")
                    .append(System.lineSeparator());
            sb.append("会话开始时间 : ").append(LocalDateTime.now().format(TS)).append(System.lineSeparator());
            sb.append("Minecraft    : ").append(versionOf("minecraft")).append(System.lineSeparator());
            sb.append("本 mod 版本  : ").append(versionOf("random_island")).append(System.lineSeparator());
            sb.append("Fabric Loader: ").append(versionOf("fabricloader")).append(System.lineSeparator());
            sb.append("Java         : ").append(System.getProperty("java.version"))
                    .append(" (").append(System.getProperty("os.name")).append(")")
                    .append(System.lineSeparator());
            sb.append("日志文件     : ").append(logFile.toAbsolutePath()).append(System.lineSeparator());
            sb.append("提示         : 空岛仅在创建世界时选择「随机空岛（虚空）」预设才会生成。")
                    .append(System.lineSeparator());
            sb.append("------------------------------------------------------------")
                    .append(System.lineSeparator());

            append(sb.toString());
        } catch (Exception e) {
            LOGGER.warn("[随机空岛] 初始化日志文件失败，将只输出到控制台", e);
            logFile = null;
        }
        return logFile;
    }

    private static void append(String text) {
        try {
            Files.writeString(logFile, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // 写文件失败时静默降级为仅控制台输出，避免日志本身引发崩溃
            logFile = null;
        }
    }

    public static Path getLogFile() {
        return logFile;
    }

    public static void info(String message) {
        LOGGER.info(message);
        write("INFO", message, null);
    }

    /** 支持 {@link String#format} 的 {@code %s} 占位符。 */
    public static void info(String format, Object... args) {
        info(format(format, args));
    }

    public static void warn(String message) {
        LOGGER.warn(message);
        write("WARN", message, null);
    }

    /** 支持 {@link String#format} 的 {@code %s} 占位符。 */
    public static void warn(String format, Object... args) {
        warn(format(format, args));
    }

    public static void error(String message, Throwable t) {
        LOGGER.error(message, t);
        write("ERROR", message, t);
    }

    private static String format(String format, Object... args) {
        try {
            return String.format(format, args);
        } catch (Exception e) {
            return format;
        }
    }

    private static String versionOf(String modId) {
        try {
            ModContainer container = FabricLoader.getInstance().getModContainer(modId).orElse(null);
            if (container == null) {
                return "未知";
            }
            return container.getMetadata().getVersion().getFriendlyString();
        } catch (Exception e) {
            return "未知";
        }
    }

    private static synchronized void write(String level, String message, Throwable t) {
        if (logFile == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(LocalDateTime.now().format(TS)).append("] [").append(level).append("] ")
                .append(message).append(System.lineSeparator());
        if (t != null) {
            sb.append("    异常: ").append(t).append(System.lineSeparator());
        }
        append(sb.toString());
    }
}