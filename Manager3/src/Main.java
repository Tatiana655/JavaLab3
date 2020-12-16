import ru.spbstu.pipeline.RC;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Main {
    public static void main(String[] args) {
        Logger logger = Logger.getLogger("MyLog");
        FileHandler fh;
        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler("log.log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (args == null) {logger.info("No cfg manager"); return;}
        if (args[0] == null) {logger.info("No cfg manager"); return;}
        Manager manager = new Manager(logger);
        RC rc = manager.setConfig(args[0]);
        if (rc!=RC.CODE_SUCCESS)
            logger.info(rc.name());
        else {
            rc = manager.begin();
            logger.info(rc.name());
        }
    }
}
