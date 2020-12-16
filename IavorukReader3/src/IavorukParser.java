import ru.spbstu.pipeline.RC;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static ru.spbstu.pipeline.RC.CODE_CONFIG_GRAMMAR_ERROR;
import static ru.spbstu.pipeline.RC.CODE_SUCCESS;


enum Param{
    COUNTB("COUNTB"),
    EXECUTORNAME("EXECUTORNAME"), READERNAME("READERNAME"),WRITERNAME("WRITERNAME"),CFGREADER("CFGREADER"), CFGWRITER("CFGWRITER"),CFGEXECUTOR("CFGEXECUTOR"), INFILE("INFILE"),OUTFILE("OUTFILE"),
    BUFFSIZE("BUFFSIZE");
    private final String param;

    Param(String param) {
        this.param = param;
    }

    public String getStringParam(){return param;}

}

public class IavorukParser {

    public static RC checkParam(Map<String,String> param, IavorukBaseGrammar baseGrammar, Logger logger)
    {
        if (param == null)
        {
            logger.info("ERROR: No Param");
            return CODE_CONFIG_GRAMMAR_ERROR;
        }
        int i = 0 ;
        while (i < baseGrammar.numberTokens()) {
            if (!param.containsKey(baseGrammar.token(i)))
            {
                logger.info("ERROR: not enough arguments");
                return CODE_CONFIG_GRAMMAR_ERROR;
            }
            i++;
        }

        return CODE_SUCCESS;
    }

    public static Map<String,String> GetParam(String filename, IavorukBaseGrammar baseGrammar, Logger log)
    {
        if (filename == null) return null;
        Map<String, String> param = new HashMap<>();

        try (FileReader fr = (new FileReader(filename))) {
            BufferedReader reader = new BufferedReader(fr);
            String buff;

            while (reader.ready()) {
                //save in (Map)
                buff = reader.readLine();
                String[] subStr;
                subStr = buff.split(baseGrammar.delimiter());//const

                //check
                if (subStr.length == 2) {
                    param.put(subStr[0], subStr[1]);
                }

            }
            //check(param,baseGrammar)
        } catch (Exception ex1) {
            log.info("ERROR: manager filename exception");
            return  null;
        }
        return param;
    }
}
