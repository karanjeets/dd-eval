package gov.nasa.jpl.util;

import java.io.File;

/**
 * Created by karanjeetsingh on 10/11/16.
 */
public class CommonUtil {

    public static void makeSafeDir(String dirPath) throws Exception {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

}
