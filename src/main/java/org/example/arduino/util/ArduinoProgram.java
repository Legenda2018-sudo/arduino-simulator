package org.example.arduino.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small deterministic Arduino subset. No native code, blocking waits or arbitrary execution. */
public final class ArduinoProgram {
    public interface Pins {
        void mode(int pin, boolean output);
        void write(int pin, boolean high);
    }
    private record Command(String name, int pin, int value) { }
    private static final Pattern FUNCTION = Pattern.compile("\\s*void\\s+(setup|loop)\\s*\\(\\s*\\)\\s*\\{([^{}]*)}");
    private static final Pattern COMMAND = Pattern.compile("\\s*(pinMode|digitalWrite|delay)\\s*\\(([^()]*)\\)\\s*;");
    private final List<Command> setup;
    private final List<Command> loop;
    private boolean inSetup = true;
    private int position;
    private long resumeAt;

    private ArduinoProgram(List<Command> setup, List<Command> loop) {
        this.setup = setup;
        this.loop = loop;
    }

    public static ArduinoProgram compile(String source) {
        if (source == null || source.length() > 20000) {
            throw new IllegalArgumentException("Программа должна содержать не более 20000 символов");
        }
        String clean = source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\r\\n]*", " ");
        Matcher functions = FUNCTION.matcher(clean);
        int end = 0;
        List<Command> setup = null, loop = null;
        while (functions.find()) {
            if (!clean.substring(end, functions.start()).isBlank()) throw syntax();
            List<Command> body = parseBody(functions.group(2));
            if (functions.group(1).equals("setup")) {
                if (setup != null) throw syntax();
                setup = body;
            } else {
                if (loop != null) throw syntax();
                loop = body;
            }
            end = functions.end();
        }
        if (!clean.substring(end).isBlank() || setup == null || loop == null) throw syntax();
        return new ArduinoProgram(setup, loop);
    }

    private static List<Command> parseBody(String body) {
        List<Command> result = new ArrayList<>();
        Matcher commands = COMMAND.matcher(body);
        int end = 0;
        while (commands.find()) {
            if (!body.substring(end, commands.start()).isBlank()) throw syntax();
            String name = commands.group(1);
            String[] args = commands.group(2).split(",", -1);
            if (name.equals("delay")) {
                if (args.length != 1) throw syntax();
                int ms = integer(args[0]);
                if (ms < 0 || ms > 60000) throw new IllegalArgumentException("delay: от 0 до 60000 мс");
                result.add(new Command(name, 0, ms));
            } else {
                if (args.length != 2) throw syntax();
                int pin = args[0].trim().equals("LED_BUILTIN") ? 13 : integer(args[0]);
                if (pin < 0 || pin > 13) throw new IllegalArgumentException("Цифровой пин: от 0 до 13");
                String value = args[1].trim();
                boolean on;
                if (name.equals("pinMode")) {
                    if (!value.equals("OUTPUT") && !value.equals("INPUT")) throw syntax();
                    on = value.equals("OUTPUT");
                } else {
                    if (!List.of("HIGH", "LOW", "1", "0").contains(value)) throw syntax();
                    on = value.equals("HIGH") || value.equals("1");
                }
                result.add(new Command(name, pin, on ? 1 : 0));
            }
            if (result.size() > 1000) throw new IllegalArgumentException("Слишком много команд");
            end = commands.end();
        }
        if (!body.substring(end).isBlank()) throw syntax();
        return List.copyOf(result);
    }

    private static int integer(String value) {
        if (!value.trim().matches("[0-9]{1,6}")) throw syntax();
        return Integer.parseInt(value.trim());
    }

    private static IllegalArgumentException syntax() {
        return new IllegalArgumentException("Нужны void setup() и void loop(). Команды: pinMode(13, OUTPUT); digitalWrite(13, HIGH); delay(500); Поддерживаются только постоянные аргументы.");
    }

    public void reset() {
        inSetup = true;
        position = 0;
        resumeAt = 0;
    }

    /** Executes at most one loop iteration per frame; delay never sleeps on the UI thread. */
    public void tick(long nowMillis, Pins pins) {
        if (nowMillis < resumeAt) return;
        if (!inSetup && position >= loop.size()) position = 0;
        for (int budget = 0; budget < 2002; budget++) {
            List<Command> commands = inSetup ? setup : loop;
            if (position >= commands.size()) {
                position = 0;
                if (inSetup) { inSetup = false; continue; }
                return;
            }
            Command command = commands.get(position++);
            switch (command.name()) {
                case "pinMode" -> pins.mode(command.pin(), command.value() != 0);
                case "digitalWrite" -> pins.write(command.pin(), command.value() != 0);
                case "delay" -> {
                    resumeAt = nowMillis + command.value();
                    return;
                }
                default -> throw new IllegalStateException("Unknown command");
            }
        }
    }
}
