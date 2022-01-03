package com.velocitypowered.proxy.console;

import com.velocitypowered.darkcode.LogManager;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class SimpleTerminalConsole {

    protected abstract boolean isRunning();
    protected abstract void runCommand(String command);
    protected abstract void shutdown();
    protected void processInput(String input) {
        String command = input.trim();
        if (!command.isEmpty()) {
            runCommand(command);
        }
    }

    protected LineReader buildReader(LineReaderBuilder builder) {
        LineReader reader = builder.build();
        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
        reader.unsetOpt(LineReader.Option.INSERT_TAB);
        return reader;
    }

    public void start() {
        try {
            readCommands(System.in);
        } catch (IOException e) {
            LogManager.getLogger(SimpleTerminalConsole.class).error("Failed to read console input", e);
        }
    }

    private void readCommands(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while (isRunning() && (line = reader.readLine()) != null) {
                processInput(line);
            }
        }
    }
}
