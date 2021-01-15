package config;

import java.util.ArrayList;

public class Config {
    //Config version not written to file
    public static final int CURRENT_CFG_VERSION = 1;
    //Config version written in file
    public int cfgVersion = CURRENT_CFG_VERSION;

    public long cycleDuration = (long) 8.64e+7;
    public long cycleDelay = 0L;

    public String dbUsername = "example";
    public String dbPass = "example";
    public String dbHost = "localhost";
    public String dbPort = "3306";
    public String dbTimezone = "Europe/Berlin";
    public ArrayList<String> databases = new ArrayList<>();
    public String tempPath = System.getProperty("java.io.tmpdir");

    public boolean addIfNotExists = true;

    public boolean saveToFile = true;
    public String path = "backups/";

    public boolean enableEmail = false;
    public String mailServer = "mail.example.com";
    public String mailPort = "25";
    public String mailUser = "example";
    public String mailPass = "example";
    public String sendAs = "backup@example.com";
    public String sendTo = "user@example.com";
    public String mailSubject = "Backup of {db}";
    public String mailMessage = "Daily Database-Backup of database {db} is finished";

    public boolean uploadToCloud = false;
    public String baseUrl = "http://files.example.com/uploads/";
}
