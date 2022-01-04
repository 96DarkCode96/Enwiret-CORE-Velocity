package com.velocitypowered.darkcode;

import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Objects;
import java.util.regex.Pattern;

public class Logger {

    private static final ReplacementSpecification[] REPLACEMENTS = new ReplacementSpecification[]{
            compile(ChatColor.BLACK, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).boldOff().toString()),
            compile(ChatColor.DARK_BLUE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).boldOff().toString()),
            compile(ChatColor.DARK_GREEN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).boldOff().toString()),
            compile(ChatColor.DARK_AQUA, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).boldOff().toString()),
            compile(ChatColor.DARK_RED, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).boldOff().toString()),
            compile(ChatColor.DARK_PURPLE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff().toString()),
            compile(ChatColor.GOLD, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).boldOff().toString()),
            compile(ChatColor.GRAY, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).boldOff().toString()),
            compile(ChatColor.DARK_GRAY, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold().toString()),
            compile(ChatColor.BLUE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).bold().toString()),
            compile(ChatColor.GREEN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).bold().toString()),
            compile(ChatColor.AQUA, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).bold().toString()),
            compile(ChatColor.RED, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).bold().toString()),
            compile(ChatColor.LIGHT_PURPLE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).bold().toString()),
            compile(ChatColor.YELLOW, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).bold().toString()),
            compile(ChatColor.WHITE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).bold().toString()),
            compile(ChatColor.MAGIC, Ansi.ansi().a(Ansi.Attribute.BLINK_SLOW).toString()),
            compile(ChatColor.BOLD, Ansi.ansi().a(Ansi.Attribute.UNDERLINE_DOUBLE).toString()),
            compile(ChatColor.STRIKETHROUGH, Ansi.ansi().a(Ansi.Attribute.STRIKETHROUGH_ON).toString()),
            compile(ChatColor.UNDERLINE, Ansi.ansi().a(Ansi.Attribute.UNDERLINE).toString()),
            compile(ChatColor.ITALIC, Ansi.ansi().a(Ansi.Attribute.ITALIC).toString()),
            compile(ChatColor.RESET, "\u001B[0m")
    };
    private static String DEFAULT_COLOR = "\u001B[36m";
    private static String RESET = "\u001B[0m";
    private static String INFO = DEFAULT_COLOR + "[%1$s] %2$s : \u001B[36m%3$s" + RESET;
    private static String DEBUG = DEFAULT_COLOR + "[%1$s] %2$s : \u001B[32m%3$s" + RESET;
    private static String ERROR = DEFAULT_COLOR + "[%1$s] %2$s : \u001B[31m%3$s" + RESET;
    private static String WARN = DEFAULT_COLOR + "[%1$s] %2$s : \u001B[33m%3$s" + RESET;
    private String name;
    private Logger.Level level;
    private PrintStream p;
    private boolean debug = true;

    public Logger(String name, Level level) {
        this.name = name;
        this.level = level;
        this.p = new PrintStream(new OutputStream() {
            StringBuilder a = new StringBuilder();
            @Override
            public void write(int b) {
                if(b == '\n'){
                    error(a.toString());
                    a = new StringBuilder();
                    return;
                }
                a.append(new String(new byte[]{(byte)b}));
            }
        });
    }

    private static ReplacementSpecification compile(ChatColor color, String ansi) {
        return new ReplacementSpecification(Pattern.compile("(?i)" + color.toString()), ansi);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public PrintStream getErrorStream(){
        return p;
    }

    public void debug(String msgFormat, Object... toFormat) {
        if (!debug) {
            return;
        }
        if(level.equals(Level.OFF)){
            return;
        }
        System.out.printf((DEBUG) + "%n", new SimpleDateFormat("kk:mm:ss").format(Calendar.getInstance().getTime()), name, format(msgFormat, toFormat));
    }

    public void error(String msgFormat, Object... toFormat) {
        if(level.equals(Level.OFF)){
            return;
        }
        System.out.printf((ERROR) + "%n", new SimpleDateFormat("kk:mm:ss").format(Calendar.getInstance().getTime()), name, format(msgFormat, toFormat));
    }

    public void warn(String msgFormat, Object... toFormat) {
        if(level.equals(Level.OFF)){
            return;
        }
        System.out.printf((WARN) + "%n", new SimpleDateFormat("kk:mm:ss").format(Calendar.getInstance().getTime()), name, format(msgFormat, toFormat));
    }

    public void info(String msgFormat, Object... toFormat) {
        if(level.equals(Level.OFF)){
            return;
        }
        System.out.printf((INFO) + "%n", new SimpleDateFormat("kk:mm:ss").format(Calendar.getInstance().getTime()), name, format(msgFormat, toFormat));
    }

    private Object format(String msgFormat, Object[] toFormat) {
        try {
            return colorFormat(MessageFormat.format(msgFormat, Arrays.stream(toFormat).map(Object::toString).toArray()));
        } catch (Exception e) {
            return colorFormat(msgFormat);
        }
    }

    private Object colorFormat(String msgFormat) {
        for (ReplacementSpecification replacement : REPLACEMENTS) {
            msgFormat = replacement.pattern.matcher(msgFormat).replaceAll(replacement.replacement);
        }
        return msgFormat;
    }

    public enum Level {
        OFF, ON;
    }

    private static class ReplacementSpecification {
        private final Pattern pattern;
        private final String replacement;

        public ReplacementSpecification(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }

        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof ReplacementSpecification)) return false;
            ReplacementSpecification other = (ReplacementSpecification) o;
            if (!other.canEqual(this)) return false;
            Object this$pattern = getPattern(), other$pattern = other.getPattern();
            if (!Objects.equals(this$pattern, other$pattern)) return false;
            Object this$replacement = getReplacement(), other$replacement = other.getReplacement();
            return Objects.equals(this$replacement, other$replacement);
        }

        protected boolean canEqual(Object other) {
            return other instanceof ReplacementSpecification;
        }

        public int hashCode() {
            int result = 1;
            Object $pattern = getPattern();
            result = result * 59 + (($pattern == null) ? 43 : $pattern.hashCode());
            Object $replacement = getReplacement();
            return result * 59 + (($replacement == null) ? 43 : $replacement.hashCode());
        }

        public String toString() {
            return "ColouredWriter.ReplacementSpecification(pattern=" + getPattern() + ", replacement=" + getReplacement() + ")";
        }


        public Pattern getPattern() {
            return this.pattern;
        }

        public String getReplacement() {
            return this.replacement;
        }
    }
}
