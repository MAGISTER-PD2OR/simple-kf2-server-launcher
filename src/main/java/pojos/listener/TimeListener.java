package pojos.listener;

import constants.Constants;
import entities.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pojos.kf2factory.Kf2Common;
import pojos.kf2factory.Kf2Factory;
import pojos.session.Session;
import services.DatabaseService;
import services.DatabaseServiceImpl;
import services.PropertyService;
import services.PropertyServiceImpl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class TimeListener extends TimerTask {

    private static final Logger logger = LogManager.getLogger(TimeListener.class);
    private final DatabaseService databaseService;

    public TimeListener() {
        super();
        databaseService = new DatabaseServiceImpl();
    }

    @Override
    public void run() {
        if (!Session.getInstance().isRunningProcess()) {
            logger.info("Starting the process of checking downloaded custom maps.");
            try {
                List<Map> notDownloadedMapList = databaseService.listNotDownloadedMaps();
                if (notDownloadedMapList != null && !notDownloadedMapList.isEmpty()) {
                    PropertyService propertyService = new PropertyServiceImpl();
                    String installationFolder = propertyService.getPropertyValue("properties/config.properties", Constants.CONFIG_INSTALLATION_FOLDER);
                    List<String> mapNameListToAdd = new ArrayList<String>();
                    for (Map map : notDownloadedMapList) {
                        try {
                            List<Path> kfmFilesPath = Files.walk(Paths.get(installationFolder + "/KFGame/Cache/" + map.getIdWorkShop()))
                                    .filter(Files::isRegularFile)
                                    .filter(f -> f.getFileName().toString().startsWith("KF-"))
                                    .filter(f -> f.getFileName().toString().endsWith(".kfm"))
                                    .collect(Collectors.toList());

                            if (kfmFilesPath != null && !kfmFilesPath.isEmpty()) {
                                String filenameWithExtension = kfmFilesPath.get(0).getFileName().toString();
                                String[] array = filenameWithExtension.split(".kfm");
                                String filenameWithoutExtension = array[0];
                                map.setCode(filenameWithoutExtension);
                                map.setDownloaded(true);
                                databaseService.updateMap(map);
                                mapNameListToAdd.add(map.getCode());
                            }
                        } catch (Exception ex) {
                            String message = "The custom map: " + map.getCode() + " with idWorkShop: " + map.getIdWorkShop() + " could not be checked as 'Downloaded Map'.\nSee stacktrace for more details:";
                            logger.error(message, ex);
                        }
                    }
                    if (!mapNameListToAdd.isEmpty()) {
                        Kf2Common kf2Common = Kf2Factory.getInstance();
                        kf2Common.addCustomMapsToKfGameIni(mapNameListToAdd,
                                installationFolder,
                                databaseService.listDownloadedMaps());
                    }
                }
                logger.info("The process of checking downloaded custom maps has finished.");
            } catch (Exception e) {
                String message = "Error checking if the maps have been downloaded. See stacktrace for more details:";
                logger.error(message, e);
            }
        }
    }
}
