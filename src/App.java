import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

class CHUNK{
    int s;
    String fname;
    int e;
}

class KeyGenerator {
    public static int []ARMSTRONG_DIGITS ={1,5,3,3,7,0,3,7,1,4,0,7};
    public static int KEY_LENGTH = ARMSTRONG_DIGITS.length;
    Vector<Integer> numerickey =new Vector<>();
    Set<Integer>st=new HashSet<>();
    KeyGenerator( String user_remark) {
        int sum = 0;
        for (int i=0;i<user_remark.length();i++) {
            int temp = user_remark.charAt(i);


                if(!st.contains(temp) && numerickey.size()<KEY_LENGTH) {
                    numerickey.add(temp);

                }

            st.add(temp);
            sum += temp;            //System.out.println(numerickey.get(i));


        }


        for (int i = 0; i < KEY_LENGTH; i++) {
             numerickey.set( i,(ARMSTRONG_DIGITS[i]+numerickey.get(i))%256 );
        }
        numerickey.add(sum);
    }

    Vector<Integer> get_key(){
            return numerickey;
    }

}


abstract class Cryptography {
    public int color_index;
    public int numericKey_index;
    public Vector<Integer>numericKey=new Vector<Integer>();
    public int []color;
    public int size;

    Cryptography(String user_remark) {
        this.numericKey = new KeyGenerator(user_remark).get_key();
        this.color_index = 0;
        this.numericKey_index = 0;
        this.color = this.makeColor(user_remark);
        this.size = color.length;
    }
    int[] makeColor(String user_remark){
        int[] temp_color =new int[user_remark.length()];
        for (int i=0;i<user_remark.length();i++) {
            temp_color[i]=user_remark.charAt(i) % 256;
        }
        return temp_color;
    }


    //    @abc.abstractmethod
    abstract int process(int data);
}


class Encryptor extends Cryptography {

    Encryptor(String user_remark) {
        super(user_remark);
    }

    int process(int data) {

        data = data ^ numericKey.get(numericKey_index);
        numericKey_index = (numericKey_index + 1) % (KeyGenerator.KEY_LENGTH);

        int encoded = (color[color_index] + data) % 256;
        color_index = (color_index + 1) % this.size;

        return encoded;
    }
}


class Decryptor extends Cryptography {
    Decryptor(String user_remark) {
        super( user_remark);
    }

    int process(int encoded) {

        //level 2
        int temp = (encoded - color[color_index] + 256) % 256;
        color_index = (color_index + 1) % size;

        //level1
        int data = temp ^ numericKey.get(numericKey_index);
        numericKey_index = (numericKey_index + 1) % KeyGenerator.KEY_LENGTH;
        return data;
    }
}


class ChunkProcessor extends Thread {

    public String src_file_name;
    public String trgt_file_name;
    //private Runnable process;
    int start_pos;
    int end_pos;
    Cryptography objCrypto;



    ChunkProcessor(String src_file_name, String trgt_file_name, int start_pos, int end_pos, Cryptography objCrypto) {
        //input data
        this.src_file_name = src_file_name;
        this.trgt_file_name = trgt_file_name;
        this.start_pos = start_pos;
        this.end_pos = end_pos;
        this.objCrypto = objCrypto;

        //a thread as a member of the class

        // ChunkProcessor thrd = new ChunkProcessor();
        //activate the thread
        this.start();
    }

    //my comment(this method run during each thread)
    public void run() {
        try {


            //open the target file for writing

            RandomAccessFile src_handle = new RandomAccessFile(this.src_file_name,"r");
            FileOutputStream trgt_handle = new FileOutputStream(this.trgt_file_name); // #is created/overwritten


            System.out.println("file opened successfully");


            //ensure that chunk is read within the limits
            //src_handle.seek(0);
            src_handle.seek(this.start_pos);
            int x = this.start_pos;
            while (x < this.end_pos) {
                int buff = src_handle.read();
                buff = this.objCrypto.process(buff);//encoded data
                trgt_handle.write(buff);
                x += 1;
            }
            //close
            trgt_handle.close();
            src_handle.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class FileProcessor{

    String src_file_name;
    String trgt_file_name;
    char action;
    String user_key;

    FileProcessor(String src_file_name, String trgt_file_name,char action, String user_key) {
//        if not os.path.exists(src_file_name):  #checks whether the file exists or not
//        raise Exception (src_file_name + ' doesnt exist!')
        this.src_file_name  = src_file_name;
        this.trgt_file_name = trgt_file_name;
        this.action = action;
        this.user_key = user_key;
    }
    void process() throws IOException, InterruptedException {

        int n = 4;  //       # number of chunks
        CHUNK[] chunks;
        chunks = this.divide_into_chunks(n);
        Vector<ChunkProcessor> cps = new Vector<>();
        System.out.println("chunking done!!");


        for (CHUNK ch : chunks){
            if (this.action == 'E') {
                Encryptor objE = new Encryptor(this.user_key);

                cps.add(new ChunkProcessor(this.src_file_name, ch.fname, ch.s, ch.e, objE));


            }
            else if( this.action == 'D'){
                Decryptor objD = new Decryptor(this.user_key);
                cps.add(new ChunkProcessor(this.src_file_name, ch.fname, ch.s, ch.e, objD));
            }
        }

        //  suspend this thread until chunk processors are done
        for( ChunkProcessor cp: cps)
            cp.join();

        // merge into the trgt_file_name
        OutputStream trgt_handle = new BufferedOutputStream(new FileOutputStream(this.trgt_file_name));
        for (CHUNK ch: chunks){
            InputStream ch_handle = new BufferedInputStream(new FileInputStream(ch.fname));
            byte[] buff = new byte[1];
            while (ch_handle.read(buff) != -1) {

                //BufferedOutputStream buff = new BufferedOutputStream(ch_handle.read(i));

                trgt_handle.write(buff);
            }
            ch_handle.close();
        }

        trgt_handle.close();

        for (CHUNK ch : chunks)
            Files.deleteIfExists(Paths.get(ch.fname));
    }

    CHUNK[] divide_into_chunks(int n)  {

        // chunk boundaries
        File f = new File(src_file_name);
        int src_file_size = (int) f.length();     // returns size of file in bytes, raises FileNotFoundError if file doesn't exist.
        int size_of_chunk = src_file_size/n;       //n
        int end = 0;

        //generate the names
        String[] tup =new String[2];
        int dot = src_file_name.lastIndexOf('.');
        tup[0] = (dot == -1) ? src_file_name : src_file_name.substring(0, dot);
        tup[1] = (dot == -1) ? "" : src_file_name.substring(dot+1);

        System.out.println(tup[0]);
        System.out.println(tup[1]);
        //n-1 chunks
        CHUNK []chunks=new CHUNK[n];
        int i =0;
        int start;

        for(;i<n-1;i++) {
            chunks[i]=new CHUNK();
            start = end;    //  #0, 31, 62
            end = start + size_of_chunk;  //31, 62, 93
            chunks[i].fname=tup[0] + i +"."+tup[1];
            chunks[i].s=start;
            chunks[i].e= end;
            System.out.println(chunks[i].fname+" "+start+" "+end);
        }

        //nth chunk
        chunks[i]=new CHUNK();
        chunks[i].fname= (tup[0] + (i)+"." + tup[1]);
        chunks[i].s= end;
        chunks[i].e=src_file_size;
        System.out.println(chunks[i].fname+" "+end+" "+src_file_size);
        return chunks;

    }

}



public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        String sfname ="D:/images/curry.jpg";
        String efname = "D:/images/encrycurry.jpg";
        String tfname = "D:/images/decryCurry.jpg";
        String user_key = "Where is the smallest island?";

        FileProcessor fp1 = new FileProcessor(sfname, efname, 'E', user_key);
        fp1.process();

        System.out.println("-------------------------");

        //hyuser_key='this is not me here playunhg'
        FileProcessor fp2 = new FileProcessor(efname, tfname, 'D', user_key);
        fp2.process();
    }
}

