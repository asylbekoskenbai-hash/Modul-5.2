import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;

enum LogLevel {
    INFO, WARNING, ERROR
}

class Logger {
    private static volatile Logger instance;
    private static final Object lock = new Object();
    private LogLevel currentLevel;
    private String logFilePath;
    private long maxFileSize = 1024 * 1024;
    private int fileIndex = 0;

    private Logger() {
        this.currentLevel = LogLevel.INFO;
        this.logFilePath = "app.log";
        loadConfiguration();
    }

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Logger();
                }
            }
        }
        return instance;
    }

    private void loadConfiguration() {
        try {
            Path configPath = Paths.get("logger_config.txt");
            if (Files.exists(configPath)) {
                List<String> lines = Files.readAllLines(configPath);
                for (String line : lines) {
                    if (line.startsWith("level=")) {
                        String level = line.substring(6).trim();
                        this.currentLevel = LogLevel.valueOf(level);
                    } else if (line.startsWith("logfile=")) {
                        this.logFilePath = line.substring(8).trim();
                    } else if (line.startsWith("maxsize=")) {
                        this.maxFileSize = Long.parseLong(line.substring(8).trim()) * 1024;
                    }
                }
                System.out.println("Конфигурация жүктелді: " + currentLevel + ", " + logFilePath);
            }
        } catch (Exception e) {
            System.out.println("Конфигурация жүктеу қатесі, әдепкі мәндер қолданылады");
        }
    }

    public void setLogLevel(LogLevel level) {
        this.currentLevel = level;
        log("Лог деңгейі өзгертілді: " + level, LogLevel.INFO);
    }

    private void checkFileRotation() {
        try {
            Path path = Paths.get(logFilePath);
            if (Files.exists(path) && Files.size(path) > maxFileSize) {
                fileIndex++;
                Path rotatedPath = Paths.get("app_" + fileIndex + ".log");
                Files.move(path, rotatedPath);
                log("Файл ротацияланды: " + rotatedPath.getFileName(), LogLevel.INFO);
            }
        } catch (IOException e) {
            System.err.println("Файл ротациясы қатесі: " + e.getMessage());
        }
    }

    public void log(String message, LogLevel level) {
        synchronized (lock) {
            if (level.ordinal() >= currentLevel.ordinal()) {
                checkFileRotation();

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String threadName = Thread.currentThread().getName();
                String logMessage = String.format("[%s] [%s] [%s] %s%n", timestamp, threadName, level, message);

                try {
                    Files.write(Paths.get(logFilePath), logMessage.getBytes(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);

                    if (level == LogLevel.ERROR) {
                        System.err.print("[CONSOLE] " + logMessage);
                    } else {
                        System.out.print("[CONSOLE] " + logMessage);
                    }
                } catch (IOException e) {
                    System.err.println("Лог жазу қатесі: " + e.getMessage());
                }
            }
        }
    }

    public void logInfo(String message) {
        log(message, LogLevel.INFO);
    }

    public void logWarning(String message) {
        log(message, LogLevel.WARNING);
    }

    public void logError(String message) {
        log(message, LogLevel.ERROR);
    }
}

class LogReader {
    public void readAllLogs() throws IOException {
        readLogsWithFilter(null);
    }

    public void readLogsWithFilter(LogLevel filterLevel) throws IOException {
        Path path = Paths.get("app.log");
        if (!Files.exists(path)) {
            System.out.println("Лог файлы табылмады");
            return;
        }

        List<String> lines = Files.readAllLines(path);
        System.out.println("\n=== ЛОГ ФАЙЛЫ (" + (filterLevel == null ? "БАРЛЫҒЫ" : filterLevel) + ") ===");

        for (String line : lines) {
            if (filterLevel == null) {
                System.out.println(line);
            } else {
                String levelStr = "[" + filterLevel + "]";
                if (line.contains(levelStr)) {
                    System.out.println(line);
                }
            }
        }
        System.out.println("========================\n");
    }

    public void readLogsByTime(LocalDateTime from, LocalDateTime to) throws IOException {
        Path path = Paths.get("app.log");
        if (!Files.exists(path)) return;

        List<String> lines = Files.readAllLines(path);
        System.out.println("\n=== ЛОГТАР (" + from + " - " + to + ") ===");

        for (String line : lines) {
            try {
                if (line.length() > 20) {
                    String dateStr = line.substring(1, 20);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime logTime = LocalDateTime.parse(dateStr, formatter);

                    if (!logTime.isBefore(from) && !logTime.isAfter(to)) {
                        System.out.println(line);
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
    }
}

interface IReportBuilder {
    IReportBuilder setHeader(String header);
    IReportBuilder setContent(String content);
    IReportBuilder setFooter(String footer);
    IReportBuilder addSection(String sectionName, String sectionContent);
    IReportBuilder setStyle(ReportStyle style);
    Report getReport();
}

class ReportStyle {
    private String backgroundColor;
    private String fontColor;
    private int fontSize;
    private String fontFamily;

    public ReportStyle(String backgroundColor, String fontColor, int fontSize, String fontFamily) {
        this.backgroundColor = backgroundColor;
        this.fontColor = fontColor;
        this.fontSize = fontSize;
        this.fontFamily = fontFamily;
    }

    public String getBackgroundColor() { return backgroundColor; }
    public String getFontColor() { return fontColor; }
    public int getFontSize() { return fontSize; }
    public String getFontFamily() { return fontFamily; }
}

class Report {
    private String header;
    private String content;
    private String footer;
    private Map<String, String> sections;
    private ReportStyle style;

    public Report() {
        this.sections = new LinkedHashMap<>();
    }

    public void setHeader(String header) { this.header = header; }
    public void setContent(String content) { this.content = content; }
    public void setFooter(String footer) { this.footer = footer; }
    public void setStyle(ReportStyle style) { this.style = style; }

    public void addSection(String name, String content) {
        sections.put(name, content);
    }

    public String getHeader() { return header; }
    public String getContent() { return content; }
    public String getFooter() { return footer; }
    public Map<String, String> getSections() { return sections; }
    public ReportStyle getStyle() { return style; }

    public void exportToText() {
        System.out.println("\n--- ТЕКСТІК ФОРМАТ ---");
        System.out.println(header);
        System.out.println(content);
        for (Map.Entry<String, String> section : sections.entrySet()) {
            System.out.println("[" + section.getKey() + "]");
            System.out.println(section.getValue());
        }
        System.out.println(footer);
        System.out.println("--------------------\n");
    }

    public void exportToHtml() {
        System.out.println("\n--- HTML ФОРМАТ ---");
        System.out.println("<html><head><title>" + header + "</title>");
        if (style != null) {
            System.out.println("<style>body { background: " + style.getBackgroundColor() +
                    "; color: " + style.getFontColor() +
                    "; font-size: " + style.getFontSize() + "px; }</style>");
        }
        System.out.println("</head><body>");
        System.out.println("<h1>" + header + "</h1>");
        System.out.println("<p>" + content + "</p>");
        for (Map.Entry<String, String> section : sections.entrySet()) {
            System.out.println("<h2>" + section.getKey() + "</h2>");
            System.out.println("<p>" + section.getValue() + "</p>");
        }
        System.out.println("<footer>" + footer + "</footer>");
        System.out.println("</body></html>");
        System.out.println("--------------------\n");
    }
}

class TextReportBuilder implements IReportBuilder {
    private Report report;

    public TextReportBuilder() {
        this.report = new Report();
    }

    @Override
    public IReportBuilder setHeader(String header) {
        report.setHeader("=== " + header + " ===\n");
        return this;
    }

    @Override
    public IReportBuilder setContent(String content) {
        report.setContent(content + "\n");
        return this;
    }

    @Override
    public IReportBuilder setFooter(String footer) {
        report.setFooter("--- " + footer + " ---\n");
        return this;
    }

    @Override
    public IReportBuilder addSection(String sectionName, String sectionContent) {
        report.addSection(sectionName, "  " + sectionName + ":\n    " + sectionContent + "\n");
        return this;
    }

    @Override
    public IReportBuilder setStyle(ReportStyle style) {
        report.setStyle(style);
        return this;
    }

    @Override
    public Report getReport() {
        return report;
    }
}

class HtmlReportBuilder implements IReportBuilder {
    private Report report;

    public HtmlReportBuilder() {
        this.report = new Report();
    }

    @Override
    public IReportBuilder setHeader(String header) {
        report.setHeader("<h1>" + header + "</h1>\n");
        return this;
    }

    @Override
    public IReportBuilder setContent(String content) {
        report.setContent("<p>" + content + "</p>\n");
        return this;
    }

    @Override
    public IReportBuilder setFooter(String footer) {
        report.setFooter("<footer>" + footer + "</footer>\n");
        return this;
    }

    @Override
    public IReportBuilder addSection(String sectionName, String sectionContent) {
        report.addSection(sectionName, "<h2>" + sectionName + "</h2>\n<p>" + sectionContent + "</p>\n");
        return this;
    }

    @Override
    public IReportBuilder setStyle(ReportStyle style) {
        report.setStyle(style);
        return this;
    }

    @Override
    public Report getReport() {
        return report;
    }
}

class ReportDirector {
    public Report constructBasicReport(IReportBuilder builder, String header, String content, String footer) {
        return builder.setHeader(header)
                .setContent(content)
                .setFooter(footer)
                .getReport();
    }

    public Report constructComplexReport(IReportBuilder builder, String header, String content,
                                         String footer, ReportStyle style, Map<String, String> sections) {
        builder.setHeader(header).setContent(content).setFooter(footer).setStyle(style);
        for (Map.Entry<String, String> section : sections.entrySet()) {
            builder.addSection(section.getKey(), section.getValue());
        }
        return builder.getReport();
    }
}

class Weapon implements Cloneable {
    private String name;
    private int damage;
    private double speed;

    public Weapon(String name, int damage, double speed) {
        this.name = name;
        this.damage = damage;
        this.speed = speed;
    }

    public Weapon(Weapon other) {
        this.name = other.name;
        this.damage = other.damage;
        this.speed = other.speed;
    }

    @Override
    public Weapon clone() {
        return new Weapon(this);
    }

    @Override
    public String toString() {
        return name + " (зарпы: " + damage + ", жылд: " + speed + ")";
    }
}

class Armor implements Cloneable {
    private String name;
    private int defense;
    private int durability;

    public Armor(String name, int defense, int durability) {
        this.name = name;
        this.defense = defense;
        this.durability = durability;
    }

    public Armor(Armor other) {
        this.name = other.name;
        this.defense = other.defense;
        this.durability = other.durability;
    }

    @Override
    public Armor clone() {
        return new Armor(this);
    }

    @Override
    public String toString() {
        return name + " (қорғ: " + defense + ", төз: " + durability + ")";
    }
}

class Skill implements Cloneable {
    private String name;
    private int power;
    private int manaCost;
    private String type;

    public Skill(String name, int power, int manaCost, String type) {
        this.name = name;
        this.power = power;
        this.manaCost = manaCost;
        this.type = type;
    }

    public Skill(Skill other) {
        this.name = other.name;
        this.power = other.power;
        this.manaCost = other.manaCost;
        this.type = other.type;
    }

    @Override
    public Skill clone() {
        return new Skill(this);
    }

    @Override
    public String toString() {
        return name + " (" + type + ", күш: " + power + ", мана: " + manaCost + ")";
    }
}

class Character implements Cloneable {
    private String name;
    private int health;
    private int strength;
    private int agility;
    private int intelligence;
    private Weapon weapon;
    private Armor armor;
    private List<Skill> skills;

    public Character(String name, int health, int strength, int agility, int intelligence) {
        this.name = name;
        this.health = health;
        this.strength = strength;
        this.agility = agility;
        this.intelligence = intelligence;
        this.skills = new ArrayList<>();
    }

    public Character(Character other) {
        this.name = other.name + " (клон)";
        this.health = other.health;
        this.strength = other.strength;
        this.agility = other.agility;
        this.intelligence = other.intelligence;
        this.weapon = other.weapon != null ? other.weapon.clone() : null;
        this.armor = other.armor != null ? other.armor.clone() : null;
        this.skills = new ArrayList<>();
        for (Skill skill : other.skills) {
            this.skills.add(skill.clone());
        }
    }

    public void equipWeapon(Weapon weapon) {
        this.weapon = weapon;
    }

    public void equipArmor(Armor armor) {
        this.armor = armor;
    }

    public void addSkill(Skill skill) {
        skills.add(skill);
    }

    public void removeSkill(String skillName) {
        skills.removeIf(s -> s.toString().contains(skillName));
    }

    public void setHealth(int health) {
        this.health = health;
    }

    @Override
    public Character clone() {
        return new Character(this);
    }

    public void printInfo() {
        System.out.println("\n=== ПЕРСОНАЖ: " + name + " ===");
        System.out.println("Атрибуттар: Ден=" + health + ", Күш=" + strength +
                ", Епт=" + agility + ", Инт=" + intelligence);
        System.out.println("Қару: " + (weapon != null ? weapon : "жоқ"));
        System.out.println("Сауыт: " + (armor != null ? armor : "жоқ"));
        System.out.println("Қабілеттер:");
        for (Skill skill : skills) {
            System.out.println("  - " + skill);
        }
        System.out.println("====================\n");
    }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("МОДУЛЬ 05 ПРАКТИКАЛЫҚ ЖҰМЫС");
        System.out.println("========================================\n");

        System.out.println("--- 1. SINGLETON LOGGER ТЕСТІ ---\n");

        Runnable loggingTask = () -> {
            Logger logger = Logger.getInstance();
            String threadName = Thread.currentThread().getName();

            for (int i = 0; i < 3; i++) {
                logger.logInfo(threadName + " - INFO хабарлама " + i);
                logger.logWarning(threadName + " - WARNING хабарлама " + i);
                logger.logError(threadName + " - ERROR хабарлама " + i);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Thread t = new Thread(loggingTask, "Thread-" + i);
            threads.add(t);
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Logger.getInstance().setLogLevel(LogLevel.ERROR);
        Logger.getInstance().logInfo("Бұл хабарлама жазылмайды");
        Logger.getInstance().logError("Бұл ERROR хабарлама жазылады");

        try {
            LogReader logReader = new LogReader();
            logReader.readLogsWithFilter(LogLevel.ERROR);

            LocalDateTime now = LocalDateTime.now();
            logReader.readLogsByTime(now.minusMinutes(1), now);
        } catch (IOException e) {
            System.out.println("Лог оқу қатесі: " + e.getMessage());
        }

        System.out.println("\n--- 2. BUILDER PATTERN ТЕСТІ ---\n");

        ReportDirector director = new ReportDirector();
        ReportStyle style = new ReportStyle("#f0f0f0", "#333333", 12, "Arial");

        Map<String, String> sections = new HashMap<>();
        sections.put("Кіріспе", "Бұл есептің мақсаты - сатылымды талдау");
        sections.put("Статистика", "Қаңтар: 1.2M, Ақпан: 1.5M, Наурыз: 1.8M");
        sections.put("Қорытынды", "Өсім 50% құрады");

        TextReportBuilder textBuilder = new TextReportBuilder();
        Report textReport = director.constructComplexReport(
                textBuilder,
                "АЙЛЫҚ ЕСЕП",
                "2025 жылғы 1 тоқсан қорытындысы",
                "Бас бухгалтер",
                style,
                sections
        );
        textReport.exportToText();

        HtmlReportBuilder htmlBuilder = new HtmlReportBuilder();
        Report htmlReport = director.constructComplexReport(
                htmlBuilder,
                "АЙЛЫҚ ЕСЕП",
                "2025 жылғы 1 тоқсан қорытындысы",
                "Бас бухгалтер",
                style,
                sections
        );
        htmlReport.exportToHtml();

        System.out.println("\n--- 3. PROTOTYPE PATTERN ТЕСТІ ---\n");

        Character warrior = new Character("Айбар", 100, 20, 15, 10);
        warrior.equipWeapon(new Weapon("Қылыш", 50, 1.5));
        warrior.equipArmor(new Armor("Темір сауыт", 30, 100));
        warrior.addSkill(new Skill("Қатты соққы", 40, 10, "физикалық"));
        warrior.addSkill(new Skill("Айқай", 0, 5, "физикалық"));

        System.out.println("БАСТАПҚЫ ПЕРСОНАЖ:");
        warrior.printInfo();

        Character warriorClone = warrior.clone();
        System.out.println("КЛОНДАЛҒАН ПЕРСОНАЖ (өзгертулер енгізілген):");

        warriorClone.setHealth(80);
        warriorClone.equipWeapon(new Weapon("Алмас қылыш", 70, 1.8));
        warriorClone.removeSkill("Айқай");
        warriorClone.addSkill(new Skill("Отты соққы", 60, 25, "магиялық"));

        warriorClone.printInfo();

        System.out.println("БАСТАПҚЫ ПЕРСОНАЖ (өзгермеген):");
        warrior.printInfo();

        System.out.println("original == cloned? " + (warrior == warriorClone));

    }
}