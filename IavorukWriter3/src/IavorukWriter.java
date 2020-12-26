import ru.spbstu.pipeline.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static ru.spbstu.pipeline.TYPE.BYTE;

public class IavorukWriter implements ru.spbstu.pipeline.IWriter {

    private IProducer producer;
    private Map<String, String> param = new HashMap<>();
    private final IavorukBaseGrammar baseGrammar = new IavorukBaseGrammar(new String[]{Param.BUFFSIZE.getStringParam()});
    private final Logger log;
    private FileOutputStream fileOutputStream;
    private byte[] buffer = null; //(?)
    private int pos = 0;
    private IMediator med;

    public IavorukWriter(Logger logger)
    {
        log = logger;
    }

    public RC setOutputStream(FileOutputStream var1)
    {
        if (var1==null) return RC.CODE_INVALID_OUTPUT_STREAM;
        this.fileOutputStream = var1;
        return RC.CODE_SUCCESS;
    }

    public RC setConfig(String var1) {
        param = IavorukParser.GetParam(var1,baseGrammar,log);
        RC rc = IavorukParser.checkParam(param,baseGrammar,log);
        if (rc!=RC.CODE_SUCCESS) return rc;
        if (Integer.parseInt(param.get(Param.BUFFSIZE.getStringParam())) <=0 ) return RC.CODE_CONFIG_SEMANTIC_ERROR;
        buffer = new byte[Integer.parseInt(param.get(Param.BUFFSIZE.getStringParam()))];
        return RC.CODE_SUCCESS;
    }
    public RC setConsumer(IConsumer var1)
    {
        return RC.CODE_SUCCESS;
    }
    public RC setProducer(IProducer var1)//бесполезно? да. А зачем? "На будущее"(с).
    {
        if (var1 == null) return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        producer = (IExecutor) var1;
        TYPE[] types = producer.getOutputTypes();
        int flag=0;
        for (TYPE t : types)
        {
            if (t == BYTE) {
                flag = 1;
                break;
            }
        }
        if (flag == 0) {
            //log.info("Wrong types");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        med = producer.getMediator(BYTE);
        return RC.CODE_SUCCESS;
    }
    public RC execute()
    {
        byte[] var1 = (byte[]) med.getData();
        if (var1==null) {
            try {
                if (buffer[0]!=(byte)0)
                    fileOutputStream.write(buffer,0,buffer.length);
            } catch (IOException e) {
                return RC.CODE_FAILED_TO_WRITE;
            }
            return RC.CODE_SUCCESS;
        }
        //if (var1 == null) return RC.CODE_INVALID_ARGUMENT;
        //сохраняй в буффер, печатай если буффер полный, очищай и заполняй снова.

        int buffSize = Integer.parseInt(param.get(Param.BUFFSIZE.getStringParam()));
        if (var1.length < buffSize-pos)//сохрани в буфер, если длина меньше.
        {
            //соедини два массива
            byte[] both = Arrays.copyOf(buffer, pos + var1.length);
            System.arraycopy(var1, 0, both, pos, var1.length);
            buffer = both;
            pos+=var1.length;
        }
        else
        {
            //если pos !=0 закинь сколько не хватает в буффер и напечатай буффер, обнули pos
            //посмотри сколько блоков размера буффера осталось и напечатай, если есть хвост, сохрани в буффер и поменяй позицию
            int delt = 0;// сколько байтов не хватает для заполнения буфера
            if (pos != 0)
            {
                delt = buffSize - pos;
                byte[] both = Arrays.copyOf(buffer, buffSize);
                System.arraycopy(var1, 0, both, pos,delt);
                buffer = both;
                try {
                    fileOutputStream.write(buffer,0,buffer.length);
                } catch (IOException e) {
                    log.info(RC.CODE_FAILED_TO_WRITE.name());
                    return RC.CODE_FAILED_TO_WRITE;
                }
                pos = 0;
            }

            int blockCount = (var1.length - delt)/ buffSize;
            for (int i=0;i < blockCount;i++ )
            {
                try {
                    fileOutputStream.write(var1,delt + buffSize*i, buffSize);
                    //System.out.println(Arrays.toString(var1));
                } catch (IOException e) {
                    log.info(RC.CODE_FAILED_TO_WRITE.name());
                    return RC.CODE_FAILED_TO_WRITE;
                }
            }

            int tail = (var1.length - delt) % buffSize;
            if (tail !=0)
            {
                pos = tail;
                buffer = Arrays.copyOfRange(var1, buffSize*blockCount,buffSize*blockCount+tail);
            }
            else
            {
                Arrays.fill(buffer, (byte) 0);
            }
        }
        return RC.CODE_SUCCESS;
    }
}
