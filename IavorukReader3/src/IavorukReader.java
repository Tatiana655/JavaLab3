import ru.spbstu.pipeline.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static ru.spbstu.pipeline.TYPE.*;

public class IavorukReader implements IReader {
    //поле консумера
    //params
    //log
    //baseGrammar

    private IConsumer consumer;
    private Map<String, String> param = new HashMap<>();
    private final IavorukBaseGrammar baseGrammar = new IavorukBaseGrammar(new String[]{Param.BUFFSIZE.getStringParam()});
    private Logger log = Logger.getLogger("MyLog");
    private FileInputStream fileInputStream;
    private byte[] byteStream;
    private IMediator MedByte;
    private IMediator MedShort;
    private IMediator MedChar;

    class MediatorByte implements IMediator
    {
        public byte[] getData() {
            if (byteStream == null) return null;
            return byteStream.clone();
        }
    }
    class MediatorShort implements IMediator
    {
        public short[] getData() {
            if (IavorukReader.this.byteStream!= null && IavorukReader.this.byteStream.length % 2 == 0) {
                short[] res = new short[IavorukReader.this.byteStream.length / 2];
                ByteBuffer byteBuffer = ByteBuffer.wrap(IavorukReader.this.byteStream);

                for(int i = 0; i < res.length; ++i) {
                    res[i] = byteBuffer.getShort(2 * i);
                }

                return res;
            } else {
                return null;
            }
        }
    }

    class  MediatorChar implements IMediator
    {
        public char[] getData() {
            if (IavorukReader.this.byteStream == null) {
                return null;
            } else {
                char[] res = new char[IavorukReader.this.byteStream.length / 2 + IavorukReader.this.byteStream.length % 2];

                for(int i = 0; i < IavorukReader.this.byteStream.length / 2; ++i) {
                    res[i] = (char)(((IavorukReader.this.byteStream[2 * i] & 255) << 8) + (IavorukReader.this.byteStream[2 * i + 1] & 255));
                }

                if (IavorukReader.this.byteStream.length % 2 == 1) {
                    res[IavorukReader.this.byteStream.length / 2] = (char)((IavorukReader.this.byteStream[IavorukReader.this.byteStream.length - 1] & 255) << 8);
                }

                return res;
            }
        }
    }

    public IavorukReader(Logger logger)//файл лога мб
    {
        log = logger;
    }

    public RC setConfig(String var1)
    {
        param = IavorukParser.GetParam(var1,baseGrammar,log);
        RC rc = IavorukParser.checkParam(param,baseGrammar,log);
        if (rc!=RC.CODE_SUCCESS) return rc;
        if (Integer.parseInt(param.get(Param.BUFFSIZE.getStringParam())) <=0 ) return RC.CODE_CONFIG_SEMANTIC_ERROR;
        return RC.CODE_SUCCESS;
    }
    public RC setInputStream(FileInputStream var1)
    {
        if (var1 == null) return RC.CODE_INVALID_INPUT_STREAM;
        fileInputStream = var1;
        return RC.CODE_SUCCESS;
    }
    public RC setConsumer(IConsumer var1)
    {
        if (var1 == null) return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        consumer =  var1;
        return RC.CODE_SUCCESS;
    }

    public RC setProducer(IProducer var1)
    {
        return RC.CODE_SUCCESS;
    }

    public RC execute()
    {
        //Считай кусок размером с буффер и перекить воркеру
        int buffSize = Integer.parseInt(param.get(Param.BUFFSIZE.getStringParam()));
        byteStream = new byte[buffSize];
        int k = 0;
        while (true) {
            try {
                k = (fileInputStream != null) ? (fileInputStream.read(byteStream)) : -1;
                if (k == -1) break;
                if (consumer.execute() !=RC.CODE_SUCCESS)//"Продьюсер вызывает consumer.execute(), когда готов передать консьюмеру кусок данных."
                    return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
                Arrays.fill(byteStream, (byte)0);
            } catch (Exception e) {
                //log.info("ERROR: wrong inputstream");
                return RC.CODE_FAILED_TO_READ;
            }
        }
        byteStream = null;
        consumer.execute();

        if (fileInputStream!= null) {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                //log.info("ERROR: cannot close input file, wrong stream");
                return RC.CODE_INVALID_INPUT_STREAM;
            } catch (Exception e1) {
                log.info( RC.CODE_INVALID_INPUT_STREAM.name());
                return RC.CODE_INVALID_INPUT_STREAM;
            }
        }
        return RC.CODE_SUCCESS;
    }


    public TYPE[] getOutputTypes() {
        return  new TYPE[]{BYTE, SHORT, CHAR};
    }

    public IMediator getMediator(TYPE type) {

        switch (type) {
            case BYTE:
                return new MediatorByte();
            case SHORT:
                return new MediatorShort();
            case CHAR:
                return new MediatorChar();

        }
        return null;
    }
}
