package org.example.youtubescrapping;

import com.microsoft.playwright.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Main {

    public static Set listUsedUrl = new HashSet<>();

    public static HashMap<String, String> queryListChannel(String nameGame, int numberChannel) throws IOException, InterruptedException {
            // channel  channelUrl
        HashMap<String,String> listChannel = new HashMap<>();
        HashSet<String> seenUrl = new HashSet<>();
        ProcessBuilder  builder = new ProcessBuilder();
        builder.command(
                "yt-dlp",
                "ytsearch"+numberChannel *10 +":" + nameGame,
                "--flat-playlist",
                "--lazy-playlist",
                "--print", "%(channel)s|%(channel_url)s",
                "--no-warnings"
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();


        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while((line= reader.readLine()) != null){
//            System.out.println(line);
            if(line.length() < 2){
                continue;
            }
            String[] data = line.split("\\|",2);
            String channelName = data[0].trim();
            String channelUrl = data[1].trim();

            if (!seenUrl.contains(channelUrl)){
                listChannel.put(channelName, channelUrl);
                seenUrl.add(channelUrl);
                System.out.println("ADD: " + channelName + " | " + channelUrl);
            }
            else{
                System.out.println("SKIP: " + channelName + " | " + channelUrl);
            }

            if (listChannel.size() >= numberChannel) {
                process.destroy();
                break;
            }


        }

        int exitCode = process.waitFor();

        System.out.println("Exist with code : " + exitCode);


        return  listChannel;

    }
//    yt-dlp "urlVideo" \
//            --print "%(view_count)s|%(comment_count)s|%(like_count)s"
//

//    yt-dlp "urlChannel"  --print "%(channel)s|%(epoch)s|%(playlist_count)s|%(channel_follower_count)s"
//    yt-dlp "ytsearch10:nameChannel nameGame" --skip-download --print "%(uploader)s|%(title)s|%(webpage_url)"

    public static HashMap<String, String> getListVideosChannel(String nameChannel, String nameGame) throws IOException, InterruptedException {
        HashMap<String, String> listVideoChannel = new HashMap<>();

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(
                "yt-dlp",
                "ytsearch10:" + nameChannel + " " + nameGame,
                "--flat-playlist",
                "--skip-download",
                "--print", "%(uploader)s|%(title)s|%(webpage_url)s",
                "--no-warnings"
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader rd = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line ;
        while((line = rd.readLine()) != null) {
            if (line.length() < 2) {
                continue;
            }
            String[] datas = line.split("\\|");

            if (!datas[0].equals(nameChannel)) {
                break;
            } else {
                listVideoChannel.put(datas[1], datas[2]);
            }
        }

        return listVideoChannel;
    }


    //    yt-dlp "urlVideo" \
//            --print "%(view_count)s|%(comment_count)s|%(like_count)s"
//

    public static Video getDataVideo(String urlVideo) throws IOException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(
                "yt-dlp", urlVideo,
                "--print",
                "%(view_count)s|%(comment_count)s|%(like_count)s",
                "--no-warnings"
        );
        builder.redirectErrorStream(true);

        Process process = builder.start();

        BufferedReader rb = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = rb.readLine();

        String[] datas = line.split("\\|");
        return new Video(datas[0].trim(), datas[1].trim(), datas[2].trim());
//        return new Video();



    }

    //    yt-dlp "urlChannel"  --print "%(channel)s|%(epoch)s|%(playlist_count)s|%(channel_follower_count)s"


    public static Channel getDataChannel(String urlChannel, ArrayList<Video> listVideos) throws IOException, InterruptedException {

        ProcessBuilder builder = new ProcessBuilder();
        builder.command(
                "yt-dlp", urlChannel,
                "--print",
                "%(channel)s|%(epoch)s|%(playlist_count)s|%(channel_follower_count)s",
                "--no-warnings"
        );
        builder.redirectErrorStream(true);

        Process process = builder.start();

        BufferedReader rb = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = rb.readLine();
        process.destroy();
        System.out.println(line);
        String[] datas = line.split("\\|");

        return new Channel(datas[0], datas[1], datas[2], datas[3], listVideos);



    }

    public static HashMap<String, String> sumUpProcess(String nameChannel, String nameGame) throws IOException, InterruptedException {
              //title   urlVideo
        HashMap<String, String> listVideoInChannel = new HashMap();

//        HashMap<String, String> listChannelVideo = getVideoChannel(nameChannel, nameGame);

        //          title     urlVideo
        for(Map.Entry<String, String> nameVideo  : getVideosChannel(nameChannel, nameGame).entrySet()){
            if(!listUsedUrl.contains(nameVideo.getValue())){
                System.out.println("ADD: " + nameChannel + " | " + nameVideo.getKey() + " | " + nameVideo.getValue());
                listVideoInChannel.put(nameVideo.getKey(), nameVideo.getValue());
                listUsedUrl.add(nameVideo.getValue());
            }
            else{
                System.out.println("SKIP: " + nameChannel + " | " + nameVideo + "had already existed");
            }
        }

        System.out.println("Channel " + nameChannel + " has " + listVideoInChannel.size() + " videos");

        return listVideoInChannel;

    }




    public static void main(String[] args) throws IOException, InterruptedException {

//        String nameGame = "Undertale";
//        HashMap<String, String> listChannel =  queryListChannel(nameGame, 20);
//
//        System.out.println("Size : " + listChannel.size());
//
//        for(Map.Entry<String, String> nameVideo : listChannel.entrySet()){
//            getDataChannel(nameVideo.getKey(), nameGame);
//        }

        Video video1 = getDataVideo("https://www.youtube.com/watch?v=hgSdNAGP4VI");
        Video video2 = getDataVideo("youtube.com/watch?v=F4QavPTT77k ");

        ArrayList<Video> listVideos = new ArrayList<>();
        listVideos.add(video1);
        listVideos.add(video2);

        System.out.println(getDataChannel("https://www.youtube.com/channel/UCg7ha0OQC6Jn3MZKO807R9Q", listVideos));
    }

}
