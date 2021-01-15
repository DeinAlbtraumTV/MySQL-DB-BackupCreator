package core;

import com.smattme.MysqlExportService;
import config.ConfigController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.commons.FileUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import static config.ConfigController.cfg;

/*
*
* Created by Jasper Gomes
* 2020
*
 */

public class BackupCreator {

    private static final Logger logger = LoggerFactory.getLogger(BackupCreator.class);

    public static void main(String[] args) {
        ConfigController.loadConfig();
        if (cfg.cycleDuration < 3.6e+6) logger.warn("Backup-Frequency is under one hour, may have a severe impact on your performance!");
        if (!cfg.saveToFile && !cfg.enableEmail) logger.warn("No Method for Backups to be saved with is enabled, Backups will be discarded");
        if (cfg.dbHost.isEmpty()) {
            logger.error("No DB-Host set, exiting");
            System.exit(1);
        }
        if (cfg.dbUsername.isEmpty()) {
            logger.error("No DB-Username set, exiting");
            System.exit(1);
        }
        if (cfg.dbPass.isEmpty()) {
            logger.error("No DB-Pass set, exiting");
            System.exit(1);
        }
        if (cfg.databases.isEmpty())
            logger.warn("No databases set, no Backups for Databases will be created");
        if (!cfg.enableEmail)
            logger.info("Sending the Backup via email is disabled, no mail will be send");
        File file = new File(cfg.tempPath);
        if (!file.exists())
            if (file.mkdir()) {
                logger.info("temp-dir created");
                logger.info(file.getAbsolutePath());
            } else {
                logger.warn("failed to create temp-dir, things may not work as expected");
            }
        else {
            logger.info("The temp-dir is set to the following path");
            logger.info(file.getAbsolutePath());
        }
        if (cfg.saveToFile) {
            file = new File(cfg.path);
            if (!file.exists())
                if (file.mkdir()) {
                    logger.info("save-dir created");
                    logger.info(file.getAbsolutePath());
                } else {
                    logger.warn("failed to create save-dir, things may not work as expected");
                }
            else {
                logger.info("The save-dir is set to the following path");
                logger.info(file.getAbsolutePath());
            }
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                for (String db : cfg.databases) {
                    Properties props = new Properties();
                    props.setProperty(MysqlExportService.DB_NAME, db);
                    props.setProperty(MysqlExportService.DB_USERNAME, cfg.dbUsername);
                    props.setProperty(MysqlExportService.DB_PASSWORD, cfg.dbPass);
                    try {
                        props.setProperty(MysqlExportService.JDBC_CONNECTION_STRING, "jdbc:mysql://" + cfg.dbHost + ":" + cfg.dbPort + "/" + URLEncoder.encode(db, StandardCharsets.UTF_8.toString()) + "?useLegacyDatetimeCode=false&serverTimezone=" + URLEncoder.encode(cfg.dbTimezone, StandardCharsets.UTF_8.toString()));
                    } catch (UnsupportedEncodingException e) {
                        logger.error("Failed to encode jdbc url", e);
                    }
                    logger.debug("JDBC-Connection-string set to");
                    logger.debug(props.getProperty(MysqlExportService.JDBC_CONNECTION_STRING));
                    props.setProperty(MysqlExportService.ADD_IF_NOT_EXISTS, String.valueOf(cfg.addIfNotExists));

                    if (cfg.enableEmail) {
                        props.setProperty(MysqlExportService.EMAIL_HOST, cfg.mailServer);
                        props.setProperty(MysqlExportService.EMAIL_PORT, cfg.mailPort);
                        props.setProperty(MysqlExportService.EMAIL_USERNAME, cfg.mailUser);
                        props.setProperty(MysqlExportService.EMAIL_PASSWORD, cfg.mailPass);
                        props.setProperty(MysqlExportService.EMAIL_FROM, cfg.sendAs);
                        props.setProperty(MysqlExportService.EMAIL_TO, cfg.sendTo);
                        props.setProperty(MysqlExportService.EMAIL_SUBJECT, cfg.mailSubject.replaceAll("\\{db}", db));
                        props.setProperty(MysqlExportService.EMAIL_MESSAGE, cfg.mailMessage.replaceAll("\\{db}", db));
                    }
                    props.setProperty(MysqlExportService.TEMP_DIR, new File(cfg.tempPath).getPath());
                    if (cfg.saveToFile || cfg.uploadToCloud) {
                        props.setProperty(MysqlExportService.PRESERVE_GENERATED_ZIP, "true");
                    }
                    MysqlExportService exporter = new MysqlExportService(props);
                    try {
                        exporter.export();
                    } catch (IOException | ClassNotFoundException | SQLException e) {
                        logger.error("Failed to export the database called \"" + db + "\"", e);
                    }
                    if (cfg.saveToFile) {
                        File file = exporter.getGeneratedZipFile();
                        try {
                            FileUtils.moveFile(file, new File(cfg.path + "/" + file.getName()));
                        } catch (IOException e) {
                            logger.error("Failed to move file to storage directory! Is the path \"" + cfg.path + "\" correct?", e);
                        }
                        exporter.clearTempFiles();
                    }
                    if (cfg.uploadToCloud) {
                        try {
                            uploadFile(exporter.getGeneratedZipFile(), new URL(cfg.baseUrl + exporter.getGeneratedZipFile().getName()));
                            exporter.clearTempFiles();
                        } catch (MalformedURLException e) {
                            logger.error("Failed to create a valid url from the baseurl and filename!", e);
                        }
                    }
                }
            }
        };

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, cfg.cycleDelay, cfg.cycleDuration);
    }

    public static void uploadFile (File file, URL url) {
        URLConnection urlconnection;
        try {
            urlconnection = url.openConnection();
            urlconnection.setDoOutput(true);
            urlconnection.setDoInput(true);

            if (urlconnection instanceof HttpURLConnection) {
                ((HttpURLConnection) urlconnection).setRequestMethod("PUT");
                urlconnection.setRequestProperty("Content-type", "text/plain");
                urlconnection.connect();
            }

            BufferedOutputStream outputStream = new BufferedOutputStream(urlconnection.getOutputStream());
            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
            int i;
            byte[] buffer = new byte[4096];
            while ((i = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, i);
            }
            inputStream.close();
            outputStream.close();

        } catch (Exception e) {
            logger.error("Failed to upload file!", e);
        }
    }
}