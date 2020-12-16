import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Manager implements IConfigurable {
    private static final String delimiter = "-";// разделитель для экзекуторов

    private List<IExecutor> executors = new ArrayList<>();
    private IWriter writer;
    private IReader reader;
    private Map<String, String> param = new HashMap<>();
    private IavorukBaseGrammar baseGrammar = new IavorukBaseGrammar(new String[]{ Param.EXECUTORNAME.getStringParam(),Param.READERNAME.getStringParam(),
            Param.WRITERNAME.getStringParam(),Param.CFGREADER.getStringParam(), Param.CFGWRITER.getStringParam(), Param.CFGEXECUTOR.getStringParam(),
            Param.INFILE.getStringParam(),Param.OUTFILE.getStringParam()});
    private Logger log;
    //на  вхот manager_cfg.
    //внутри:
    // имена классов IW,IE,IR. создаём экземпляр каждого с помощью рефлексии.
    // cfg.txt каждого IW,IE,IR

    //Инит экзкмпляры
    //пинаем ридера

    //парсер конфига
    public RC setConfig(String var1)//распарсить кофиг
    {
        //парсер принимает на вход baseGrammar
        if (var1 == null) return  RC.CODE_INVALID_ARGUMENT;
        param = IavorukParser.GetParam(var1,baseGrammar,log);
        return IavorukParser.checkParam(param,baseGrammar,log);
    }
    //создание экземпляров классов + init + запуск + close
    public RC begin()
    {
        if (param.isEmpty()) {log.info(RC.CODE_CONFIG_SEMANTIC_ERROR.name()); return RC.CODE_CONFIG_SEMANTIC_ERROR;}
        //reflection
        try {
            Class cls = Class.forName(param.get(Param.READERNAME.getStringParam()));

            reader = (IReader)cls.getConstructor(Logger.class).newInstance(log);

            Class cls1 = Class.forName(param.get(Param.WRITERNAME.getStringParam()));

            writer = (IWriter)cls1.getConstructor(Logger.class).newInstance(log);

            String[] tmpNameExecutors = param.get(Param.EXECUTORNAME.getStringParam()).split(delimiter);
            for (String i : tmpNameExecutors)
            {
                cls = Class.forName(i);
                IExecutor executor = (IExecutor)cls.getConstructor(Logger.class).newInstance(log);
                executors.add(executor);
            }
            }  catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            log.info(RC.CODE_CONFIG_SEMANTIC_ERROR.name());
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
            }

        //relation
        RC rc = reader.setConsumer(executors.get(0));
        if (rc != RC.CODE_SUCCESS) {log.info(rc.name()); return rc; };
        rc = executors.get(0).setProducer(reader);
        if (rc != RC.CODE_SUCCESS) {log.info(rc.name()); return rc; };

        for (int i =0; i< executors.size()-1;i++)
        {
            rc = executors.get(i).setConsumer(executors.get(i+1));
            if (rc != RC.CODE_SUCCESS) {log.info(rc.name()); return rc; };
            rc = executors.get(i+1).setProducer(executors.get(i));
            if (rc != RC.CODE_SUCCESS) {log.info(rc.name()); return rc; };
        }

        rc = writer.setProducer(executors.get(executors.size() - 1));
        if (rc != RC.CODE_SUCCESS) {log.info(rc.name()); return rc; };
        rc = executors.get(executors.size() - 1).setConsumer(writer);
        if (rc != RC.CODE_SUCCESS) {log.info(rc.name()); return rc; };
        //setcfg
        rc = writer.setConfig(param.get(Param.CFGWRITER.getStringParam()));
        if (rc != RC.CODE_SUCCESS) {log.info(rc.name()); return rc; };
        rc = reader.setConfig(param.get(Param.CFGREADER.getStringParam()));
        if (rc != RC.CODE_SUCCESS) {log.info(rc.name()); return rc; };
        String[] tmpCfg = param.get(Param.CFGEXECUTOR.getStringParam()).split(delimiter);
        if (tmpCfg.length != executors.size()) return RC.CODE_CONFIG_SEMANTIC_ERROR;
        int k=0;
        for(IExecutor i : executors)
        {
            rc = i.setConfig(tmpCfg[k]);
            if (rc != RC.CODE_SUCCESS) {log.info(rc.name()); return rc; };
            k++;
        }
        //setinputstream
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(param.get(Param.INFILE.getStringParam()));
        } catch (IOException ex) {
            log.info(RC.CODE_CONFIG_SEMANTIC_ERROR.name());
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        } catch (Exception ex1) {
            log.info(RC.CODE_INVALID_INPUT_STREAM.name());
            return RC.CODE_INVALID_INPUT_STREAM;
        }
        rc = reader.setInputStream(fis);//nnen
        if (rc != RC.CODE_SUCCESS)log.info(rc.name());
        //setoutputstream
        FileOutputStream fos = null;
        //open outfile
        try {
            fos = new FileOutputStream(param.get(Param.OUTFILE.getStringParam()));
        } catch (IOException ex) {
            log.info(RC.CODE_CONFIG_SEMANTIC_ERROR.name());
            return RC.CODE_CONFIG_SEMANTIC_ERROR;
        } catch (Exception ex1) {
            log.info(RC.CODE_INVALID_OUTPUT_STREAM.name());
            return RC.CODE_INVALID_OUTPUT_STREAM;
        }
        rc = writer.setOutputStream(fos);
        if (rc != RC.CODE_SUCCESS)log.info(rc.name());
        rc = reader.execute();
        if (rc != RC.CODE_SUCCESS) log.info(rc.name());
        //close outstream
        try {
            fos.close();
        }  catch (IOException e) {
            log.info(RC.CODE_INVALID_ARGUMENT.name());
            return RC.CODE_INVALID_ARGUMENT;
        } catch (Exception e1) {
            log.info(RC.CODE_INVALID_OUTPUT_STREAM.name());
            return RC.CODE_INVALID_OUTPUT_STREAM;
        }
        //close instream
        try {
            fis.close();
        } catch (IOException e) {
            log.info(RC.CODE_INVALID_ARGUMENT.name());
            return RC.CODE_INVALID_ARGUMENT;
        } catch (Exception e1) {
            log.info(RC.CODE_INVALID_INPUT_STREAM.name());
            return RC.CODE_INVALID_INPUT_STREAM;
        }
        return RC.CODE_SUCCESS;
    }
    public Manager(Logger log)//файл лога мб указать?
    {
       this.log = log;
    }

}
