package stories.installupdateserver;

import java.sql.SQLException;

public interface InstallUpdateServerFacade {
    boolean saveOrUpdateProperty(String key, String newValue) throws Exception;
    String findPropertyValue(String key) throws Exception;
}
