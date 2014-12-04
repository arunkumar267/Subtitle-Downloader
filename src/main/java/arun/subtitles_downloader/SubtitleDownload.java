package arun.subtitles_downloader;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
/**
 * Hello world!
 *
 */
public class SubtitleDownload 
{
	private static String OPEN_SUBTITLES_SERVER="http://api.opensubtitles.org/xml-rpc";
	private static String MOVIE_EXTENSIONS="mp4,mkv,avi,mov";
	XmlRpcClientConfigImpl xmlRpcClientConfig;
	XmlRpcClient xmlRpcClient;
	String strToken="";
	String fileHash="";
	File movie;
	FilenameFilter fileNameFilter;
	ArrayList movieFileExtensions;
	
	
	public SubtitleDownload(){
		xmlRpcClientConfig=new XmlRpcClientConfigImpl();
		xmlRpcClient=new XmlRpcClient();
		movieFileExtensions=new ArrayList();
		String movieExtensionArray[]=MOVIE_EXTENSIONS.split(",");
		for(String extn : movieExtensionArray ){
			movieFileExtensions.add(extn);
		}
		
		
		try {
			xmlRpcClientConfig.setServerURL(new URL(OPEN_SUBTITLES_SERVER));
			xmlRpcClient.setConfig(xmlRpcClientConfig);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public String login(){
		List params=new ArrayList();
		HashMap<?,?> retVal;
		params.add("");
		params.add("");
		params.add("eng");
		params.add("moviejukebox 1.0.15");
		try {
			 retVal=(HashMap)xmlRpcClient.execute("LogIn", params);
			 strToken = (String) retVal.get("token");
			 
		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return strToken;
		
	}
	
	public void logOut(){
		List params=new ArrayList();
		params.add(strToken);
		try {
			xmlRpcClient.execute("LogOut",params);
		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void computeHash(String filePath){
		try {
			movie=new File(filePath);
			fileHash=OpenSubtitlesHasher.computeHash(movie);
			System.out.println(fileHash);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void Search(String filePath){
		computeHash(filePath);
		Map<String,Object> parameterMap = new HashMap();
		System.out.println(fileHash);
		System.out.println(movie.length());
		HashMap<?,?> retVal;
		 parameterMap.put("sublanguageid", Locale.getDefault().getISO3Language());
         //parameterMap.put("moviehash", fileHash);
         //parameterMap.put("moviebytesize", new Double(movie.length()));
		 String fileName=movie.getName();
		 fileName=fileName.substring(0, fileName.length()-4);
		 System.out.println(fileName);
		 parameterMap.put("query", fileName);
         Object[] paramsArray = new Object[]{strToken, new Object[]{parameterMap}};
         try {
			retVal=(HashMap<?, ?>) xmlRpcClient.execute("SearchSubtitles",paramsArray);
			System.out.println(retVal.keySet());
			if(retVal.get("data") instanceof Object[]){
			Object[] data=(Object [])retVal.get("data");
		  HashMap<?,?> firstLink=(HashMap<?, ?>) data[0];
		  try {
			URL url=new URL((String)firstLink.get("SubDownloadLink"));
			downloadSubtitle(url);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			}
		} catch (XmlRpcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
		
	}
	
	public  String getExtension(HttpURLConnection webConnection) {

        try {

            String strContentDisposition = webConnection.getHeaderField("Content-Disposition");
            System.out.println(strContentDisposition);
            String strExtention = strContentDisposition.replaceAll(".*\\.([a-z]{3})\\..*", "$1");

            return strExtention;

        } catch (Exception e) {
            return null;
        }

    }
	
    private void downloadSubtitle(URL url) {
    	// Now that we have the URL, we can download the file. The file is in
        // the GZIP format so we have to uncompress it.
        File filSubtitleFile = new File(movie.getPath().substring(0, movie.getPath().length() - 4));

        HttpURLConnection httpConnection = null;
        FileOutputStream fileOutputStream = null;
        GZIPInputStream gzipInputStream = null;

        try {

           httpConnection = (HttpURLConnection)((url).openConnection());
           fileOutputStream = new FileOutputStream(filSubtitleFile);
           gzipInputStream = new GZIPInputStream(httpConnection.getInputStream());

            

            Integer intLength = 0;
            byte[] bytBuffer = new byte[1024];

            fileOutputStream.close();
            filSubtitleFile.delete();
            if (httpConnection.getHeaderField("Content-Disposition").isEmpty() == false) {
                filSubtitleFile = new File(filSubtitleFile.getPath() + "."+getExtension(httpConnection) );
            }

            fileOutputStream.close();
            fileOutputStream = new FileOutputStream(filSubtitleFile);
            while ((intLength = gzipInputStream.read(bytBuffer)) > 0) {
                fileOutputStream.write(bytBuffer, 0, intLength);
            }
            httpConnection.disconnect();

           

        } catch (Exception e) {
           
        } finally {
            try {
				fileOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            try {
				gzipInputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

		
	}
    
    public void searchAndDownloadDirectory(File dir){
    	if(dir.isDirectory()){
    		File files[]=dir.listFiles();
    		for(File f: files){
    			if(f.isDirectory()){
    				searchAndDownloadDirectory(f);
    			}
    			else{
    				if(isVideoFile(f)){
    				Search(f.getAbsolutePath());
    				}
    			}
    		}
    	}
    }

	private boolean isVideoFile(File f) {
		String fileName=f.getName();
		String extension=fileName.substring(fileName.lastIndexOf('.')+1);
		if(movieFileExtensions.contains(extension)){
			System.out.println("hey!!! its a video file"+f.getName());
			return true;
		}
		return false;
	}

	public static void main( String[] args ) throws IOException
    {
       SubtitleDownload sd=new SubtitleDownload();
       sd.login();
       BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
       System.out.print("Enter the directory path to download: ");
       String filePath=br.readLine();
       sd.searchAndDownloadDirectory(new File(filePath));
       //sd.Search("C:\\Users\\Arunkumar\\Downloads\\The.Newsroom.2012.S03E01.HDTV.x264-KILLERS.mp4");
       sd.logOut();
    }
}

/**
 * Hash code is based on Media Player Classic. In natural language it calculates: size + 64bit
 * checksum of the first and last 64k (even if they overlap because the file is smaller than
 * 128k).
 */
 class OpenSubtitlesHasher {
        
        /**
         * Size of the chunks that will be hashed in bytes (64 KB)
         */
        private static final int HASH_CHUNK_SIZE = 64 * 1024;
        
        
        public static String computeHash(File file) throws IOException {
                long size = file.length();
                long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);
                
                FileChannel fileChannel = new FileInputStream(file).getChannel();
                
                try {
                        long head = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, 0, chunkSizeForFile));
                        long tail = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile));
                        
                        return String.format("%016x", size + head + tail);
                } finally {
                        fileChannel.close();
                }
        }
        

        public static String computeHash(InputStream stream, long length) throws IOException {
                
                int chunkSizeForFile = (int) Math.min(HASH_CHUNK_SIZE, length);
                
                // buffer that will contain the head and the tail chunk, chunks will overlap if length is smaller than two chunks
                byte[] chunkBytes = new byte[(int) Math.min(2 * HASH_CHUNK_SIZE, length)];
                
                DataInputStream in = new DataInputStream(stream);
                
                // first chunk
                in.readFully(chunkBytes, 0, chunkSizeForFile);
                
                long position = chunkSizeForFile;
                long tailChunkPosition = length - chunkSizeForFile;
                
                // seek to position of the tail chunk, or not at all if length is smaller than two chunks
                while (position < tailChunkPosition && (position += in.skip(tailChunkPosition - position)) >= 0);
                
                // second chunk, or the rest of the data if length is smaller than two chunks
                in.readFully(chunkBytes, chunkSizeForFile, chunkBytes.length - chunkSizeForFile);
                
                long head = computeHashForChunk(ByteBuffer.wrap(chunkBytes, 0, chunkSizeForFile));
                long tail = computeHashForChunk(ByteBuffer.wrap(chunkBytes, chunkBytes.length - chunkSizeForFile, chunkSizeForFile));
                
                return String.format("%016x", length + head + tail);
        }
        

        private static long computeHashForChunk(ByteBuffer buffer) {
                
                LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
                long hash = 0;
                
                while (longBuffer.hasRemaining()) {
                        hash += longBuffer.get();
                }
                
                return hash;
        }
        
}
