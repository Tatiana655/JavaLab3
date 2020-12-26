import ru.spbstu.pipeline.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static ru.spbstu.pipeline.TYPE.*;


public class IavorukExecutor implements ru.spbstu.pipeline.IExecutor {

    private IProducer produser;
    private IConsumer consumer;
    private Map<String, String> param = new HashMap<>();
    private final IavorukBaseGrammar baseGrammar = new IavorukBaseGrammar(new String[]{Param.COUNTB.getStringParam()});//куда и сколько двигать какие-то свои параметры
    private final Logger log;
    private static final int BYTE_SIZE = 8;
    private static final int ONES = 255;
    private byte[] dataBuf;
    private IMediator medByte;
    IMediator med;
    public IavorukExecutor(Logger logger)
    {
        log = logger;
    }

    class MediatorByte implements IMediator
    {
            public byte[] getData()
            {
                if (dataBuf == null) return null;
                return dataBuf.clone();
            }
    }

    class MediatorShort implements IMediator
    {
        public short[] getData() {
            if (dataBuf != null && dataBuf.length % 2 == 0) {
                short[] res = new short[dataBuf.length / 2];
                ByteBuffer byteBuffer = ByteBuffer.wrap(dataBuf);

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
            if (dataBuf == null) return null;
            char[] res = new char[dataBuf.length/2 + dataBuf.length%2];
            for (int i =0; i< dataBuf.length/2;i++)
            {
                res[i]=(char)(((dataBuf[2*i]&255)<<8)+ (dataBuf[2*i+1]&255));
            }
            if (dataBuf.length % 2== 1)
                res[dataBuf.length / 2] = (char)((dataBuf[dataBuf.length-1]&255)<<8);
            return res;
            /*
            byte[] buff = dataBuf.clone();
            String readable = Arrays.toString(buff);
            return readable.toCharArray();*/
        }
    }
    public RC setConfig(String var1) {
        param = IavorukParser.GetParam(var1,baseGrammar,log);
        return IavorukParser.checkParam(param,baseGrammar,log);
    }
    public RC setConsumer(IConsumer var1)
    {
        if (var1 == null){
            //log.info("ERROR: no consumer");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        consumer = var1;
        return RC.CODE_SUCCESS;
    }
    public RC setProducer(IProducer var1) //бесполезно? да. А зачем? "На будущее"(с).
    {
        if (var1 == null) {
            //log.info("ERROR: no produser");
            return RC.CODE_FAILED_PIPELINE_CONSTRUCTION;
        }
        produser = var1;
        TYPE[] types = produser.getOutputTypes();
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
        med = produser.getMediator(BYTE);
        return RC.CODE_SUCCESS;
    }
    public byte shift(byte oneByte) {
        int count =Integer.parseInt(param.get(Param.COUNTB.getStringParam()));/*Integer.parseInt(param.get("COUNTB")) % 8*/
        if (count == 0) return oneByte;
        //right shift
        if (count > 0) {
            int x = oneByte & 0xFF;
            int y = count % BYTE_SIZE;
            return (byte) ((x >> y) | (x << (BYTE_SIZE - y)));
        }
        //left shift
        else {
            int x = oneByte & 0xFF;
            int y = -1 * count % BYTE_SIZE;
            return (byte) ((x << y) | (x >> (BYTE_SIZE - y)));
        }
    }
    public byte[] shiftAllBytes(byte[] oldBytes) {
        if (oldBytes == null) return null;
        int len = oldBytes.length;
        byte[] newByte = new byte[len];
        for (int i = 0; i < len; i++) {
            newByte[i] = shift(oldBytes[i]);
            //System.out.println("aft" + (newByte[i] & 0xFF));
        }
        return newByte;
    }

    public RC execute()
    {
        //if (var1==null) return RC.CODE_INVALID_ARGUMENT;
        //System.out.println("ee"+ Arrays.toString(var1));

        byte[] data = (byte[]) med.getData();
        if (data == null) {dataBuf = null; return consumer.execute();}
        dataBuf = shiftAllBytes(data.clone());
        return consumer.execute();

    }

    public TYPE[] getOutputTypes() {
        return new TYPE[]{BYTE, SHORT, CHAR};
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
